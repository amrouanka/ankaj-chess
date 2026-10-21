package com.amr.chess.engine;

import com.amr.chess.model.Board;

public class Evaluation {

    private static final int CHECKMATE = 100000;

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
     * Returns a score from the perspective of the side to move.
     * Positive = good for the side to move.
     */
    public static int rating(Board board, int moveCount) {
        if (moveCount == 0) {
            if (GameLogic.kingSafe(board))
                return 0; // stalemate
            else
                return -CHECKMATE; // we are checkmated
        }

        int counter = 0;
        counter += moveCount / 5;
        counter += rateMaterial(board);

        board.flipBoard();
        int opponentMoves = MoveGenerator.generatePossibleMoves(board).size();
        if (opponentMoves == 0) {
            board.flipBoard();
            if (GameLogic.kingSafe(board))
                return 0; // opponent stalemated
            else
                return CHECKMATE; // we checkmated opponent
        }

        counter -= opponentMoves / 5;
        counter -= rateMaterial(board);
        board.flipBoard();

        return counter; // No negation — negamax handles the sign
    }

    private static int rateMaterial(Board board) {
        int[] knightAdj = { -20, -16, -12, -8, -4, 0, 4, 8 };
        int[] rookAdj = { 15, 12, 9, 6, 3, 0, -3, -6 };

        int counter = 0;
        int bishopCount = 0;
        int knightCount = 0;
        int rookCount = 0;
        int pawnCount = -1;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                switch (board.get(i, j)) {
                    case 'P' -> {
                        counter += 100 - DFCT[i][j];
                        pawnCount++;
                    }
                    case 'N' -> {
                        counter += 325 - DFCT[i][j];
                        knightCount++;
                    }
                    case 'B' -> {
                        counter += 325 - DFCT[i][j];
                        bishopCount++;
                    }
                    case 'R' -> {
                        counter += 500 + DFCT[i][j];
                        rookCount++;
                    }
                    case 'Q' -> counter += 975;
                }
            }
        }

        if (knightCount > 1)
            counter -= 20;
        if (rookCount > 1)
            counter -= 20;
        if (bishopCount > 1)
            counter += 50;

        if (pawnCount != -1) {
            counter += (knightAdj[pawnCount] * knightCount) + (rookAdj[pawnCount] * rookCount);
        }

        return counter;
    }
}