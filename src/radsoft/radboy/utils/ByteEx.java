package radsoft.radboy.utils;

public class ByteEx
{
    public final static byte ZERO = (byte) 0x00;
    public final static byte MIN = (byte) 0x00;
    public final static byte MAX = (byte) 0xFF;
    
    public final static byte Bit0 = (byte) (1 << 0);
    public final static byte Bit1 = (byte) (1 << 1);
    public final static byte Bit2 = (byte) (1 << 2);
    public final static byte Bit3 = (byte) (1 << 3);
    public final static byte Bit4 = (byte) (1 << 4);
    public final static byte Bit5 = (byte) (1 << 5);
    public final static byte Bit6 = (byte) (1 << 6);
    public final static byte Bit7 = (byte) (1 << 7);
    
    public final static int Mask = 0xFF;
    public final static int HighNibbleMask = 0xF0;
    public final static int LowNibbleMask = 0x0F;
    
    public static byte rotateLeft(byte b, boolean x)
    {
        b = (byte) (ub(b) << 1);
        if (x) b |= Bit0;
        return b;
    }

    public static byte rotateRight(byte b, boolean x)
    {
        b = (byte) (ub(b) >>> 1);
        if (x) b |= Bit7;
        return b;
    }
    
    public static byte swap(byte b)
    {
        byte h = (byte) ((b & HighNibbleMask) >>> 4);
        byte l = (byte) ((b & LowNibbleMask) << 4);
        return (byte) (h | l);
    }

    // unsigned cast to int
    public static int ub(byte b)
    {
        return b & Mask;
        //return Byte.toUnsignedInt(b);
    }

    // Function to extract k bits from p position
    // and returns the extracted value as integer
    public static int extractBits(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> p));
    }

    public static boolean testBit(byte bit, byte b)
    {
        return (bit & b) != 0;
    }

    public static byte bitSet(byte bit, byte b)
    {
        return (byte) (bit | b);
    }
    
    public static byte bitClear(byte bit, byte b)
    {
        return (byte) (~bit & b);
    }

    public static String toHexString(byte b)
    {
        return String.format("%02X", ub(b));
    }
}
