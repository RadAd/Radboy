package radsoft.radboy;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import radsoft.radboy.core.Joypad;

class JoypadKeyListener implements KeyListener
{
    private final Joypad joypad;
    
    JoypadKeyListener(Joypad joypad)
    {
        this.joypad = joypad;
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
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        //debug("keyPressed %s\n", e);
        Joypad.Keys k = getKey(e.getKeyCode());
        if (k != null)
        {
            e.consume();
            joypad.setKey(k, true);
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e)
    {
        //debug("keyReleased %s\n", e);
        Joypad.Keys k = getKey(e.getKeyCode());
        if (k != null)
        {
            e.consume();
            joypad.setKey(k, false);
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e)
    {
    }
}
