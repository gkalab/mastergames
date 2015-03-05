package com.kalab.database;

import android.database.AbstractCursor;
import android.os.Bundle;
import android.util.Log;

import com.kalab.database.ScidProviderMetaData.ScidMetaData;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ScidCursor extends AbstractCursor {
    private static String TAG = ScidCursor.class.getSimpleName();
    private static GameFilter gameFilter;
    private int count;
    private GameInfo gameInfo;
    private int startPosition;
    private int[] projection;
    private boolean loadPGN = false; // True if projection contains pgn column
    private boolean singleGame = false;
    private int limit = -1;

    public ScidCursor(String fileName, String[] projection, int limit) {
        super();
        gameFilter = null;
        init(fileName, projection, 0, limit);
    }

    public ScidCursor(String fileName, String[] projection, int startPosition,
                      int limit) {
        this(fileName, projection, limit);
        this.startPosition = startPosition;
    }

    public ScidCursor(String fileName, String[] projection, int startPosition,
                      String[] selectionArgs, int limit) {
        super();
        init(fileName, projection, startPosition, limit);
        searchHeader(selectionArgs);
    }

    public ScidCursor(String fileName, String[] projection, int startPosition,
                      String filterOperation, String fen, int searchType,
                      int limit) {
        super();
        init(fileName, projection, startPosition, limit);
        searchBoard(filterOperation, fen, searchType);
    }

    private void init(String fileName, String[] projection, int startPosition, int limit) {
        this.limit = limit;
        this.singleGame = limit == 1 ? true : false;
        DataBase.loadFile(fileName);
        this.count = DataBase.getSize();
        this.startPosition = startPosition;
        handleProjection(projection);
    }

    private void handleProjection(String[] projection) {
        if (projection == null) {
            this.projection = new int[ScidMetaData.columns.length];
            for (int i = 0; i < ScidMetaData.columns.length; i++) {
                this.projection[i] = i;
            }
        } else {
            ArrayList<Integer> proj = new ArrayList<>();
            for (String p : projection) {
                int idx = 0;
                for (int i = 0; i < ScidMetaData.columns.length; i++) {
                    if (ScidMetaData.columns[i].equals(p)) {
                        idx = i;
                        break;
                    }
                }
                proj.add(idx);
            }
            this.projection = new int[proj.size()];
            for (int i = 0; i < proj.size(); i++) {
                this.projection[i] = proj.get(i);
            }
        }
        loadPGN = false;
        for (int p : this.projection) {
            if (p == 8) {
                loadPGN = true;
                break;
            }
        }
    }

    private void searchHeader(String[] selectionArgs) {
        int filterOp = getFilterOperation(selectionArgs[0]);
        SearchHeaderRequest request = new SearchHeaderRequest();

        request.white = selectionArgs[1];
        request.black = selectionArgs[2];
        request.ignoreColors = Boolean.parseBoolean(selectionArgs[3]);
        request.resultWhiteWins = Boolean.parseBoolean(selectionArgs[4]);
        request.resultDraw = Boolean.parseBoolean(selectionArgs[5]);
        request.resultBlackWins = Boolean.parseBoolean(selectionArgs[6]);
        request.resultNone = Boolean.parseBoolean(selectionArgs[7]);
        request.event = selectionArgs[8];
        request.site = selectionArgs[9];
        request.round = selectionArgs[10];
        String dateFrom = selectionArgs[11];
        String dateTo = selectionArgs[12];
        //request.ecoFrom = selectionArgs[13];
        //request.ecoTo = selectionArgs[14];
        request.dateMin = SearchHeaderRequest.makeDate(getYearFromDate(dateFrom, 0),
                getMonthFromDate(dateFrom, 1), getDayFromDate(dateFrom, 1));
        request.dateMax = SearchHeaderRequest.makeDate(getYearFromDate(dateTo, 9999),
                getMonthFromDate(dateTo, 12), getDayFromDate(dateTo, 31));
        short[] filter = GameFilter.getFilterArray(null, count);
        Progress progress = new Progress() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void publishProgress(int value) {
                // ignore
            }
        };
        DataBase.searchHeader(request, filterOp, filter, progress);
        gameFilter = new GameFilter(filter);
    }

    private int getDayFromDate(String date, int defaultValue) {
        return intFromString(date, 8, 10, defaultValue);
    }

    private int getMonthFromDate(String date, int defaultValue) {
        return intFromString(date, 5, 7, defaultValue);
    }

    private int getYearFromDate(String date, int defaultValue) {
        return intFromString(date, 0, 4, defaultValue);
    }

    private int intFromString(String strValue, int start, int end, int defaultValue) {
        int result = defaultValue;
        if (strValue.length() >= end) {
            try {
                result = Integer.parseInt(strValue.substring(start, end));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return result;
    }

    @Override
    public Bundle getExtras() {
        Bundle bundle = new Bundle();
        if (gameFilter != null) {
            bundle.putInt("filterSize", gameFilter.getSize());
            bundle.putInt("count", count);
        }
        return bundle;
    }

    private void searchBoard(String filterOperation,
                             String fen, int searchType) {
        short[] filter = GameFilter.getFilterArray(null, count);
        int filterOp = getFilterOperation(filterOperation);
        Progress progress = new Progress() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void publishProgress(int value) {
                // ignore
            }
        };
        DataBase.searchBoard(fen, searchType, filterOp, filter, progress);
        gameFilter = new GameFilter(filter);
    }

    private int getFilterOperation(String filterOperation) {
        int filterOp = 0;
        if (gameFilter != null && filterOperation != null) {
            filterOp = Integer.parseInt(filterOperation);
        }
        return filterOp;
    }

    @Override
    public String[] getColumnNames() {
        String[] ret = new String[projection.length];
        int idx = 0;
        for (int i : projection) {
            ret[idx++] = ScidMetaData.columns[i];
        }
        return ret;
    }

    /**
     * Return the number of games in the cursor. If there's a current filter
     * only return the number of games in the filter.
     *
     * @see android.database.AbstractCursor#getCount()
     */
    @Override
    public int getCount() {
        int result;
        if (gameFilter != null) {
            result = gameFilter.getSize();
        } else {
            result = this.count;
        }
        if (this.limit > 0 && result > this.limit) {
            result = this.limit;
        }
        return result;
    }

    private void setGameInfo(int gameNo, boolean isFavorite) {
        this.gameInfo = new GameInfo();
        try {
            gameInfo.setEvent(getSanitizedString(DataBase.getEvent()));
            if (gameInfo.getEvent().equals("?")) {
                gameInfo.setEvent("");
            }
            gameInfo.setSite(getSanitizedString(DataBase.getSite()));
            if (gameInfo.getSite().equals("?")) {
                gameInfo.setSite("");
            }
            String date = DataBase.getDate();
            if (date == null) {
                date = "";
            } else if (date.endsWith(".??.??")) {
                date = date.substring(0, date.length() - 6);
            } else if (date.endsWith(".??")) {
                date = date.substring(0, date.length() - 3);
            }
            if (date.equals("?") || date.equals("????")) {
                date = "";
            }
            gameInfo.setDate(date);
            gameInfo.setRound(getSanitizedString(DataBase.getRound()));
            if (gameInfo.getRound().equals("?")) {
                gameInfo.setRound("");
            }
            gameInfo.setWhite(getSanitizedString(DataBase.getWhite()));
            gameInfo.setBlack(getSanitizedString(DataBase.getBlack()));
            String[] results = {"*", "1-0", "0-1", "1/2"};
            gameInfo.setResult(results[DataBase.getResult()]);
            byte[] dbPgn = DataBase.getPGN();
            if (dbPgn != null) {
                gameInfo.setPgn(loadPGN ? new String(DataBase.getPGN(),
                        DataBase.SCID_ENCODING) : null);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e("SCID", "Error converting byte[] to String", e);
        }
        gameInfo.setId(gameNo);
        gameInfo.setFavorite(isFavorite);
        gameInfo.setDeleted(DataBase.isDeleted());
    }

    private String getSanitizedString(byte[] value)
            throws UnsupportedEncodingException {
        if (value == null) {
            return "";
        } else {
            try {
                return Utf8Converter.convertToUTF8(new String(value,
                        DataBase.SCID_ENCODING));
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
    }

    /**
     * @param oldPosition the position that we're moving from
     * @param newPosition the position that we're moving to
     * @return true if the move is successful, false otherwise
     */
    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        boolean result = true;
        if (gameFilter != null) {
            result = this.onFilterMove(newPosition);
        } else {
            int gameNo = startPosition + newPosition;
            boolean onlyHeaders = !loadPGN;
            boolean isFavorite = DataBase.loadGame(gameNo, onlyHeaders);
            setGameInfo(gameNo, isFavorite);
        }
        return result;
    }

    private boolean onFilterMove(int newPosition) {
        boolean result = false;
        int gameNo = gameFilter.getGameId(startPosition + newPosition);
        if (gameNo >= 0) {
            boolean onlyHeaders = !loadPGN;
            boolean isFavorite = DataBase.loadGame(gameNo, onlyHeaders);
            setGameInfo(gameNo, isFavorite);
            gameInfo.setCurrentPly(gameFilter.getGamePly(startPosition
                    + newPosition));
            result = true;
        }
        return result;
    }

    @Override
    public double getDouble(int arg0) {
        return 0;
    }

    @Override
    public float getFloat(int arg0) {
        return 0;
    }

    @Override
    public int getInt(int position) {
        if (this.gameInfo != null) {
            return Integer.parseInt(this.gameInfo
                    .getColumn(projection[position]));
        }
        return 0;
    }

    @Override
    public long getLong(int position) {
        if (this.gameInfo != null) {
            return Long
                    .parseLong(this.gameInfo.getColumn(projection[position]));
        }
        return 0;
    }

    @Override
    public short getShort(int position) {
        if (this.gameInfo != null) {
            return Short.parseShort(this.gameInfo
                    .getColumn(projection[position]));
        }
        return 0;
    }

    @Override
    public String getString(int position) {
        if (this.gameInfo != null) {
            int column = projection[position];
            return this.gameInfo.getColumn(column);
        }
        return null;
    }

    @Override
    public boolean isNull(int position) {
        return this.gameInfo == null || "".equals(this.gameInfo.getColumn(projection[position]));
    }

    @Override
    public Bundle respond(Bundle extras) {
        if (extras.containsKey("loadPGN")) {
            this.loadPGN = extras.getBoolean("loadPGN");
        }
        if (extras.containsKey("isDeleted")) {
            this.gameInfo.setDeleted(extras.getBoolean("isDeleted"));
        }
        if (extras.containsKey("isFavorite")) {
            this.gameInfo.setFavorite(extras.getBoolean("isFavorite"));
        }
        return null;
    }
}
