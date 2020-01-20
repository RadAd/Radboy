package radsoft.radboy;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.*;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.Types.*;

public class Screen implements Video.Lcd, KeyListener
{
    JFrame frame = new JFrame("Gameboy");
    JComponent comp;
    final BufferedImage m_bmp = new BufferedImage(Video.Width, Video.Height, BufferedImage.TYPE_INT_RGB);
    final int[] buffer = new int[Video.Width];
    
    private final Video video;
    private final Joypad joypad;
    
    Screen(Video video, Joypad joypad)
    {
        this.video = video;
        this.joypad = joypad;
        
        video.lcd = this;
    }
    
    void open()
    {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addKeyListener(this);
        {
            Graphics2D g2d = m_bmp.createGraphics();
            g2d.setBackground(Color.BLACK);
            //g2d.fillRect(0, 0, m_bmp.getWidth(null), m_bmp.getHeight(null));
            //g2d.setColor(Color.BLACK);
            //g2d.drawLine(0, 0, 100, 100);
        }
        //comp = new JLabel(new ImageIcon(m_bmp));
        comp = new ScalablePane(m_bmp);
        frame.getContentPane().add(comp);
        frame.pack();
        frame.setVisible(true);
    }
    
    Joypad.Keys getKey(int code)
    {
        switch (code)
        {
        case KeyEvent.VK_Q:     return Joypad.Keys.Start;
        case KeyEvent.VK_W:     return Joypad.Keys.Select;
        case KeyEvent.VK_A:     return Joypad.Keys.A;
        case KeyEvent.VK_S:     return Joypad.Keys.B;
        case KeyEvent.VK_UP:    return Joypad.Keys.Up;
        case KeyEvent.VK_DOWN:  return Joypad.Keys.Down;
        case KeyEvent.VK_LEFT:  return Joypad.Keys.Left;
        case KeyEvent.VK_RIGHT: return Joypad.Keys.Right;
        default: return null;
        }
    }
    
    // KeyListener
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        //debug("keyPressed %s\n", e);
        Joypad.Keys k = getKey(e.getKeyCode());
        if (k != null)
            joypad.setKey(k, true);
    }
    
    @Override
    public void keyReleased(KeyEvent e)
    {
        //debug("keyReleased %s\n", e);
        Joypad.Keys k = getKey(e.getKeyCode());
        if (k != null)
            joypad.setKey(k, false);
    }
    
    @Override
    public void keyTyped(KeyEvent e)
    {
    }
    
    //static final Color c[] = { new Color(0x08, 0x18, 0x20), Color.GREEN, Color.BLUE, Color.WHITE };
    static final int c[] = { 0xe0f8d0, 0x346856, 0x88c070, 0x081820 };
    
    public void updateRow(byte y, byte data[])
    {
        //final long start = System.nanoTime();
        
        if (false)
        {
            int x = 0;
            for (byte tx : data)
            {
                //debugln("x=%d y=%d", x, ub(y));
                //m_bmp.setRGB(x++, ub(y), c[tx].getRGB());
                m_bmp.setRGB(x++, ub(y), c[tx]);
            }
        }
        else if (true)
        {
            int x = 0;
            for (byte tx : data)
            {
                //buffer[x++] = c[tx].getRGB();
                buffer[x++] = c[tx];
            }
            
            //m_bmp.setRGB(0, ub(y), buffer.length, 1, buffer, 0, buffer.length);
            int[] pixels = ((DataBufferInt) m_bmp.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, pixels, ub(y) * m_bmp.getWidth(), buffer.length);
        }
        else
        {
            int x = 0;
            for (byte tx : data)
            {
                //buffer[x++] = c[tx].getRGB();
                buffer[x++] = c[tx];
            }
            m_bmp.setRGB(0, ub(y), buffer.length, 1, buffer, 0, buffer.length);
            //java.util.Arrays.fill(buffer, 0xFF0000);
            //m_bmp.setRGB(0, (ub(y) + 1) % Video.Height, buffer.length, 1, buffer, 0, buffer.length);
        }
        //frame.invalidate();
        //frame.validate();
        //frame.revalidate();
        comp.invalidate();
        frame.repaint();
        
        //final long end = System.nanoTime();
        //debugln("updateRow %d", end-start);
    }
    
    public static class ScalablePane extends JPanel
    {
        private static final long serialVersionUID = 1;
        
        private Image master;
        private boolean toFit;
        private Image scaled;

        public ScalablePane(Image master)
        {
            this(master, true);
        }

        public ScalablePane(Image master, boolean toFit)
        {
            this.master = master;
            setToFit(toFit);
        }

        @Override
        public Dimension getPreferredSize()
        {
            return master == null ? super.getPreferredSize() : getMasterSize();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Image toDraw = null;
            if (scaled != null)
                toDraw = scaled;
            else if (master != null)
                toDraw = master;

            if (toDraw != null)
            {
                int x = (getWidth() - toDraw.getWidth(this)) / 2;
                int y = (getHeight() - toDraw.getHeight(this)) / 2;
                g.drawImage(toDraw, x, y, this);
            }
        }

        @Override
        public void invalidate()
        {
            generateScaledInstance();
            super.invalidate();
        }

        public boolean isToFit()
        {
            return toFit;
        }

        public void setToFit(boolean value)
        {
            if (value != toFit)
            {
                toFit = value;
                invalidate();
            }
        }
        
        private Dimension getMasterSize()
        {
            return new Dimension(master.getWidth(this), master.getHeight(this));
        }

        protected void generateScaledInstance()
        {
            Dimension masterSize = getMasterSize();
            Dimension size = getSize();
            scaled = getScaledInstance(
                            toBufferedImage(masterSize),
                            isToFit() ? getScaleFactorToFit(masterSize, size) : getScaleFactorToFill(masterSize, size),
                            //RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                            true);
        }

        protected BufferedImage toBufferedImage(Dimension masterSize)
        {
            BufferedImage image = (BufferedImage) master;
            if (image == null)
            {
                image = createCompatibleImage(masterSize, Transparency.OPAQUE);
                Graphics2D g2d = image.createGraphics();
                g2d.drawImage(master, 0, 0, this);
                g2d.dispose();
            }
            return image;
        }

        static private double getScaleFactor(int iMasterSize, int iTargetSize)
        {
            return (double) iTargetSize / (double) iMasterSize;
        }

        static private double getScaleFactorToFit(Dimension original, Dimension toFit)
        {
            double dScale = 1d;
            if (original != null && toFit != null)
            {
                double dScaleWidth = getScaleFactor(original.width, toFit.width);
                double dScaleHeight = getScaleFactor(original.height, toFit.height);
                dScale = Math.min(dScaleHeight, dScaleWidth);
            }
            return dScale;
        }

        static private double getScaleFactorToFill(Dimension masterSize, Dimension targetSize)
        {
            double dScaleWidth = getScaleFactor(masterSize.width, targetSize.width);
            double dScaleHeight = getScaleFactor(masterSize.height, targetSize.height);

            return Math.max(dScaleHeight, dScaleWidth);
        }

        private BufferedImage createCompatibleImage(Dimension size, int transparency)
        {
            return createCompatibleImage(size.width, size.height, transparency);
        }

        private BufferedImage createCompatibleImage(int width, int height, int transparency)
        {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null)
                gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

            BufferedImage image = gc.createCompatibleImage(width, height, transparency);
            image.coerceData(true);
            return image;
        }

        private Image getScaledInstance(BufferedImage img, double dScaleFactor, Object hint, boolean bHighQuality)
        {
            Image imgScale = img;
            int iImageWidth = (int) Math.round(img.getWidth() * dScaleFactor);
            int iImageHeight = (int) Math.round(img.getHeight() * dScaleFactor);
            
            if (!bHighQuality)
            {
                imgScale = getResizedInstance(img, iImageWidth, iImageHeight, BufferedImage.TYPE_INT_RGB, hint);
            }
            else if (dScaleFactor <= 1.0d)
            {
                imgScale = getScaledDownInstance(img, iImageWidth, iImageHeight, hint);
            }
            else
            {
                imgScale = getScaledUpInstance(img, iImageWidth, iImageHeight, hint);
            }

            return imgScale;
        }

        private Image getScaledDownInstance(BufferedImage img,
                        int targetWidth,
                        int targetHeight,
                        Object hint)
        {
            int type = (img.getTransparency() == Transparency.OPAQUE)
                            ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

            Image ret = img;

            if (targetHeight > 0 || targetWidth > 0)
            {
                // Use multi-step technique: start with original size, then
                // scale down in multiple passes with drawImage()
                // until the target size is reached
                int w = img.getWidth();
                int h = img.getHeight();

                while (w != targetWidth || h != targetHeight)
                {
                    if (w > targetWidth)
                    {
                        w /= 2;
                        if (w < targetWidth)
                            w = targetWidth;
                    }
                    
                    if (h > targetHeight)
                    {
                        h /= 2;
                        if (h < targetHeight)
                            h = targetHeight;
                    }

                    ret = getResizedInstance(ret, w, h, type, hint);
                }
            }
            else
            {
                //ret = new BufferedImage(1, 1, type);
                ret = createCompatibleImage(1, 1, type);
            }

            return ret;
        }

        private Image getScaledUpInstance(BufferedImage img,
                        int targetWidth,
                        int targetHeight,
                        Object hint)
        {
            int type = (img.getTransparency() == Transparency.OPAQUE)
                            ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

            Image ret = img;

            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            int w = img.getWidth();
            int h = img.getHeight();

            while (w != targetWidth || h != targetHeight)
            {
                if (w < targetWidth)
                {
                    w *= 2;
                    if (w > targetWidth)
                        w = targetWidth;
                }

                if (h < targetHeight)
                {
                    h *= 2;
                    if (h > targetHeight)
                        h = targetHeight;
                }

                ret = getResizedInstance(ret, w, h, type, hint);
            }
            
            return ret;
        }
    
        private BufferedImage getResizedInstance(Image img,
                            int width,
                            int height,
                            int type,
                            Object hint)
        {
            //BufferedImage tmp = new BufferedImage(width, height, type);
            BufferedImage tmp = createCompatibleImage(width, height, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(img, 0, 0, width, height, null);
            g2.dispose();
            return tmp;
        }
    }
}
