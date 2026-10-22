package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;
import java.util.List;

/**
 * Perft (Performance Test) — counts leaf nodes at a given depth.
 *
 * Used to verify move generation correctness by comparing against
 * known-correct values from reference engines.
 *
 * Usage: mvn -q exec:java -Dexec.mainClass="com.amr.chess.engine.Perft"
 */
public class Perft {

    // ─── Core perft ────────────────────────────────────────────────

    /**
     * Recursively counts all leaf nodes at the given depth.
     * Follows the same make → flip → recurse → flip → undo pattern
     * that AlphaBeta uses, so it tests the exact same code path.
     */
    public static long perft(Board board, int depth) {
        if (depth == 0)
            return 1;

        List<Move> moves = MoveGenerator.generatePossibleMoves(board);

        // Leaf-node optimization: at depth 1, just count the moves
        // instead of making/unmaking each one.
        if (depth == 1)
            return moves.size();

        long nodes = 0;
        for (Move move : moves) {
            board.makeMove(move);
            board.flipBoard();
            nodes += perft(board, depth - 1);
            board.flipBoard();
            board.undoMove(move);
        }
        return nodes;
    }

    // ─── Divide (the real debugging tool) ──────────────────────────

    /**
     * Shows the perft count for EACH root move individually.
     * Compare each line against a reference to find exactly which
     * move is generating the wrong subtree.
     */
    public static void divide(Board board, int depth) {
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);
        long total = 0;

        System.out.println("Move            | Nodes");
        System.out.println("----------------|-------------");

        for (Move move : moves) {
            board.makeMove(move);
            board.flipBoard();
            long count = (depth <= 1) ? 1 : perft(board, depth - 1);
            board.flipBoard();
            board.undoMove(move);

            System.out.printf("%-15s | %,d%n", moveToString(move), count);
            total += count;
        }

        System.out.println("----------------|-------------");
        System.out.printf("Total           | %,d%n", total);
    }

    // ─── Move display ──────────────────────────────────────────────

    private static String moveToString(Move move) {
        String from = "" + (char) ('a' + move.fromCol()) + (8 - move.fromRow());
        String to = "" + (char) ('a' + move.toCol()) + (8 - move.toRow());
        String extra = "";
        if (move.isCastling())
            extra = " (castle)";
        if (move.isEnPassant())
            extra = " (ep)";
        if (move.isPromotion())
            extra = " (=" + move.promotionPiece() + ")";
        return from + to + extra;
    }

    // ─── FEN parser (minimal, for test positions) ──────────────────

    /**
     * Creates a Board from a FEN string.
     * Only handles: piece placement, active color, castling, en passant.
     * (Ignores half-move clock and full-move number.)
     *
     * FEN example: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
     */
    public static Board fromFEN(String fen) {
        Board board = new Board(); // starts with initial position, we overwrite it
        String[] parts = fen.split(" ");
        String[] rows = parts[0].split("/");

        // Parse piece placement
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    for (int i = 0; i < ch - '0'; i++) {
                        board.set(r, c++, ' ');
                    }
                } else {
                    board.set(r, c++, ch);
                }
            }
        }

        // If it's black's turn, flip the board so the side to move
        // is always "white" (uppercase) — matching the flip convention
        if (parts.length > 1 && parts[1].equals("b")) {
            board.flipBoard();
        }

        // Parse castling rights
        int rights = 0;
        if (parts.length > 2 && !parts[2].equals("-")) {
            for (char ch : parts[2].toCharArray()) {
                switch (ch) {
                    case 'K' -> rights |= Board.WHITE_KINGSIDE;
                    case 'Q' -> rights |= Board.WHITE_QUEENSIDE;
                    case 'k' -> rights |= Board.BLACK_KINGSIDE;
                    case 'q' -> rights |= Board.BLACK_QUEENSIDE;
                }
            }
        }
        board.setCastlingRights(rights);

        // Parse en passant target square
        if (parts.length > 3 && !parts[3].equals("-")) {
            int epCol = parts[3].charAt(0) - 'a';
            board.setEnPassantCol(epCol);
        } else {
            board.setEnPassantCol(-1);
        }

        board.updateKingPositions();
        return board;
    }

    // ─── Main: run standard test positions ─────────────────────────

    public static void main(String[] args) {
        System.out.println("=== AnkaJ Perft Test ===\n");

        // ── Test 1: Starting position ──
        // Known-correct values (from Stockfish / chessprogramming wiki)
        // Depth 1: 20
        // Depth 2: 400
        // Depth 3: 8,902
        // Depth 4: 197,281
        System.out.println("--- Starting Position ---");
        Board start = fromFEN(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        for (int d = 1; d <= 4; d++) {
            long t0 = System.currentTimeMillis();
            long nodes = perft(start, d);
            long ms = System.currentTimeMillis() - t0;
            System.out.printf("Depth %d: %,10d nodes  (%d ms)%n", d, nodes, ms);
        }

        // ── Test 2: Kiwipete (the gold standard for castling/ep/promo bugs) ──
        // Known-correct values:
        // Depth 1: 48
        // Depth 2: 2,039
        // Depth 3: 97,862
        System.out.println("\n--- Kiwipete (castling + en passant + promotion) ---");
        Board kiwipete = fromFEN(
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        for (int d = 1; d <= 3; d++) {
            long t0 = System.currentTimeMillis();
            long nodes = perft(kiwipete, d);
            long ms = System.currentTimeMillis() - t0;
            System.out.printf("Depth %d: %,10d nodes  (%d ms)%n", d, nodes, ms);
        }

        // ── Divide on Kiwipete depth 2 (to isolate castling bugs) ──
        System.out.println("\n--- Kiwipete Divide (depth 2) ---");
        Board kiwipete2 = fromFEN(
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        divide(kiwipete2, 2);
    }
}