package com.amr.chess;

import com.amr.chess.engine.AlphaBeta;
import com.amr.chess.model.Board;
import com.amr.chess.model.Move;
import com.amr.chess.ui.UserInterface;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        startGame();
    }

    private static void startGame() {
        Board board = new Board();
        board.display();

        JFrame frame = new JFrame("Amr Chess Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        UserInterface ui = new UserInterface(board);
        frame.add(ui);
        frame.setSize(900, 920);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        Object[] options = { "Black", "White" };
        int choice = JOptionPane.showOptionDialog(null,
                "Who should play as white?", "ABC Options",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);

        if (choice == 0) {
            playComputerMove(board, frame);
        }
    }

    private static void playComputerMove(Board board, JFrame frame) {
        Move bestMove = AlphaBeta.findBestMove(board);
        if (bestMove != null) {
            board.makeMove(bestMove);
            board.flipBoard();
            frame.repaint();
        }
    }
}