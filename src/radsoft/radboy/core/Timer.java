package radsoft.radboy.core;

import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import static radsoft.radboy.utils.Types.*;

public class Timer implements Memory.Module
{
    final static short DIV  = (short) 0xFF04;
    final static short TIMA = (short) 0xFF05;
    final static short TMA  = (short) 0xFF06;
    final static short TAC  = (short) 0xFF07;
    
    private final Memory mem;
    
    private byte div = 0;
    private byte tima = 0;
    private byte tma = 0;
    private byte tac = 0;
    
    private int divcycle = 0;
    private int timacycle = 0;
    
    Timer(Memory m)
    {
        mem = m;
        mem.port(DIV, this);
        mem.port(TIMA, this);
        mem.port(TMA, this);
        mem.port(TAC, this);
    }
    
    void update(java.io.PrintStream o, int cycles)
    {
        // Clock Speed  - 4194.304 MHz
        
        divcycle += cycles;
        div += divcycle / 256; // 16384 hz
        divcycle %= 256;
        
        if (testBit(Bit2, tac))
        {
            timacycle += cycles;
            
                // { 4096 hz, 262144 hz, 65536 hz, 16384 hz }
            int freq[] = { 1024, 16, 64, 256 };
            int f = freq[tac & 0x03];
            //debug("freq %d\n", f);
            
            final byte pretima = tima;
            tima += timacycle / f;
            timacycle %= f;
            
            if (ub(tima) < ub(pretima)) // overflow
            {
                tima = tma;
                mem.interrupt(o, Bit2);
            }
        }
    }

    @Override
    public byte read(short loc)
    {
        switch (loc)
        {
        case DIV:  return div;
        case TIMA: return tima;
        case TMA:  return tma;
        case TAC:  return tac;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void write(short loc, byte val)
    {
        //debug("Timer write %04X %02X\n", us(loc), ub(val));
        switch (loc)
        {
        case DIV:  div = 0; break;
        case TIMA: tima = val; break;
        case TMA:  tma = val; break;
        case TAC:  tac = val; break;
        default: throw new IllegalArgumentException();
        }
    }
}
