package com.amr.chess.model;

/**
 * Represents a chess move.
 * 
 * Normal move: new Move(6, 4, 4, 4, ' ') — e2 to e4, no capture
 * Capture: new Move(6, 4, 5, 3, 'p') — captures a black pawn
 * Promotion: new Move(1, 0, 0, 0, 'r', 'Q') — captures rook, promotes to queen
 */
public record Move(
        int fromRow,
        int fromCol,
        int toRow,
        int toCol,
        char capturedPiece,
        char promotionPiece) {
    /** Normal move (no promotion). */
    public Move(int fromRow, int fromCol, int toRow, int toCol, char capturedPiece) {
        this(fromRow, fromCol, toRow, toCol, capturedPiece, ' ');
    }

    public boolean isPromotion() {
        return promotionPiece != ' ';
    }

    public boolean isCapture() {
        return capturedPiece != ' ';
    }
}