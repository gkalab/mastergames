package com.kalab.database;

public class SearchHeaderRequest {
    // from date.h
    public static final int YEAR_SHIFT = 9, MONTH_SHIFT = 5, YEAR_MAX = 2047;
    // from index.h
    public static final int MAX_ELO = 4000;
    public String white, black, event, site, round,
            ecoFrom = "", ecoTo = "";
    public boolean ignoreColors,
            whiteExact, blackExact, eventExact, siteExact, roundExact,
            resultNone = true, resultWhiteWins = true, resultBlackWins = true, resultDraw = true,
            halfMovesEven = true, halfMovesOdd = true,
            allowEcoNone = true, allowUnknownElo = true, annotatedOnly;
    public int dateMin = 0, dateMax = makeDate(YEAR_MAX, 12, 31),
            idMin = 0, idMax = 10000000, halfMovesMin = 0, halfMovesMax = 9999,
            whiteEloMin = 0, whiteEloMax = MAX_ELO, blackEloMin = 0, blackEloMax = MAX_ELO,
            diffEloMin = 0, diffEloMax = MAX_ELO,
            minEloMin = 0, minEloMax = MAX_ELO, maxEloMin = 0, maxEloMax = MAX_ELO;

    public static int makeDate(int y, int m, int d) {
        return (y == 0) ? 0 : (y << YEAR_SHIFT) | (m << MONTH_SHIFT) | d;
    }
}
