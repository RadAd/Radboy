package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;

import radsoft.radboy.utils.ByteEx;

// TODO
// Can I consolidate the half/carry flag calculation?

public class Cpu implements Memory.Module
{
    public static interface Listener
    {
        void changedRegister(Reg8 r);
    }
    
    final static short IE = (short) 0xFFFF;
    final static short IF = (short) 0xFF0F;
    
    private final static short CX = (short) 0xFF00;
    
    public byte rif;

    public enum State { Normal, Stop, Halt }
    
    public boolean interrupts = false;
    public State state = State.Normal;
    int cycle = 0;

    public enum Reg8
    { B, C, D, E, H, L,
        HL {
            @Override
            public String toString()
            {
                return "(HL)";
            }
        }, A, F
    }
    
    public enum Reg16
    { BC, DE, HL, SP, AF }
    
    public enum Flags
    { Z(Bit7), N(Bit6), H(Bit5), C(Bit4);

        Flags(byte m)
        {
            mask = m;
        }
        public final byte mask;
    }
    
    public enum Test
    { NZ(Flags.Z, true), Z(Flags.Z, false),
      NC(Flags.C, true), C(Flags.C, false);

        Test(Flags f, boolean n)
        {
            flag = f;
            negate = n;
        }
        public final Flags flag;
        public final boolean negate;
    }

    public byte a;
    public byte f;
    public byte b;
    public byte c;
    public byte d;
    public byte e;
    public byte h;
    public byte l;

    public short sp;
    public short pc = 0x0000;

    private final Memory mem;
    
    private final java.util.Set<Listener> ls = new java.util.HashSet<Listener>();
    
    Cpu(Memory m)
    {
        mem = m;
        mem.port(IF, this);
    }
    
    public void reset()
    {
        a = 0;
        f = 0;
        b = 0;
        c = 0;
        d = 0;
        e = 0;
        h = 0;
        l = 0;
        sp = 0x0000;
        pc = 0x0000;
    }
    
    public int cycle() { return cycle; }

    public void add(Listener l)
    {
        ls.add(l);
    }

    public void remove(Listener l)
    {
        ls.remove(l);
    }

    public void step(java.io.PrintStream o, boolean execute, Gameboy.Monitor mon)
    {
        final boolean dointerrupt = execute && interrupts && checkinterrupt(o);
        
        if (!dointerrupt && execute && state != State.Normal)
            cycle += 4;
        else if (!dointerrupt)
            opcode(o, execute, mon);
    }
    
    private void opcode(final java.io.PrintStream o, final boolean execute, Gameboy.Monitor mon)
    {
        // https://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html
        // http://www.devrs.com/gb/files/opcodes.html
        // http://www.z80.info/decoding.htm
        // https://gb-archive.github.io/salvage/decoding_gbz80_opcodes/Decoding Gamboy Z80 Opcodes.html

        final int precycle = cycle;
        final short orig_pc = pc;
        if (o != null) o.printf(" $%04X: ", pc);
        final byte opcode = fetch();

        boolean unknown = false;
        final int x = extractBits(opcode, 2, 6);
        final int y = extractBits(opcode, 3, 3);
        final int z = extractBits(opcode, 3, 0);
        final int p = extractBits(opcode, 2, 4);
        final int q = extractBits(opcode, 1, 3);

        //if (o != null) o.printf("--- 0x%02X, x=%d y=%d z=%d ", opcode, x, y, z);
        switch (x)
        {
        case 0: // x
            switch (z)
            {
            case 0: // x=0, z
                switch (y)
                {
                case 0:
                    {
                        if (o != null) o.printf("NOP\n");
                        assert(cycle - precycle == 4);
                    }
                    break;
                case 1:
                    {
                        short loc = d16();
                        if (o != null) o.printf("LD ($%04X), SP\n", loc);

                        if (execute)
                        {
                            st(loc, lowbyte(sp)); // TODO is this the correct order
                            loc++;
                            st(loc, highByte(sp));
                            
                            if (o != null) o.printf("    %s\n", sr(Reg16.SP));
                            assert(cycle - precycle == 20);
                        }
                    }
                    break;
                case 2:
                    {
                        byte v = d8(); // ignored
                        if (o != null) o.printf("STOP %02X\n", v);

                        if (execute)
                        {
                            mon.onStop();
                            state(State.Stop);
                            assert(cycle - precycle == 8); // Some docs say 4
                        }
                    }
                    break;
                case 3:
                    {
                        byte v = d8(); // signed
                        if (o != null) o.printf("JR %d -> $%04X\n", v, us(pc) + v);

                        if (execute)
                        {
                            if (v == -2 || v == -3)
                                mon.onInfiniteLoop();
                                
                            cycle += 4;
                            pc += v;
                            
                            if (o != null) o.printf("    %s\n", spc());
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                case 4: case 5: case 6: case 7:
                    {
                        Test t = GetTest(y - 4);
                        byte v = d8(); // signed
                        if (o != null) o.printf("JR %s, %d ->  $%04X\n", t, (int) v, us(pc) + v);

                        if (execute)
                        {
                            boolean tr = cc(t);
                            if (tr)
                            {
                                cycle += 4;
                                pc += v;
                            }
                            
                            if (o != null) o.printf("    %b F=%s %s\n", tr, flags(), spc());
                            assert(cycle - precycle == (tr ? 12 : 8));
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 1: // x=0, z
                switch (q)
                {
                case 0:
                    {
                        Reg16 r = GetReg16(p);
                        short v = d16();
                        if (o != null) o.printf("LD %s, $%04X\n", r, v);

                        if (execute)
                        {
                            reg(r, v);
                            
                            if (o != null) o.printf("    %s\n", sr(r));
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                case 1:
                    {
                        Reg16 r = GetReg16(p);
                        if (o != null) o.printf("ADD HL, %s\n", r);

                        if (execute)
                        {
                            cycle += 4;
                            final short ohl = reg(Reg16.HL);
                            final short off = reg(r); // signed short???
                            
                            short nhl = ohl;
                            nhl += off;
                            reg(Reg16.HL, nhl);
                            
                            flag(Flags.N, false);
                            flag(Flags.H, ((ohl & 0xFFF) + (off & 0xFFF)) > 0xFFF);
                            flag(Flags.C, ((ohl & 0xFFFF) + (off & 0xFFFF)) > 0xFFFF);
                            
                            if (o != null) o.printf("    %s F=%s\n", sr(Reg16.HL), flags());
                            assert(cycle - precycle == 8);
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 2: // x=0, z
                {
                    Reg16 r = GetReg16G(p);
                    char pm = "  +-".charAt(p);
                    if (o != null) switch (q)
                    {
                    case 0: o.printf("LD (%s%c), A\n", r, pm); break;
                    case 1: o.printf("LD A, (%s%c)\n", r, pm); break;
                    default: unknown = true; break;
                    }

                    if (execute)
                    {
                        short loc = reg(r);
                        switch (q)
                        {
                        case 0: st(loc, a);  if (o != null) o.printf("    %s %s\n", sr(r), sr(Reg8.A)); break;
                        case 1: a = ld(loc); if (o != null) o.printf("    %s %s\n", sr(Reg8.A), sr(r)); notifyChanged(Reg8.A); break;
                        default: unknown = true; break;
                        }
                            
                        if (p == 2)
                            reg(Reg16.HL, (short) (loc + 1));
                        else if (p == 3)
                            reg(Reg16.HL, (short) (loc - 1));
                        
                        assert(cycle - precycle == 8);
                    }
                }
                break;
            case 3: // x=0, z
                {
                    Reg16 r = GetReg16(p);
                    String op[] = { "INC %s\n", "DEC %s\n"};
                    if (o != null) o.printf(op[q], r);

                    if (execute)
                    {
                        short v = reg(r);
                        switch (q)
                        {
                        case 0: ++v; break;
                        case 1: --v; break;
                        default: unknown = true; break;
                        }
                        cycle += 4;
                        reg(r, v);
                        
                        if (o != null) o.printf("    %s F=%s\n", sr(r), flags());
                        assert(cycle - precycle == 8);
                    }
                }
                break;
            case 4: // x=0, z
                {
                    Reg8 r = GetReg8(y);
                    if (o != null) o.printf("INC %s\n", r);

                    if (execute)
                    {
                        byte v = reg(r);
                        boolean half = ((v & 0xF) + 1) > 0xF;
                        ++v;
                        reg(r, v);

                        flag(Flags.Z, v == 0);
                        flag(Flags.N, false);
                        flag(Flags.H, half);

                        if (o != null) o.printf("    %s F=%s\n", sr(r), flags());
                        assert(cycle - precycle == (r == Reg8.HL ? 12 : 4));
                    }
                }
                break;
            case 5: // x=0, z
                {
                    Reg8 r = GetReg8(y);
                    if (o != null) o.printf("DEC %s\n", r);

                    if (execute)
                    {
                        byte v = reg(r);
                        boolean half = ((v & 0xF) - 1) < 0x0;
                        --v;
                        reg(r, v);

                        flag(Flags.Z, v == 0);
                        flag(Flags.N, true);
                        flag(Flags.H, half);

                        if (o != null) o.printf("    %s F=%s\n", sr(r), flags());
                        assert(cycle - precycle == (r == Reg8.HL ? 12 : 4));
                    }
                }
                break;
            case 6: // x=0, z
                {
                    Reg8 r = GetReg8(y);
                    byte v = d8();
                    if (o != null) o.printf("LD %s, 0x%02X\n", r, v);

                    if (execute)
                    {
                        reg(r, v);
                        if (o != null) o.printf("    %s\n", sr(r));
                        assert(cycle - precycle == ((r == Reg8.HL) ? 12 : 8));
                    }
                }
                break;
            case 7: // x=0, z
                switch (y)
                {
                case 0: case 1: case 2: case 3:
                    {
                        Reg8 r = Reg8.A;
                        rot(o, y, r, false, execute);
                        assert(cycle - precycle == 4);
                    }
                    break;
                case 4:
                    {
                        if (o != null) o.printf("DAA\n");
                        
                        if (execute)
                        {
                            if (false)
                            {
                                byte correction = 0;
                                
                                final boolean flagN = flag(Flags.N);
                                boolean setFlagC = false;
                                if (flag(Flags.H) || (!flagN && (a & 0x0F) > 9))
                                {
                                    correction |= 0x06;
                                }

                                if (flag(Flags.C) || (!flagN && ub(a) > 0x99))
                                {
                                    correction |= 0x60;
                                    setFlagC = true;
                                }

                                a += flagN ? -correction : correction;
                                flag(Flags.C, setFlagC);
                            }
                            else
                            {
                                if (!flag(Flags.N))
                                {  // after an addition, adjust if (half-)carry occurred or if result is out of bounds
                                    if (flag(Flags.C) || ub(a) > 0x99)       { a += 0x60; flag(Flags.C, true); }
                                    if (flag(Flags.H) || (a & 0x0f) > 0x09) { a += 0x06; }
                                }
                                else
                                {  // after a subtraction, only adjust if (half-)carry occurred
                                    if (flag(Flags.C)) { a -= 0x60; }
                                    if (flag(Flags.H)) { a -= 0x06; }
                                }
                            }

                            flag(Flags.H, false);
                            flag(Flags.Z, a == 0);

                            if (o != null) o.printf("    %s F=%s\n", sr(Reg8.A), flags());
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                case 5:
                    {
                        if (o != null) o.printf("CPL\n");
                        
                        if (execute)
                        {
                            a = (byte) (~a);
                            
                            flag(Flags.N, true);
                            flag(Flags.H, true);
                            
                            notifyChanged(Reg8.A);
                            if (o != null) o.printf("    %s\n", sr(Reg8.A));
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                case 6:
                    {
                        if (o != null) o.printf("SCF\n");
                        
                        if (execute)
                        {
                            flag(Flags.N, false);
                            flag(Flags.H, false);
                            flag(Flags.C, true);
                            
                            if (o != null) o.printf("    F=%s\n", flags());
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                case 7:
                    {
                        if (o != null) o.printf("CCF\n");
                        
                        if (execute)
                        {
                            flag(Flags.N, false);
                            flag(Flags.H, false);
                            flag(Flags.C, !flag(Flags.C));
                            
                            if (o != null) o.printf("    F=%s\n", flags());
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            default: unknown = true; break;
            }
            break;
        case 1: // x
            if (y == 6 && z == 6)
            {
                if (o != null) o.printf("HALT\n");
                
                if (execute)
                {
                    state(State.Halt);
                    assert(cycle - precycle == 4);
                }
            }
            else
            {
                Reg8 rs = GetReg8(z);
                Reg8 rd = GetReg8(y);
                if (o != null) o.printf("LD %s, %s\n", rd, rs);

                if (execute)
                {
                    reg(rd, reg(rs));
                    if (o != null) o.printf("    %s\n", sr(rd));
                    assert(cycle - precycle == ((rs == Reg8.HL || rd == Reg8.HL) ? 8 : 4));
                }
            }
            break;
        case 2: // x
            {
                Reg8 r = GetReg8(z);
                alu(o, y, reg(r), r.toString(), execute);
                assert(cycle - precycle == (r == Reg8.HL ? 8 : 4));
            }
            break;
        case 3: // x
            switch (z)
            {
            case 0: // x=3, z
                switch (y)
                {
                case 0: case 1: case 2: case 3:
                    {
                        Test t = GetTest(y);
                        if (o != null) o.printf("RET %s\n", t);

                        if (execute)
                        {
                            boolean tr = cc(t);
                            cycle += 4;
                            if (tr)
                            {
                                cycle += 4;
                                pc = pop16();
                            }
                            
                            if (o != null) o.printf("    %b F=%s %s\n", tr, flags(), spc());
                            assert(cycle - precycle == (tr ? 20 : 8));
                        }
                    }
                    break;
                case 4:
                    {
                        byte off = d8();    // unsigned
                        if (o != null) o.printf("LD ($FF00+%02X), A\n", off);

                        if (execute)
                        {
                            short loc = CX;
                            loc += ub(off);
                            st(loc, a);
                            
                            if (o != null) o.printf("    %s, %s\n", sr(Reg8.A), smem(loc));
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                case 5:
                    {
                        byte off = d8();    // signed
                        if (o != null) o.printf("ADD SP, %02X\n", off);

                        if (execute)
                        {
                            cycle += 4;
                            sp = AddSP(off);
                            
                            if (o != null) o.printf("    %s\n", sr(Reg16.SP));
                            assert(cycle - precycle == 16);
                        }
                    }
                    break;
                case 6:
                    {
                        byte off = d8();    // unsigned
                        if (o != null) o.printf("LD A, ($FF00+%02X)\n", off);

                        if (execute)
                        {
                            short loc = CX;
                            loc += ub(off);
                            a = ld(loc);
                            
                            notifyChanged(Reg8.A);
                            if (o != null) o.printf("    %s, %s\n", sr(Reg8.A), smem(loc));
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                case 7:
                    {
                        byte off = d8();    // signed
                        if (o != null) o.printf("LD HL, SP+%02X\n", off);

                        if (execute)
                        {
                            // TODO This is very similar to "ADD SP"
                            reg(Reg16.HL, AddSP(off));
                            
                            if (o != null) o.printf("    %s, %s\n", sr(Reg16.HL), sr(Reg16.SP));
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 1: // x=3, z
                switch (q)
                {
                case 0:
                    {
                        Reg16 r = GetReg162(p);
                        if (o != null) o.printf("POP %s\n", r);

                        if (execute)
                        {
                            short v = pop16();
                            reg(r, v);
                            if (o != null) o.printf("    %s %s\n", sr(r), sr(Reg16.SP));
                            assert(cycle - precycle == 12);
                        }
                    }
                    break;
                case 1:
                    switch (p)
                    {
                    case 0:
                        {
                            if (o != null) o.printf("RET\n");

                            if (execute)
                            {
                                cycle += 4;
                                pc = pop16();
                                if (o != null) o.printf("    %s\n", spc());
                                assert(cycle - precycle == 16);
                            }
                        }
                        break;
                    case 1:
                        {
                            if (o != null) o.printf("RETI\n");
                            
                            if (execute)
                            {
                                cycle += 4;
                                pc = pop16();
                                interrupts = true;
                                if (o != null) o.printf("    %s\n", spc());
                                assert(cycle - precycle == 16);
                            }
                        }
                        break;
                    case 2:
                        {
                            if (o != null) o.printf("JP HL\n");

                            if (execute)
                            {
                                pc = reg(Reg16.HL);
                                if (o != null) o.printf("    %s\n", spc());
                                assert(cycle - precycle == 4);
                            }
                        }
                        break;
                    case 3:
                        {
                            if (o != null) o.printf("LD SP, HL\n");

                            if (execute)
                            {
                                cycle += 4;
                                sp = reg(Reg16.HL);
                                if (o != null) o.printf("    %s\n", sr(Reg16.SP));
                                assert(cycle - precycle == 8);
                            }
                        }
                        break;
                    default: unknown = true; break;
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 2: // x=3, z
                switch (y)
                {
                case 0: case 1: case 2: case 3:
                    {
                        Test t = GetTest(y);
                        short loc = d16();
                        if (o != null) o.printf("JP %s $%04X\n", t, loc);

                        if (execute)
                        {
                            boolean tr = cc(t);
                            if (tr)
                            {
                                cycle += 4;
                                pc = loc;
                            }
                            
                            
                            if (o != null) o.printf("    %b F=%s %s\n", tr, flags(), spc());
                            assert(cycle - precycle == (tr ? 16 : 12));
                        }
                    }
                    break;
                case 4:
                    {
                        if (o != null) o.printf("LD ($FF00+C), A\n");

                        if (execute)
                        {
                            short loc = CX;
                            loc += ub(c);
                            st(loc, a);
                            if (o != null) o.printf("    %s, %s\n", sr(Reg8.C), smem(loc));
                            assert(cycle - precycle == 8);
                        }
                    }
                    break;
                case 5:
                    {
                        short loc = d16();
                        if (o != null) o.printf("LD ($%04X), A\n", loc);

                        if (execute)
                        {
                            st(loc, a);
                            if (o != null) o.printf("    %s\n", smem(loc));
                            assert(cycle - precycle == 16);
                        }
                    }
                    break;
                case 6:
                    {
                        if (o != null) o.printf("LD A, ($FF00+C)\n");

                        if (execute)
                        {
                            short loc = CX;
                            loc += ub(c);
                            a = ld(loc);
                            
                            notifyChanged(Reg8.A);
                            if (o != null) o.printf("    %s, %s\n", sr(Reg8.C), smem(loc));
                            assert(cycle - precycle == 8);
                        }
                    }
                    break;
                case 7:
                    {
                        short loc = d16();
                        if (o != null) o.printf("LD A, ($%04X)\n", loc);

                        if (execute)
                        {
                            a = ld(loc);
                            
                            notifyChanged(Reg8.A);
                            if (o != null) o.printf("    %s\n", smem(loc));
                            assert(cycle - precycle == 16);
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 3: // x=3, z
                switch (y)
                {
                case 0:
                    {
                        short loc = d16();
                        if (o != null) o.printf("JP $%04X\n", loc);

                        if (execute)
                        {
                            if (loc == orig_pc)
                                mon.onInfiniteLoop();
                            
                            cycle += 4;
                            pc = loc;
                            if (o != null) o.printf("    %s\n", spc());
                            assert(cycle - precycle == 16);
                        }
                    }
                    break;
                case 1:
                    opcodeCB(precycle, o, execute);
                    break;
                case 6:
                    {
                        if (o != null) o.printf("DI\n");
                        
                        if (execute)
                        {
                            interrupts = false;
                            if (o != null) o.printf("    interrupts=%b\n", interrupts);
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                case 7:
                    {
                        if (o != null) o.printf("EI\n");
                        
                        if (execute)
                        {
                            interrupts = true;
                            if (o != null) o.printf("    interrupts=%b\n", interrupts);
                            assert(cycle - precycle == 4);
                        }
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 4: // x=4, z
                switch (y)
                {
                case 0: case 1: case 2: case 3:
                    {
                        Test t = GetTest(y);
                        short loc = d16();
                        if (o != null) o.printf("CALL %s $%04X\n", t, loc);

                        if (execute)
                        {
                            boolean tr = cc(t);
                            if (tr)
                            {
                                push(pc);
                                cycle += 4;
                                pc = loc;
                            }
                            if (o != null) o.printf("    %b F=%s %s\n", tr, flags(), spc());
                            assert(cycle - precycle == (tr ? 24 : 12));
                        }
                    }
                    break;
                }
                break;
            case 5: // x=3, z
                switch (q)
                {
                case 0:
                    {
                        Reg16 r = GetReg162(p);
                        if (o != null) o.printf("PUSH %s\n", r);

                        if (execute)
                        {
                            short v = reg(r);
                            push(v);
                            cycle += 4;

                            if (o != null) o.printf("    %s %s\n", sr(r), sr(Reg16.SP));
                            assert(cycle - precycle == 16);
                        }
                    }
                    break;
                case 1:
                    switch (p)
                    {
                    case 0:
                        {
                            short loc = d16();
                            if (o != null) o.printf("CALL $%04X\n", loc);

                            if (execute)
                            {
                                push(pc);
                                cycle += 4;
                                pc = loc;
                                if (o != null) o.printf("    %s %s\n", spc(), sr(Reg16.SP));
                                assert(cycle - precycle == 24);
                            }
                        }
                        break;
                    default: unknown = true; break;
                    }
                    break;
                default: unknown = true; break;
                }
                break;
            case 6: // x=3, z
                {
                    byte b = d8();
                    alu(o, y, b, String.format("0x%02X", b), execute);
                    assert(cycle - precycle == 8);
                }
                break;
            case 7: // x=3, z
                {
                    byte v = (byte) (y * 8);
                    if (o != null) o.printf("RST %02X\n", ub(v));
                    
                    if (execute)
                    {
                        push(pc);
                        pc = (short) ub(v);
                        
                        assert(cycle - precycle == 16);
                    }
                }
                break;
            default: unknown = true; break;
            }
            break;
        default: unknown = true; break;
        }
        
        if (unknown)
        {
            if (o != null) o.printf("??\n");
            if (execute) error("Unknown opcode: 0x%02X x: %d y: %d z: %d p: %d q: %d", opcode, x, y, z, p, q);
        }
    }

    private byte fetch()
    {
        return ld(pc++);
    }
    
    private boolean checkinterrupt(java.io.PrintStream o)
    {
        final byte ii = (byte) Integer.lowestOneBit(mem.read(IE) & rif);
        if (ii != 0)
        {
            //debug("check interrupt %d\n", ii);
            rif = bitClear(ii, rif);
            // callinterrupt(o, 0x0040 + log2(ii) * 0x08);
            switch (ii)
            {
            case Bit0: callinterrupt(o, (short) 0x0040); break;
            case Bit1: callinterrupt(o, (short) 0x0048); break;
            case Bit2: callinterrupt(o, (short) 0x0050); break;
            case Bit3: callinterrupt(o, (short) 0x0058); break;
            case Bit4: callinterrupt(o, (short) 0x0060); break;
            case Bit5: callinterrupt(o, (short) 0x0068); break;
            default: throw new IllegalStateException();
            }
            return true;
        }
        else
            return false;
    }
    
    private void callinterrupt(java.io.PrintStream o, short loc)
    {
        if (o != null) o.printf("    %s %s\n", spc(), sr(Reg16.SP));
        interrupts = false;
        push(pc);
        pc = loc;
        if (o != null) o.printf("    interrupt %02d\n", loc);
        // todo what is the cycle count
    }
    
    private void opcodeCB(final int precycle, final java.io.PrintStream o, final boolean execute)
    {
        final byte opcode = fetch();

        final int x = extractBits(opcode, 2, 6);
        final int y = extractBits(opcode, 3, 3);
        final int z = extractBits(opcode, 3, 0);

        //if (o != null) o.printf("--- 0xCB 0x%02X x=%d y=%d z=%d ", opcode, x, y, z);
        switch (x)
        {
        case 0:
            {
                Reg8 r = GetReg8(z);
                rot(o, y, r, true, execute);
                assert(cycle - precycle == (r == Reg8.HL ? 16 : 8));
            }
            break;
        case 1:
            {
                Reg8 r = GetReg8(z);
                if (o != null) o.printf("BIT %d, %s\n", y, r);

                if (execute)
                {
                    byte v = reg(r);
                    
                    flag(Flags.Z, !testBit((byte) (1 << y), v));
                    flag(Flags.N, false);
                    flag(Flags.H, true);
                    
                    if (o != null) o.printf("    %s F=%s %d\n", sr(r), flags(), cycle - precycle);
                    assert(cycle - precycle == (r == Reg8.HL ? 12 : 8));
                }
            }
            break;
        case 2:
            {
                Reg8 r = GetReg8(z);
                if (o != null) o.printf("RES %d, %s\n", y, r);
                
                if (execute)
                {
                    byte v = reg(r);
                    v = bitClear((byte) (1 << y), v);
                    reg(r, v);
                    
                    assert(cycle - precycle == (r == Reg8.HL ? 16 : 8));
                }
            }
            break;
        case 3:
            {
                Reg8 r = GetReg8(z);
                if (o != null) o.printf("SET %d, %s\n", y, r);
                
                if (execute)
                {
                    byte v = reg(r);
                    v = bitSet((byte) (1 << y), v);
                    reg(r, v);
                    
                    assert(cycle - precycle == (r == Reg8.HL ? 16 : 8));
                }
            }
            break;
        default:
            if (o != null) o.printf("??\n");
            if (execute) error("Unknown opcode: 0xCB 0x%02X x: %d y: %d z: %d", opcode, x, y, z);
            break;
        }
    }

    private void push8(byte v)
    {
        st(--sp, v);
    }

    private void push(short v)
    {
        push8(highByte(v));    // TODO Not sure of order
        push8(lowbyte(v));
    }

    private byte pop8()
    {
        return ld(sp++);
    }

    private short pop16()
    {
        byte l = pop8();
        byte h = pop8();
        return makeShort(h, l);
    }

    private void state(State s)
    {
        //debug("state -> %s\n", s);
        state = s;
    }
    
    private short AddSP(byte off)
    {
        cycle += 4;
        short loc = sp;
        loc += off;
        
        flag(Flags.Z, false);
        flag(Flags.N, false);
        flag(Flags.H, (((sp & 0xF) + (off & 0xF)) > 0xF));
        flag(Flags.C, (((sp & 0xFF) + ub(off)) > 0xFF));
        
        return loc;
    }
    
    private enum AluOp { ADD(false), ADC(false, true), SUB(true), SBC(true, true), AND(false), XOR(false), OR(false), CP(true);
        AluOp(boolean neg)
        {
            this.neg = neg;
            this.carry = false;
        }
        
        AluOp(boolean neg, boolean carry)
        {
            this.neg = neg;
            this.carry = carry;
        }
        
        final boolean neg;
        final boolean carry;
    }
    
    private void alu(java.io.PrintStream o, int op, byte v, String n, boolean execute)
    {
        final AluOp ope = AluOp.values()[op];
        if (o != null) o.printf("%s %s\n", ope, n);
        
        if (execute)
        {
            int r = ub(a);
            boolean half = false;
            boolean carry = false;
            final int c = ope.carry && flag(Flags.C) ? 1 : 0;
            
            switch (ope)
            {
            case ADD: case ADC:
                      r += ub(v) + c;               half = (((a & 0xF) + (v & 0xF) + c) > 0xF); carry = (r > 0xFF); break;
            case SUB: case SBC: case CP:
                      r -= ub(v) + c;               half = (((a & 0xF) - (v & 0xF) - c) < 0x0); carry = (r < 0x00); break;
            case AND: r &= ub(v);                   half = true; break;
            case XOR: r ^= ub(v);                   break;
            case OR:  r |= ub(v);                   break;
            default: throw new IllegalArgumentException();
            }
            
            flag(Flags.Z, (byte) r == 0);
            flag(Flags.N, ope.neg);
            flag(Flags.H, half);
            flag(Flags.C, carry);
            //flag(Flags.C, testBit(Bit7, r) && !testBit(Bit7, a));
            if (ope != AluOp.CP)
            {
                a = (byte) r;
                notifyChanged(Reg8.A);
            }
                
            if (o != null) o.printf("    %s F=%s\n", sr(Reg8.A), flags());
        }
    }

    private enum RotOp { RLC(Bit7), RRC(Bit0), RL(Bit7), RR(Bit0), SLA(Bit7), SRA(Bit0), SWAP(ByteEx.ZERO), SRL(Bit0);
        RotOp(byte carry)
        {
            this.carry = carry;
        }
        
        final byte carry;
    }
    
    private void rot(java.io.PrintStream o, int op, Reg8 r, boolean zc, boolean execute)
    {
        final RotOp ope = RotOp.values()[op];
        if (o != null) o.printf(zc ? "%s %s\n" : "%s%s\n", ope, r);
        
        if (execute)
        {
            byte b = reg(r);
            final boolean carry = (b & ope.carry) != 0;
            
            switch (ope)
            {
            case RLC:   b = rotateLeft(b, carry); break;
            case RRC:   b = rotateRight(b, carry); break;
            case RL:    b = rotateLeft(b, flag(Flags.C)); break;
            case RR:    b = rotateRight(b, flag(Flags.C)); break;
            case SLA:   b = rotateLeft(b, false); break;
            case SRA:   b = rotateRight(b, (b & Bit7) != 0); break;
            case SWAP:  b = swap(b); break;
            case SRL:   b = rotateRight(b, false); break;
            default: throw new IllegalArgumentException();
            }
            
            reg(r, b);
            
            flag(Flags.Z, zc ? b == 0 : false);
            flag(Flags.N, false);
            flag(Flags.H, false);
            flag(Flags.C, carry);
            
            if (o != null) o.printf("    %s F=%s\n", sr(r), flags());
        }
    }

    public boolean flag(Flags fl)
    {
        return (f & fl.mask) != 0;
    }

    private void flag(Flags fl, boolean v)
    {
        if (v)
            f |= fl.mask;
        else
            f &= ~fl.mask;
        // TODO May want to streamline this - its called many times per cycle
        notifyChanged(Reg8.F);
    }

    public String flags()
    {
        String s = "";
        for (Flags f : Flags.values())
        {
            s += flag(f) ? f : '-';
        }
        return s;
    }

    private String sr(Reg8 r)
    {
        final int precycle = cycle;
        String ret = r == Reg8.HL
            ? String.format("(HL $%04X)=0x%02X", reg(Reg16.HL), reg(r))
            : String.format("%s=0x%02X", r, reg(r));
        if (r == Reg8.HL) cycle -= 4;
        assert(precycle == cycle);
        return ret;
    }

    // TODO Can I make this a generic function for an enum?
    public static Reg8 FindReg8(String s)
    {
        for(Reg8 r : Reg8.values())
        {
            if (r.toString().equalsIgnoreCase(s))
                return r;
        }
        return null;
    }

    public static Reg16 FindReg16(String s)
    {
        for(Reg16 r : Reg16.values())
        {
            if (r.toString().equalsIgnoreCase(s))
                return r;
        }
        return null;
    }

    public byte reg(Reg8 r)
    {
        switch (r)
        {
        case A: return a;
        case F: return f;
        case B: return b;
        case C: return c;
        case D: return d;
        case E: return e;
        case H: return h;
        case L: return l;
        case HL: return ld(reg(Reg16.HL));
        default: throw new IllegalArgumentException();
        }
    }

    public void reg(Reg8 r, byte v)
    {
        switch (r)
        {
        case A: a = v; break;
        case F: f = v; break;
        case B: b = v; break;
        case C: c = v; break;
        case D: d = v; break;
        case E: e = v; break;
        case H: h = v; break;
        case L: l = v; break;
        case HL: st(reg(Reg16.HL), v); break;
        default: throw new IllegalArgumentException();
        }
        notifyChanged(r);
    }

    private String sr(Reg16 r)
    {
        final int precycle = cycle;
        String ret = String.format("%s=$%04X", r, reg(r));
        assert(precycle == cycle);
        return ret;
    }

    private String spc()
    {
        return String.format("PC=$%04X", pc);
    }

    private String smem(short loc)
    {
        return String.format("($%04X)=%02X", loc, mem.read(loc));
    }

    public short reg(Reg16 r)
    {
        switch (r)
        {
        case BC: return makeShort(b, c);
        case DE: return makeShort(d, e);
        case HL: return makeShort(h, l);
        case SP: return sp;
        case AF: return makeShort(a, f);
        default: throw new IllegalArgumentException();
        }
    }

    public void reg(Reg16 r, short v)
    {
        switch (r)
        {
        case BC: b = highByte(v); c = lowbyte(v); notifyChanged(Reg8.B); notifyChanged(Reg8.C); break;
        case DE: d = highByte(v); e = lowbyte(v); notifyChanged(Reg8.D); notifyChanged(Reg8.E); break;
        case HL: h = highByte(v); l = lowbyte(v); notifyChanged(Reg8.H); notifyChanged(Reg8.L); break;
        case SP: sp = v; break;
        case AF: a = highByte(v); f = (byte) (lowbyte(v) & 0xF0); notifyChanged(Reg8.A); notifyChanged(Reg8.F); break;
        default: throw new IllegalArgumentException();
        }
        
        /* TODO
        for (Listener l : ls)
        {
            l.changedRegister(r.lo);
            l.changedRegister(r.hi);
        }
        */
    }
    
    private void notifyChanged(Reg8 r)
    {
        for (Listener l : ls)
            l.changedRegister(r);
    }
    
    private static Test GetTest(int i)
    {
        return Test.values()[i];
    }

    private boolean cc(Test t)
    {
        boolean b = flag(t.flag);
        if (t.negate) b = !b;
        return b;
    }

    private static Reg8 GetReg8(int i)
    {
        return Reg8.values()[i];
    }

    private static Reg16 GetReg16(int i)
    {
        return Reg16.values()[i];
    }

    private static Reg16 GetReg162(int i)
    {
        return GetReg16(i == 3 ? 4 : i);
    }

    private static Reg16 GetReg16G(int i)
    {
        return GetReg16(i == 3 ? 2 : i);
    }

    private byte d8()
    {
        return fetch();
    }

    private short d16()
    {
        byte bl = d8();
        byte bh = d8();
        return makeShort(bh, bl);
    }

    private byte ld(short loc)
    {
        cycle += 4;
        return mem.read(loc);
    }

    private void st(short loc, byte v)
    {
        cycle += 4;
        mem.write(loc, v);
    }

    @Override
    public byte read(short loc)
    {
        switch (loc)
        {
        case IF: return rif;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void write(short loc, byte val)
    {
        //debug("Cpu write %04X %02X\n", us(loc), ub(val));
        switch (loc)
        {
        case IF: rif = val; state(State.Normal); break;
        default: throw new IllegalArgumentException();
        }
    }
}
