package radsoft.radboy;

import radsoft.radboy.Debugger;
import radsoft.radboy.core.Gameboy;

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
        
        byte ret = 0;

        if (screen && !debug)
        {
            Gameboy gb = new Gameboy(bios, rom);
            GameboyMonitor mon = new GameboyMonitor();
            javax.swing.JFrame frame = new javax.swing.JFrame("Gameboy");
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
            Screen s = new Screen();
            gb.video.lcd = s;
            frame.getContentPane().add(s.comp);
            frame.pack();
            frame.setVisible(true);
            ret = gb.run(null, mon);
        }
        else
        {
            //System.out.println("Gameboy test");
            Debugger dbg = new Debugger(rom, screen, bios);
            dbg.trace = trace;
            //dbg.gb.link.out = null;
            if (rom == null || debug)
                dbg.go();
            else
                ret = dbg.run();
        }
            
        System.exit(ret & 0xFF);
    }
    
    static java.awt.Image loadImage(String f)
    {
        return new javax.swing.ImageIcon(Main.class.getResource(f)).getImage();
    }
}
