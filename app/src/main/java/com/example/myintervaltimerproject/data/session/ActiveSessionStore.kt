package com.example.myintervaltimerproject.data.session

import android.content.Context
import android.provider.Settings

import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.timer.TimerPhase

import org.json.JSONArray
import org.json.JSONObject


data class ActiveSession(
    val config: TimerConfig,
    val phase: TimerPhase,
    val currentRound: Int,
    val totalRounds: Int,
    val remainingSeconds: Int,
    val phaseEndsAtElapsedRealtime: Long,
    val phases: List<SessionPhase>,
    val phaseIndex: Int,
    val remainingMillis: Long = remainingSeconds.coerceAtLeast(0) * 1000L,
    val sessionStartedAtElapsedRealtime: Long = 0L,
    val elapsedBeforePauseMillis: Long = 0L,
    val soundEnabled: Boolean = true,
    val bootCount: Int = 0,
) {

    fun next(): ActiveSession? {

        /*
         * Defensive check.
         *
         * A valid session must point to an existing phase.
         */
        if (
            phaseIndex < 0 ||
            phaseIndex >= phases.size
        ) {
            return null
        }

        val index =
            phaseIndex + 1

        if (
            index >= phases.size
        ) {
            return null
        }

        val next =
            phases[index]

        return copy(

            phase =
                next.phase,

            currentRound =
                next.round,

            remainingSeconds =
                next.durationSeconds
                    .coerceAtLeast(0),

            /*
             * Active timer timing is reconstructed by TimerEngine.
             */
            phaseEndsAtElapsedRealtime =
                0L,

            phaseIndex =
                index
        )
    }
}


data class SessionPhase(
    val phase: TimerPhase,
    val durationSeconds: Int,
    val round: Int
)


class ActiveSessionStore(
    private val context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "active_session",
            Context.MODE_PRIVATE
        )


    // ---------------------------------------------------------
    // SAVE
    // ---------------------------------------------------------

    fun save(
        session: ActiveSession
    ) {

        val phases =
            JSONArray().apply {

                session.phases.forEach { phase ->

                    put(
                        JSONObject().apply {

                            put(
                                "phase",
                                phase.phase.name
                            )

                            put(
                                "duration",
                                phase.durationSeconds
                            )

                            put(
                                "round",
                                phase.round
                            )
                        }
                    )
                }
            }


        val json =
            JSONObject().apply {

                put(
                    "work",
                    session.config.workSeconds
                )

                put(
                    "rest",
                    session.config.restSeconds
                )

                put(
                    "rounds",
                    session.config.rounds
                )

                put(
                    "warm",
                    session.config.warmUpSeconds
                )

                put(
                    "cool",
                    session.config.cooldownSeconds
                )

                put(
                    "phase",
                    session.phase.name
                )

                put(
                    "currentRound",
                    session.currentRound
                )

                put(
                    "totalRounds",
                    session.totalRounds
                )

                put(
                    "remaining",
                    session.remainingSeconds
                        .coerceAtLeast(0)
                )

                put(
                    "schema",
                    SCHEMA_VERSION
                )

                put(
                    "endElapsed",
                    session.phaseEndsAtElapsedRealtime
                        .coerceAtLeast(0L)
                )

                put(
                    "remainingMillis",
                    session.remainingMillis.coerceAtLeast(0L)
                )

                put(
                    "sessionStartedAt",
                    session.sessionStartedAtElapsedRealtime.coerceAtLeast(0L)
                )

                put(
                    "elapsedBeforePause",
                    session.elapsedBeforePauseMillis.coerceAtLeast(0L)
                )

                put(
                    "soundEnabled",
                    session.soundEnabled
                )

                put(
                    "bootCount",
                    currentBootCount()
                )

                put(
                    "index",
                    session.phaseIndex
                )

                put(
                    "phases",
                    phases
                )
            }


        prefs
            .edit()
            .putString(
                KEY,
                json.toString()
            )
            .apply()
    }


    // ---------------------------------------------------------
    // LOAD
    // ---------------------------------------------------------

    fun load(): ActiveSession? {

        val stored =
            prefs.getString(
                KEY,
                null
            )
                ?: return null


        return runCatching {

            val objectJson =
                JSONObject(
                    stored
                )

            if (objectJson.optInt("schema", 0) != SCHEMA_VERSION) {
                error("Unsupported active session schema")
            }


            val phasesJson =
                objectJson.getJSONArray(
                    "phases"
                )


            if (
                phasesJson.length() == 0
            ) {
                error(
                    "Active session contains no phases"
                )
            }


            val phases =
                buildList(
                    phasesJson.length()
                ) {

                    for (
                    index in
                    0 until phasesJson.length()
                    ) {

                        val phaseJson =
                            phasesJson
                                .getJSONObject(
                                    index
                                )


                        val phase =
                            TimerPhase.valueOf(
                                phaseJson
                                    .getString(
                                        "phase"
                                    )
                            )


                        val duration =
                            phaseJson
                                .getInt(
                                    "duration"
                                )
                                .coerceAtLeast(
                                    0
                                )


                        val round =
                            phaseJson
                                .getInt(
                                    "round"
                                )


                        add(
                            SessionPhase(

                                phase =
                                    phase,

                                durationSeconds =
                                    duration,

                                round =
                                    round
                            )
                        )
                    }
                }


            val phaseIndex =
                objectJson
                    .getInt(
                        "index"
                    )


            /*
             * A corrupted phase index means that this
             * is not a usable active session.
             */
            if (
                phaseIndex < 0 ||
                phaseIndex >= phases.size
            ) {
                error(
                    "Invalid active session phase index"
                )
            }


            val currentRound =
                objectJson.getInt(
                    "currentRound"
                )

            val totalRounds =
                objectJson.getInt(
                    "totalRounds"
                )


            if (
                totalRounds <= 0 ||
                currentRound < 0 ||
                currentRound > totalRounds
            ) {
                error(
                    "Invalid active session round"
                )
            }


            val phase =
                TimerPhase.valueOf(
                    objectJson.getString(
                        "phase"
                    )
                )


            val remaining =
                objectJson
                    .getInt(
                        "remaining"
                    )
                    .coerceAtLeast(0)


            val phaseEndsAtElapsedRealtime =
                objectJson
                    .getLong(
                        "endElapsed"
                    )
                    .coerceAtLeast(0L)

            val remainingMillis =
                objectJson.optLong(
                    "remainingMillis",
                    remaining.toLong() * 1000L
                ).coerceAtLeast(0L)

            val sessionStartedAtElapsedRealtime =
                objectJson.optLong(
                    "sessionStartedAt",
                    0L
                ).coerceAtLeast(0L)

            val elapsedBeforePauseMillis =
                objectJson.optLong(
                    "elapsedBeforePause",
                    0L
                ).coerceAtLeast(0L)

            val soundEnabled =
                objectJson.optBoolean(
                    "soundEnabled",
                    true
                )

            val bootCount =
                objectJson.optInt(
                    "bootCount",
                    -1
                )

            val currentBootCount =
                runCatching {
                    Settings.Global.getInt(
                        context.contentResolver,
                        Settings.Global.BOOT_COUNT,
                    )
                }.getOrDefault(bootCount)

            if (bootCount >= 0 && currentBootCount >= 0 && bootCount != currentBootCount) {
                clear()
                return null
            }


            val config =
                TimerConfig(

                    workSeconds =
                        objectJson
                            .getInt("work")
                            .coerceAtLeast(0),

                    restSeconds =
                        objectJson
                            .getInt("rest")
                            .coerceAtLeast(0),

                    rounds =
                        objectJson
                            .getInt("rounds")
                            .coerceAtLeast(0),

                    warmUpSeconds =
                        objectJson
                            .getInt("warm")
                            .coerceAtLeast(0),

                    cooldownSeconds =
                        objectJson
                            .getInt("cool")
                            .coerceAtLeast(0)
                )


            ActiveSession(

                config =
                    config,

                phase =
                    phase,

                currentRound =
                    currentRound,

                totalRounds =
                    totalRounds,

                remainingSeconds =
                    remaining,

                phaseEndsAtElapsedRealtime =
                    phaseEndsAtElapsedRealtime,

                phases =
                    phases,

                phaseIndex =
                    phaseIndex,

                remainingMillis =
                    remainingMillis,

                sessionStartedAtElapsedRealtime =
                    sessionStartedAtElapsedRealtime,

                elapsedBeforePauseMillis =
                    elapsedBeforePauseMillis,

                soundEnabled =
                    soundEnabled,

                bootCount =
                    bootCount
            )

        }.getOrElse {

            /*
             * Do not leave a broken session in storage.
             *
             * Otherwise every future load will fail again.
             */
            clear()

            null
        }
    }


    // ---------------------------------------------------------
    // CLEAR
    // ---------------------------------------------------------

    fun clear() {

        prefs
            .edit()
            .remove(
                KEY
            )
            .apply()
    }


    private fun currentBootCount(): Int =
        runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.BOOT_COUNT,
            )
        }.getOrDefault(0)

    private companion object {

        const val KEY =
            "session"

        const val SCHEMA_VERSION = 3
    }
}