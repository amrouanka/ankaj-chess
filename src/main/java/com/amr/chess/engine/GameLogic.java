package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

public class GameLogic {

    public static boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // ─── Attack detection ──────────────────────────────────────────────

    /**
     * Returns true if the given square is attacked by any enemy (lowercase) piece.
     */
    public static boolean isSquareAttacked(Board board, int row, int col) {
        // Diagonals (bishop/queen)
        for (int rowDir = -1; rowDir <= 1; rowDir += 2) {
            for (int colDir = -1; colDir <= 1; colDir += 2) {
                if (isAttackedBySlider(board, row, col, rowDir, colDir, 'b', 'q')) {
                    return true;
                }
            }
        }

        // Straights (rook/queen)
        int[][] straightDirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] dir : straightDirs) {
            if (isAttackedBySlider(board, row, col, dir[0], dir[1], 'r', 'q')) {
                return true;
            }
        }

        // Knights
        int[][] knightOffsets = {
                { -1, -2 }, { 1, 2 }, { -2, -1 }, { 2, 1 },
                { 1, -2 }, { -1, 2 }, { 2, -1 }, { -2, 1 }
        };
        for (int[] offset : knightOffsets) {
            int r = row + offset[0], c = col + offset[1];
            if (isValidPosition(r, c) && board.get(r, c) == 'n')
                return true;
        }

        // Pawns (enemy pawns attack diagonally toward higher rows)
        if (row >= 1) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int c = col + dc;
                if (isValidPosition(row - 1, c) && board.get(row - 1, c) == 'p')
                    return true;
            }
        }

        // Enemy king
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0)
                    continue;
                int r = row + dr, c = col + dc;
                if (isValidPosition(r, c) && board.get(r, c) == 'k')
                    return true;
            }
        }

        return false;
    }

    private static boolean isAttackedBySlider(Board board, int row, int col,
            int rowDir, int colDir,
            char attacker1, char attacker2) {
        int r = row + rowDir;
        int c = col + colDir;
        while (isValidPosition(r, c) && board.get(r, c) == ' ') {
            r += rowDir;
            c += colDir;
        }
        if (!isValidPosition(r, c))
            return false;
        char piece = board.get(r, c);
        return piece == attacker1 || piece == attacker2;
    }

    /**
     * Returns true if the white king is not under attack.
     */
    public static boolean kingSafe(Board board) {
        return !isSquareAttacked(board,
                board.getWhiteKingPos() / 8,
                board.getWhiteKingPos() % 8);
    }

    // ─── Move legality checks ──────────────────────────────────────────

    /**
     * Tries a normal move, checks if the king is safe, then undoes it.
     * Returns the Move if legal, or null if it leaves the king in check.
     */
    public static Move checkMove(Board board, int fromRow, int fromCol,
            int toRow, int toCol) {
        char capturedPiece = board.get(toRow, toCol);
        char movingPiece = board.get(fromRow, fromCol);
        int prevCastling = board.getCastlingRights();
        int prevEP = board.getEnPassantCol();

        board.set(toRow, toCol, movingPiece);
        board.set(fromRow, fromCol, ' ');

        Move move = null;
        if (kingSafe(board)) {
            move = new Move(fromRow, fromCol, toRow, toCol,
                    capturedPiece, prevCastling, prevEP);
        }

        board.set(fromRow, fromCol, movingPiece);
        board.set(toRow, toCol, capturedPiece);
        return move;
    }

    /**
     * Checks a promotion move (always to queen).
     */
    public static Move checkPromotionMove(Board board, int fromRow, int fromCol,
            int toRow, int toCol) {
        char capturedPiece = board.get(toRow, toCol);
        char movingPiece = board.get(fromRow, fromCol);
        int prevCastling = board.getCastlingRights();
        int prevEP = board.getEnPassantCol();

        board.set(toRow, toCol, movingPiece);
        board.set(fromRow, fromCol, ' ');

        Move move = null;
        if (kingSafe(board)) {
            move = new Move(fromRow, fromCol, toRow, toCol,
                    capturedPiece, 'Q', prevCastling, prevEP);
        }

        board.set(fromRow, fromCol, movingPiece);
        board.set(toRow, toCol, capturedPiece);
        return move;
    }

    /**
     * Checks an en passant capture.
     * The captured pawn is on (fromRow, toCol), NOT on the destination.
     */
    public static Move checkEnPassantMove(Board board, int fromRow, int fromCol,
            int toRow, int toCol) {
        int prevCastling = board.getCastlingRights();
        int prevEP = board.getEnPassantCol();

        // Make the en passant move
        board.set(toRow, toCol, 'P');
        board.set(fromRow, fromCol, ' ');
        board.set(fromRow, toCol, ' '); // remove captured pawn from the side

        Move move = null;
        if (kingSafe(board)) {
            move = Move.enPassant(fromRow, fromCol, toRow, toCol, prevCastling, prevEP);
        }

        // Undo
        board.set(fromRow, fromCol, 'P');
        board.set(toRow, toCol, ' ');
        board.set(fromRow, toCol, 'p'); // restore captured pawn
        return move;
    }

    /**
     * Checks a castling move. Moves both king and rook, checks safety, undoes.
     */
    public static Move checkCastlingMove(Board board, int row, int kingFromCol,
            int kingToCol, int rookFromCol, int rookToCol) {
        int prevCastling = board.getCastlingRights();
        int prevEP = board.getEnPassantCol();

        // Make the castling move (both pieces)
        board.set(row, kingToCol, 'K');
        board.set(row, kingFromCol, ' ');
        board.set(row, rookToCol, 'R');
        board.set(row, rookFromCol, ' ');
        board.setWhiteKingPos(row * 8 + kingToCol);

        Move move = null;
        if (kingSafe(board)) {
            move = Move.castling(row, kingFromCol, row, kingToCol, prevCastling, prevEP);
        }

        // Undo
        board.set(row, kingFromCol, 'K');
        board.set(row, kingToCol, ' ');
        board.set(row, rookFromCol, 'R');
        board.set(row, rookToCol, ' ');
        board.setWhiteKingPos(row * 8 + kingFromCol);
        return move;
    }
}