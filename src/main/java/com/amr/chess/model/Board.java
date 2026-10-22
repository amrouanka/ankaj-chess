package com.amr.chess.model;

/**
 * Represents the chess board state.
 * 'P' = white pawn, 'r' = black rook, ' ' = empty.
 *
 * Castling rights are stored as 4 bits:
 * bit 0 (1): white kingside bit 2 (4): black kingside
 * bit 1 (2): white queenside bit 3 (8): black queenside
 * "White" and "black" are relative to the CURRENT board orientation
 * (the side to move is always "white" due to the flip convention).
 */
public class Board {

    public static final int WHITE_KINGSIDE = 1;
    public static final int WHITE_QUEENSIDE = 2;
    public static final int BLACK_KINGSIDE = 4;
    public static final int BLACK_QUEENSIDE = 8;
    public static final int ALL_CASTLING = 15;

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
    private int castlingRights;
    private int enPassantCol;

    public Board() {
        this.squares = new char[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(INITIAL_POSITION[r], 0, this.squares[r], 0, 8);
        }
        this.whiteKingPos = 60;
        this.blackKingPos = 4;
        this.castlingRights = ALL_CASTLING;
        this.enPassantCol = -1;
    }

    public Board(Board other) {
        this.squares = new char[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.squares[r], 0, this.squares[r], 0, 8);
        }
        this.whiteKingPos = other.whiteKingPos;
        this.blackKingPos = other.blackKingPos;
        this.castlingRights = other.castlingRights;
        this.enPassantCol = other.enPassantCol;
    }

    // ─── Accessors ─────────────────────────────────────────────────

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

    public int getCastlingRights() {
        return castlingRights;
    }

    public void setCastlingRights(int rights) {
        this.castlingRights = rights;
    }

    public int getEnPassantCol() {
        return enPassantCol;
    }

    public void setEnPassantCol(int col) {
        this.enPassantCol = col;
    }

    // ─── Move execution ────────────────────────────────────────────

    public void makeMove(Move move) {
        char piece = squares[move.fromRow()][move.fromCol()];

        if (move.isCastling()) {
            squares[move.toRow()][move.toCol()] = piece;
            squares[move.fromRow()][move.fromCol()] = ' ';
            whiteKingPos = move.toRow() * 8 + move.toCol();

            int rookFromCol = move.toCol() > move.fromCol() ? 7 : 0;
            int rookToCol = move.toCol() > move.fromCol()
                    ? move.toCol() - 1
                    : move.toCol() + 1;
            squares[move.fromRow()][rookToCol] = squares[move.fromRow()][rookFromCol];
            squares[move.fromRow()][rookFromCol] = ' ';

        } else if (move.isEnPassant()) {
            squares[move.toRow()][move.toCol()] = piece;
            squares[move.fromRow()][move.fromCol()] = ' ';
            squares[move.fromRow()][move.toCol()] = ' ';

        } else if (move.isPromotion()) {
            squares[move.toRow()][move.toCol()] = move.promotionPiece();
            squares[move.fromRow()][move.fromCol()] = ' ';

        } else {
            if (piece == 'K') {
                whiteKingPos = move.toRow() * 8 + move.toCol();
            }
            squares[move.toRow()][move.toCol()] = piece;
            squares[move.fromRow()][move.fromCol()] = ' ';
        }

        updateCastlingRights(move, piece);

        if (piece == 'P' && move.fromRow() - move.toRow() == 2) {
            enPassantCol = move.fromCol();
        } else {
            enPassantCol = -1;
        }
    }

    public void undoMove(Move move) {
        castlingRights = move.previousCastlingRights();
        enPassantCol = move.previousEnPassantCol();

        if (move.isCastling()) {
            squares[move.fromRow()][move.fromCol()] = 'K';
            squares[move.toRow()][move.toCol()] = ' ';
            whiteKingPos = move.fromRow() * 8 + move.fromCol();

            int rookFromCol = move.toCol() > move.fromCol() ? 7 : 0;
            int rookToCol = move.toCol() > move.fromCol()
                    ? move.toCol() - 1
                    : move.toCol() + 1;
            squares[move.fromRow()][rookFromCol] = 'R';
            squares[move.fromRow()][rookToCol] = ' ';

        } else if (move.isEnPassant()) {
            squares[move.fromRow()][move.fromCol()] = 'P';
            squares[move.toRow()][move.toCol()] = ' ';
            squares[move.fromRow()][move.toCol()] = 'p';

        } else if (move.isPromotion()) {
            squares[move.fromRow()][move.fromCol()] = 'P';
            squares[move.toRow()][move.toCol()] = move.capturedPiece();

        } else {
            char piece = squares[move.toRow()][move.toCol()];
            if (piece == 'K') {
                whiteKingPos = move.fromRow() * 8 + move.fromCol();
            }
            squares[move.fromRow()][move.fromCol()] = piece;
            squares[move.toRow()][move.toCol()] = move.capturedPiece();
        }
    }

    private void updateCastlingRights(Move move, char piece) {
        if (piece == 'K') {
            castlingRights &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
        }
        if (move.fromRow() == 7 && move.fromCol() == 7)
            castlingRights &= ~WHITE_KINGSIDE;
        if (move.fromRow() == 7 && move.fromCol() == 0)
            castlingRights &= ~WHITE_QUEENSIDE;
        if (move.toRow() == 0 && move.toCol() == 7)
            castlingRights &= ~BLACK_KINGSIDE;
        if (move.toRow() == 0 && move.toCol() == 0)
            castlingRights &= ~BLACK_QUEENSIDE;
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

    /**
     * Vertical flip: swap rows, keep columns, swap case.
     * This preserves the e-file (king) and a/h-files (rooks),
     * so castling geometry stays correct after the flip.
     */
    public void flipBoard() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 8; c++) {
                int oppRow = 7 - r;
                char temp = swapCase(squares[r][c]);
                squares[r][c] = swapCase(squares[oppRow][c]);
                squares[oppRow][c] = temp;
            }
        }

        int tempKing = blackKingPos;
        blackKingPos = (7 - whiteKingPos / 8) * 8 + whiteKingPos % 8;
        whiteKingPos = (7 - tempKing / 8) * 8 + tempKing % 8;

        castlingRights = ((castlingRights & 0x3) << 2) | ((castlingRights & 0xC) >> 2);
        // enPassantCol stays the same (vertical flip doesn't change columns)
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