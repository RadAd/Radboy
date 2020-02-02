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

// TODO Use java.util.logging.Logger

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
            final Gameboy gb = new Gameboy(bios);
            final GameboyMonitor mon = new GameboyMonitor(gb);
            final javax.swing.JFrame frame = new javax.swing.JFrame("RadBoy");
            frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            frame.setIconImage(loadImage("/game-boy-icon-53-32x32.png"));
            final Thread.UncaughtExceptionHandler eh = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e)
                {
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run()
                        {
                            javax.swing.JOptionPane.showMessageDialog(frame, e, "Error running thread ", javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    e.printStackTrace();
                }
            };
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    mon.setBreak(true);
                }
            });
            frame.addKeyListener(new JoypadKeyListener(gb.joypad));
            java.awt.event.ActionListener al = new java.awt.event.ActionListener() {
                java.io.File dir = new java.io.File(".").getAbsoluteFile();
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event)
                {
                    javax.swing.JMenuItem item = (javax.swing.JMenuItem) event.getSource();
                    Command id = (Command) item.getClientProperty(MenuBarBuilder.ID);
                    //System.out.println("actionPerformed " + id);
                    switch (id)
                    {
                    case FILE_OPEN:
                        javax.swing.JFileChooser fc = new javax.swing.JFileChooser(dir);
                        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Gameboy files", "gb"));
                        int returnVal = fc.showOpenDialog(frame);
                        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
                        {
                            dir = fc.getCurrentDirectory();
                            java.io.File selectedFile = fc.getSelectedFile();
                            try
                            {
                                // TODO Need to reset gameboy
                                mon.stop();
                                gb.cart.load(selectedFile.getAbsolutePath());
                                gb.reset();
                                mon.start(eh);
                            }
                            catch (InterruptedException e)
                            {
                                javax.swing.JOptionPane.showMessageDialog(frame, e, "Error stopping thread ", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                            catch (java.io.IOException e)
                            {
                                javax.swing.JOptionPane.showMessageDialog(frame, e, "Error loading rom", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        break;
                        
                    case FILE_RESET:
                        gb.reset();
                        break;
                        
                    case FILE_EXIT:
                        frame.dispose();
                        break;
                        
                    case DEBUG_PAUSE:
                        if (mon.isPaused())
                            mon.start(eh);
                        else
                            try
                            {
                                mon.stop();
                            }
                            catch (InterruptedException e)
                            {
                                javax.swing.JOptionPane.showMessageDialog(frame, e, "Error stopping thread ", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        break;
                        
                    case DEBUG_TRACE:
                        TraceView tv = new TraceView(frame, mon);
                        tv.open();
                        break;
                        
                    case DEBUG_REG:
                        Registers rv = new Registers(gb.cpu);
                        //rv.open();
                        break;
                        
                    case DEBUG_MEM:
                        MemWatchView mwv = new MemWatchView(frame, gb.mem);
                        mwv.open();
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
            radsoft.radboy.utils.MenuModel mm = new radsoft.radboy.utils.MenuModel() {
                @Override
                public boolean isEnabled(javax.swing.JMenuItem item)
                {
                    return true;
                }
                
                @Override
                public boolean isSelected(javax.swing.JMenuItem item)
                {
                    Command id = (Command) item.getClientProperty(MenuBarBuilder.ID);
                    //System.out.println("isSelected " + id);
                    switch (id)
                    {
                    case DEBUG_PAUSE:
                        return mon.isPaused();
                    default:
                        return false;
                    }
                }
            };
            frame.setJMenuBar(new MenuBarBuilder(al, mm)
                .menu("File", 'F')
                    .item(Command.FILE_OPEN, "Load ROM...", 'L', KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK))
                    .item(Command.FILE_RESET, "Reset", 'R')
                    .item(Command.FILE_EXIT, "Exit", 'X', KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK))
                    .pop()
                .menu("Debug", 'D')
                    .check(Command.DEBUG_PAUSE, "Pause", 'P')
                    .item(Command.DEBUG_TRACE, "Trace...", 'T')
                    .item(Command.DEBUG_REG, "Registers...", 'R')
                    .item(Command.DEBUG_MEM, "Memory...", 'M')
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
                mon.stop();
                mon.start(eh);
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
        FILE_RESET,
        FILE_EXIT,
        DEBUG_PAUSE,
        DEBUG_TRACE,
        DEBUG_REG,
        DEBUG_MEM,
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
