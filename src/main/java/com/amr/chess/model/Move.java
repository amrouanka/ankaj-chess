package com.amr.chess.model;

/**
 * Immutable representation of a chess move.
 * Stores the board state before the move for correct undo.
 */
public record Move(
        int fromRow,
        int fromCol,
        int toRow,
        int toCol,
        char capturedPiece,
        char promotionPiece,
        boolean enPassant,
        boolean castling,
        int previousCastlingRights,
        int previousEnPassantCol) {
    // ─── Convenience constructors ──────────────────────────────────────

    /** Normal move (no promotion, no en passant, no castling). */
    public Move(int fromRow, int fromCol, int toRow, int toCol,
            char capturedPiece, int prevCastling, int prevEP) {
        this(fromRow, fromCol, toRow, toCol, capturedPiece, ' ',
                false, false, prevCastling, prevEP);
    }

    /** Promotion move. */
    public Move(int fromRow, int fromCol, int toRow, int toCol,
            char capturedPiece, char promotionPiece,
            int prevCastling, int prevEP) {
        this(fromRow, fromCol, toRow, toCol, capturedPiece, promotionPiece,
                false, false, prevCastling, prevEP);
    }

    // ─── Static factories for special moves ────────────────────────────

    /** En passant capture. The captured pawn is NOT on the destination square. */
    public static Move enPassant(int fromRow, int fromCol, int toRow, int toCol,
            int prevCastling, int prevEP) {
        return new Move(fromRow, fromCol, toRow, toCol, 'p', ' ',
                true, false, prevCastling, prevEP);
    }

    /** Castling move. The rook movement is handled by Board.makeMove/undoMove. */
    public static Move castling(int fromRow, int fromCol, int toRow, int toCol,
            int prevCastling, int prevEP) {
        return new Move(fromRow, fromCol, toRow, toCol, ' ', ' ',
                false, true, prevCastling, prevEP);
    }

    // ─── Queries ───────────────────────────────────────────────────────

    public boolean isPromotion() {
        return promotionPiece != ' ';
    }

    public boolean isCapture() {
        return capturedPiece != ' ';
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    public boolean isCastling() {
        return castling;
    }
}