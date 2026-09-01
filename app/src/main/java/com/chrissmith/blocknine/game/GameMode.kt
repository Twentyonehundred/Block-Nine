package com.chrissmith.blocknine.game

/**
 * Which set of rules a game is played under.
 *
 * Classic is the original endless game and the only one that feeds the online leaderboard.
 * Every challenge mode keeps its own records under its own prefs prefix, so a mode with
 * different pressure can neither flatter nor spoil a classic score.
 */
enum class GameMode(
    val title: String,
    /** One line for the challenge list. */
    val blurb: String,
    /** How the mode explains itself before you start. */
    val rules: String,
    /** Prefix for this mode's records. Classic keeps the original unprefixed keys. */
    val prefsPrefix: String,
) {
    CLASSIC(
        title = "Classic",
        blurb = "The endless game.",
        rules = "Fill a row, a column or a 3x3 box to clear it.",
        prefsPrefix = "",
    ),
    RISING_TIDE(
        title = "Rising Tide",
        blurb = "The water surges in from below and shoves your board out of shape.",
        rules = "Every three pieces the tide pushes columns up by different amounts, bending " +
            "whatever you've built out of line. The bar under the board shows where it's " +
            "coming and how hard. Let it push a block off the top and you're done.",
        prefsPrefix = "tide_",
    ),
    ;

    val isChallenge: Boolean get() = this != CLASSIC

    /** True if games in this mode are worth keeping across a restart. */
    val isResumable: Boolean get() = this == CLASSIC

    companion object {
        /** Everything that belongs on the challenges screen. */
        val challenges: List<GameMode> = entries.filter { it.isChallenge }
    }
}
