import java.awt.*;
import javax.swing.*;
import java.awt.image.*;

import java.awt.event.*;

import static java.awt.event.KeyEvent.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Main
    extends Frame
    implements Runnable,
    ActionListener,
    KeyListener,
    WindowListener, 
    WindowFocusListener
{
    /*** STATIC DATA ***/

    private static final long FPS = 30;
    private static final long PERIOD = 1000000000L / FPS;

    private static final int WIDTH = 480;
    private static final int HEIGHT = 640;
    private static final int MARGIN = 50;

    private static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
    private static final Font HEAD = BASE.deriveFont(Font.BOLD, 20.f);
    private static final Font TEXT = BASE.deriveFont(Font.BOLD | Font.ITALIC, 30.f);
    private static final Font INFO = BASE.deriveFont(15.f);

    /*** INSTANCE VARIABLES ***/

    private Thread worker = new Thread(this);
    private Deque<AWTEvent> queue = new ConcurrentLinkedDeque<AWTEvent>();
    private Grid grid = new Grid();
    private Piece piece = null;
    private Kind hold = Kind.NONE;
    private List<Kind> bag = new LinkedList<>();

    private int score = 0;
    private int lines = 0;
    private int level = 1;

    private boolean paused = false;
    private boolean gameover = false;
    private boolean holding = false;

    private Timer lock = new Timer(500, this);
    private Timer down = new Timer(1000, this);
    private long elapsed = 0;

    /*** METHODS ***/

    /**
     * Create new Main.
     */
    public Main()
    {
        super("Jetris");
        setIgnoreRepaint(true);
        setSize(WIDTH, HEIGHT);
        setBackground(Color.BLACK);
        
        addKeyListener(this);
        addWindowListener(this);
        down.setRepeats(true);
        
        setVisible(true);
        worker.start();
    }

    /**
     * Add to the player score, lines
     * cleared, and adjust speed.
     * @param n number of lines cleared
     */
    public void score(int n)
    {
        if (n <= 0)
            return;

        // variable level goal
        this.lines += n;
        if (this.lines >= 5 * this.level)
            this.level++;

        // speed up
        int ms = (int) Math.pow(
                (0.8 - (this.level - 1) * 0.007),
                (this.level - 1));
        down.setInitialDelay(ms);
        down.restart();

        // add to score
        this.score += 100 * n * this.level;
    }

    /**
     * Generate a new piece.
     */
    private void next()
    {
        Kind k;

        // 7-bag generator
        if (this.bag.size() < 6) {
            List<Kind> q = new LinkedList<>();
            for (int i = 1; i < 8; i++)
            {
                k = Kind.values()[i];
                q.add(k);
            }
            Collections.shuffle(q);
            this.bag.addAll(q);
        }

        this.holding = false;
        k = this.bag.remove(0);
        this.piece = new Piece(k, this.grid);

        // test for block-out
        if (this.piece.collides())
            this.gameover = true;

        // see guideline
        this.piece.move(Direction.DOWN);

        this.lock.stop();
        this.down.restart();
    }

    /**
     * Hold current piece.
     */
    private void hold()
    {
        if (this.holding)
            return;

        if (!this.hold.equals(Kind.NONE))
            this.bag.add(0, this.hold);

        this.hold = this.piece.getKind();
        next();
        this.holding = true;
    }

    /**
     * Toggle pause.
     * @param p
     */
    private void pause(boolean p)
    {
        this.paused = p;
        if (p) {
            down.stop();
            lock.stop();
        } else {
            down.restart();
        }
    }

    /**
     * Stop the game.
     */
    private void stop()
    {
        this.down.stop();
        this.lock.stop();
    }

    /**
     * Process user input.
     * @param key
     */
    private void input(int key)
    {
        if (this.gameover)
            return;
        
        if (key == VK_ESCAPE || key == VK_F1)
            pause(!this.paused);

        if (this.paused)
            return;
        
        switch (key)
        {
            // rotate left (ccw)
            case VK_Z:
            case VK_CONTROL:
                if (this.piece.rotate(Direction.LEFT)
                    && this.lock.isRunning())
                    this.lock.restart();
                break;

            // rotate right (cw)
            case VK_X:
            case VK_UP:
                if (this.piece.rotate(Direction.RIGHT)
                    && this.lock.isRunning())
                    this.lock.restart();
                break;
            
            // move left
            case VK_LEFT:
                if (this.piece.move(Direction.LEFT)
                    && this.lock.isRunning())
                    this.lock.restart();
                break;

            // move right
            case VK_RIGHT:
                if (this.piece.move(Direction.RIGHT)
                    && this.lock.isRunning())
                    this.lock.restart();
                break;

            // soft drop
            case VK_DOWN:
                this.down.restart();
                if (!this.piece.move(Direction.DOWN))
                    this.lock.restart();
                else
                    // 1 pt. per block (soft) dropped
                    this.score += 1;
                break;

            // hard drop
            case VK_SPACE:
                // 2 pts. per block (hard) dropped
                this.score += 2 * this.piece.drop();
                this.gameover = this.piece.place();
                score(this.grid.clear());
                next();
                break;

            // hold
            case VK_C:
            case VK_SHIFT:
                hold();
                break;
        }

        if (this.gameover)
            stop();
    }

    /**
     * Render the screen.
     * @param g graphics context
     */
    public void render(Graphics g)
    {
        // create un-scaled image
        GraphicsConfiguration gc = getGraphicsConfiguration();
        BufferedImage bi = gc.createCompatibleImage(642, 682);
        Graphics2D g2d = bi.createGraphics();
        
        // field
        g2d.setColor(Color.WHITE);
        g2d.fillRect(162, 62, 318, 682);
        
        Graphics fg = g2d.create(166, 0, 310, 678);
        grid.draw(fg);
        piece.draw(fg);

        // hold
        g2d.setFont(HEAD);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 62, 166, 126);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(4, 92, 158, 92);

        g2d.drawString("HOLD", 4, 86);
        Facade fa = new Facade(this.hold);
        if (this.hold.equals(Kind.I))
            fa.fill(g2d, 21, 184, Facade.SIZE);
        else if (!this.hold.equals(Kind.NONE))
            fa.fill(g2d, 36, 169, Facade.SIZE);

        // preview
        g2d.setColor(Color.WHITE);
        g2d.fillRect(476, 62, 166, 503);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(480, 92, 158, 469);
        g2d.drawString("NEXT", 480, 86);

        for (int i = 0; i < 5; i++)
        {
            Kind k = this.bag.get(i);
            Facade f = new Facade(k);
            int j = (i + 1) * 3 * Facade.SIZE;
            
            if (k.equals(Kind.I))
                f.fill(g2d, 497, 92 + j, Facade.SIZE);
            else if (!k.equals(Kind.NONE))
                f.fill(g2d, 512, 92 + j, Facade.SIZE);
        }

        // messages
        g2d.setFont(TEXT);
        if (this.gameover) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(166, 310, 310, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Game Over", 228, 341);
        }
        if (this.paused) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(166, 310, 310, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Paused", 260, 341);
        }

        // inf
        g2d.setFont(INFO);
        g2d.setColor(Color.WHITE);
        String scr = String.format("SCORE: %d", score);
        g2d.drawString(scr, 150 - scr.length() * 10, 592);
        String lin = String.format("LINES: %d", lines);
        g2d.drawString(lin, 160 - lin.length() * 10, 622);
        String lvl = String.format("LEVEL: %d", level);
        g2d.drawString(lvl, 156 - lvl.length() * 10, 652);
        String time = String.format("TIME: %d.%03d", elapsed / 1000L, elapsed % 1000L);
        g2d.drawString(time, 160 - time.length() * 10, 682);

        // scale and blit
        int wh = getHeight();
        int ww = getWidth();
        int ih = wh - 2 * MARGIN;
        int iw = ih * 642/682;
        Image si = bi.getScaledInstance(iw, ih, Image.SCALE_SMOOTH);
        g.drawImage(si, (ww - iw) / 2, MARGIN, Color.BLACK, null);
    }

    @Override
    public void run()
    {
        createBufferStrategy(2);
        BufferStrategy buffer = getBufferStrategy();

        long beforeTime = System.nanoTime();
        long afterTime;
        long deltaTime = 0;
        long sleepTime;
        long overSlept = 0;
        long nDelays = 0;

        next();
        while (true)
        {
            // process events
            while (!queue.isEmpty())
            {
                AWTEvent e = queue.remove();

                if (e instanceof WindowEvent)
                {
                    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                        return;
                    } else {
                        pause(true);
                    }
                }

                if (e instanceof ActionEvent)
                {
                    Object o = e.getSource();

                    if (o.equals(down)) {
                        if (!this.piece.move(Direction.DOWN))
                            this.lock.restart();
                    }

                    if (o.equals(lock)) {
                        this.gameover = this.piece.place();
                        if (this.gameover) {
                            stop();
                        } else {
                            score(this.grid.clear());
                            next();
                        }
                    }
                }

                if (e instanceof KeyEvent)
                {
                    KeyEvent ke = (KeyEvent) e;
                    input(ke.getKeyCode());
                }
            }

            // render screen
            do {
                do {
                    Graphics g = buffer.getDrawGraphics();
                    render(g);
                    g.dispose();
                } while (buffer.contentsRestored());
                buffer.show();
            } while (buffer.contentsLost());

            // regulate framerate
            afterTime = System.nanoTime();
            deltaTime = afterTime - beforeTime;
            sleepTime = (PERIOD - deltaTime) - overSlept;

            if (sleepTime > 0) {
                // kill time left in frame
                try {
                    Thread.sleep(sleepTime / 1000000L);
                } catch (InterruptedException ex) {
                    System.err.println("Sleep failed!");
                    return;
                }
                overSlept = (System.nanoTime() - afterTime) - sleepTime;
            } else {
                // hogging CPU, yield for a bit
                overSlept = 0;
                if (++nDelays >= FPS/2) {
                    Thread.yield();
                    nDelays = 0;
                }
            }
            
            if (!gameover && !paused)
                elapsed += (deltaTime + sleepTime + overSlept) / 1000000L;

            beforeTime = System.nanoTime();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        queue.add(e);
    }

    @Override
    public void keyPressed(KeyEvent e) 
    {
        queue.add(e);
    }

    @Override
    public void windowDeactivated(WindowEvent e) 
    {   
        queue.add(e);
    }

    @Override
    public void windowIconified(WindowEvent e) 
    {
        queue.add(e);
    }

    @Override
    public void windowLostFocus(WindowEvent e) 
    {
        queue.add(e);
    }

    @Override
    public void windowClosing(WindowEvent e) 
    {
        try {
            queue.add(e);
            if (worker.isAlive())
                worker.join();
        } catch (InterruptedException ex) {
            System.err.println("Join failed!");
        } finally {
            this.dispose();
            System.exit(0);
        }
    }

    /*** UNUSED EVENT HANDLERS ***/

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowGainedFocus(WindowEvent e) {}

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() 
            {
                new Main();
            }
        });
    }
};
