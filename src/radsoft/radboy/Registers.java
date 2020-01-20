package radsoft.radboy;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout.*;
import javax.swing.event.*;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.Types.*;

public class Registers implements ActionListener, AncestorListener, Cpu.Listener
{
    JFrame frame = new JFrame("Registers");
    //JComponent comp;
    java.util.Map<Cpu.Reg8, JTextField> map = new java.util.HashMap<>();
    Cpu cpu;
    
    Registers(Cpu cpu) throws java.text.ParseException
    {
        this.cpu = cpu;
        cpu.add(this);
        
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JComponent cp = (JComponent) frame.getContentPane();
        cp.addAncestorListener(this);
        cp.setLayout(new GridBagLayout());
        cp.setBackground(UIManager.getColor("control"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        
        boolean a = false;
        
        Cpu.Reg8 rs[] = { Cpu.Reg8.A, Cpu.Reg8.F, Cpu.Reg8.B, Cpu.Reg8.C, Cpu.Reg8.D, Cpu.Reg8.E, Cpu.Reg8.H, Cpu.Reg8.L };
        for(Cpu.Reg8 r : rs )
        {
            JTextField t;
            map.put(r, t = add(cp, r.toString(), c));
            t.putClientProperty("reg", r);
            if (a)
                ++c.gridy;
            a = !a;
        }
        
        frame.pack();
        frame.setVisible(true);
        
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
        JTextField t = map.get(r);
        if (t != null)
            t.setText(String.format("%02X", cpu.reg(r)));
    }
    
    JTextField add(Container cp, String label, GridBagConstraints c) throws java.text.ParseException
    {
        JLabel l;
        JTextField t;
        cp.add(l = new JLabel(label, SwingConstants.RIGHT), c);
        cp.add(t = new JFormattedTextField(new javax.swing.text.MaskFormatter("HH")), c);
        t.addActionListener(this);
        FontMetrics fm = t.getFontMetrics(t.getFont());
        t.setPreferredSize(new Dimension(fm.stringWidth("FFF"), fm.getHeight()));
        return t;
    }
    
    // ActionListener
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        JComponent comp = (JComponent) e.getSource();
        Cpu.Reg8 r = (Cpu.Reg8) comp.getClientProperty("reg");
        byte b = (byte) Integer.parseInt(e.getActionCommand(), 16);
        cpu.reg(r, b);
    }
    
    // AncestorListener
    
    @Override
    public void ancestorAdded(AncestorEvent e)
    {
    }
    
    @Override
    public void ancestorMoved(AncestorEvent e)
    {
    }
    
    @Override
    public void ancestorRemoved(AncestorEvent e)
    {
        cpu.remove(this);
    }
    
    // Cpu.Listener
    
    @Override
    public void changedRegister(Cpu.Reg8 r)
    {
        update(r);
    }
}
