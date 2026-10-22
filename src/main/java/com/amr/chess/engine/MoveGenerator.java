package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    private static final int[][] KNIGHT_OFFSETS = {
            { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 },
            { 1, -2 }, { 1, 2 }, { 2, -1 }, { 2, 1 }
    };

    private static final int[][] BISHOP_DIRS = {
            { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 }
    };

    private static final int[][] ROOK_DIRS = {
            { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }
    };

    private static final int[][] QUEEN_DIRS = {
            { -1, -1 }, { -1, 0 }, { -1, 1 },
            { 0, -1 }, { 0, 1 },
            { 1, -1 }, { 1, 0 }, { 1, 1 }
    };

    public static List<Move> generatePossibleMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        board.updateKingPositions();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                switch (board.get(row, col)) {
                    case 'P' -> moves.addAll(pawnMoves(board, row, col));
                    case 'N' -> moves.addAll(knightMoves(board, row, col));
                    case 'B' -> moves.addAll(bishopMoves(board, row, col));
                    case 'R' -> moves.addAll(rookMoves(board, row, col));
                    case 'Q' -> moves.addAll(queenMoves(board, row, col));
                    case 'K' -> moves.addAll(kingMoves(board, row, col));
                }
            }
        }
        return moves;
    }

    // ─── Pawn ──────────────────────────────────────────────────────────

    private static List<Move> pawnMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();

        // Diagonal captures
        for (int dir = -1; dir <= 1; dir += 2) {
            if (!GameLogic.isValidPosition(row - 1, col + dir))
                continue;
            if (Character.isLowerCase(board.get(row - 1, col + dir))) {
                if (row > 1) {
                    addIfLegal(moves, GameLogic.checkMove(board, row, col, row - 1, col + dir));
                } else {
                    addIfLegal(moves, GameLogic.checkPromotionMove(board, row, col, 0, col + dir));
                }
            }
        }

        // En passant capture
        int epCol = board.getEnPassantCol();
        if (epCol >= 0 && row == 3 && Math.abs(col - epCol) == 1) {
            addIfLegal(moves, GameLogic.checkEnPassantMove(board, row, col, 2, epCol));
        }

        // Forward moves
        if (GameLogic.isValidPosition(row - 1, col) && board.get(row - 1, col) == ' ') {
            if (row > 1) {
                addIfLegal(moves, GameLogic.checkMove(board, row, col, row - 1, col));
            } else {
                addIfLegal(moves, GameLogic.checkPromotionMove(board, row, col, 0, col));
            }

            // Double push from starting row
            if (row == 6 && board.get(row - 2, col) == ' ') {
                addIfLegal(moves, GameLogic.checkMove(board, row, col, row - 2, col));
            }
        }

        return moves;
    }

    // ─── Knight ────────────────────────────────────────────────────────

    private static List<Move> knightMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        for (int[] offset : KNIGHT_OFFSETS) {
            int newRow = row + offset[0];
            int newCol = col + offset[1];
            if (GameLogic.isValidPosition(newRow, newCol)
                    && !Character.isUpperCase(board.get(newRow, newCol))) {
                addIfLegal(moves, GameLogic.checkMove(board, row, col, newRow, newCol));
            }
        }
        return moves;
    }

    // ─── Sliding pieces ───────────────────────────────────────────────

    private static List<Move> bishopMoves(Board board, int row, int col) {
        return slidingMoves(board, row, col, BISHOP_DIRS);
    }

    private static List<Move> rookMoves(Board board, int row, int col) {
        return slidingMoves(board, row, col, ROOK_DIRS);
    }

    private static List<Move> queenMoves(Board board, int row, int col) {
        return slidingMoves(board, row, col, QUEEN_DIRS);
    }

    private static List<Move> slidingMoves(Board board, int row, int col, int[][] directions) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            while (GameLogic.isValidPosition(newRow, newCol)) {
                char target = board.get(newRow, newCol);
                if (target != ' ') {
                    if (Character.isLowerCase(target)) {
                        addIfLegal(moves, GameLogic.checkMove(board, row, col, newRow, newCol));
                    }
                    break;
                }
                addIfLegal(moves, GameLogic.checkMove(board, row, col, newRow, newCol));
                newRow += dir[0];
                newCol += dir[1];
            }
        }
        return moves;
    }

    // ─── King (including castling) ─────────────────────────────────────

    private static List<Move> kingMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int originalKingPos = board.getWhiteKingPos();

        // Normal king moves (one square in any direction)
        for (int rowDir = -1; rowDir <= 1; rowDir++) {
            for (int colDir = -1; colDir <= 1; colDir++) {
                if (rowDir == 0 && colDir == 0)
                    continue;
                int newRow = row + rowDir;
                int newCol = col + colDir;
                if (!GameLogic.isValidPosition(newRow, newCol))
                    continue;
                if (Character.isUpperCase(board.get(newRow, newCol)))
                    continue;

                board.setWhiteKingPos(newRow * 8 + newCol);
                addIfLegal(moves, GameLogic.checkMove(board, row, col, newRow, newCol));
                board.setWhiteKingPos(originalKingPos);
            }
        }

        // Castling (only from the home row)
        if (row == 7) {
            addCastlingMoves(board, row, col, moves);
        }

        return moves;
    }

    /**
     * Generates castling moves if all conditions are met.
     * Detects castling by checking if the king can move 2 squares toward a rook.
     */
    private static void addCastlingMoves(Board board, int row, int kingCol, List<Move> moves) {
        int rights = board.getCastlingRights();

        // King must not be in check to castle
        if (GameLogic.isSquareAttacked(board, row, kingCol))
            return;

        // ── Kingside ──
        if ((rights & Board.WHITE_KINGSIDE) != 0) {
            int col1 = kingCol + 1; // square king passes through
            int col2 = kingCol + 2; // square king lands on
            if (col2 <= 7
                    && board.get(row, col1) == ' '
                    && board.get(row, col2) == ' '
                    && board.get(row, 7) == 'R'
                    && !GameLogic.isSquareAttacked(board, row, col1)) {
                addIfLegal(moves, GameLogic.checkCastlingMove(
                        board, row, kingCol, col2, 7, col1));
            }
        }

        // ── Queenside ──
        if ((rights & Board.WHITE_QUEENSIDE) != 0) {
            int col1 = kingCol - 1; // square king passes through
            int col2 = kingCol - 2; // square king lands on
            int col3 = kingCol - 3; // square rook passes through (must be empty)
            if (col2 >= 0 && col3 >= 0
                    && board.get(row, col1) == ' '
                    && board.get(row, col2) == ' '
                    && board.get(row, col3) == ' '
                    && board.get(row, 0) == 'R'
                    && !GameLogic.isSquareAttacked(board, row, col1)) {
                addIfLegal(moves, GameLogic.checkCastlingMove(
                        board, row, kingCol, col2, 0, col1));
            }
        }
    }

    private static void addIfLegal(List<Move> moves, Move move) {
        if (move != null)
            moves.add(move);
    }
}