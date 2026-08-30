package com.chrissmith.blocknine.game

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
 */
class PersonalBests(private val prefs: SharedPreferences) {

    var allTime by mutableIntStateOf(prefs.getInt(KEY_ALL, 0))
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
            day = if (prefs.getString(KEY_DAY_KEY, null) == today) prefs.getInt(KEY_DAY, 0) else 0
        }

        val thisMonth = Periods.monthKey()
        if (thisMonth != monthKey) {
            monthKey = thisMonth
            month =
                if (prefs.getString(KEY_MONTH_KEY, null) == thisMonth) prefs.getInt(KEY_MONTH, 0) else 0
        }
    }

    /** Files [score] against all three records, persisting whichever ones it beats. */
    fun record(score: Int) {
        refresh()
        val edit = prefs.edit()
        if (score > allTime) {
            allTime = score
            edit.putInt(KEY_ALL, score)
        }
        if (score > day) {
            day = score
            edit.putInt(KEY_DAY, score).putString(KEY_DAY_KEY, dayKey)
        }
        if (score > month) {
            month = score
            edit.putInt(KEY_MONTH, score).putString(KEY_MONTH_KEY, monthKey)
        }
        edit.apply()
    }

    private companion object {
        // Keeps the original key so existing installs don't lose their all-time best.
        const val KEY_ALL = "best_score"
        const val KEY_DAY = "best_day"
        const val KEY_DAY_KEY = "best_day_key"
        const val KEY_MONTH = "best_month"
        const val KEY_MONTH_KEY = "best_month_key"
    }
}
