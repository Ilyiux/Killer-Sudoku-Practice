import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Panel extends JPanel implements Runnable, KeyListener {
    public final static int WIDTH = 900;
    public final static int HEIGHT = 900;
    public int FPS = 120;
    private Thread thread;
    private Random random = new Random();

    private static final int BOX_SIZE = 200;
    private static final int BOX_OFFSET = 50; // offset from top left corner
    private static final int[] insideCells = {2, 11, 18, 19, 20};
    private static final int[] insideAdjacents = {3, 12, 27, 28, 29};
    private static final int[] allInsideCells = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int[] outsideCells = {3, 12, 21, 27, 28, 29};
    private static final int[] outsideAdjacents = {2, 11, 20, 18, 19, 20};
    private static final int[] allOutsideCells = {3, 12, 21, 27, 28, 29, 30};

    private boolean showNumbers = false;

    private double answeredCorrect = 0.0;
    private double answeredIncorrect = 0.0;

    private int correct = 0;
    private int incorrect = 0;

    private boolean insideCell;
    private int[][] sudokuGrid = new int[9][9];
    private List<List<Integer>> cages;
    private int targetCell;

    public Panel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.WHITE);
        this.setDoubleBuffered(false);

        this.setFocusable(true);
        init();
    }

    public void startThread() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / (double) FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (thread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;

            lastTime = currentTime;

            if (delta > 1) {
                update();
                repaint();
                delta --;
            }
        }
    }

    private void nextGrid() {
        generateSudokuGrid();
        insideCell = random.nextBoolean();
        generateTargetCellAndCages();
    }

    // use a form of wave function collapse to fill out the grid
    private void generateSudokuGrid() {
        // fill out the grid with 0
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                sudokuGrid[x][y] = 0;
            }
        }

        Map<Integer, List<Integer>> blacklists = new HashMap<>();

        int i = 0;
        while (i < 81) {
            int x = i % 9;
            int y = i / 9;

            blacklists.putIfAbsent(i, new ArrayList<>());
            List<Integer> blacklist = blacklists.get(i);

            if (sudokuGrid[x][y] != 0) {
                blacklist.add(sudokuGrid[x][y]);
                sudokuGrid[x][y] = 0;
            }

            List<Integer> possibilities = getPossibilities(x, y);
            possibilities.removeAll(blacklist);

            if (possibilities.isEmpty()) {
                // backtrack
                i--;
            } else {
                // continue
                Collections.shuffle(possibilities);
                sudokuGrid[x][y] = possibilities.get(0);
                blacklists.put(i + 1, new ArrayList<>());
                i++;
            }
        }
    }

    private List<Integer> getPossibilities(int x, int y) {
        List<Integer> possibilities = new ArrayList<>();
        for (int v = 1; v <= 9; v++) {
            possibilities.add(v);
        }

        for (int c = 0; c < 9; c++) {
            possibilities.remove((Integer) sudokuGrid[x][c]); // column
            possibilities.remove((Integer) sudokuGrid[c][y]); // row
            possibilities.remove((Integer) sudokuGrid[(x / 3) * 3 + c % 3][(y / 3) * 3 + c / 3]); // box
        }

        return possibilities;
    }

    private void printSudokuGrid() {
        System.out.println(Arrays.deepToString(sudokuGrid));
    }

    private void generateTargetCellAndCages() {
        List<List<Integer>> insideCages = new ArrayList<>();
        List<List<Integer>> outsideCages = new ArrayList<>();

        int adjacent;

        if (insideCell) {
            int i = random.nextInt(insideCells.length);

            List<Integer> list = new ArrayList<>();
            targetCell = insideCells[i];
            list.add(insideCells[i]);
            list.add(insideAdjacents[i]);

            outsideCages.add(list);
        } else {
            int i = random.nextInt(outsideCells.length);

            List<Integer> list = new ArrayList<>();
            targetCell = outsideCells[i];
            list.add(outsideCells[i]);
            list.add(outsideAdjacents[i]);

            insideCages.add(list);
        }

        for (int i = 0; i < 3; i++) {
            int v = random.nextInt(3) * 9 + random.nextInt(3);

            if (insideCages.stream().noneMatch(l -> l.contains(v))
                    && outsideCages.stream().noneMatch(l -> l.contains(v))) {
                List<Integer> list = new ArrayList<>();
                list.add(v);
                insideCages.add(list);
            }
        }

        // grow the cages
        while (true) {
            boolean changed = false;

            for (List<Integer> cage : insideCages) {
                List<Integer> growableCells = new ArrayList<>();
                for (Integer cell : cage) {
                    if (cell % 9 > 2 || cell / 9 > 2) continue;

                    List<Integer> possibleGrowableCells = new ArrayList<>();
                    if (cell - 9 > 0) possibleGrowableCells.add(cell - 9);
                    if (cell % 9 > 0) possibleGrowableCells.add(cell - 1);
                    if (cell + 9 < 27) possibleGrowableCells.add(cell + 9);
                    if (cell % 9 < 2) possibleGrowableCells.add(cell + 1);

                    for (Integer possibleCell : possibleGrowableCells) {
                        if (insideCages.stream().noneMatch(l -> l.contains(possibleCell))
                                && outsideCages.stream().noneMatch(l -> l.contains(possibleCell))) {
                            growableCells.add(possibleCell);
                        }
                    }
                }

                if (!growableCells.isEmpty()) {
                    Collections.shuffle(growableCells);
                    cage.add(growableCells.remove(0));
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        cages = new ArrayList<>();
        cages.addAll(insideCages);
        cages.addAll(outsideCages);

        for (List<Integer> cage : cages) {
            Collections.sort(cage);
        }
    }

    private void checkNumber(int number) {
        boolean check = sudokuGrid[targetCell % 9][targetCell / 9] == number;

        if (check) {
            correct++;
            answeredCorrect = 1.0;
        } else {
            incorrect++;
            answeredIncorrect = 1.0;
        }

        nextGrid();
    }

    private void init() {
        addKeyListener(this);

        nextGrid();
    }

    private void update() {

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (List<Integer> cage : cages) {
            for (Integer cell : cage) {
                g.setColor(Color.getHSBColor(0.125f * cages.indexOf(cage), 0.1f, 0.95f));
                g.fillRect(BOX_OFFSET + (cell % 9) * BOX_SIZE, BOX_OFFSET + (cell / 9) * BOX_SIZE, BOX_SIZE, BOX_SIZE);

                if (cage.indexOf(cell) == 0) {
                    int total = 0;
                    for (Integer c : cage) {
                        total += sudokuGrid[c % 9][c / 9];
                    }

                    g.setColor(Color.BLACK);
                    g.setFont(new Font("Bahnschrift", Font.PLAIN, 20));
                    g.drawString(String.valueOf(total), BOX_OFFSET + (cell % 9) * BOX_SIZE + 10, BOX_OFFSET + (cell / 9) * BOX_SIZE + 30);
                }
            }
        }

        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                g.drawRect(BOX_OFFSET + x * BOX_SIZE, BOX_OFFSET + y * BOX_SIZE, BOX_SIZE, BOX_SIZE);
            }
        }

        g.setColor(Color.BLACK);
        g.drawLine(BOX_OFFSET, BOX_OFFSET, BOX_OFFSET + 4 * BOX_SIZE, BOX_OFFSET);
        g.drawLine(BOX_OFFSET, BOX_OFFSET, BOX_OFFSET, BOX_OFFSET + 4 * BOX_SIZE);
        g.drawLine(BOX_OFFSET, BOX_OFFSET + 3 * BOX_SIZE, BOX_OFFSET + 4 * BOX_SIZE, BOX_OFFSET + 3 * BOX_SIZE);
        g.drawLine(BOX_OFFSET + 3 * BOX_SIZE, BOX_OFFSET, BOX_OFFSET + 3 * BOX_SIZE, BOX_OFFSET + 4 * BOX_SIZE);

        g.setColor(Color.RED);
        g.drawRect(BOX_OFFSET + (targetCell % 9) * BOX_SIZE, BOX_OFFSET + (targetCell / 9) * BOX_SIZE, BOX_SIZE, BOX_SIZE);

        if (showNumbers) {
            g.setFont(new Font("Bahnschrift", Font.PLAIN, 100));
            g.setColor(Color.LIGHT_GRAY);
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    g.drawString(String.valueOf(sudokuGrid[x][y]), BOX_OFFSET + x * BOX_SIZE + 70, BOX_OFFSET + y * BOX_SIZE + 130);
                }
            }
        }

        g.setFont(new Font("Bahnschrift", Font.PLAIN, 20));
        g.setColor(Color.BLACK);
        g.drawString(String.format("Correct: %s   Incorrect: %s", correct, incorrect), 50, 35);

        g.setColor(new Color(0, 1, 0, (float)answeredCorrect * 0.5f));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        answeredCorrect -= 3.0 / FPS;
        if (answeredCorrect < 0) answeredCorrect = 0;

        g.setColor(new Color(1, 0, 0, (float)answeredIncorrect * 0.5f));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        answeredIncorrect -= 3.0 / FPS;
        if (answeredIncorrect < 0) answeredIncorrect = 0;

        g.dispose();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (Character.isDigit(e.getKeyChar())) {
            checkNumber(Character.getNumericValue(e.getKeyChar()));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_H) {
            showNumbers = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_H) {
            showNumbers = false;
        }
    }
}
