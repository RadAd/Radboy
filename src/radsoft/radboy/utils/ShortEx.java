package radsoft.radboy.utils;

public class ShortEx
{
    public final static byte ZERO = (byte) 0x0000;
    public final static byte MIN = (byte) 0x0000;
    public final static byte MAX = (byte) 0xFFFF;
    
    public final static int Mask = 0xFFFF;
    
    public static byte highByte(short s)
    {
        return (byte) ((s >> 8) & ByteEx.Mask);
    }

    public static byte lowbyte(short s)
    {
        return (byte) (s & ByteEx.Mask);
    }

    public static short makeShort(byte bh, byte bl)
    {
        return (short) ((ByteEx.ub(bh) << 8) | ByteEx.ub(bl));
    }
    
    // unsigned cast to int
    public static int us(short s)
    {
        return (((int) s) & Mask);
        //return Short.toUnsignedInt(s);
    }
    
    public static short parseShort(String s, int radix)
    {
        return (short) Integer.parseInt(s, radix);
    }
    
    public static String toHexString(short s)
    {
        return String.format("%04X", us(s));
    }
}
