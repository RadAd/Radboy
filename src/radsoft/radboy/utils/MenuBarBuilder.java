package radsoft.radboy.utils;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class MenuBarBuilder
{
    public static String ID = "ID";
    
    private final java.awt.event.ActionListener al;
    private final JMenuBar menuBar = new JMenuBar();
    private final java.util.Stack<JMenu> s = new java.util.Stack<JMenu>();
    
    public MenuBarBuilder(java.awt.event.ActionListener al)
    {
        this.al = al;
    }
    
    public JMenuBar get()
    {
        return menuBar;
    }
    
    private JMenu menu()
    {
        return s.empty() ? null : s.peek();
    }
    
    public MenuBarBuilder menu(String n)
    {
        final JMenu newMenu = new JMenu(n);
        return menu(newMenu);
    }
    
    public MenuBarBuilder menu(String n, int mnemonic)
    {
        final JMenu newMenu = new JMenu(n);
        newMenu.setMnemonic(mnemonic);
        return menu(newMenu);
    }
    
    public MenuBarBuilder menu(JMenu newMenu)
    {
        final JMenu menu = menu();
        if (menu == null)
            menuBar.add(newMenu);
        else
            menu.add(newMenu);
        s.push(newMenu);
        return this;
    }
    
    public MenuBarBuilder item(Object id, String n)
    {
        JMenu menu = menu();
        JMenuItem item = menu.add(n);
        item.putClientProperty(ID, id);
        item.addActionListener(al);
        return this;
    }
    
    public MenuBarBuilder item(Object id, String n, int mnemonic)
    {
        JMenu menu = menu();
        JMenuItem item = new JMenuItem(n);
        item.putClientProperty(ID, id);
        item.setMnemonic(mnemonic);
        item.addActionListener(al);
        menu.add(item);
        return this;
    }
    
    public MenuBarBuilder item(Object id, String n, int mnemonic, KeyStroke ks)
    {
        JMenu menu = menu();
        JMenuItem item = new JMenuItem(n);
        item.putClientProperty(ID, id);
        item.setMnemonic(mnemonic);
        item.setAccelerator(ks);
        item.addActionListener(al);
        menu.add(item);
        return this;
    }
    
    public MenuBarBuilder pop()
    {
        s.pop();
        return this;
    }
};
