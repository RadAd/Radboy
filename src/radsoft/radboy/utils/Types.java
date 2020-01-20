package radsoft.radboy.utils;

public class Types
{
    public static String bs(byte b)
    {
        return String.format("%8s", Integer.toBinaryString(ByteEx.ub(b))).replace(' ', '0');
    }

    public static void debug(String msg, Object... args)
    {
        System.out.printf(msg, args);
    }

    public static void debugln(String msg, Object... args)
    {
        System.out.printf(msg + "\n", args);
    }
    
    public static void error(String msg, Object... args)
    {
        throw new IllegalStateException(String.format(msg, args));
    }
    
    public static void notImplemented()
    {
        error("Not Implemented");
    }
}
