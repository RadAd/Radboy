package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import java.util.Arrays;

// TODO  https://blog.ryanlevick.com/DMG-01/public/book/memory_map.html
// 0xE000 - 0xFDFF: Echo RAM - Repeats  0xC000 - 0xDFFF: Working RAM
// 0xFEA0 - 0xFEFF: Unused

// TODO Separate out the Bus from Memory
public class Memory
{
    static interface Module
    {
        byte read(short loc);
        void write(short loc, byte val);
    }
    
    public static interface Listener
    {
        void changedMemory(short loc);
    }
    
    private final static short PORT_BEGIN = (short) 0xFF00;
    private final static short PORT_END = (short) 0xFF80;
    private final static short UNUSED_BEGIN = (short) 0xFEA0;
    private final static short UNUSED_END = (short) 0xFF00;
    
    private final byte data[] = new byte[65536];
    private final Module module[] = new Module[16];
    private final Module port[] = new Module[PORT_END - PORT_BEGIN];
    
    private final Module defaultModule = new Module()
        {
            public byte read(short loc)
            {
                return data[us(loc)];
            }
            
            public void write(short loc, byte val)
            {
                data[us(loc)] = val;
            }
        };
        
    final BootRom bootRom; // TODO a more generic way to redirect blocks of memory?
    private final java.util.Set<Listener> ls = new java.util.HashSet<Listener>();
    
    Memory(int biosMode)
    {
        Arrays.fill(port, defaultModule);
        Arrays.fill(module, defaultModule);
        Arrays.fill(data, us(PORT_BEGIN), us(PORT_END), (byte) 0xFF);
        bootRom = new BootRom(this, biosMode);
    }
    
    public void reset()
    {
        bootRom.reset();
    }
    
    public void add(Listener l)
    {
        ls.add(l);
    }

    public void remove(Listener l)
    {
        ls.remove(l);
    }

    public byte read(short loc)
    {
        if (isPort(loc))
            return port(loc).read(loc);
        else if (isUnused(loc))
            return (byte) 0xFF;
        else if (bootRom.doLoad(loc))
            return bootRom.read(loc);
        else
            return module(loc).read(loc);
    }

    public void write(short loc, byte val)
    {
        //debug("Memory write %04X %02X\n",  us(loc), ub(val));
        if (isPort(loc))
            port(loc).write(loc, val);
        else if (isUnused(loc))
            ; // do nothing
        else
            module(loc).write(loc, val);
            
        //debug("Memory write %04X\n", us(loc));
        for (Listener l : ls)
            l.changedMemory(loc);
    }

    public void read(int loc, byte[] vals)
    {
        //debug("Memory read %04X %d\n", (short) loc, vals.length);
        System.arraycopy(data, loc, vals, 0, vals.length);
    }

    public void write(int loc, byte[] vals)
    {
        //debug("Memory write %04X %d\n", (short) loc, vals.length);
        System.arraycopy(vals, 0, data, loc, vals.length);
    }

    public void write(int loc, short[] vals)
    {
        for (int i = 0; i < vals.length; i++)
        {
            write((short) (loc + i), (byte) vals[i]);
        }
    }
    
    Module port(short loc)
    {
        assert(isPort(loc));
        return port[portIndex(loc)];
    }
    
    Module module(short loc)
    {
        return module[moduleIndex(loc)];
    }
    
    void unloadPort(short loc)
    {
        port(loc, defaultModule);
    }
    
    void port(short loc, Module p)
    {
        assert(isPort(loc));
        checkForNull(p);
        port[portIndex(loc)] = p;
    }
    
    private static boolean isPort(short loc)
    {
        return us(loc) >= us(PORT_BEGIN) && us(loc) < us(PORT_END);
    }
    
    private static boolean isUnused(short loc)
    {
        return us(loc) >= us(UNUSED_BEGIN) && us(loc) < us(UNUSED_END);
    }
    
    private static int portIndex(short loc)
    {
        return us(loc) - us(PORT_BEGIN);
    }
    
    private static int moduleIndex(short loc)
    {
        return us(loc) >> 12;
    }
    
    void module(short loc, Module p)
    {
        checkForNull(p);
        module[moduleIndex(loc)] = p;
    }
    
    void interrupt(java.io.PrintStream o, byte i)
    {
        if (o != null) o.printf("interrupt %02X\n", i);
        //debug("interrupt %02X\n", i);
        write(Cpu.IF, bitSet(i, read(Cpu.IF)));
    }
    
    private static void checkForNull(Object o)
    {
        if (o == null)
            throw new NullPointerException();
    }
}
