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
    private final boolean humanIsWhite;
    private boolean isHumanTurn;
    private boolean engineThinking = false;

    private final Map<Character, Image> pieceImages = new HashMap<>();
    private int squareSize;
    private int boardOffsetX, boardOffsetY;
    private int pressRow = -1, pressCol = -1;

    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color HIGHLIGHT = new Color(255, 255, 0, 80);

    public UserInterface(Board board, boolean humanIsWhite) {
        this.board = board;
        this.humanIsWhite = humanIsWhite;
        this.isHumanTurn = humanIsWhite; // white moves first
        loadPieceImages();
        setBackground(new Color(50, 50, 50));
        addMouseListener(this);
    }

    /**
     * Called from Main when the human plays black and the engine moves first.
     */
    public void startEngineMove(JFrame frame) {
        final Board engineBoard = new Board(board);
        engineThinking = true;
        repaint();

        SwingWorker<Move, Void> worker = new SwingWorker<>() {
            private long elapsed;

            @Override
            protected Move doInBackground() {
                long start = System.currentTimeMillis();
                Move best = AlphaBeta.findBestMove(engineBoard);
                elapsed = System.currentTimeMillis() - start;
                return best;
            }

            @Override
            protected void done() {
                engineThinking = false;
                try {
                    Move bestMove = get();
                    if (bestMove != null) {
                        long nps = elapsed > 0 ? (AlphaBeta.c * 1000L) / elapsed : 0;
                        System.out.printf("Depth %d | %,d nodes | %,d ms | %,d nps%n",
                                AlphaBeta.globalDepth, AlphaBeta.c, elapsed, nps);
                        board.makeMove(bestMove);
                        board.flipBoard();
                        isHumanTurn = true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                repaint();
            }
        };
        worker.execute();
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

    // ─── Display coordinate mapping ────────────────────────────────────
    // The board data flips after every half-move (flip-board convention).
    // The GUI always renders from the human's perspective by reversing
    // the row/col mapping and swapping piece colors when it's the engine's turn.

    private int dataRow(int screenRow) {
        return isHumanTurn ? screenRow : 7 - screenRow;
    }

    private int dataCol(int screenCol) {
        return isHumanTurn ? screenCol : 7 - screenCol;
    }

    private char displayPiece(char c) {
        if (c == ' ')
            return c;
        // Swap piece colors when the data's "white" isn't the human's actual color
        boolean shouldSwap = (humanIsWhite != isHumanTurn);
        if (!shouldSwap)
            return c;
        return Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c);
    }

    // ─── Rendering ─────────────────────────────────────────────────────

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

        squareSize = Math.min(getWidth(), getHeight()) / 8;
        boardOffsetX = (getWidth() - squareSize * 8) / 2;
        boardOffsetY = (getHeight() - squareSize * 8) / 2;

        for (int screenRow = 0; screenRow < 8; screenRow++) {
            for (int screenCol = 0; screenCol < 8; screenCol++) {
                int dr = dataRow(screenRow);
                int dc = dataCol(screenCol);

                // Draw square
                g2.setColor((screenRow + screenCol) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE);
                g2.fillRect(boardOffsetX + screenCol * squareSize,
                        boardOffsetY + screenRow * squareSize,
                        squareSize, squareSize);

                // Highlight pressed square
                if (dr == pressRow && dc == pressCol) {
                    g2.setColor(HIGHLIGHT);
                    g2.fillRect(boardOffsetX + screenCol * squareSize,
                            boardOffsetY + screenRow * squareSize,
                            squareSize, squareSize);
                }

                // Draw piece
                char c = displayPiece(board.get(dr, dc));
                if (c == ' ')
                    continue;
                Image img = pieceImages.get(c);
                if (img != null) {
                    int pad = squareSize / 10;
                    g2.drawImage(img,
                            boardOffsetX + screenCol * squareSize + pad,
                            boardOffsetY + screenRow * squareSize + pad,
                            squareSize - pad * 2,
                            squareSize - pad * 2, this);
                }
            }
        }

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
        if (engineThinking || !isHumanTurn)
            return;

        int screenCol = (e.getX() - boardOffsetX) / squareSize;
        int screenRow = (e.getY() - boardOffsetY) / squareSize;

        if (screenRow >= 0 && screenRow < 8 && screenCol >= 0 && screenCol < 8) {
            pressRow = dataRow(screenRow);
            pressCol = dataCol(screenCol);
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (engineThinking || !isHumanTurn || pressRow < 0)
            return;

        int screenCol = (e.getX() - boardOffsetX) / squareSize;
        int screenRow = (e.getY() - boardOffsetY) / squareSize;

        int fromRow = pressRow;
        int fromCol = pressCol;
        pressRow = -1;
        pressCol = -1;

        if (screenRow < 0 || screenRow >= 8 || screenCol < 0 || screenCol >= 8) {
            repaint();
            return;
        }

        int toRow = dataRow(screenRow);
        int toCol = dataCol(screenCol);

        List<Move> legalMoves = MoveGenerator.generatePossibleMoves(board);
        Move userMove = findMove(legalMoves, fromRow, fromCol, toRow, toCol);

        if (userMove == null) {
            repaint();
            return;
        }

        // Human's move
        board.makeMove(userMove);
        board.flipBoard();
        isHumanTurn = false;
        repaint();

        // Engine's turn on a background thread
        final Board engineBoard = new Board(board);
        engineThinking = true;
        repaint();

        SwingWorker<Move, Void> worker = new SwingWorker<>() {
            private long elapsed;

            @Override
            protected Move doInBackground() {
                long start = System.currentTimeMillis();
                List<Move> engineMoves = MoveGenerator.generatePossibleMoves(engineBoard);
                if (engineMoves.isEmpty())
                    return null;
                Move best = AlphaBeta.findBestMove(engineBoard);
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
                        long nps = elapsed > 0 ? (AlphaBeta.c * 1000L) / elapsed : 0;
                        System.out.printf("Depth %d | %,d nodes | %,d ms | %,d nps%n",
                                AlphaBeta.globalDepth, AlphaBeta.c, elapsed, nps);
                        board.makeMove(bestMove);
                        board.flipBoard();
                        isHumanTurn = true;
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