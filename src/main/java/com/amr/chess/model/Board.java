package com.amr.chess.model;

import java.util.Arrays;

/**
 * Represents the chess board state.
 * Uses char[][] instead of String[][] for performance.
 * 'P' = white pawn, 'r' = black rook, ' ' = empty.
 */
public class Board {

    private static final char[][] INITIAL_POSITION = {
            { 'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r' },
            { 'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p' },
            { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' },
            { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' },
            { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' },
            { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' },
            { 'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P' },
            { 'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R' }
    };

    private final char[][] squares;
    private int whiteKingPos;
    private int blackKingPos;

    public Board() {
        this.squares = new char[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(INITIAL_POSITION[r], 0, this.squares[r], 0, 8);
        }
        this.whiteKingPos = 60; // e1
        this.blackKingPos = 4; // e8
    }

    /** Copy constructor — needed for search to clone positions. */
    public Board(Board other) {
        this.squares = new char[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.squares[r], 0, this.squares[r], 0, 8);
        }
        this.whiteKingPos = other.whiteKingPos;
        this.blackKingPos = other.blackKingPos;
    }

    // --- Accessors ---

    public char get(int row, int col) {
        return squares[row][col];
    }

    public char get(int square) {
        return squares[square / 8][square % 8];
    }

    public void set(int row, int col, char piece) {
        squares[row][col] = piece;
    }

    public int getWhiteKingPos() {
        return whiteKingPos;
    }

    public int getBlackKingPos() {
        return blackKingPos;
    }

    public void setWhiteKingPos(int pos) {
        this.whiteKingPos = pos;
    }

    public void setBlackKingPos(int pos) {
        this.blackKingPos = pos;
    }

    // --- Move execution ---

    public void makeMove(Move move) {
        char piece = squares[move.fromRow()][move.fromCol()];

        if (move.isPromotion()) {
            squares[move.toRow()][move.toCol()] = move.promotionPiece();
        } else {
            if (piece == 'K') {
                whiteKingPos = move.toRow() * 8 + move.toCol();
            }
            squares[move.toRow()][move.toCol()] = piece;
        }
        squares[move.fromRow()][move.fromCol()] = ' ';
    }

    public void undoMove(Move move) {
        char piece = squares[move.toRow()][move.toCol()];

        if (move.isPromotion()) {
            squares[move.fromRow()][move.fromCol()] = Character.isUpperCase(move.promotionPiece()) ? 'P' : 'p';
        } else {
            if (piece == 'K') {
                whiteKingPos = move.fromRow() * 8 + move.fromCol();
            }
            squares[move.fromRow()][move.fromCol()] = piece;
        }
        squares[move.toRow()][move.toCol()] = move.capturedPiece();
    }

    public void updateKingPositions() {
        for (int i = 0; i < 64; i++) {
            char c = squares[i / 8][i % 8];
            if (c == 'K')
                whiteKingPos = i;
            else if (c == 'k')
                blackKingPos = i;
        }
    }

    public void flipBoard() {
        for (int i = 0; i < 32; i++) {
            int row = i / 8, col = i % 8;
            int oppRow = 7 - row, oppCol = 7 - col;

            char temp = swapCase(squares[row][col]);
            squares[row][col] = swapCase(squares[oppRow][oppCol]);
            squares[oppRow][oppCol] = temp;
        }

        int tempKing = blackKingPos;
        blackKingPos = 63 - whiteKingPos;
        whiteKingPos = 63 - tempKing;
    }

    private static char swapCase(char c) {
        if (c == ' ')
            return ' ';
        return Character.isUpperCase(c)
                ? Character.toLowerCase(c)
                : Character.toUpperCase(c);
    }

    public void display() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                System.out.print(squares[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}