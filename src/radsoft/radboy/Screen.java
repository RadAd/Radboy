package radsoft.radboy;

import radsoft.radboy.core.Video;
import static radsoft.radboy.utils.ByteEx.*;
//import static radsoft.radboy.utils.Types.*;

class Screen implements Video.Lcd
{
    private final java.awt.image.BufferedImage bmp = new java.awt.image.BufferedImage(Video.Width, Video.Height, java.awt.image.BufferedImage.TYPE_INT_RGB);
    private final int[] buffer = new int[Video.Width];
    //final javax.swing.JComponent comp = new JLabel(new ImageIcon(bmp));
    final javax.swing.JComponent comp = new ScalableImagePane(bmp);
    
    Screen()
    {
        java.awt.Graphics2D g2d = bmp.createGraphics();
        g2d.setBackground(java.awt.Color.BLACK);
    }
    
    //static final Color c[] = { new Color(0x08, 0x18, 0x20), Color.GREEN, Color.BLUE, Color.WHITE };
    static final int c[] = { 0xe0f8d0, 0x346856, 0x88c070, 0x081820 };
    
    @Override
    public void updateRow(byte y, byte data[])
    {
        //final long start = System.nanoTime();
        
        if (false)
        {
            int x = 0;
            for (byte tx : data)
            {
                //debugln("x=%d y=%d", x, ub(y));
                //bmp.setRGB(x++, ub(y), c[tx].getRGB());
                bmp.setRGB(x++, ub(y), c[tx]);
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
            
            //bmp.setRGB(0, ub(y), buffer.length, 1, buffer, 0, buffer.length);
            int[] pixels = ((java.awt.image.DataBufferInt) bmp.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, pixels, ub(y) * bmp.getWidth(), buffer.length);
        }
        else
        {
            int x = 0;
            for (byte tx : data)
            {
                //buffer[x++] = c[tx].getRGB();
                buffer[x++] = c[tx];
            }
            bmp.setRGB(0, ub(y), buffer.length, 1, buffer, 0, buffer.length);
            //java.util.Arrays.fill(buffer, 0xFF0000);
            //bmp.setRGB(0, (ub(y) + 1) % Video.Height, buffer.length, 1, buffer, 0, buffer.length);
        }
        //comp.invalidate();
        comp.repaint();
        
        //final long end = System.nanoTime();
        //debugln("updateRow %d", end-start);
    }
}
