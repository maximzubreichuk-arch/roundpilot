package com.example.myintervaltimerproject.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.preferences.AppLanguage
import java.util.Locale

private enum class VoiceCue {
    GET_READY,
    WORK,
    REST,
    COOLDOWN,
}

class AndroidAudioController(
    context: Context,
) : AudioController {

    private val appContext = context.applicationContext

    /**
     * SoundPool is used for the short countdown cue. Unlike ToneGenerator, the
     * sample is decoded once and playback is a lightweight audio-stream start,
     * which is much less prone to audible jitter when several cues are one
     * second apart.
     */
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val countdownSoundId: Int =
        soundPool.load(appContext, R.raw.countdown_beep, 1)

    @Volatile
    private var countdownSoundLoaded = false

    private var fallbackToneGenerator: ToneGenerator? = null
    private var textToSpeech: TextToSpeech? = null

    @Volatile
    private var ttsReady = false

    @Volatile
    private var muted = false

    @Volatile
    private var currentLanguage = AppLanguage.ENGLISH

    @Volatile
    private var pendingSpeech: VoiceCue? = null

    @Volatile
    private var releaseRequested = false

    @Volatile
    private var isSpeaking = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == countdownSoundId && status == 0) {
                countdownSoundLoaded = true
            }
        }

        textToSpeech = TextToSpeech(appContext) { status ->
            synchronized(this) {
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = applyLanguage(currentLanguage)
                    textToSpeech?.setSpeechRate(0.9f)
                    textToSpeech?.setPitch(1.0f)

                    val pending = pendingSpeech
                    pendingSpeech = null
                    if (pending != null && !muted && ttsReady) {
                        speakInternal(textFor(pending))
                    }
                } else {
                    ttsReady = false
                    pendingSpeech = null
                    if (releaseRequested) releaseTts()
                }
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    if (releaseRequested) {
                        synchronized(this@AndroidAudioController) {
                            releaseTts()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    if (releaseRequested) {
                        synchronized(this@AndroidAudioController) {
                            releaseTts()
                        }
                    }
                }
            },
        )
    }

    @Synchronized
    override fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (ttsReady) {
            applyLanguage(language)
        }
    }

    private fun applyLanguage(language: AppLanguage): Boolean {
        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.RUSSIAN -> Locale("ru", "RU")
        }
        val result = textToSpeech?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    @Synchronized
    override fun setMuted(muted: Boolean) {
        this.muted = muted
        if (muted) {
            pendingSpeech = null
            textToSpeech?.stop()
        }
    }

    @Synchronized
    override fun playStart() {
        if (!muted) playTone(ToneGenerator.TONE_PROP_ACK, 220)
    }

    @Synchronized
    override fun playCountdown() {
        if (muted) return

        if (countdownSoundLoaded) {
            soundPool.play(
                countdownSoundId,
                1.0f,
                1.0f,
                1,
                0,
                1.0f,
            )
        } else {
            // Only a very early first cue can reach this fallback, before the
            // tiny WAV sample has finished loading.
            playTone(ToneGenerator.TONE_PROP_BEEP, 120)
        }
    }

    @Synchronized
    override fun playPhaseChange() {
        if (!muted) playTone(ToneGenerator.TONE_PROP_BEEP2, 220)
    }

    @Synchronized
    override fun playFinish() {
        if (!muted) playTone(ToneGenerator.TONE_PROP_PROMPT, 500)
    }

    override fun speakGetReady() = speakCue(VoiceCue.GET_READY)
    override fun speakWork() = speakCue(VoiceCue.WORK)
    override fun speakRest() = speakCue(VoiceCue.REST)
    override fun speakCooldown() = speakCue(VoiceCue.COOLDOWN)

    private fun speakCue(cue: VoiceCue) {
        if (muted) return
        if (!ttsReady) {
            pendingSpeech = cue
            return
        }
        speakInternal(textFor(cue))
    }

    private fun textFor(cue: VoiceCue): String = when (cue) {
        VoiceCue.GET_READY -> when (currentLanguage) {
            AppLanguage.ENGLISH -> "Get ready!"
            AppLanguage.RUSSIAN -> "Приготовьтесь!"
        }
        VoiceCue.WORK -> when (currentLanguage) {
            AppLanguage.ENGLISH -> "Work"
            AppLanguage.RUSSIAN -> "Работа"
        }
        VoiceCue.REST -> when (currentLanguage) {
            AppLanguage.ENGLISH -> "Rest"
            AppLanguage.RUSSIAN -> "Отдых"
        }
        VoiceCue.COOLDOWN -> when (currentLanguage) {
            AppLanguage.ENGLISH -> "Cooldown"
            AppLanguage.RUSSIAN -> "Восстановление"
        }
    }

    private fun speakInternal(text: String) {
        if (muted) return
        val tts = textToSpeech ?: return
        isSpeaking = true
        val result = runCatching {
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "timer_${text.hashCode()}_${System.nanoTime()}",
            )
        }.getOrElse { TextToSpeech.ERROR }

        if (result == TextToSpeech.ERROR) {
            isSpeaking = false
            if (releaseRequested) releaseTts()
        }
    }

    @Synchronized
    override fun release() {
        releaseRequested = true
        muted = false
        pendingSpeech = null
        if (!isSpeaking) releaseTts()
    }

    private fun releaseTts() {
        soundPool.release()
        fallbackToneGenerator?.release()
        fallbackToneGenerator = null
        toneGeneratorReleaseSafe()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsReady = false
        isSpeaking = false
        releaseRequested = false
        pendingSpeech = null
    }

    private fun toneGeneratorReleaseSafe() {
        // Kept separate so all legacy/non-countdown tones are released in one place.
        fallbackToneGenerator?.release()
        fallbackToneGenerator = null
    }

    private fun playTone(tone: Int, durationMs: Int) {
        val generator = fallbackToneGenerator ?: ToneGenerator(
            AudioManager.STREAM_MUSIC,
            100,
        ).also { fallbackToneGenerator = it }
        runCatching { generator.startTone(tone, durationMs) }
    }
}
