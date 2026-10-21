package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

public class GameLogic {

    public static boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Returns true if the white king is not currently under attack.
     * Checks all attack vectors: sliding pieces, knights, pawns, and enemy king.
     */
    public static boolean kingSafe(Board board) {
        int kingRow = board.getWhiteKingPos() / 8;
        int kingCol = board.getWhiteKingPos() % 8;

        // Check diagonals (bishop/queen)
        for (int rowDir = -1; rowDir <= 1; rowDir += 2) {
            for (int colDir = -1; colDir <= 1; colDir += 2) {
                if (isAttackedBySlider(board, kingRow, kingCol, rowDir, colDir, 'b', 'q')) {
                    return false;
                }
            }
        }

        // Check rows and columns (rook/queen)
        int[][] straightDirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] dir : straightDirs) {
            if (isAttackedBySlider(board, kingRow, kingCol, dir[0], dir[1], 'r', 'q')) {
                return false;
            }
        }

        // Check knight positions
        int[][] knightOffsets = {
                { -1, -2 }, { 1, 2 }, { -2, -1 }, { 2, 1 },
                { 1, -2 }, { -1, 2 }, { 2, -1 }, { -2, 1 }
        };
        for (int[] offset : knightOffsets) {
            int newRow = kingRow + offset[0];
            int newCol = kingCol + offset[1];
            if (isValidPosition(newRow, newCol) && board.get(newRow, newCol) == 'n') {
                return false;
            }
        }

        // Check pawn attacks (black pawns attack diagonally downward from king's view)
        if (board.getWhiteKingPos() >= 16) {
            for (int colOffset = -1; colOffset <= 1; colOffset += 2) {
                int newRow = kingRow - 1;
                int newCol = kingCol + colOffset;
                if (isValidPosition(newRow, newCol) && board.get(newRow, newCol) == 'p') {
                    return false;
                }
            }
        }

        // Check adjacent enemy king
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                if (rowOffset == 0 && colOffset == 0)
                    continue;
                int newRow = kingRow + rowOffset;
                int newCol = kingCol + colOffset;
                if (isValidPosition(newRow, newCol) && board.get(newRow, newCol) == 'k') {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Walks in a direction from the king, looking for an enemy slider.
     * Returns true if an enemy piece of type attacker1 or attacker2 is found.
     */
    private static boolean isAttackedBySlider(Board board, int kingRow, int kingCol,
            int rowDir, int colDir,
            char attacker1, char attacker2) {
        int row = kingRow + rowDir;
        int col = kingCol + colDir;

        while (isValidPosition(row, col) && board.get(row, col) == ' ') {
            row += rowDir;
            col += colDir;
        }

        if (!isValidPosition(row, col))
            return false;
        char piece = board.get(row, col);
        return piece == attacker1 || piece == attacker2;
    }

    /**
     * Tries a move, checks if the king is safe, then undoes it.
     * Returns the Move if legal, or null if it leaves the king in check.
     */
    public static Move checkMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        char capturedPiece = board.get(toRow, toCol);
        char movingPiece = board.get(fromRow, fromCol);

        // Make the move
        board.set(toRow, toCol, movingPiece);
        board.set(fromRow, fromCol, ' ');

        Move move = null;
        if (kingSafe(board)) {
            move = new Move(fromRow, fromCol, toRow, toCol, capturedPiece);
        }

        // Undo the move
        board.set(fromRow, fromCol, movingPiece);
        board.set(toRow, toCol, capturedPiece);

        return move;
    }

    /**
     * Same as checkMove, but creates a promotion move (always to queen).
     */
    public static Move checkPromotionMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        char capturedPiece = board.get(toRow, toCol);
        char movingPiece = board.get(fromRow, fromCol);

        board.set(toRow, toCol, movingPiece);
        board.set(fromRow, fromCol, ' ');

        Move move = null;
        if (kingSafe(board)) {
            move = new Move(fromRow, fromCol, toRow, toCol, capturedPiece, 'Q');
        }

        board.set(fromRow, fromCol, movingPiece);
        board.set(toRow, toCol, capturedPiece);

        return move;
    }
}