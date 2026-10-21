package com.amr.chess.engine;

import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

import java.util.ArrayList;
import java.util.List;

public class AlphaBeta {

    public static int c = 0;
    public static int globalDepth = 4;

    public static Move findBestMove(Board board) {
        c = 0;
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);
        if (moves.isEmpty())
            return null;

        moves = orderMoves(board, moves);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            board.makeMove(move);
            board.flipBoard();
            int score = -negamax(board, globalDepth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            board.flipBoard();
            board.undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private static int negamax(Board board, int depth, int alpha, int beta) {
        List<Move> moves = MoveGenerator.generatePossibleMoves(board);
        if (depth == 0 || moves.isEmpty()) {
            return Evaluation.rating(board, moves.size());
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

    private static List<Move> orderMoves(Board board, List<Move> moves) {
        record ScoredMove(Move move, int score) {
        }
        List<ScoredMove> scored = new ArrayList<>();

        for (Move move : moves) {
            int score;
            if (move.isPromotion()) {
                score = 10000;
            } else {
                board.makeMove(move);
                int bonus = GameLogic.kingSafe(board) ? 0 : 10;
                score = (Evaluation.rating(board, -1) + bonus) * -1;
                board.undoMove(move);
            }
            scored.add(new ScoredMove(move, score));
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        return scored.stream().map(ScoredMove::move).toList();
    }
}