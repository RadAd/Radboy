package radsoft.radboy.utils;

import javax.swing.JMenuItem;

public interface MenuModel
{
    boolean isEnabled(JMenuItem item);
    boolean isSelected(JMenuItem item);
}
