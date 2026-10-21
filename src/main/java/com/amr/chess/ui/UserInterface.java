package com.amr.chess.ui;

import com.amr.chess.engine.AlphaBeta;
import com.amr.chess.engine.GameLogic;
import com.amr.chess.engine.MoveGenerator;
import com.amr.chess.model.Board;
import com.amr.chess.model.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserInterface extends JPanel implements MouseListener {

    private final Board board;
    private final Map<Character, Image> pieceImages = new HashMap<>();
    private int squareSize;
    private int boardOffsetX, boardOffsetY;
    private int pressRow = -1, pressCol = -1;
    private boolean engineThinking = false;

    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color HIGHLIGHT = new Color(255, 255, 0, 80);

    public UserInterface(Board board) {
        this.board = board;
        loadPieceImages();
        setBackground(new Color(50, 50, 50));
        addMouseListener(this);
    }

    private void loadPieceImages() {
        String[] names = { "king", "queen", "rook", "bishop", "knight", "pawn" };
        char[] symbols = { 'K', 'Q', 'R', 'B', 'N', 'P' };

        for (int i = 0; i < names.length; i++) {
            pieceImages.put(symbols[i], loadImage("white-" + names[i] + ".png"));
            pieceImages.put(Character.toLowerCase(symbols[i]), loadImage("black-" + names[i] + ".png"));
        }
    }

    private Image loadImage(String name) {
        return new ImageIcon(Objects.requireNonNull(getClass().getResource(name))).getImage();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 800);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Dynamic sizing: fit the board to the window
        squareSize = Math.min(getWidth(), getHeight()) / 8;
        boardOffsetX = (getWidth() - squareSize * 8) / 2;
        boardOffsetY = (getHeight() - squareSize * 8) / 2;

        // Draw squares
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                g2.setColor((row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE);
                g2.fillRect(boardOffsetX + col * squareSize,
                        boardOffsetY + row * squareSize,
                        squareSize, squareSize);
            }
        }

        // Highlight the square the user pressed
        if (pressRow >= 0 && pressCol >= 0) {
            g2.setColor(HIGHLIGHT);
            g2.fillRect(boardOffsetX + pressCol * squareSize,
                    boardOffsetY + pressRow * squareSize,
                    squareSize, squareSize);
        }

        // Draw pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                char c = board.get(row, col);
                if (c == ' ')
                    continue;
                Image img = pieceImages.get(c);
                if (img != null) {
                    int padding = squareSize / 10; // small margin around each piece
                    g2.drawImage(img,
                            boardOffsetX + col * squareSize + padding,
                            boardOffsetY + row * squareSize + padding,
                            squareSize - padding * 2,
                            squareSize - padding * 2, this);
                }
            }
        }

        // "Thinking..." indicator
        if (engineThinking) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            String text = "Engine thinking...";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    getHeight() / 2);
        }
    }

    // ─── Mouse Handling ────────────────────────────────────────────────

    @Override
    public void mousePressed(MouseEvent e) {
        if (engineThinking)
            return;

        int col = (e.getX() - boardOffsetX) / squareSize;
        int row = (e.getY() - boardOffsetY) / squareSize;

        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            pressRow = row;
            pressCol = col;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (engineThinking || pressRow < 0)
            return;

        int col = (e.getX() - boardOffsetX) / squareSize;
        int row = (e.getY() - boardOffsetY) / squareSize;
        pressRow = -1;
        pressCol = -1;

        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            repaint();
            return;
        }

        // Find matching legal move
        List<Move> legalMoves = MoveGenerator.generatePossibleMoves(board);
        Move userMove = findMove(legalMoves, pressRow, pressCol, row, col);

        if (userMove == null) {
            repaint();
            return;
        }

        // Make the human's move IMMEDIATELY so the GUI updates
        board.makeMove(userMove);
        board.flipBoard();
        repaint();

        // Run engine search on a BACKGROUND thread so the GUI stays responsive
        engineThinking = true;
        repaint();

        SwingWorker<Move, Void> worker = new SwingWorker<>() {
            private long elapsed;

            @Override
            protected Move doInBackground() {
                long start = System.currentTimeMillis();

                List<Move> engineMoves = MoveGenerator.generatePossibleMoves(board);
                if (engineMoves.isEmpty())
                    return null;

                Move best = AlphaBeta.findBestMove(board);
                elapsed = System.currentTimeMillis() - start;
                return best;
            }

            @Override
            protected void done() {
                engineThinking = false;
                try {
                    Move bestMove = get();

                    if (bestMove == null) {
                        if (GameLogic.kingSafe(board)) {
                            JOptionPane.showMessageDialog(UserInterface.this,
                                    "Stalemate! It's a draw.");
                        } else {
                            JOptionPane.showMessageDialog(UserInterface.this,
                                    "Checkmate! You WON! 🎉");
                        }
                    } else {
                        System.out.println("That took " + elapsed + " ms | "
                                + AlphaBeta.c / 1000 + "K moves searched");

                        board.makeMove(bestMove);
                        board.flipBoard();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                repaint();
            }
        };
        worker.execute();
    }

    private Move findMove(List<Move> moves, int fromRow, int fromCol, int toRow, int toCol) {
        for (Move m : moves) {
            if (m.fromRow() == fromRow && m.fromCol() == fromCol
                    && m.toRow() == toRow && m.toCol() == toCol) {
                return m;
            }
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}