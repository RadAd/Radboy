package radsoft.radboy;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.GroupLayout.*;
import javax.swing.event.*;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.Types.*;

public class Registers implements PropertyChangeListener, Cpu.Listener
{
    final JDialog dlg;
    //JComponent comp;
    java.util.Map<Cpu.Reg8, JFormattedTextField> map = new java.util.HashMap<>();
    Cpu cpu;
    
    Registers(Frame f, Cpu cpu)
    {
        this.dlg = new JDialog(f, "Stack");
        this.cpu = cpu;
        cpu.add(this);
        
        dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                cpu.remove(Registers.this);
            }
        });
        
        dlg.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        
        boolean a = false;
        
        Cpu.Reg8 rs[] = { Cpu.Reg8.A, Cpu.Reg8.F, Cpu.Reg8.B, Cpu.Reg8.C, Cpu.Reg8.D, Cpu.Reg8.E, Cpu.Reg8.H, Cpu.Reg8.L };
        for(Cpu.Reg8 r : rs )
        {
            JFormattedTextField t;
            map.put(r, t = add(dlg, r.toString(), c));
            t.putClientProperty("reg", r);
            if (a)
                ++c.gridy;
            a = !a;
        }
        
        dlg.pack();
        dlg.setVisible(true);
        
        update();
    }
    
    void update()
    {
        for(Cpu.Reg8 r : map.keySet())
        {
            update(r);
        }
    }
    
    void update(Cpu.Reg8 r)
    {
        JFormattedTextField t = map.get(r);
        if (t != null)
        {
            t.setValue(ub(cpu.reg(r)));
        }
    }
    
    JFormattedTextField add(Container cp, String label, GridBagConstraints c)
    {
        try
        {
            javax.swing.JFormattedTextField.AbstractFormatter fmter = new javax.swing.text.MaskFormatter("HH") {
                @Override
                public String valueToString(Object value)
                {
                    String s = null;
                    if (value instanceof Integer)
                        s = String.format("%02X", (int) value);
                    else
                        s = "00";
                    return s;
                }
                @Override
                public Object stringToValue(String s)
                {
                    int v = Integer.parseInt(s, 16);
                    return v;
                }
            };
            JLabel l;
            JFormattedTextField t;
            cp.add(l = new JLabel(label, SwingConstants.RIGHT), c);
            cp.add(t = new JFormattedTextField(fmter), c);
            t.addPropertyChangeListener(this);
            FontMetrics fm = t.getFontMetrics(t.getFont());
            t.setPreferredSize(new Dimension(fm.stringWidth("FFF"), fm.getHeight()));
            return t;
        }
        catch (java.text.ParseException e)
        {
            throw new Error(e);
        }
    }
    
    // PropertyChangeListener
    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getPropertyName().equals("value"))
        {
            JComponent comp = (JComponent) e.getSource();
            Cpu.Reg8 r = (Cpu.Reg8) comp.getClientProperty("reg");
            if (r != null)
            {
                byte b = (byte) (int) e.getNewValue();
                cpu.reg(r, b);
            }
        }
    }
    
    // Cpu.Listener
    
    @Override
    public void changedRegister(Cpu.Reg8 r)
    {
        update(r);
    }
}
