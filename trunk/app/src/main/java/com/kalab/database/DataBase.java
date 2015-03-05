package com.kalab.database;

public class DataBase {
    /**
     * SCID's encoding seems to be CP1252 under Windows and under Linux TODO:
     * this is not true - fix encoding
     */
    public static final String SCID_ENCODING = "CP1252";

    static {
        System.loadLibrary("jni");
    }

    // Make sure the following is in sync with jniscid.cpp
    public static final int // nameType is one of
            NAME_PLAYER = 0,
            NAME_EVENT = 1, NAME_SITE = 2, NAME_ROUND = 3;
    public static final int /* from common.h */
            RESULT_NONE = 0, RESULT_WHITE_WINS = 1, RESULT_BLACK_WINS = 2,
            RESULT_DRAW = 3;

    // / Loading and operations with loaded file
    public static final native boolean loadFile(String fileName);

    public static final native int getSize();

    public static final native int getNamesCount(int nameType);

    public static final native byte[] getName(int nameType, int id);

    // TODO: all Java->C++ calls should use byte[] recoded back to DB encoding,
    // or
    // we should switch DB to modified UTF-8

    public static final native int[] getMatchingNames(int nameType,
                                                      String prefix);

    // / Loading and operations with the loaded game
    public static final native boolean loadGame(int gameId, boolean onlyHeaders);

    /**
     * Get the complete PGN of the current game.
     */
    public static final native byte[] getPGN();

    /**
     * Get the move list (including the result) of the current game.
     */
    public static final native String getMoves();

    /**
     * Get the header [Result] of the current game.
     */
    public static final native int getResult();

    /**
     * Get the header [White] of the current game.
     */
    public static final native byte[] getWhite();

    /**
     * Get the header [Black] of the current game.
     */
    public static final native byte[] getBlack();

    /**
     * Get the header [Event] of the current game.
     */
    public static final native byte[] getEvent();

    /**
     * Get the header [Site] of the current game.
     */
    public static final native byte[] getSite();

    /**
     * Get the header [Date] of the current game.
     */
    public static final native String getDate();

    /**
     * Get the header [Round] of the current game.
     */
    public static final native byte[] getRound();

    public static final native int getWhiteElo();

    public static final native int getBlackElo();

    /**
     * Return true if the current game is favorite.
     */
    public static final native boolean isFavorite();

    /**
     * Return true if the current game is marked as deleted.
     */
    public static final native boolean isDeleted();

    // / Create database (new or import)

    /**
     * Create new empty database
     */
    public static final native String create(String fileName);

    /**
     * Import a pgn file and create a SCID database.
     */
    public static final native String importPgn(String fileName,
                                                Progress progress);

    // / Filtering

    /**
     * Do a board search and return the found game numbers and plys in a short
     * array
     *
     * @param fen             the FEN position to search for
     * @param searchType      0=exact, 1=pawns, 2=files, 3=any
     * @param filterOperation the type of filter restriction (0=IGNORE, 1=OR, 2=AND)
     * @param filter          in-out array with ply for each game or 0 if the game is not
     *                        selected
     */
    public static final native boolean searchBoard(String fen, int searchType,
                                                   int filterOperation, short[]/* in-out */filter, Progress progress);

    public static final native boolean searchHeader(
            SearchHeaderRequest request, int filterOperation,
            short[]/* in-out */filter, Progress progress);

    /**
     * Return the list of favorites.
     */
    public static final native int[] getFavorites(Progress progress);

    // / Modifications

    /**
     * Set the favorite flag on the current game.
     */
    public static final native boolean setFavorite(boolean isFavorite);

    /**
     * Set the deleted flag on the current game.
     */
    public static final native boolean setDeleted(boolean isDeleted);

    /**
     * Save the game with the game number.
     */
    public static final native String saveGame(int gameId, String pgn);

    /**
     * Export the current filter to a PGN file
     */
    public static final native boolean exportFilter(String pgnFileName,
                                                    short[] filter, Progress progress);
}
