package radsoft.radboy;

import radsoft.radboy.Debugger;

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
        //System.out.println("Gameboy test");
        Debugger dbg = new Debugger(rom, screen, bios);
        dbg.trace = trace;
        //dbg.gb.link.out = null;
        if (rom == null || debug)
            dbg.go();
        else
            ret = dbg.run();
            
        System.exit(ret & 0xFF);
    }
}
