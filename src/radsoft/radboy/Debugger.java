package radsoft.radboy;

import radsoft.radboy.core.*;
import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import radsoft.radboy.utils.ShortEx;
import java.io.*;

public class Debugger
{
    final Gameboy gb;
    final java.util.Set<Short> bp = new java.util.HashSet<Short>();
    public boolean trace = false;
    public boolean sspinner = true;
    private final GameboyMonitor mon = new GameboyMonitor();
    
    final PrintStream o = System.out;
    final BufferedReader i = new BufferedReader(new InputStreamReader(System.in));
    // TODO Maybe try scanner

    final static char spinner[] = { '|', '/', '-', '\\' };
    final static char c[] = { '.', '-', '+', 'X' };
            
    public Debugger(String rom, boolean screen, int biosMode) throws IOException
    {
        gb = new Gameboy(biosMode, rom);
        if (false)
        {
            gb.video.lcd = new Video.Lcd() {
                public void updateRow(byte y, byte data[])
                {
                    for (byte tx : data)
                        debug("%s", c[tx]);
                    debugln("");
                
                    //throw new IllegalArgumentException();
                }
            };
        }
        if (screen)
            openScreen();
        gb.link.out = o;
        
        mon.breakOnInfiniteLoop = true;
        mon.breakOnStop = true;

        // TODO Dump rom details - name, id, romsize
        o.printf("Title: %s\n", gb.cart.getTitle());
        o.printf("Manufacturer: %s\n", gb.cart.getManufacturer());
        o.printf("Licensee: %s\n", gb.cart.getLicensee());
        o.printf("MBC: %s\n", gb.cart.getMBC());
        o.printf("Rom Size: %d\n", gb.cart.getRomSize());
        o.printf("Ram Size: %d\n", gb.cart.getRamSize());
    }
    
    public void go()
    {
        sspinner = true;
        boolean exit = false;
        while (!exit)
        {
            try
            {
                o.print("gb> ");
                final String line = i.readLine();
                final String args[] = line.split(" ");
                switch (args[0])
                {
                case "":
                    break;

                case "bp":
                    if (args.length <= 1)
                    {
                        for (short l : bp)
                        {
                            o.printf(" $%04X\n", l);
                        }
                    }
                    else if (args.length <= 2)
                    {
                        o.println(" bp [add|del] [loc]");
                    }
                    else switch (args[1])
                    {
                        case "add":
                        {
                            short l = parseShort(args[2]);
                            bp.add(l);
                        }
                        break;
                        case "del":
                        {
                            short l = parseShort(args[2]);
                            bp.remove(l);
                        }
                        break;
                    }
                    break;

                case "g":
                case "go":
                    run();
                    break;

                case "flushlcd":
                    gb.video.updateLcd();
                    break;

                case "l":
                case "list":
                    {
                        short loc = gb.cpu.pc;
                        if (args.length > 1 && !args[1].equals("pc"))
                            loc = parseShort(args[1]);
                            
                        int count = 10;
                        if (args.length > 2)
                            count = Integer.parseInt(args[2]);
                            
                        short pc = gb.cpu.pc;
                        gb.cpu.pc = loc;
                        for (int i = 0; i < count; ++i)
                        {
                            o.print(bp.contains(gb.cpu.pc) ? '*' : ' ');
                            gb.cpu.step(o, false, mon);
                        }
                        gb.cpu.pc = pc;
                    }
                    break;

                case "maptile":
                    {
                        PrintStream f = o;
                        if (args.length > 2)
                            f = new PrintStream(new File(args[1]));
                        showMapTile(f);
                        //f.close();
                    }
                    break;

                case "map":
                    {
                        PrintStream f = o;
                        if (args.length > 2)
                            f = new PrintStream(new File(args[1]));
                        showMap(f);
                        //f.close();
                    }
                    break;

                case "mem":
                    {
                        short loc;
                        if (args.length <= 1)
                            loc = gb.cpu.reg(Cpu.Reg16.HL);
                        else
                            loc = parseShort(args[1]);
                        for (byte i = 0; i < 8; ++i)
                        {
                            short sloc = loc;
                            o.printf(" $%04X: ", loc);
                            for (byte l = 0; l < 8; ++l)
                            {
                                o.printf(" %02X", gb.mem.read(loc++));
                            }
                            o.print("     ");
                            for (byte l = 0; l < 8; ++l)
                            {
                                char ch = (char) gb.mem.read(sloc++);
                                o.print(Character.isISOControl(ch) ? '.' : ch);
                            }
                            o.println();
                        }
                    }
                    break;

                case "r":
                case "reg":
                    o.printf(" A=%02X     F=%02X    %s\n", gb.cpu.a, gb.cpu.f, gb.cpu.flags());
                    o.printf(" B=%02X     C=%02X \n", gb.cpu.b, gb.cpu.c);
                    o.printf(" D=%02X     E=%02X \n", gb.cpu.d, gb.cpu.e);
                    o.printf(" H=%02X     L=%02X    HL=%04X \n", gb.cpu.h, gb.cpu.l, gb.cpu.reg(Cpu.Reg16.HL));
                    o.printf(" PC=%04X  SP=%04X \n", gb.cpu.pc, gb.cpu.sp);
                    o.printf(" interrupts = %b   state = %s \n", gb.cpu.interrupts, gb.cpu.state);
                    o.printf("\n");
                    o.printf(" LCDC=%02X  STAT=%02X \n", gb.video.lcdc, gb.video.stat);
                    o.printf(" LY=%02X    LYC=%02X \n", gb.video.ly, gb.video.lyc);
                    o.printf(" SCX=%02X   SCY=%02X \n", gb.video.scx, gb.video.scy);
                    o.printf(" WX=%02X    WY=%02X \n", gb.video.wx, gb.video.wy);
                    break;

                case "set":
                    {
                        String n = args[1];
                        Cpu.Reg8 r8 = Cpu.FindReg8(n);
                        Cpu.Reg16 r16 = Cpu.FindReg16(n);
                        if (n.equalsIgnoreCase("pc"))
                        {
                            short v = parseShort(args[2]);
                            gb.cpu.pc = v;
                        }
                        else if (r8 != null)
                        {
                            byte v = parseByte(args[2]);
                            gb.cpu.reg(r8, v);
                        }
                        else if (r16 != null)
                        {
                            short v = parseShort(args[2]);
                            gb.cpu.reg(r16, v);
                        }
                        else
                        {
                            o.printf("Unknown register %s\n", n);
                        }
                    }
                    break;
                    
                case "show":
                    switch (args[1])
                    {
                    case "mem":
                        MemWatchView mwv = new MemWatchView(gb.mem);
                        mwv.open();
                        break;
                    case "reg":
                        Registers rv = new Registers(gb.cpu);
                        break;
                    };
                    break;
                    
                case "tile":
                    {
                        byte n = parseByte(args[1]);
                        o.printf(" tile: %02X\n", n);
                        byte data[] = new byte[8];
                        for (byte tileY = 0; tileY < 8; ++tileY)
                        {
                            gb.video.getTileLine(n, tileY, data);
                            for (byte bx : data)
                                o.print(c[bx]);
                            o.println();
                        }
                        o.println();
                    }
                    break;

                case "screen":
                    openScreen();
                    break;

                case "s":
                case "step":
                    {
                        if (args.length > 1)
                        {
                            short l = parseShort(args[1]);
                            boolean contains = bp.contains(l);
                            bp.add(l);
                            run();
                            if (!contains)
                                bp.remove(l);
                        }
                        else
                            gb.step(o, mon);
                    }
                    break;

                case "trace":
                    if (args.length > 1)
                    {
                        switch (args[1])
                        {
                        case "false":
                        case "off":
                            trace = false;
                            break;
                        case "true":
                        case "on":
                            trace = true;
                            break;
                        default:
                            o.println("trace <on|off>");
                            break;
                        }
                    }
                    else
                    {
                        o.printf("trace: %b\n", trace);
                    }
                    break;

                case "x":
                case "exit":
                    System.gc();
                    for (java.awt.Window window : java.awt.Window.getWindows())
                    {
                        window.dispose();
                    }
                    exit = true;
                    break;

                default:
                    o.printf("Unknown command: %s\n", args[0]);
                    break;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            catch (Error e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private javax.swing.JFrame sf;
    
    void openScreen()
    {
        if (sf == null)
        {
            sf = new javax.swing.JFrame("Gameboy");
            sf.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            sf.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    sf = null;
                    gb.video.lcd =  null;
                }
            });
            sf.addKeyListener(new JoypadKeyListener(gb.joypad));
            Screen s = new Screen();
            gb.video.lcd = s;
            sf.getContentPane().add(s.comp);
            sf.pack();
        }
        sf.setVisible(true);
    }
    
    void showMapTile(java.io.PrintStream o)
    {
        o.printf(" map: scx: %d scy: %d\n", ub(gb.video.scx), ub(gb.video.scy));
        byte mapdata[] = new byte[32];
        for (byte y = 0; y < 32; ++y)
        {
            gb.video.getTileLine(y, mapdata);
            for (byte bx : mapdata)
                o.printf(" %02X", bx);
            o.println();
        }
    }
    
    void showMap(java.io.PrintStream o)
    {
        showMapTile(o);
        o.println();
        
        o.printf(" map: scx: %d scy: %d\n", ub(gb.video.scx), ub(gb.video.scy));
        byte bgdata[] = new byte[Video.Width];

        for (byte y = 0; y < Video.Height; ++y)
        {
            gb.video.getPixelLine(y, bgdata);
            
            for (byte tx : bgdata)
                o.print(c[tx]);
            o.println();
        }
        o.println();
    }

    public byte run() throws InterruptedException
    {
        int si = 0;
        int p = 0;
        mon.setBreak(false);
        ReadyThread t = new ReadyThread();
        t.start();
        while (!mon.doBreak())
        {
            gb.step(trace ? o : null, mon);
            ++p;
            if (sspinner && !trace && p == 10000) { o.print(spinner[si++]); o.print('\b'); p = 0; si = si % spinner.length; }
            if (bp.contains(gb.cpu.pc))
                break;
        }
        t.exit = true;
        t.join();
        if (!trace) o.println();
        return gb.cpu.a;
    }

    private static byte parseByte(String s)
    {
        // TODO Use decode
        return (byte) Integer.parseInt(s, 16);
    }
    
    private static short parseShort(String s)
    {
        // TODO Use decode
        return (short) Integer.parseInt(s, 16);
    }

    class ReadyThread extends Thread
    {
        public boolean exit = false;
        public void run()
        {
            try
            {
                while (!exit && !i.ready())
                {
                }
            }
            catch (IOException e)
            {
            }
            mon.setBreak(true);
        }
    };
}
