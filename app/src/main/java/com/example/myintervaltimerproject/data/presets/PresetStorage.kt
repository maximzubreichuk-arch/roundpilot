package com.example.myintervaltimerproject.data.presets

import android.content.Context

import com.example.myintervaltimerproject.timer.TimerConfig

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import org.json.JSONArray
import org.json.JSONObject


class PresetStorage(
    context: Context,
) {

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )


    private val _items =
        MutableStateFlow(
            loadFromStorage()
        )


    val items: StateFlow<List<Preset>> =
        _items.asStateFlow()


    init {

        /*
         * Create the default presets only once.
         *
         * After the user deletes one of them, it will NOT
         * come back on the next launch.
         */
        if (
            !prefs.getBoolean(
                KEY_INITIALIZED,
                false
            )
        ) {

            val defaults =
                defaultPresets()


            saveAllInternal(
                defaults
            )


            prefs.edit()
                .putBoolean(
                    KEY_INITIALIZED,
                    true
                )
                .apply()


            _items.value =
                defaults
        }
    }


    // ---------------------------------------------------------
    // LOAD
    // ---------------------------------------------------------

    fun load(): List<Preset> {

        return _items.value
    }


    // ---------------------------------------------------------
    // ADD
    // ---------------------------------------------------------

    fun add(
        preset: Preset
    ): Boolean {

        val current =
            _items.value


        /*
         * Maximum number of presets.
         */
        if (
            current.size >=
            MAX_PRESETS
        ) {

            return false
        }


        /*
         * Prevent duplicate IDs.
         */
        if (
            current.any {
                it.id == preset.id
            }
        ) {

            return false
        }


        saveAllInternal(

            current + preset

        )


        return true
    }


    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    fun update(
        preset: Preset
    ): Boolean {

        val current =
            _items.value


        val index =
            current.indexOfFirst {

                it.id ==
                        preset.id
            }


        if (
            index == -1
        ) {

            return false
        }


        val next =
            current.toMutableList()


        next[index] =
            preset


        saveAllInternal(
            next
        )


        return true
    }


    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    fun delete(
        id: Long
    ) {

        val next =
            _items.value.filterNot {

                it.id ==
                        id
            }


        /*
         * Nothing to do if such a preset
         * does not exist.
         */
        if (
            next.size ==
            _items.value.size
        ) {

            return
        }


        saveAllInternal(
            next
        )
    }


    // ---------------------------------------------------------
    // SAVE ALL
    // ---------------------------------------------------------

    fun saveAll(
        items: List<Preset>
    ) {

        saveAllInternal(
            items.take(
                MAX_PRESETS
            )
        )
    }


    // ---------------------------------------------------------
    // INTERNAL SAVE
    // ---------------------------------------------------------

    private fun saveAllInternal(
        items: List<Preset>
    ) {

        /*
         * Keep only the supported number of presets.
         */
        val limitedItems =
            items.take(
                MAX_PRESETS
            )


        val json =
            JSONArray()


        limitedItems.forEach { preset ->

            json.put(

                JSONObject().apply {

                    put(
                        "id",
                        preset.id
                    )

                    put(
                        "name",
                        preset.name
                    )

                    put(
                        "work",
                        preset.config.workSeconds
                    )

                    put(
                        "rest",
                        preset.config.restSeconds
                    )

                    put(
                        "rounds",
                        preset.config.rounds
                    )

                    put(
                        "warmUp",
                        preset.config.warmUpSeconds
                    )

                    put(
                        "cooldown",
                        preset.config.cooldownSeconds
                    )
                }
            )
        }


        prefs.edit()
            .putString(
                KEY_ITEMS,
                json.toString()
            )
            .apply()


        _items.value =
            limitedItems
    }


    // ---------------------------------------------------------
    // LOAD FROM STORAGE
    // ---------------------------------------------------------

    private fun loadFromStorage():
            List<Preset> {

        val raw =
            prefs.getString(
                KEY_ITEMS,
                null
            )


        /*
         * Nothing has been stored yet.
         */
        if (
            raw.isNullOrBlank()
        ) {

            return emptyList()
        }


        return try {

            val json =
                JSONArray(
                    raw
                )


            buildList {

                val count =
                    json.length()
                        .coerceAtMost(
                            MAX_PRESETS
                        )


                for (
                i in 0 until count
                ) {

                    val item =
                        json.getJSONObject(
                            i
                        )


                    val id =
                        item.getLong(
                            "id"
                        )


                    /*
                     * Ignore duplicated IDs.
                     */
                    if (
                        any {
                            it.id == id
                        }
                    ) {

                        continue
                    }


                    val name =
                        item
                            .optString(
                                "name",
                                ""
                            )
                            .trim()


                    val work =
                        item
                            .getInt(
                                "work"
                            )
                            .coerceAtLeast(
                                1
                            )


                    val rest =
                        item
                            .getInt(
                                "rest"
                            )
                            .coerceAtLeast(
                                0
                            )


                    val rounds =
                        item
                            .getInt(
                                "rounds"
                            )
                            .coerceAtLeast(
                                1
                            )


                    val warmUp =
                        item
                            .optInt(
                                "warmUp",
                                0
                            )
                            .coerceAtLeast(
                                0
                            )


                    val cooldown =
                        item
                            .optInt(
                                "cooldown",
                                0
                            )
                            .coerceAtLeast(
                                0
                            )


                    add(

                        Preset(

                            id =
                                id,

                            name =
                                name,

                            config =
                                TimerConfig(

                                    workSeconds =
                                        work,

                                    restSeconds =
                                        rest,

                                    rounds =
                                        rounds,

                                    warmUpSeconds =
                                        warmUp,

                                    cooldownSeconds =
                                        cooldown
                                )
                        )
                    )
                }

            }

        } catch (
            exception: Exception
        ) {

            /*
             * The stored data is corrupted.
             *
             * Remove the broken data and allow the
             * normal initialization path to recreate
             * the default presets.
             */
            prefs.edit()
                .remove(
                    KEY_ITEMS
                )
                .remove(
                    KEY_INITIALIZED
                )
                .apply()


            emptyList()
        }
    }


    // ---------------------------------------------------------
    // DEFAULT PRESETS
    // ---------------------------------------------------------

    private fun defaultPresets():
            List<Preset> {

        return listOf(

            Preset(

                id =
                    DEFAULT_HIIT_ID,

                name =
                    "HIIT",

                config =
                    TimerConfig(

                        workSeconds =
                            30,

                        restSeconds =
                            15,

                        rounds =
                            8
                    )
            ),


            Preset(

                id =
                    DEFAULT_TABATA_ID,

                name =
                    "Tabata",

                config =
                    TimerConfig(

                        workSeconds =
                            20,

                        restSeconds =
                            10,

                        rounds =
                            8
                    )
            ),


            Preset(

                id =
                    DEFAULT_BOXING_ID,

                name =
                    "Boxing",

                config =
                    TimerConfig(

                        workSeconds =
                            180,

                        restSeconds =
                            60,

                        rounds =
                            12
                    )
            )
        )
    }


    private companion object {

        const val PREFS_NAME =
            "presets"


        const val KEY_ITEMS =
            "items"


        const val KEY_INITIALIZED =
            "initialized"


        const val MAX_PRESETS =
            5


        const val DEFAULT_HIIT_ID =
            1001L


        const val DEFAULT_TABATA_ID =
            1002L


        const val DEFAULT_BOXING_ID =
            1003L
    }
}
