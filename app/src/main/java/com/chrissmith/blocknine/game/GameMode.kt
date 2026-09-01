package com.chrissmith.blocknine.game

/**
 * Which set of rules a game is played under.
 *
 * Every mode keeps its own records, both on the device (under its own prefs prefix) and online
 * (in its own Firestore collection), so a mode with different pressure can neither flatter nor
 * spoil another mode's score. Classic keeps the original unprefixed keys and the original
 * collection, which is what stops existing installs and existing rows losing their history.
 */
enum class GameMode(
    val title: String,
    /** One line for the challenge list. */
    val blurb: String,
    /** How the mode explains itself before you start. */
    val rules: String,
    /** Prefix for this mode's records. Classic keeps the original unprefixed keys. */
    val prefsPrefix: String,
    /** Firestore collection holding this mode's board. */
    val collection: String,
    /** Short label for the leaderboard's mode tabs, where there isn't room for the full title. */
    val shortTitle: String,
) {
    CLASSIC(
        title = "Classic",
        blurb = "The endless game.",
        rules = "Fill a row, a column or a 3x3 box to clear it.",
        prefsPrefix = "",
        collection = "players",
        shortTitle = "Classic",
    ),
    RISING_TIDE(
        title = "Rising Tide",
        blurb = "The water surges in from below and shoves your board out of shape.",
        rules = "Every three pieces the tide pushes a few columns up by different amounts, " +
            "bending whatever you've built out of line. The bar under the board shows where " +
            "it's coming and how hard.\n\nThe water only carries what it's touching. Clear a " +
            "row and everything resting on it comes loose — the tide can't move it again " +
            "until the water climbs back up to it. Let it push a block off the top and " +
            "you're done.",
        prefsPrefix = "tide_",
        collection = "tidePlayers",
        shortTitle = "Tide",
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
