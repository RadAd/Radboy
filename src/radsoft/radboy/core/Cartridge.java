package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import java.util.Arrays;
import java.io.*;

// TODO
// Header Checksum
// Global Checksum

public class Cartridge implements Memory.Module
{
    final static short ROM_BEGIN = (short) 0x0000;
    final static short ROM_END = (short) 0x8000;
    final static short ROM_BANK_0_END = (short) 0x4000;
    final static short RAM_BEGIN = (short) 0xA000;
    final static short RAM_END = (short) 0xC000;
    
    final byte rom[];
    final byte ram[];
    byte rom_bank = 0x01;
    byte ram_bank = 0x00;
    byte ram_enable = 0x00;
    byte mode = 0x00;
    
    Cartridge(String romfile, Memory m) throws IOException
    {
        rom = loadbin(romfile);
        ram = new byte[getRamSize()];
        
        for (int i = us(ROM_BEGIN); i < us(ROM_END); i += 0x1000)
            m.module((short) i, this);
        for (int i = us(RAM_BEGIN); i < us(RAM_END); i += 0x1000)
            m.module((short) i, this);
    }

    @Override
    public byte read(short loc)
    {
        //debug("Catridge read %04X\n", us(loc));
        assert((loc >= us(ROM_BEGIN) && loc < us(ROM_END)) || (loc >= us(RAM_BEGIN) && loc < us(RAM_END)));
        if (us(loc) < us(ROM_BANK_0_END))
            return rom[us(loc)];
        else if (us(loc) < us(ROM_END))
        {
            //debug("Catridge read %04X %04X\n", us(loc), calcRomBankAddr());
            return rom[calcRomBankAddr() + us(loc) - us(ROM_BANK_0_END)];
        }
        else if (us(loc) >= us(RAM_BEGIN) && us(loc) < us(RAM_END))
        {
            if (ram_enable == 0x0A)
                return ram[calcRamBankAddr() + us(loc) - us(RAM_BEGIN)];
            else
                return 0x00;
        }
        else
            throw new IllegalStateException();
    }
    
    @Override
    public void write(short loc, byte val)
    {
        //debug("Catridge write %04X\n", us(loc));
        if (us(loc) >= us(ROM_BEGIN) && us(loc) < us(ROM_END))
        {
            switch ((loc & 0xF000) >> 12)
            {
            case 0: case 1: ram_enable = (byte) (val & 0x0F); break;
            case 2: case 3: rom_bank = fixRomBank(val); /* debug("ROM bank %02X\n", ub(rom_bank)); */ break;
            case 4: case 5: ram_bank = val; break;
            case 6: case 7: mode = val; break;
            }
        }
        else if (us(loc) >= us(RAM_BEGIN) && us(loc) < us(RAM_END))
        {
            if (ram_enable == 0x0A)
                ram[calcRamBankAddr() + us(loc) - us(RAM_BEGIN)] = val;
        }
        else
            throw new IllegalStateException();
    }
    
    static byte fixRomBank(byte b)
    {
        if ((b & 0x1F) == 0x00) // TODO Only for MBC1
            b |= 0x01;
        return b;
    }
    
    public String getTitle()
    {
        return getString((short) 0x0134, 11);
    }
    
    public String getManufacturer()
    {
        return getString((short) 0x013F, 4);
    }
    
    public String getLicensee()
    {
        if (rom[0x014B] == 0x33)
            return getString((short) 0x0144, 2);
        else
            return getString((short) 0x014B, 1);
    }
    
    enum MBC { NONE, MBC1 };
    
    public MBC getMBC()
    {
        switch (rom[0x0147])
        {
        case 0x00: return MBC.NONE;
        case 0x01: case 0x02: case 0x03: return MBC.MBC1;
        default: error("Unknown MBC"); return null;
        }
    }
    
    public int getRomSize()
    {
        // TODO Fix for 0x52 - 0x54
        assert(rom[0x148] >= 0x00 && rom[0x148] <= 0x08);
        return 0x8000 << rom[0x148];
    }
    
    public int getRamSize()
    {
        switch (rom[0x149])
        {
        case 0x00:  return 0x0000;  // None
        case 0x01:  return 0x0800;  // 2Kb
        case 0x02:  return 0x2000;  // 8Kb
        case 0x03:  return 0x8000;  // 32Kb
        case 0x04:  return 0x20000; // 128Kb
        case 0x05:  return 0x10000; // 64Kb
        default: throw new IllegalStateException("Invalid RAM Size");
        }
    }
    
    int calcRomBankAddr()
    {
        if (mode == 0x00)
            return (rom_bank << 14) + (ram_bank << 16);
        else
            return rom_bank << 14;
    }
    
    int calcRamBankAddr()
    {
        if (mode == 0x01)
            return RAM_BEGIN + (ram_bank << 12);
        else
            return RAM_BEGIN;
    }
    
    private String getString(short begin, int length)
    {
       return new String(rom, begin, length); 
    }
    
    private static byte[] loadbin(String filename) throws IOException
    {
        File file = new File(filename);
        byte[] fileData = new byte[(int) file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(fileData);
        dis.close();
        return fileData;
    }
}
