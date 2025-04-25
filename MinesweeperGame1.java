//Name: Harshith Kolli
//Student NUmber: 999904298
//Section:MSIS5103_032242S
/**
 * MinesweeperGame1.java
 * Entry point for launching the Minesweeper game.
 *
 * @author Harshith
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;

/**
 * The main class for starting the Minesweeper game.
 * Launches the difficulty selection window.
 *
 * @author Harshith
 */
public class MinesweeperGame1 {
    /**
     * Launches the GUI on the Event Dispatch Thread.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DifficultySelector selector = new DifficultySelector();
            selector.showDifficultyOptions();
        });
    }
}

/**
 * Handles the difficulty selection logic for Minesweeper game.
 * Creates a GameBoard object based on the selected difficulty.
 *
 * @author Harshith
 */
class DifficultySelector {
    /**
     * Displays difficulty options and starts the game with selected settings.
     */
    public void showDifficultyOptions() {
        String[] options = {"Beginner", "Advanced", "Expert"};
        int choice = JOptionPane.showOptionDialog(null, "Choose Difficulty:", "Minesweeper",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == -1) return;

        int rows = 6, cols = 9, mines = 11, seconds = 60;
        if (choice == 1) {
            rows = 12; cols = 18; mines = 36; seconds = 180;
        } else if (choice == 2) {
            rows = 21; cols = 26; mines = 92; seconds = 660;
        }

        GameBoard board = new GameBoard(rows, cols, mines, seconds); // Object instantiation
    }
}

/**
 * The main game board class that handles game logic, timer, UI and sound.
 * Initializes and manages the Minesweeper gameplay.
 *
 * @author Harshith
 */
class GameBoard {
    private JFrame frame;
    private JButton[][] buttons;
    private boolean[][] mines, revealed, flagged;
    private int rows, cols, mineCount, timeLimit, flagsPlaced = 0, timePassed = 0;
    private Timer timer;
    private JLabel timerLabel, flagLabel;

    /**
     * Constructs a GameBoard with specified size and mine configuration.
     * @param rows number of rows
     * @param cols number of columns
     * @param mineCount number of mines
     * @param timeLimit time limit in seconds
     */
    public GameBoard(int rows, int cols, int mineCount, int timeLimit) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.timeLimit = timeLimit;

        buttons = new JButton[rows][cols];
        mines = new boolean[rows][cols];
        revealed = new boolean[rows][cols];
        flagged = new boolean[rows][cols];

        initFrame();
        placeMines();
        startTimer();
    }

    /**
     * Initializes the frame and layout for the game UI.
     */
    private void initFrame() {
        frame = new JFrame("Minesweeper - Harshith Kolli");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        timerLabel = new JLabel("Time: 0s");
        flagLabel = new JLabel("Flags: 0/" + mineCount);
        topPanel.add(timerLabel, BorderLayout.WEST);
        topPanel.add(flagLabel, BorderLayout.EAST);

        JPanel boardPanel = new JPanel(new GridLayout(rows, cols));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(30, 30));
                final int i = r, j = c;
                btn.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (revealed[i][j]) {
                                autoRevealIfFlagsMatch(i, j);
                            } else {
                                handleLeftClick(i, j);
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            handleRightClick(i, j);
                        }
                    }
                });
                buttons[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Randomly places mines on the board.
     */
    private void placeMines() {
        Random rand = new Random();
        int placed = 0;
        while (placed < mineCount) {
            int r = rand.nextInt(rows), c = rand.nextInt(cols);
            if (!mines[r][c]) {
                mines[r][c] = true;
                placed++;
            }
        }
    }

    /**
     * Starts the game timer and updates the label every second.
     */
    private void startTimer() {
        timer = new Timer(1000, e -> {
            timePassed++;
            timerLabel.setText("Time: " + timePassed + "s");
            if (timePassed >= timeLimit) {
                timer.stop();
                revealMines();
                endGame("Time's up! You lost.");
            }
        });
        timer.start();
    }

    /**
     * Handles left-click interaction on a cell.
     * @param r row index
     * @param c column index
     */
    private void handleLeftClick(int r, int c) {
        if (flagged[r][c] || revealed[r][c]) return;
        if (mines[r][c]) {
            revealMines();
            endGame("Boom! You clicked a mine.");
            return;
        }
        revealCell(r, c);
        checkWin();
    }

    /**
     * Handles right-click interaction to toggle flags.
     * @param r row index
     * @param c column index
     */
    private void handleRightClick(int r, int c) {
        if (revealed[r][c]) return;
        flagged[r][c] = !flagged[r][c];
        buttons[r][c].setText(flagged[r][c] ? "F" : "");
        flagsPlaced += flagged[r][c] ? 1 : -1;
        flagLabel.setText("Flags: " + flagsPlaced + "/" + mineCount);
    }

    /**
     * Recursively reveals cells with 0 adjacent mines.
     * @param r row index
     * @param c column index
     */
    private void revealCell(int r, int c) {
        if (!inBounds(r, c) || revealed[r][c] || flagged[r][c]) return;
        revealed[r][c] = true;
        buttons[r][c].setEnabled(false);
        int count = countAdjacentMines(r, c);
        if (count > 0) {
            buttons[r][c].setText(String.valueOf(count));
        } else {
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++)
                    if (dr != 0 || dc != 0) revealCell(r + dr, c + dc);
        }
    }

    /**
     * Counts how many mines are adjacent to a cell.
     * @param r row index
     * @param c column index
     * @return number of adjacent mines
     */
    private int countAdjacentMines(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (inBounds(r + dr, c + dc) && mines[r + dr][c + dc]) count++;
        return count;
    }

    /**
     * Checks whether a cell coordinate is within board bounds.
     * @param r row index
     * @param c column index
     * @return true if within bounds, false otherwise
     */
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /**
     * Reveals all mines and plays explosion animation and sound.
     */
    private void revealMines() {
        playExplosionSound();
        Timer explosionTimer = new Timer(100, null);
        final int[] step = {0};

        explosionTimer.addActionListener(e -> {
            if (step[0] > 5) {
                ((Timer) e.getSource()).stop();
                return;
            }
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++) {
                    if (mines[i][j] && !flagged[i][j] && !revealed[i][j]) {
                        buttons[i][j].setBackground(step[0] % 2 == 0 ? Color.RED : Color.ORANGE);
                        buttons[i][j].setText("💥");
                        buttons[i][j].setFont(new Font("Arial", Font.BOLD, 20));
                    } else if (flagged[i][j] && !mines[i][j]) {
                        buttons[i][j].setText("❌");
                        buttons[i][j].setForeground(Color.RED);
                        buttons[i][j].setFont(new Font("Arial", Font.BOLD, 18));
                    }
                }
            step[0]++;
        });
        explosionTimer.start();
    }

    /**
     * Checks if the player has successfully revealed all safe cells.
     */
    private void checkWin() {
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                if (!mines[i][j] && !revealed[i][j]) return;

        timer.stop();
        endGame("Congratulations! You won.");
    }

    /**
     * Automatically reveals safe adjacent cells if surrounding flags match the number on a revealed cell.
     * @param r row index
     * @param c column index
     */
    private void autoRevealIfFlagsMatch(int r, int c) {
        int mineCount = countAdjacentMines(r, c);
        int flagCount = 0;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (inBounds(r + dr, c + dc) && flagged[r + dr][c + dc]) flagCount++;

        if (flagCount == mineCount) {
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr, nc = c + dc;
                    if (inBounds(nr, nc) && !flagged[nr][nc]) handleLeftClick(nr, nc);
                }
        }
    }

    /**
     * Ends the current game and offers to restart.
     * @param message game over message
     */
    private void endGame(String message) {
        int option = JOptionPane.showConfirmDialog(frame, message + "\nPlay again?", "Game Over", JOptionPane.YES_NO_OPTION);
        frame.dispose();
        if (option == JOptionPane.YES_OPTION) {
            DifficultySelector selector = new DifficultySelector();
            selector.showDifficultyOptions();
        }
    }

    /**
     * Plays an explosion sound when the game ends due to a mine.
     */
    private void playExplosionSound() {
        try {
            InputStream audioSrc = getClass().getResourceAsStream("/explosion.wav");
            if (audioSrc == null) {
                System.err.println("⚠️ explosion.wav file not found!");
                return;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
