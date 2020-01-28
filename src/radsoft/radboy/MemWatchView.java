package radsoft.radboy;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.image.*;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import static radsoft.radboy.utils.Types.*;

// TODO
// Change background of address to make it look like a header
// Change cell editor to only allow hex numbers
// Change cell renderer to convert to hex on the fly
// Add a listener to mem to pass on to TableModelListener

public class MemWatchView
{
    final JDialog dlg;
    final MemoryTableModel model = new MemoryTableModel();
    final JTable table = new JTable(model);
    final Memory mem;
    
    static int COLUMNS = 16;
    
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
                return toHexString((byte) (columnIndex - 1));
        }
        
        public int getRowCount()
        {
            return (0xFFFF + 1) / COLUMNS;
        }
        
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (columnIndex == 0)
            {
                short address = (short) (rowIndex * COLUMNS);
                return us(address);
            }
            else
            {
                byte b = mem.read((short) (rowIndex * COLUMNS + columnIndex - 1));
                return ub(b);
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
            int row = us(loc) / COLUMNS;
            int col = us(loc) % COLUMNS + 1;
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
            short loc = column > 0 ? (short) (row * COLUMNS + (column - 1)) : -1;
            if (value instanceof Integer)
                value = String.format(column == 0 ? "%04X" : "%02X", value);

            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //cellComponent.setForeground(value.equals("00") ? Color.RED : color);
            //cellComponent.setFont(column == 2 ? bold : normal);
            Font f = cellComponent.getFont();
            cellComponent.setFont(modified.contains(loc) ? f.deriveFont(Font.BOLD) : f.deriveFont(Font.PLAIN));
            return cellComponent;
        }
    }
    
    MemWatchView(JFrame f, Memory mem)
    {
        this.dlg = new JDialog(f, "Memory");
        this.mem = mem;
        
        dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                mem.remove(model);;
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(column.getPreferredWidth() * 2);
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
