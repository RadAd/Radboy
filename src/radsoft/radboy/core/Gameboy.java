package radsoft.radboy.core;

//import static radsoft.radboy.utils.Types.*;

// http://gbdev.gg8.se/wiki/articles/CPU_Comparision_with_Z80
// http://www.devrs.com/gb/files/opcodes.html
// http://bgb.bircd.org/pandocs.htm#cpuregistersandflags

// http://gbdev.gg8.se/files/roms/blargg-gb-tests/

public class Gameboy
{
    public interface Monitor
    {
        boolean doBreak();
        void onInfiniteLoop();
        void onStop();
    };
    
    public final Memory mem;
    public final Cartridge cart;
    public final Cpu cpu;
    public final Timer timer;
    public final Video video;
    public final Link link;
    public final Joypad joypad;

    public Gameboy(int biosMode, String romfile) throws java.io.IOException
    {
        mem = new Memory(biosMode);
        cart = new Cartridge(romfile, mem);
        cpu = new Cpu(mem);
        timer = new Timer(mem);
        video = new Video(mem);
        link = new Link(mem);
        joypad = new Joypad(mem);
    }

    public void step(java.io.PrintStream o, Monitor mon)
    {
        final int precycle = cpu.cycle;
        cpu.step(o, true, mon);
        
        final int diffcycle = cpu.cycle - precycle;
        timer.update(o, diffcycle);
        video.update(o, diffcycle);
        link.update(o, diffcycle);
        joypad.update(o, diffcycle);
    }

    public byte run(java.io.PrintStream o, Monitor mon)
    {
        while (!mon.doBreak())
            step(o, mon);
        return cpu.a;
    }
}
