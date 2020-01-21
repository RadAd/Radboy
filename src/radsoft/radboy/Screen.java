package radsoft.radboy;

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.Types.*;

public class Screen implements Video.Lcd
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
        frame.addKeyListener(new JoypadKeyListener(joypad));
        {
            Graphics2D g2d = m_bmp.createGraphics();
            g2d.setBackground(Color.BLACK);
            //g2d.fillRect(0, 0, m_bmp.getWidth(null), m_bmp.getHeight(null));
            //g2d.setColor(Color.BLACK);
            //g2d.drawLine(0, 0, 100, 100);
        }
        //comp = new JLabel(new ImageIcon(m_bmp));
        comp = new ScalableImagePane(m_bmp);
        frame.getContentPane().add(comp);
        frame.pack();
        frame.setVisible(true);
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
}
