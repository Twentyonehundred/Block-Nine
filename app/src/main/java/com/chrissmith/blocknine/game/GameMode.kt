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
            "it's coming and how hard.\n\nThe water only moves what's directly in its way, one " +
            "column at a time. A block with air beneath it stays put and the water rises past " +
            "underneath, so gaps soak up the push.\n\nA column squashed solid from floor to " +
            "ceiling clears like any other, even if the water had to crush a block off the top " +
            "to fill it. The run ends the same way a classic one does: when none of your three " +
            "pieces will fit.",
        prefsPrefix = "tide_",
        collection = "tidePlayers",
        shortTitle = "Tide",
    ),
    COLLAPSE(
        title = "Collapse",
        blurb = "Clear a line and everything above it falls into the hole.",
        rules = "Place pieces anywhere you like, exactly as you would in Classic — floating in " +
            "mid-air if you want. The change is what happens after a clear: the tiles standing " +
            "over the hole fall into it. Clear a row and everything above it comes down one. " +
            "Clear a 3x3 box and those three columns drop three, while the rest of the board " +
            "stays exactly where you put it.\n\nTiles that land somewhere new can complete " +
            "lines you never placed for, and those collapse in turn. Each link in a chain is " +
            "worth more than the one before it, so the board you want isn't the tidiest one — " +
            "it's the one that's about to come down.",
        prefsPrefix = "collapse_",
        collection = "collapsePlayers",
        shortTitle = "Collapse",
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
