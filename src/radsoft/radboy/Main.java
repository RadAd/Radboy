package radsoft.radboy;

import radsoft.radboy.Debugger;
import radsoft.radboy.core.Gameboy;
import radsoft.radboy.utils.MenuBarBuilder;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import java.util.Enumeration;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        boolean screen = true;
        boolean debug = false;
        boolean trace = false;
        int bios = 0;
        String rom = null;
        
        for (String s : args)
        {
            switch (s)
            {
            case "-ns":
                screen = false;
                break;

            case "-bd":
            case "-b0":
                bios = 0;
                break;

            case "-bq":
            case "-b1":
                bios = 1;
                break;

            case "-d":
                debug = true;
                break;

            case "-t":
                trace = true;
                break;

            default:
                if (rom == null)
                    rom = s;
                else
                    throw new IllegalArgumentException(s);
                break;
            }
        }
        
        if (screen && !debug)
        {
            Gameboy gb = new Gameboy(bios);
            GameboyMonitor mon = new GameboyMonitor();
            javax.swing.JFrame frame = new javax.swing.JFrame("RadBoy");
            frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            frame.setIconImage(loadImage("/game-boy-icon-53-32x32.png"));
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    mon.setBreak(true);
                }
            });
            frame.addKeyListener(new JoypadKeyListener(gb.joypad));
            java.awt.event.ActionListener al = new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    javax.swing.JMenuItem item = (javax.swing.JMenuItem) e.getSource();
                    Command id = (Command) item.getClientProperty(MenuBarBuilder.ID);
                    //System.out.println("actionPerformed " + id);
                    switch (id)
                    {
                    case FILE_EXIT:
                        frame.dispose();
                        break;
                        
                    case HELP_ABOUT:
                        javax.swing.JOptionPane.showMessageDialog(frame, findVersionInfo(), "About", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        break;
                        
                    default:
                        javax.swing.JOptionPane.showMessageDialog(frame, "ID: " + id, "Not Handled", javax.swing.JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                }
            };
            frame.setJMenuBar(new MenuBarBuilder(al)
                .menu("File", 'F')
                    .item(Command.FILE_OPEN, "Load ROM", 'L', KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK))
                    .item(Command.FILE_EXIT, "Exit", 'X', KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK))
                    .pop()
                .menu("Help", 'H')
                    .item(Command.HELP_ABOUT, "About...", 'A')
                    .pop()
                .get());
            frame.getContentPane().add(new ScreenPane(gb.video));
            frame.pack();
            frame.setVisible(true);

            if (rom != null)
            try
            {
                gb.cart.load(rom);
                gb.run(null, mon);
            }
            catch (java.io.IOException e)
            {
                javax.swing.JOptionPane.showMessageDialog(frame, e, "Error loading rom", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
        else
        {
            //System.out.println("Gameboy test");
            Debugger dbg = new Debugger(rom, screen, bios);
            dbg.trace = trace;
            //dbg.gb.link.out = null;
            byte ret = 0;
            if (rom == null || debug)
                dbg.go();
            else
                ret = dbg.run();
            System.exit(ret & 0xFF);
        }
    }
    
    enum Command
    {
        FILE_OPEN,
        FILE_EXIT,
        HELP_ABOUT,
    };
    
    static java.awt.Image loadImage(String f)
    {
        return new javax.swing.ImageIcon(Main.class.getResource(f)).getImage();
    }
    
    private static String findVersionInfo()
    {
        try
        {
            //Enumeration<URL> resources = Main.class.getResources("META-INF/MANIFEST.MF");
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
            StringBuilder sb = new StringBuilder();
            while (resources.hasMoreElements())
            {
                URL manifestUrl = resources.nextElement();
                Manifest manifest = new Manifest(manifestUrl.openStream());
                Attributes mainAttributes = manifest.getMainAttributes();
                String implementationTitle = mainAttributes.getValue("Implementation-Title");
                if (implementationTitle != null)
                {
                    String implementationVersion = mainAttributes.getValue("Implementation-Version");
                    String implementationVendor = mainAttributes.getValue("Implementation-Vendor");
                    
                    if (sb.length() != 0)
                        sb.append("\n");
                    sb.append(String.format("%s by %s %s", implementationTitle, implementationVendor, implementationVersion));
                }
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            return e.toString();
        }
    }
}
