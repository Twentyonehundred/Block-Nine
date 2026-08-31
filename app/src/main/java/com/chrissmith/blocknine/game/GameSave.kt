package com.chrissmith.blocknine.game

import android.content.SharedPreferences

/**
 * The game in progress, kept on disk so that leaving the app — or Android quietly reclaiming
 * the process while it's in the background — doesn't cost you a run.
 *
 * Written after every settled move rather than on the way out, because process death arrives
 * without warning. A whole game is one 81-character string and four numbers, so writing it
 * that often costs nothing next to losing a good score.
 */
class GameSave(private val prefs: SharedPreferences) {

    /** A game read back off disk. */
    data class Snapshot(
        val board: Board,
        val tray: List<Piece?>,
        val score: Int,
        val streak: Int,
        val bestToBeat: Int,
    )

    fun write(board: Board, tray: List<Piece?>, score: Int, streak: Int, bestToBeat: Int) {
        prefs.edit()
            .putString(KEY_BOARD, board.encode())
            .putString(KEY_TRAY, encodeTray(tray))
            .putInt(KEY_SCORE, score)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_BEST_TO_BEAT, bestToBeat)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_BOARD)
            .remove(KEY_TRAY)
            .remove(KEY_SCORE)
            .remove(KEY_STREAK)
            .remove(KEY_BEST_TO_BEAT)
            .apply()
    }

    /**
     * Reads the saved game back, or null if there isn't one or it can't be made sense of.
     *
     * Anything unparseable is treated as no save at all: the worst case is a fresh board,
     * which is a great deal better than a crash on launch. An empty tray means the save was
     * caught mid-refill, which is likewise not a game worth resuming.
     */
    fun read(): Snapshot? {
        val board = prefs.getString(KEY_BOARD, null)?.let { Board.decode(it) } ?: return null
        val tray = decodeTray(prefs.getString(KEY_TRAY, null)) ?: return null

        return Snapshot(
            board = board,
            tray = tray,
            score = prefs.getInt(KEY_SCORE, 0),
            streak = prefs.getInt(KEY_STREAK, 0),
            bestToBeat = prefs.getInt(KEY_BEST_TO_BEAT, 0),
        )
    }

    companion object {
        private const val KEY_BOARD = "save_board"
        private const val KEY_TRAY = "save_tray"
        private const val KEY_SCORE = "save_score"
        private const val KEY_STREAK = "save_streak"
        private const val KEY_BEST_TO_BEAT = "save_best_to_beat"

        /** Piece ids are lower-case letters, digits and dashes, so a comma can't collide. */
        private const val SEPARATOR = ","

        /** The tray as piece ids, with a spent slot written as nothing at all. */
        fun encodeTray(tray: List<Piece?>): String = tray.joinToString(SEPARATOR) { it?.id ?: "" }

        /**
         * Reads a tray back, or null if [text] isn't one.
         *
         * A shape id retired in a later build has no piece to restore, and an all-spent tray
         * was written mid-refill; both are refused so the caller falls back to a fresh game.
         */
        fun decodeTray(text: String?): List<Piece?>? {
            val ids = text?.split(SEPARATOR) ?: return null
            if (ids.size != Pieces.TRAY_SIZE) return null
            val tray = ids.map { id -> if (id.isEmpty()) null else Pieces.byId(id) ?: return null }
            return if (tray.all { it == null }) null else tray
        }
    }
}
