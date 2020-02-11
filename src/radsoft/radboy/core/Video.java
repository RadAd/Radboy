package radsoft.radboy.core;

import static radsoft.radboy.utils.Types.*;
import static radsoft.radboy.utils.ByteEx.*;
import static radsoft.radboy.utils.ShortEx.*;
import static java.util.Arrays.*;

// www.codeslinger.co.uk/pages/projects/gameboy/graphics.html

public class Video implements Memory.Module
{
    public interface Lcd
    {
        void updateRow(byte y, byte data[]);
    }
    
    public final static int Width = 160;
    public final static int Height = 144;
    
    final static short LCDC = (short) 0xFF40;
    final static short STAT = (short) 0xFF41;
    final static short SCY  = (short) 0xFF42;
    final static short SCX  = (short) 0xFF43;
    final static short LY   = (short) 0xFF44;
    final static short LYC  = (short) 0xFF45;
    final static short DMA  = (short) 0xFF46;
    final static short BGP  = (short) 0xFF47;
    final static short OBP0 = (short) 0xFF48;
    final static short OBP1 = (short) 0xFF49;
    final static short WY   = (short) 0xFF4A;
    final static short WX   = (short) 0xFF4B;
    
    private final Memory mem;
    
    public byte lcdc = 0;
    public byte stat = 0;
    public byte scy = 0;
    public byte scx = 0;
    public byte ly = 0;
    public byte lyc = 0;
    private byte dma = 0;
    private byte bgp = 0;
    private byte obgp0 = 0;
    private byte obgp1 = 0;
    public byte wx = 0;
    public byte wy = 0;
    
    private int lycycle = 0;
    
    public Lcd lcd = null;
    
    Video(Memory m)
    {
        mem = m;
        mem.port(LCDC, this);
        mem.port(STAT, this);
        mem.port(SCY, this);
        mem.port(SCX, this);
        mem.port(LY, this);
        mem.port(LYC, this);
        mem.port(DMA, this);
        mem.port(BGP, this);
        mem.port(OBP0, this);
        mem.port(OBP0, this);
        mem.port(WY, this);
        mem.port(WX, this);
    }

    void update(java.io.PrintStream o, int cycles)
    {
        lycycle += cycles;
        //debug("Video lycycle %d\n", lycycle);
        
        byte mode = mode();
        switch (mode)
        {
        case 0: // h-blank
            if (lycycle >= 204)
            {
                //debug("Video +mode 0 %d\n", ub(ly));
                lycycle -= 204;
                ly++;
                statly(o);
                mode(o, ub(ly) >= 144 ? (byte) 1 : (byte) 2);
            }
            break;
        case 1: // v-blank
            if (lycycle >= 456)
            {
                //debug("Video +mode 1\n");
                lycycle -= 456;
                ly++;
                if (ub(ly) > 153)
                {
                    ly = 0;
                    mode(o, (byte) 2);
                }
                statly(o);
            }
            break;
        case 2: // searching oam-ram
            if (lycycle >= 80)
            {
                //debug("Video +mode 2\n");
                lycycle -= 80;
                mode(o, (byte) 3);
            }
            break;
        case 3: // transferring to lcd
            if (lycycle >= 172)
            {
                //debug("Video +mode 3\n");
                lycycle -= 172;
                if (lcd != null)
                    lcd();
                mode(o, (byte) 0);
            }
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private void mode(java.io.PrintStream o, byte mode)
    {
        stat &= ~0x03;
        stat |= (mode & 0x03);
        
        //debug("Video mode %d\n", mode);
        switch (mode)
        {
        case 0: // h-blank
            if (testBit(Bit3, stat))
                mem.interrupt(o, Bit1);
            break;
        case 1: // v-blank
            if (testBit(Bit4, stat))
                mem.interrupt(o, Bit1);
            mem.interrupt(o, Bit0);
            break;
        case 2: // searching oam-ram
            // TODO lock out OAM memory (FE00h-FE9Fh)
            if (testBit(Bit5, stat))
                mem.interrupt(o, Bit1);
            break;
        case 3: // transferring to lcd
            // TODO lock out OAM memory (FE00h-FE9Fh)
            // TODO lock out VRAM memory (8000h-97FFh)
            break;
        }
    }

    private byte mode()
    {
        return (byte) (stat & 0x03);
    }
    
    private void statly(java.io.PrintStream o)
    {
        if (ly == lyc)
        {
            stat = bitSet(Bit2, stat);
            if (testBit(Bit6, stat))
                mem.interrupt(o, Bit1);
        }
        else
            stat = bitClear(Bit2, stat);
    }
    
    private void lcd()
    {
        if (testBit(Bit7, lcdc))
        {
            if (ub(ly) >= 0 && ub(ly) < Height)
            {
                final byte bgdata[] = new byte[Width];
                
                // TODO White bg??
                if (testBit(Bit0, lcdc))
                    fill(bgdata, (byte) 0);    // TODO What color and does it go through the BGP ??? 0 for White
                    
                getPixelLine(ly, bgdata);
                
                lcd.updateRow(ly, bgdata);
            }
        }
    }
    
    public void updateLcd()
    {
        final byte bgdata[] = new byte[Width];
        for (int line = 0; line < Height; ++line)
        {
            getPixelLine((byte) line, bgdata);
            lcd.updateRow((byte) line, bgdata);
        }
    }
    
    public void getTileLine(byte tileRow, byte data[])
    {
        assert(tileRow >= 0 && tileRow < 32);
        assert(data.length == 32);
        
        /*final*/ short bgLoc = bgLoc(false);
        bgLoc += tileRow * 32;
        for (int x = 0; x < data.length; ++x)
        {
            final byte tileCol = (byte) (x % 32);
            data[x] = mem.read((short) (bgLoc + tileCol));
        }
    }

    private short bgLoc(final boolean usingWindow)
    {
        return testBit(usingWindow ? Bit6 : Bit3, lcdc) ? (short) 0x9C00 : (short) 0x9800;
    }
    
    public static byte unpackTilePixel(byte data1, byte data2, int x)
    {
        assert(x >= 0 && x < 8);
        final int bit = 8 - x - 1;
        assert(bit >= 0 && bit < 8);
        final int c1 = testBit((byte) (1 << bit), data1) ? 1 : 0;
        final int c2 = testBit((byte) (1 << bit), data2) ? 2 : 0;
        final int px = c1 | c2;
        return (byte) px;
    }
    
    public void getPixelLine(final byte line, final byte bgdata[])
    {
        final boolean usingWindow = testBit(Bit5, lcdc) && (ub(wy) <= ub(line));

        final boolean unsig = testBit(Bit4, lcdc);
        
        final short bgLoc = bgLoc(usingWindow);

        final byte yPos = usingWindow ? (byte) (ub(line) - ub(wy)) : (byte) (ub(scy) + ub(line));
        final byte windowX = (byte) (ub(wx) - 7);
        
        for (int pixel = 0; pixel < bgdata.length; pixel++)
        {
            final byte xPos = usingWindow && (pixel >= windowX) ? (byte) (pixel - ub(windowX)) : (byte) (pixel + ub(scx));

            byte col = getTilePixel(bgLoc, yPos, xPos, unsig);
            bgdata[pixel] = col;
        }
        
        final boolean usingSprites = testBit(Bit1, lcdc);
        
        if (usingSprites)
        {
            final boolean large = testBit(Bit2, lcdc);
            final byte size = (byte) (large ? 16 : 8);
            final byte y = (byte) (line + 16);
        
            final short[] sprites = new short[10];
            final int count = findSpritesForLine(line, sprites);
            
            for (int i = 0; i < count; ++i)
            {
                final short sprite = sprites[i];
                final byte sy = mem.read((short) (sprite + 0));
                final byte sx = mem.read((short) (sprite + 1));
                final byte n = mem.read((short) (sprite + 2));
                final int tileNum = ub(n); // TODO if large ??? are we in the upper or lower tile
                final short tileData = (short) 0x8000;
                final byte tileY = (byte) (y - sy); // TODO if large divide by 2?
                
                for (byte tileX = 0; tileX < 8; ++tileX)
                {
                    int ox = ub(sx) + tileX - 8;
                    if (ox >= 0)    // TODO and less than screen width bgdata length
                        bgdata[ox] = getTilePixel(tileData, tileNum, tileX, tileY);
                }
            }
        }
    }
    
    public void getTileLine(final byte n, final byte tileY, final byte data[])
    {
        assert(data.length == 8);
        final boolean unsig = testBit(Bit4, lcdc);
        final short tileData = unsig ? (short) 0x8000 : (short) 0x8800;
        final int tileNum = unsig ? ub(n) : n + 128;
        for (byte tileX = 0; tileX < 8; ++tileX)
        {
            data[tileX] = getTilePixel(tileData, tileNum, tileX, tileY);
        }
    }
    
    private int getTileNum(final short bgLoc, final boolean unsig, final byte tileRow, final byte tileCol)
    {
        assert(tileRow >= 0 && tileRow < 32);
        assert(tileCol >= 0 && tileCol < 32);
        final short tileAddress = (short) (bgLoc + ub(tileRow) * 32 + ub(tileCol));
        return unsig ? ub(mem.read(tileAddress)) : mem.read(tileAddress) + 128;
    }
    
    private byte getTilePixel(final short bgLoc, final byte yPos, final byte xPos, final boolean unsig)
    {
        final byte tileCol = (byte) (ub(xPos) / 8);
        final byte tileRow = (byte) (ub(yPos) / 8);

        final byte tileX = (byte) (ub(xPos) % 8);
        final byte tileY = (byte) (ub(yPos) % 8);
        
        final int tileNum = getTileNum(bgLoc, unsig, tileRow, tileCol);
        final short tileData = unsig ? (short) 0x8000 : (short) 0x8800;
        return getTilePixel(tileData, tileNum, tileX, tileY);
    }
    
    private byte getTilePixel(final short tileData, final int tileNum, final byte tileX, final byte tileY)
    {
        assert(tileX >= 0 && tileX < 8);
        assert(tileY >= 0 && tileY < 8);
        final short tileLocation = (short) (tileData + (tileNum * 16) + (ub(tileY) * 2));
        final byte data1 = mem.read((short) (tileLocation + 0));
        final byte data2 = mem.read((short) (tileLocation + 1));
        final byte colourNum = unpackTilePixel(data1, data2, tileX);
        
        return getColour(colourNum);
    }
    
    private int findSpritesForLine(final byte line, final short[] sprites)
    {
        // TODO Only use 10 sprites max
        // TODO Return in priority order
        final boolean large = testBit(Bit2, lcdc);
        final byte size = (byte) (large ? 16 : 8);
        final byte y = (byte) (ub(line) + 16);
        int count = 0;
        for (short sprite = (short) 0xFE00; us(sprite) < 0xFE9F; sprite += 4)
        {
            //debug("findSpritesForLine %04X\n", us(sprite));
            final byte sy = mem.read((short) (us(sprite) + 0));
            //if (ub(sy) >= ub(y) && (ub(sy) + size) < ub(y))
            if (ub(y) >= ub(sy) && ub(y) < (ub(sy) + size))
            {
                //debug("findSpritesForLine %d %d\n", ub(y), ub(sy));
                sprites[count++] = sprite;
            }
        }
        return count;
    }
    
    private byte getColour(final byte colourNum)
    {
        assert(colourNum >= 0 && colourNum < 4);
        //return (byte) bit(bgp, 2, colourNum * 2);
        return (byte) ((bgp >> (colourNum * 2)) & 0x3);
    }
    
    private void dma(byte b)
    {
        // TODO This is supposed to occur over 160 microsecs
        // and lock out the CPU except for HRAM
        short loc = (short) (b << 8);
        byte[] vals = new byte[0xA0];
        mem.read(us(loc), vals);
        mem.write(0xFE00, vals);
        //bp = true;
    }
    
    @Override
    public byte read(short loc)
    {
        switch (loc)
        {
        case LCDC: return lcdc;
        case STAT: return stat;
        case SCY:  return scy;
        case SCX:  return scx;
        case LY:   return ly;
        case LYC:  return lyc;
        case DMA:  return dma;
        case BGP:  return bgp;
        case OBP0: return obgp0;
        case OBP1: return obgp1;
        case WY:   return wy;
        case WX:   return wx;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void write(short loc, byte val)
    {
        //debug("Video write %04X %02X\n", us(loc), ub(val));
        switch (loc)
        {
        case LCDC: lcdc  = val; break;   // TODO should only be allowed during v-blank
        case STAT: stat  = bitClear((byte) 0x78, stat); stat = bitSet((byte) (val & 0x78), stat); break;
        case SCY:  scy   = val; break;
        case SCX:  scx   = val; break;
        case LY:   ly    = 0; statly(null); break;  // TODO statly should raise the interrupt in update
        case LYC:  lyc   = val; statly(null); break;  // TODO statly should raise the interrupt in update
        case DMA:  dma   = val; dma(dma); break;
        case BGP:  bgp   = val; break;
        case OBP0: obgp0 = val; break;
        case OBP1: obgp1 = val; break;
        case WY:   wy    = val; break;
        case WX:   wx    = val; break;
        default: throw new IllegalArgumentException();
        }
    }
}
