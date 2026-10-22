package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

import java.util.ArrayList;
import java.util.List;

public class AlphaBeta {

    public static int c = 0;
    public static int globalDepth = 4;
    private static final int INF = 1_000_000;
    private static final int MAX_QUIESCE_DEPTH = 6;

    public static Move findBestMove(Board board) {
        c = 0;
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);
        if (moves.isEmpty())
            return null;

        moves = orderMoves(board, moves);
        Move bestMove = null;
        int bestScore = -INF;
        int alpha = -INF;

        for (Move move : moves) {
            board.makeMove(move);
            board.flipBoard();
            int score = -negamax(board, globalDepth - 1, -INF, -alpha);
            board.flipBoard();
            board.undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha)
                alpha = score;
        }
        return bestMove;
    }

    private static int negamax(Board board, int depth, int alpha, int beta) {
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);

        if (moves.isEmpty()) {
            return GameLogic.kingSafe(board) ? 0 : -(Evaluation.CHECKMATE + depth);
        }
        if (depth == 0) {
            return quiescence(board, alpha, beta, 0);
        }

        for (Move move : moves) {
            board.makeMove(move);
            board.flipBoard();
            int score = -negamax(board, depth - 1, -beta, -alpha);
            board.flipBoard();
            board.undoMove(move);
            c++;

            if (score > alpha)
                alpha = score;
            if (alpha >= beta)
                break;
        }
        return alpha;
    }

    /**
     * Quiescence search: extends the search for captures and promotions
     * so the engine doesn't evaluate positions where pieces are hanging.
     *
     * @param qDepth how deep into quiescence we've gone (prevents explosions)
     */
    private static int quiescence(Board board, int alpha, int beta, int qDepth) {
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);

        if (moves.isEmpty()) {
            return GameLogic.kingSafe(board) ? 0 : -Evaluation.CHECKMATE;
        }

        int standPat = Evaluation.rating(board);
        if (standPat >= beta)
            return beta;
        if (standPat > alpha)
            alpha = standPat;

        // Stop extending if we've gone deep enough
        if (qDepth >= MAX_QUIESCE_DEPTH)
            return alpha;

        for (Move move : moves) {
            if (!move.isCapture() && !move.isPromotion())
                continue;

            board.makeMove(move);
            board.flipBoard();
            int score = -quiescence(board, -beta, -alpha, qDepth + 1);
            board.flipBoard();
            board.undoMove(move);

            if (score > alpha)
                alpha = score;
            if (alpha >= beta)
                break;
        }
        return alpha;
    }

    // ─── Move ordering: captures first (MVV-LVA) ──────────────────

    private static List<Move> orderMoves(Board board, List<Move> moves) {
        List<Move> ordered = new ArrayList<>(moves);
        ordered.sort((a, b) -> Integer.compare(moveScore(board, b), moveScore(board, a)));
        return ordered;
    }

    private static int moveScore(Board board, Move move) {
        int score = 0;
        if (move.isPromotion())
            score += 900;
        if (move.isCapture()) {
            score += 10 * pieceValue(move.capturedPiece())
                    - pieceValue(board.get(move.fromRow(), move.fromCol()));
        }
        return score;
    }

    private static int pieceValue(char piece) {
        return switch (Character.toUpperCase(piece)) {
            case 'P' -> 100;
            case 'N', 'B' -> 325;
            case 'R' -> 500;
            case 'Q' -> 975;
            default -> 0;
        };
    }
}