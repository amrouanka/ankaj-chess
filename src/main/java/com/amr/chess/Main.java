package com.amr.chess;

import com.amr.chess.engine.AlphaBeta;
import com.amr.chess.model.Board;
import com.amr.chess.model.Move;
import com.amr.chess.ui.UserInterface;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startGame);
    }

    private static void startGame() {
        Board board = new Board();

        Object[] options = { "Black", "White" };
        int choice = JOptionPane.showOptionDialog(null,
                "Who should play as white?", "Amr Chess Engine",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);

        boolean humanIsWhite = (choice != 0); // 0 = Black, 1 = White

        JFrame frame = new JFrame("Amr Chess Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        UserInterface ui = new UserInterface(board, humanIsWhite);
        frame.add(ui);
        frame.setSize(800, 820);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (!humanIsWhite) {
            ui.startEngineMove(frame);
        }
    }
}