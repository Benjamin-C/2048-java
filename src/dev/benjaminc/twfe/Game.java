package dev.benjaminc.twfe;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Game {

    public static void main(String args[]) {
        new Game();
    }

    private int width = 4;
    private int height = 4;
    private int numbers[][];
    private List<int[][]> history;
    private NumberLabel boxes[][];
    private int boxSize = 256;
    private double twoChance = 0.75;
    private int debugAnimSpeed = 1/60;
    private Object gosyncro = new Object();
    private Object kpsyncro = new Object();
    private Direction move;
    private Action act;

    public Game() {
        ColorMap.initColorMap();
        numbers = new int[height][width];
        boxes = new NumberLabel[height][width];

        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        history = new ArrayList<int[][]>();

        JPanel jp =  new JPanel();
        jp.setLayout(new GridLayout(4, 4));
        // Init visual boxes and number array
        int next = 0;
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                NumberLabel jl = new NumberLabel();
                boxes[r][c] = jl;
                jp.add(jl);
                numbers[r][c] = 0;
                // next *= 2;
            }
        }
        restartGame();

        regenBoxes();

        jf.add(jp);
        jf.pack();
        jf.setVisible(true);

        jf.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                // System.out.println("keyPressed");
            }
        
            public void keyReleased(KeyEvent e) {
                act = Action.MOVE;
                if(e.getKeyCode()== KeyEvent.VK_RIGHT) {
                    move = Direction.RIGHT;
                } else if(e.getKeyCode()== KeyEvent.VK_LEFT) {
                    move = Direction.LEFT;
                } else if(e.getKeyCode()== KeyEvent.VK_DOWN) {
                    move = Direction.DOWN;
                } else if(e.getKeyCode()== KeyEvent.VK_UP) {
                    move = Direction.UP;
                } else if(e.getKeyCode() == KeyEvent.VK_R) {
                    act = Action.RESTART;
                    synchronized(gosyncro) {
                        gosyncro.notifyAll();
                    } 
                } else {
                    return;
                }
                synchronized(kpsyncro) {
                    kpsyncro.notifyAll();
                }
            }
            public void keyTyped(KeyEvent e) {
                // System.out.println("keyTyped");
            }
        });

        System.out.println("Hello world");

        while(true) {
            playGame();
        }
    }

    public void saveToHistory() {
        int arr[][] = new int[height][width];
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                arr[r][c] = numbers[r][c];
            }
        }
        history.add(arr);
    }

    public void loadFromHistory() {
        loadFromHistory(1);
    }

    public void loadFromHistory(int n) {
        int index = history.size();
        int[][] arr = history.get(index - 1 - n);
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                numbers[r][c] = arr[r][c];
            }
        }
    }

    public void playGame() {
        restartGame();
        addNumber();
        addNumber();
        regenBoxes();
        boolean alive = true;
        while(alive) {
            // wait for key
            synchronized(kpsyncro) {
                try {
                    kpsyncro.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            switch(act) {
            case MOVE: {
                if(move != null) {
                    clearMarkers();
                    if(slide(move)) {
                        alive = addNumber();
                    } else {
                        alive = checkLegalMove();
                    }
                    regenBoxes();
                }
                history.add(numbers);
            } break;
            case RESTART: {
                return;
            }
            }
        }
        gameOver();
        synchronized(gosyncro) {
            try {
                gosyncro.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean checkLegalMove() {
        for(int r = 0; r < height-1; r++) {
            for(int c = 0; c < width; c++){
                if(numbers[r][c] == numbers[r+1][c]) {
                    return true;
                }
            }
        }
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width-1; c++){
                if(numbers[r][c] == numbers[r][c+1]) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clearMarkers() {
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                boxes[r][c].setMarkerColor(null);
            }
        }
    }

    public void regenBoxes() {
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                NumberLabel jl = boxes[r][c];
                jl.setNum(numbers[r][c]);
                jl.setFont(jl.getFont().deriveFont(64.f));
                jl.setSize(boxSize, boxSize);
                jl.setMinimumSize(new Dimension(boxSize, boxSize));
                jl.setPreferredSize(new Dimension(boxSize, boxSize));
                jl.setMaximumSize(new Dimension(boxSize, boxSize));
            }
        }
    }

    public int numPos(Position p) {
        return numbers[p.getR()][p.getC()];
    }

    public void repaintAll() {
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                boxes[r][c].repaint();
            }
        }
    }

    public boolean slide(Direction dir) {
        // Scan each row
        boolean changed = false;
        for(int sc = 0; sc < height; sc++) {
            for(int sr = width-1; sr > 0; sr--) {
                for(int st = sr-1; st >= 0; st--) {
                    boolean slid = false;
                    // Apply rotation
                    int r = dir.convertMovement(sr, sc, width, height).getR();
                    int c = dir.convertMovement(sr, sc, width, height).getC();
                    int tr = dir.convertMovement(st, sc, width, height).getR();
                    int tc = dir.convertMovement(st, sc, width, height).getC();
                    if(debugAnimSpeed > 0) {
                        boxes[r][c].setMarkerColor(Color.MAGENTA);
                        boxes[tr][tc].setMarkerColor(Color.BLUE);
                        repaintAll();
                        regenBoxes();
                    }
                    try {
                        Thread.sleep(debugAnimSpeed);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(numbers[r][c] > 0) {
                        if(numbers[tr][tc] == numbers[r][c]) {
                            // Merge numbers
                            numbers[r][c]++;
                            numbers[tr][tc] = 0;
                            boxes[r][c].setMarkerColor(Color.GREEN);
                            if(debugAnimSpeed > 0) {
                                boxes[r][c].setMarkerColor(null);
                                boxes[tr][tc].setMarkerColor(null);
                                repaintAll();
                                regenBoxes();
                            }
                            changed = true;
                            break;
                        } else if(numbers[tr][tc] > 0) {
                            if(debugAnimSpeed > 0) {
                                boxes[r][c].setMarkerColor(null);
                                boxes[tr][tc].setMarkerColor(null);
                                repaintAll();
                                regenBoxes();
                            }
                            break;
                        }
                    } else {
                        if(numbers[tr][tc] > 0) {
                            // Shift numbers
                            numbers[r][c] = numbers[tr][tc];
                            numbers[tr][tc] = 0;
                            boxes[r][c].setMarkerColor(Color.LIGHT_GRAY);
                            slid = true;
                            changed = true;
                        }
                    }
                    if(debugAnimSpeed > 0) {
                        boxes[r][c].setMarkerColor(null);
                        boxes[tr][tc].setMarkerColor(null);
                        repaintAll();
                        regenBoxes();
                    }
                }
            }
        }

        return changed;
    }

    public void restartGame() {
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                numbers[r][c] = 0;
                boxes[r][c].setMarkerColor(null);
            }
        }
        regenBoxes();
    }

    public void gameOver() {
        System.out.println("Game Over");
        JDialog jd = new JDialog();
        JButton jb = new JButton("Game over!");
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restartGame();
                synchronized(gosyncro) {
                    gosyncro.notifyAll();
                }
            }
        });
        jd.add(jb);
        jd.pack();
        jd.setVisible(true);
        synchronized(gosyncro) {
            try {
                gosyncro.wait();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        jd.dispose();
    }

    public int countFilledBoxes() {
        int count = 0;
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                if(numbers[r][c] > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean addNumber() {
        // Find all possible places to put a number
        List<Position> opts = new ArrayList<Position>();
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                if(numbers[r][c] <= 0) {
                    opts.add(new Position(r, c));
                }
            }
        }

        // Put in a number, or say you can't
        if(opts.size() > 0) {
            Random rand = new Random();
            int num = (rand.nextDouble() < twoChance) ? 1 : 2;
            int choice = rand.nextInt(opts.size());
            numbers[opts.get(choice).getR()][opts.get(choice).getC()] = num;
            boxes[opts.get(choice).getR()][opts.get(choice).getC()].setMarkerColor(Color.MAGENTA);;
            return true;
        } else {
            return false;
        }
    }
}
