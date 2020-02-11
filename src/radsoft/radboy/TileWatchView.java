package radsoft.radboy;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import static radsoft.radboy.utils.Types.*;

// TODO
// Change background of address to make it look like a header
// Change cell editor to only allow hex numbers

public class TileWatchView implements Memory.Listener
{
    final static int COLUMNS = 16;
    final static int BYTES_PER_TILE = 16;
    
    final JDialog dlg;
    //final java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(8 * 16, (256 + 128) / 8 * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
    final java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(8 * COLUMNS, (256 + 128) * 8 / COLUMNS, java.awt.image.BufferedImage.TYPE_INT_RGB);
    ScalableImagePane pane;
    final Memory mem;
    
    final static short BEGIN = (short) 0x8000;
    final static short END = (short) 0x9800;
    
    @Override
    public void changedMemory(short loc)
    {
        if (loc >= BEGIN && loc < END)
            update(loc);
    }
    
    TileWatchView(JFrame f, Memory mem)
    {
        this.dlg = new JDialog(f, "Tiles");
        this.mem = mem;
        
        for (short i = BEGIN; i < END; i +=2)
            update(i);
        
        dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                mem.remove(TileWatchView.this);
            }
        });
        pane = new ScalableImagePane(bi);
        
        mem.add(this);
    }
    
    void open()
    {
        pane.doLayout();
        dlg.getContentPane().add(new JScrollPane(pane));
        dlg.pack();
        dlg.setVisible(true);
    }
    
    void update(final short loc)
    {
        final short l1 = (short) ((us(loc) / 2) * 2);
        final short l2 = (short) (us(l1) + 1);
        
        final int tn = ((us(l1) - us(BEGIN)) / BYTES_PER_TILE);
        final int tnx = tn % COLUMNS;
        final int tny = tn / COLUMNS;
        
        final int tl = ((us(l1) - us(BEGIN)) - (tn * BYTES_PER_TILE)) / 2;
        
        final int x = tnx * 8;
        final int y = tny * 8 + tl;
        
        final byte data1 = mem.read(l1);
        final byte data2 = mem.read(l2);
        for (byte tileX = 0; tileX < 8; ++tileX)
        {
            final byte colourNum = radsoft.radboy.core.Video.unpackTilePixel(data1, data2, tileX);
            assert(colourNum >= 0 && colourNum < 4);
            
            bi.setRGB(x + tileX, y, ScreenPane.c[colourNum]);
        }
    }
}
