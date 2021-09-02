import java.awt.Graphics;

public class Facade
    {
        public static final int SIZE = 31;
        private Kind kind;

        public Facade(Kind k)
        {
            this.kind = k;
        }

        public static void block(Graphics g, int x, int y, int s)
        {
            g.fill3DRect(x, y, s, s, true);
        }

        /**
         * Fill facade with piece color.
         * @param g graphics context
         * @param x horizontal offset
         * @param y vertical offset
         * @param s block size
         */
        public void fill(Graphics g, int x, int y, int s)
        {
            int i = this.kind.ordinal();
            g.setColor(Piece.COLORS[i]);
            for (int[] p : Piece.BLOCKS[i])
            {
                int xx = p[0] * s + x + 1;
                int yy = -p[1] * s + y + 1;
                block(g, xx, yy, s-2);
            }
        }
    };