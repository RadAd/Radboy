package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;

public class Joypad implements Memory.Module
{
    final static short JOYP = (short) 0xFF00;
    
    private final Memory mem;
    
    private byte joyp = 0;
    private byte buttons = 0xF;
    private byte direction = 0xF;
    private boolean interrupt = false;
    
    public enum Keys {
        Start(Bit3), Select(Bit2), B(Bit1), A(Bit0),
        Down(Bit3), Up(Bit2), Left(Bit1), Right(Bit0);
            
        Keys(byte m)
        {
            mask = m;
        }
        public final byte mask;
    }
    
    Joypad(Memory m)
    {
        mem = m;
        mem.port(JOYP, this);
    }
    
    public void setKey(final Keys k, final boolean down)
    {
        //debug("setKey %s %b\n", k, down);
        boolean before = false;
        switch (k)
        {
        case Start: case Select: case B: case A:    before = !testBit(k.mask, buttons); break;
        case Down: case Up: case Left: case Right:  before = !testBit(k.mask, direction); break;
        default: throw new IllegalArgumentException();
        }
        switch (k)
        {
        case Start: case Select: case B: case A:    buttons = down ? bitClear(k.mask, buttons) : bitSet(k.mask, buttons); break;
        case Down: case Up: case Left: case Right:  direction = down ? bitClear(k.mask, direction) : bitSet(k.mask, direction); break;
        default: throw new IllegalArgumentException();
        }
        //debug("setKey --- %b %b %02X %02X\n", before, down, buttons, direction);
        if (!before & down)
            interrupt = true;
    }

    void update(java.io.PrintStream o, int cycles)
    {
        joyp &= 0xF0;
        if (!testBit(Bit4, joyp))
            joyp |= direction;
        if (!testBit(Bit5, joyp))
            joyp |= buttons;
        if (interrupt)
        {
            //debug("Joypad interruprt\n");
            interrupt = false;
            mem.interrupt(o, Bit4);
        }
    }
        
    @Override
    public byte read(short loc)
    {
        switch (loc)
        {
        case JOYP: /*debug("JOYP %02X\n", joyp);*/ return joyp;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void write(short loc, byte val)
    {
        //debug("Joypad write %04X %02X\n", us(loc), ub(val));
        switch (loc)
        {
        case JOYP: joyp &= 0x0F; val &= 0xF0; joyp |= val; break;
        default: throw new IllegalArgumentException();
        }
    }
}
