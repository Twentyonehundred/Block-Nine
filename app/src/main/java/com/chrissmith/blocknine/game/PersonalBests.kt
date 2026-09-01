package com.chrissmith.blocknine.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.chrissmith.blocknine.leaderboard.Periods

/**
 * The player's own records — best today, best this month, best ever — kept on the device.
 *
 * Separate from the Firestore leaderboard on purpose: someone who never signs in still gets
 * to track themselves. The day and month buckets use the leaderboard's own UTC keys so that
 * "today" means the same thing whichever board you are looking at.
 *
 * [prefix] scopes the records to one [GameMode]. Classic passes an empty prefix and so keeps
 * the original keys, which is what stops existing installs losing their all-time best.
 */
class PersonalBests(private val prefs: SharedPreferences, prefix: String = "") {

    private val keyAll = prefix + KEY_ALL
    private val keyDay = prefix + KEY_DAY
    private val keyDayKey = prefix + KEY_DAY_KEY
    private val keyMonth = prefix + KEY_MONTH
    private val keyMonthKey = prefix + KEY_MONTH_KEY

    var allTime by mutableIntStateOf(prefs.getInt(keyAll, 0))
        private set

    var day by mutableIntStateOf(0)
        private set

    var month by mutableIntStateOf(0)
        private set

    private var dayKey = ""
    private var monthKey = ""

    init {
        refresh()
    }

    /**
     * Rolls the day and month records over when their period ends. Cheap, and called both on
     * every new game and before every record, so a session left open past midnight UTC never
     * shows yesterday's number under "today".
     */
    fun refresh() {
        val today = Periods.dayKey()
        if (today != dayKey) {
            dayKey = today
            day = if (prefs.getString(keyDayKey, null) == today) prefs.getInt(keyDay, 0) else 0
        }

        val thisMonth = Periods.monthKey()
        if (thisMonth != monthKey) {
            monthKey = thisMonth
            month =
                if (prefs.getString(keyMonthKey, null) == thisMonth) prefs.getInt(keyMonth, 0) else 0
        }
    }

    /** Files [score] against all three records, persisting whichever ones it beats. */
    fun record(score: Int) {
        refresh()
        val edit = prefs.edit()
        if (score > allTime) {
            allTime = score
            edit.putInt(keyAll, score)
        }
        if (score > day) {
            day = score
            edit.putInt(keyDay, score).putString(keyDayKey, dayKey)
        }
        if (score > month) {
            month = score
            edit.putInt(keyMonth, score).putString(keyMonthKey, monthKey)
        }
        edit.apply()
    }

    companion object {

        /**
         * The records for [mode], read straight out of prefs.
         *
         * For screens that want a mode's numbers without owning its game — the challenge list
         * and the leaderboard's mode tabs both need a best they aren't currently playing.
         */
        fun of(context: Context, mode: GameMode): PersonalBests = PersonalBests(
            context.getSharedPreferences(GameViewModel.PREFS, Context.MODE_PRIVATE),
            mode.prefsPrefix,
        )

        // Keeps the original key so existing installs don't lose their all-time best.
        private const val KEY_ALL = "best_score"
        private const val KEY_DAY = "best_day"
        private const val KEY_DAY_KEY = "best_day_key"
        private const val KEY_MONTH = "best_month"
        private const val KEY_MONTH_KEY = "best_month_key"
    }
}
