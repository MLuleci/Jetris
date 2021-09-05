import java.util.Arrays;
import java.awt.*;

public class Grid
{
    /*** STATIC DATA ***/

    private static final int[] LINES = new int[] { 0, 1, 3, 5, 8 };

    /*** INSTANCE VARIABLES ***/

    private int[][] grid = new int[22][10];
    private int[] rows = new int[22];

    /*** METHODS ***/

    /**
     * Test grid location.
     * @param x
     * @param y
     * @return true if location (x, y) is not NONE
     */
    public boolean get(int x, int y)
    {
        if (x < 0 || x > 9 || y < 0 || y > 21)
            return false;
        return this.grid[y][x] != 0;
    }

    /**
     * Set grid value.
     * @param x
     * @param y
     * @param k
     */
    public void set(int x, int y, Kind k)
    {
        if (x < 0 || x > 9 || y < 0 || y > 21)
            return;
        this.grid[y][x] = k.ordinal();
        this.rows[y]++;
    }

    /**
     * @return number of lines cleared
     */
    public int clear()
    {
        int n = 0;
        for (int i = 0; i < 22; i++)
        {
            if (rows[i] == 10) {
                n++;
                for (int j = i; j < 22; j++)
                {
                    if (j < 21) {
                        rows[j] = rows[j+1];
                        grid[j] = Arrays.copyOf(grid[j+1], 10);
                    } else {
                        rows[j] = 0;
                        Arrays.fill(grid[j], 0);
                    }
                }
            }
        }

        return n;
    }

    /**
     * Draw the grid.
     * @param g graphics context
     */
    public void draw(Graphics g)
    {
        Rectangle r = g.getClipBounds();
        int sz = Facade.SIZE;
        int w = r.width;
        int h = r.height;

        g.setColor(Color.BLACK);
        g.fillRect(0, 2 * sz, w, h);

        for (int y = 0; y < 22; y++)
        {
            if (y > 1) {
                g.setColor(Color.WHITE);
                g.drawLine(0, y * sz, w, y * sz);
            }

            for (int x = 0; x < 10; x++)
            {
                if (x > 0) {
                    g.setColor(Color.WHITE);
                    g.drawLine(x * sz, 2 * sz, x * sz, h);
                }
                int i = this.grid[y][x];
                if (i == 0)
                    continue;
                g.setColor(Piece.COLORS[i]);
                Facade.block(g, x * sz, (21 - y) * sz, sz);
            }
        }
    }
}