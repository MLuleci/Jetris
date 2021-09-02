import java.awt.*;
import java.util.Arrays;

public class Piece
{
    /*** STATIC DATA ***/

    public static final int[][][] 
        BLOCKS = new int[][][]
    {
        // NONE
        {{0,0}, {0,0}, {0,0}, {0,0}},
        // I
        {{0,2}, {1,2}, {2,2}, {3,2}},
        // J
        {{0,2}, {0,1}, {1,1}, {2,1}},
        // L
        {{0,1}, {1,1}, {2,1}, {2,2}},
        // O
        {{1,1}, {1,2}, {2,1}, {2,2}},
        // S
        {{0,1}, {1,1}, {1,2}, {2,2}},
        // T
        {{0,1}, {1,1}, {1,2}, {2,1}},
        // Z
        {{0,2}, {1,2}, {1,1}, {2,1}}
    };

    private static final int[][][][] 
        KICKS1 = new int[][][][]
    {
        { // LEFT
            {{0,0}, { 2,0}, {-1,0}, { 2, 1}, {-1,-2}},
            {{0,0}, { 1,0}, {-2,0}, { 1,-2}, {-2, 1}},
            {{0,0}, {-2,0}, { 1,0}, {-2,-1}, { 1, 2}},
            {{0,0}, {-1,0}, { 2,0}, {-1, 2}, { 2,-1}}
        },
        { // RIGHT
            {{0,0}, { 1,0}, { 2,0}, {-1, 2}, { 2,-1}},
            {{0,0}, {-2,0}, { 1,0}, {-2,-1}, { 1, 2}},
            {{0,0}, {-1,0}, { 2,0}, {-1, 2}, { 2,-1}},
            {{0,0}, { 2,0}, {-1,0}, { 2, 1}, {-1,-2}}
        }
    };

    private static final int[][][][] 
        KICKS2 = new int[][][][]
    {
        { // LEFT
            {{0,0}, { 1,0}, { 1,-1}, {0, 2}, { 1, 2}},
            {{0,0}, {-1,0}, {-1, 1}, {0,-2}, {-1,-2}},
            {{0,0}, {-1,0}, {-1,-1}, {0, 2}, {-1, 2}},
            {{0,0}, { 1,0}, { 1, 1}, {0,-2}, { 1,-2}}
        },
        { // RIGHT
            {{0,0}, {-1,0}, {-1,-1}, {0, 2}, {-1, 2}},
            {{0,0}, {-1,0}, {-1, 1}, {0,-2}, {-1,-2}},
            {{0,0}, { 1,0}, { 1,-1}, {0, 2}, { 1, 2}},
            {{0,0}, { 1,0}, { 1, 1}, {0,-2}, { 1,-2}}
        }
    }; 

    public static final Color[]
        COLORS = new Color[]
    {
        Color.GRAY,
        Color.CYAN,
        Color.BLUE,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED
    };

    /*** INSTANCE VARIABLES ***/

    private final Kind kind;
    private final Grid grid;
    private int[][] blocks;
    private Color color;
    private Rectangle rect;
    private int state = 0;

    /*** METHODS ***/

    public static int[][] deepcopy(int[][] arr)
    {
        int[][] copy = new int[arr.length][];
        for (int i = 0; i < arr.length; i++)
            copy[i] = Arrays.copyOf(arr[i], arr[i].length);
        return copy;
    }

    /**
     * Create new Piece.
     * @param k kind of piece
     * @param g grid object
     */
    public Piece(Kind k, Grid g)
    {
        int i = k.ordinal();
        this.kind = k;
        this.grid = g;
        this.blocks = deepcopy(BLOCKS[i]);
        this.color = COLORS[i];
        
        int x, y, w, h;
        x = 3;
        y = 19;
        w = 3;
        h = 3;
        if (k.equals(Kind.I)) {
            y = 18;
            w = 4;
            h = 4;
        } else if (k.equals(Kind.O)) {
            w = 4;
        }
        this.rect = new Rectangle(x, y, w, h);
    }

    /**
     * @return if piece collides w/ anything
     */
    public boolean collides()
    {
        for (int[] p : this.blocks)
        {
            int x = p[0] + this.rect.x;
            int y = p[1] + this.rect.y;
            if (x < 0 || x > 9 || y < 0 || this.grid.get(x, y))
                return true;
        }
        return false;
    }

    /**
     * Internal movement helper.
     * @param dx
     * @param dy
     * @return true on success
     */
    private boolean _move(int dx, int dy)
    {
       rect.translate(dx, dy);
       if (collides()) {
           rect.translate(-dx, -dy);
           return false;
       }
       return true;
    }

    /**
     * Try and move the piece.
     * @param d direction
     * @return true on success
     */
    public boolean move(Direction d)
    {
        return _move(d.dx, d.dy);
    }

    /**
     * Internal rotation helper.
     * 
     * The static 'BLOCKS' array contains coordinates in the first quadrant,
     * so when they're rotated around the origin the blocks end-up in another
     * quadrant. This requires us to translate them back into the first quadrant,
     * up or to the right depending on the rotation, by exactly the size of their 
     * bounding boxes. The alternatives are either to use floating point numbers
     * for coordinates (which can be messy due to inaccuracies); not using SRS
     * (which is unacceptable [to me]); or translating the offset in 'rect' by the
     * required amount instead of the blocks.
     * 
     * If you're still confused, grab some pen and paper and walk through it.
     *  
     * @param d direction, guaranteed to be LEFT or RIGHT
     */
    private void _rotate(Direction d)
    {
        int rc, xc, yc;
        if (d.equals(Direction.LEFT)) {
            rc = 1;
            xc = 1;
            yc = 0;
        } else {
            rc = -1;
            xc = 0;
            yc = 1;
        }
        
        // rotate & translate
        for (int[] p : this.blocks)
        {
            int x = p[0];
            int y = p[1];
            p[0] = -rc * y + xc * (rect.width - 1);
            p[1] =  rc * x + yc * (rect.height - 1);
        }

        // update state
        this.state -= rc;
        if (this.state < 0)
            this.state = 3;
        if (this.state > 3)
            this.state = 0;
    }

    /**
     * Try and rotate the piece.
     * @param d direction, may only be LEFT or RIGHT
     * @return true on success
     * @throws IllegalArgumentException for invalid directions
     */
    public boolean rotate(Direction d)
    {
        if (this.kind.equals(Kind.O))
            return true;

        if (d.equals(Direction.UP) || d.equals(Direction.DOWN))
            throw new IllegalArgumentException
                ("May only rotate LEFT or RIGHT");


        // try rotation
        _rotate(d);

        // test kicks
        int ind = (d.equals(Direction.LEFT) ? 0 : 1);
        int[][] kick;
        if (this.kind.equals(Kind.I))
            kick = KICKS1[ind][state];
        else
            kick = KICKS2[ind][state];

        for (int[] p : kick)
            if (_move(p[0], p[1]))
                    return true;

        // undo rotation
        _rotate(d.inverse());
        return false;
    }

    /**
     * Place piece on the grid.
     * @return true if lock-out
     */
    public boolean place()
    {
        boolean lock = true;
        for (int[] p : this.blocks)
        {
            int x = p[0] + this.rect.x;
            int y = p[1] + this.rect.y;
            lock &= (y > 19);
            this.grid.set(x, y, this.kind);
        }
        return lock;
    }

    /**
     * Internal drop helper.
     * @return vertical drop distance
     */
    private int _drop()
    {
        int off = 22;
        for (int[] p : this.blocks)
        {
            int x = p[0] + this.rect.x;
            int y = p[1] + this.rect.y;

            // find highest block below 'y'
            int top = 0;
            for (int i = 0; i < y; i++)
                if (this.grid.get(x, i))
                    top = i + 1;

            // find shortest distance
            off = Math.min(off, y - top);
        }
        return off;
    }

    /**
     * Hard drop piece down, the caller
     * must then place it on the board.
     * @return drop distance
     */
    public int drop()
    {
        int dy = _drop();
        _move(0, -dy);
        return dy;
    }

    /**
     * @return piece kind
     */
    public Kind getKind()
    {
        return this.kind;
    }

    /**
     * Internal draw helper.
     * @param g graphics context
     */
    private void _draw(Graphics g)
    {
        int sz = Facade.SIZE;
        g.setColor(this.color);
        for (int[] p : this.blocks)
        {
            int x = (p[0] + this.rect.x) * sz;
            int y = (21 - p[1] - this.rect.y) * sz;
            Facade.block(g, x, y, sz);
        }
    }

    /**
     * Draw the piece.
     * @param g graphics context
     */
    public void draw(Graphics g)
    {
        // draw ghost
        int d = _drop();
        int c = this.color.getRGB();
        this.color = new Color(c & 0x4BFFFFFF, true);
        _move(0, -d);
        _draw(g);
        _move(0, d);
        this.color = new Color(c);

        // draw self
        _draw(g);
    }
};
