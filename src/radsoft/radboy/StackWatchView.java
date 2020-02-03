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
// Show stack upside down

public class StackWatchView
{
    final JDialog dlg;
    final MemoryTableModel model = new MemoryTableModel();
    final JTable table = new JTable(model);
    final Memory mem;
    
    static int COLUMNS = 1;
    
    private java.util.Set<Short> modified = new java.util.HashSet<Short>();
        
    private class MemoryTableModel implements TableModel, Memory.Listener
    {
        private java.util.Set<TableModelListener> ls = new java.util.HashSet<TableModelListener>();
        
        public void addTableModelListener(TableModelListener l)
        {
            ls.add(l);
        }
        
        public Class<?> getColumnClass(int columnIndex)
        {
            return String.class;
        }
        
        public int getColumnCount()
        {
            return COLUMNS + 1;
        }
        
        public String getColumnName(int columnIndex)
        {
            if (columnIndex == 0)
                return "Address";
            else
                return "Value";
        }
        
        public int getRowCount()
        {
            return (0xFFFF + 1) / (2 * COLUMNS);
        }
        
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (columnIndex == 0)
            {
                short address = (short) (rowIndex * (2 * COLUMNS));
                return us(address);
            }
            else
            {
                short address = (short) (rowIndex * (2 * COLUMNS));
                byte bl = mem.read((short) (address + columnIndex - 1));
                byte bh = mem.read((short) (address + columnIndex - 0));
                return us(makeShort(bh, bl));
            }
        }
        
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex > 0;
        }
        
        public void removeTableModelListener(TableModelListener l)
        {
            ls.remove(l);
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            // TODO
            throw new IllegalArgumentException();
        }
        
        // Memory.Listener
        
        @Override
        public void changedMemory(short loc)
        {
            modified.add(loc);
            int row = us(loc) / (2 * COLUMNS);
            int col = us(loc) % (2 * COLUMNS) + 1; // TODO
            //debug("changedMemory %04X, %d %d\n", us(loc), row, col);
            for (TableModelListener l : ls)
                l.tableChanged(new TableModelEvent(this, row, row, col));
        }
    }
    
    private class MemoryTableCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1;
        
        //Font normal = getFont();
        //Font bold = normal.deriveFont(Font.BOLD);
        Color color = getForeground();
        
        MemoryTableCellRenderer()
        {
            setHorizontalAlignment(CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            //debug("getTableCellRendererComponent %d %d %s\n", row, column, value);
            short loc = column > 0 ? (short) (row * (2 * COLUMNS) + (column - 1)) : -1;
            if (value instanceof Integer)
                value = String.format("%04X", value);

            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //cellComponent.setForeground(value.equals("00") ? Color.RED : color);
            //cellComponent.setFont(column == 2 ? bold : normal);
            Font f = cellComponent.getFont();
            cellComponent.setFont(modified.contains(loc + 0) || modified.contains(loc + 1) ? f.deriveFont(Font.BOLD) : f.deriveFont(Font.PLAIN));
            return cellComponent;
        }
    }
    
    StackWatchView(JFrame f, Memory mem)
    {
        this.dlg = new JDialog(f, "Stack");
        this.mem = mem;
        
        dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                mem.remove(model);
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(String.class, new MemoryTableCellRenderer());
        
        mem.add(model);
    }
    
    void open()
    {
        table.doLayout();
        dlg.getContentPane().add(new JScrollPane(table));
        dlg.pack();
        dlg.setVisible(true);
    }
}
