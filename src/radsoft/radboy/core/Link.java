package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;

public class Link implements Memory.Module
{
    final static short SB = (short) 0xFF01;
    final static short SC = (short) 0xFF02;
    
    private final Memory mem;
    
    private byte sb = 0;
    private byte sc = 0;
    
    public java.io.PrintStream out;
    
    Link(Memory m)
    {
        mem = m;
        mem.port(SB, this);
        mem.port(SC, this);
    }

    void update(java.io.PrintStream o, int cycles)
    {
        if (testBit(Bit7, sc))
        {
            if (testBit(Bit0, sc))
            {
                if (out != null) out.print((char) ub(sb));    // TODO Move this to a listener
                
                sc = bitClear(Bit7, sc);
                sb = (byte) 0xFF;   // TODO If connected to another Gameboy, retrieve its value
                mem.interrupt(o, Bit3);
            }
            // else check if connected Gameboy has sent a value
        }
    }
        
    @Override
    public byte read(short loc)
    {
        switch (loc)
        {
        case SB: return sb;
        case SC: return sc;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void write(short loc, byte val)
    {
        //debug("Link write %04X %02X\n", us(loc), ub(val));
        switch (loc)
        {
        case SB: sb = val; break;
        case SC: sc = val; break;
        default: throw new IllegalArgumentException();
        }
    }
}
