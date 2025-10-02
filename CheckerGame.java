import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CheckersGame extends JFrame {
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private GameLogic game;
    private boolean vsAI = true; // Human vs AI
    private boolean gameOver = false;

    public CheckersGame() {
        setTitle("Checkers Game (with Smarter AI)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);

        game = new GameLogic();
        boardPanel = new BoardPanel(game);
        statusLabel = new JLabel("Red's turn");

        JButton restartButton = new JButton("Restart");
        restartButton.addActionListener(e -> restartGame());

        JPanel topPanel = new JPanel();
        topPanel.add(statusLabel);
        topPanel.add(restartButton);

        add(topPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
    }

    private void restartGame() {
        game = new GameLogic();
        boardPanel.setGame(game);
        statusLabel.setText("Red's turn");
        gameOver = false;
        boardPanel.setEnabled(true);
        repaint();
    }

    private void nextTurn() {
        if (game.isGameOver()) {
            gameOver = true;
            String winner = game.getWinner();
            JOptionPane.showMessageDialog(this, winner + " wins!");
            boardPanel.setEnabled(false);
            return;
        }

        statusLabel.setText((game.redTurn ? "Red" : "Black") + "'s turn");

        if (vsAI && !game.redTurn && !gameOver) {
            boardPanel.setEnabled(false);
            Timer t = new Timer(300, e -> {
                Move best = Minimax.getBestMove(game, 5); // deeper search
                if (best != null) {
                    game.applyMove(best);
                    boardPanel.repaint();
                }
                boardPanel.setEnabled(true);
                nextTurn();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    class BoardPanel extends JPanel {
        private GameLogic game;
        private Point selected;

        public BoardPanel(GameLogic game) {
            this.game = game;
            setLayout(new GridLayout(8, 8));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    int cellSize = getWidth() / 8;
                    int row = e.getY() / cellSize;
                    int col = e.getX() / cellSize;
                    handleClick(row, col);
                }
            });
        }

        public void setGame(GameLogic game) {
            this.game = game;
            selected = null;
            repaint();
        }

        private void handleClick(int row, int col) {
            if (!game.redTurn) return;
            if (selected == null) {
                if (game.hasPiece(row, col) && game.isPlayersTurn(row, col)) {
                    selected = new Point(row, col);
                }
            } else {
                Move m = new Move(selected, new Point(row, col));
                if (game.isValidMove(m)) {
                    game.applyMove(m);
                    repaint();
                    selected = null;
                    nextTurn();
                } else {
                    selected = null;
                }
            }
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cellSize = getWidth() / 8;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    g.setColor((r + c) % 2 == 0 ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);

                    Piece p = game.board[r][c];
                    if (p != null) {
                        g.setColor(p.red ? Color.RED : Color.BLACK);
                        g.fillOval(c * cellSize + 10, r * cellSize + 10, cellSize - 20, cellSize - 20);
                        if (p.king) {
                            g.setColor(Color.YELLOW);
                            g.drawString("K", c * cellSize + cellSize / 2 - 5, r * cellSize + cellSize / 2);
                        }
                    }

                    if (selected != null && selected.x == r && selected.y == c) {
                        g.setColor(Color.GREEN);
                        g.drawRect(c * cellSize, r * cellSize, cellSize, cellSize);
                        g.drawRect(c * cellSize + 1, r * cellSize + 1, cellSize - 2, cellSize - 2);
                    }
                }
            }
        }
    }

    static class Move {
        Point from;
        Point to;
        int score;

        public Move(Point from, Point to) { this.from = from; this.to = to; }
        public Move(Point from, int score) { this.from = from; this.score = score; }
    }

    static class GameLogic {
        Piece[][] board = new Piece[8][8];
        boolean redTurn = true;

        public GameLogic() {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 8; c++)
                    if ((r + c) % 2 == 1) board[r][c] = new Piece(false); // Black
            for (int r = 5; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    if ((r + c) % 2 == 1) board[r][c] = new Piece(true); // Red
        }

        public boolean hasPiece(int r, int c) { return board[r][c] != null; }
        public boolean isPlayersTurn(int r, int c) { return board[r][c] != null && board[r][c].red == redTurn; }

        public boolean isValidMove(Move m) {
            if (m.from == null || m.to == null) return false;
            Piece p = board[m.from.x][m.from.y];
            if (p == null || board[m.to.x][m.to.y] != null) return false;
            int dr = m.to.x - m.from.x;
            int dc = m.to.y - m.from.y;

            if (Math.abs(dr) == 1 && Math.abs(dc) == 1) return true;
            if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {
                int midR = (m.from.x + m.to.x) / 2;
                int midC = (m.from.y + m.to.y) / 2;
                Piece mid = board[midR][midC];
                if (mid != null && mid.red != p.red) return true;
            }
            return false;
        }

        public void applyMove(Move m) {
            Piece p = board[m.from.x][m.from.y];
            board[m.from.x][m.from.y] = null;
            board[m.to.x][m.to.y] = p;

            if (Math.abs(m.to.x - m.from.x) == 2) {
                int midR = (m.from.x + m.to.x) / 2;
                int midC = (m.from.y + m.to.y) / 2;
                board[midR][midC] = null;
            }

            if ((p.red && m.to.x == 0) || (!p.red && m.to.x == 7)) p.king = true;
            redTurn = !redTurn;
        }

        public boolean isGameOver() { return getAllValidMoves(true).isEmpty() || getAllValidMoves(false).isEmpty(); }

        public String getWinner() {
            if (getAllValidMoves(true).isEmpty()) return "Black";
            if (getAllValidMoves(false).isEmpty()) return "Red";
            return "Draw";
        }

        public List<Move> getAllValidMoves(boolean forRed) {
            List<Move> moves = new ArrayList<>();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = board[r][c];
                    if (p != null && p.red == forRed) {
                        int dir = p.red ? -1 : 1;

                        // jumps first
                        for (int dc = -2; dc <= 2; dc += 4) {
                            int nr = r + 2 * dir;
                            int nc = c + dc;
                            int mr = r + dir;
                            int mc = c + dc / 2;
                            if (inBounds(nr, nc) && board[nr][nc] == null && inBounds(mr, mc) && board[mr][mc] != null && board[mr][mc].red != p.red)
                                moves.add(new Move(new Point(r, c), new Point(nr, nc)));
                        }

                        // simple moves
                        for (int dc = -1; dc <= 1; dc += 2) {
                            int nr = r + dir;
                            int nc = c + dc;
                            if (inBounds(nr, nc) && board[nr][nc] == null)
                                moves.add(new Move(new Point(r, c), new Point(nr, nc)));
                        }
                    }
                }
            }
            return moves;
        }

        private boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

        public GameLogic copy() {
            GameLogic g = new GameLogic();
            g.board = new Piece[8][8];
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    if (this.board[r][c] != null)
                        g.board[r][c] = this.board[r][c].copy();
            g.redTurn = this.redTurn;
            return g;
        }
    }

    static class Piece {
        boolean red;
        boolean king;

        public Piece(boolean red) { this.red = red; this.king = false; }
        public Piece copy() { Piece p = new Piece(this.red); p.king = this.king; return p; }
    }

    static class Minimax {
        public static Move getBestMove(GameLogic game, int depth) {
            List<Move> moves = game.getAllValidMoves(false); // AI is black
            Move bestMove = null;
            int maxEval = Integer.MIN_VALUE;

            for (Move m : moves) {
                GameLogic copy = game.copy();
                copy.applyMove(m);
                int eval = minimax(copy, depth - 1, false);
                if (eval > maxEval) {
                    maxEval = eval;
                    bestMove = m;
                }
            }

            if (bestMove != null) bestMove.score = maxEval;
            return bestMove;
        }

        private static int minimax(GameLogic game, int depth, boolean maximizing) {
            if (depth == 0 || game.isGameOver()) return evaluateBoard(game);

            List<Move> moves = maximizing ? game.getAllValidMoves(false) : game.getAllValidMoves(true);
            if (moves.isEmpty()) return evaluateBoard(game);

            int bestEval = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for (Move m : moves) {
                GameLogic copy = game.copy();
                copy.applyMove(m);
                int eval = minimax(copy, depth - 1, !maximizing);
                bestEval = maximizing ? Math.max(bestEval, eval) : Math.min(bestEval, eval);
            }
            return bestEval;
        }

        private static int evaluateBoard(GameLogic game) {
            int score = 0;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = game.board[r][c];
                    if (p != null) {
                        int val = p.king ? 3 : 1;
                        int edgeBonus = (c == 0 || c == 7) ? 1 : 0;
                        score += p.red ? -val : val;
                        score += p.red ? -edgeBonus : edgeBonus;
                    }
                }
            }
            return score;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CheckersGame().setVisible(true));
    }
}
