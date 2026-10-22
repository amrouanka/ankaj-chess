package com.amr.chess.engine;

import com.amr.chess.model.Board;

public class Evaluation {

    public static final int CHECKMATE = 100000;

    private static final int[][] DFCT = {
            { 6, 5, 4, 3, 3, 4, 5, 6 },
            { 5, 4, 3, 2, 2, 3, 4, 5 },
            { 4, 3, 2, 1, 1, 2, 3, 4 },
            { 3, 2, 1, 0, 0, 1, 2, 3 },
            { 3, 2, 1, 0, 0, 1, 2, 3 },
            { 4, 3, 2, 1, 1, 2, 3, 4 },
            { 5, 4, 3, 2, 2, 3, 4, 5 },
            { 6, 5, 4, 3, 3, 4, 5, 6 }
    };

    /**
     * Fast static evaluation: single pass over 64 squares.
     * Positive = good for the side to move (uppercase pieces).
     *
     * No cloning, no flipping, no move generation.
     * Checkmate/stalemate is handled by the search, not here.
     */
    public static int rating(Board board) {
        int counter = 0;
        int ourBishops = 0;
        int theirBishops = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                switch (board.get(i, j)) {
                    // Our pieces (uppercase) — add to score
                    case 'P' -> counter += 100 - DFCT[i][j];
                    case 'N' -> counter += 325 - DFCT[i][j];
                    case 'B' -> {
                        counter += 325 - DFCT[i][j];
                        ourBishops++;
                    }
                    case 'R' -> counter += 500 + DFCT[i][j];
                    case 'Q' -> counter += 975;
                    // Their pieces (lowercase) — subtract from score
                    case 'p' -> counter -= 100 - DFCT[i][j];
                    case 'n' -> counter -= 325 - DFCT[i][j];
                    case 'b' -> {
                        counter -= 325 - DFCT[i][j];
                        theirBishops++;
                    }
                    case 'r' -> counter -= 500 + DFCT[i][j];
                    case 'q' -> counter -= 975;
                }
            }
        }

        // Bishop pair bonus
        if (ourBishops >= 2)
            counter += 50;
        if (theirBishops >= 2)
            counter -= 50;

        return counter;
    }
}