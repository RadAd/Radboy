package radsoft.radboy;

import radsoft.radboy.core.Gameboy;

class GameboyMonitor implements Gameboy.Monitor
{
    private boolean breakpoint = false;
    
    boolean breakOnInfiniteLoop = false;
    boolean breakOnStop = false;
    
    void setBreak(boolean b)
    {
        breakpoint = b;
    }
    
    @Override
    public boolean doBreak()
    {
        return breakpoint;
    }

    @Override
    public void onInfiniteLoop()
    {
        // in an infintie loop as in some blarg tests
        // in an infintie loop as in some mooneye tests
        
        if (breakOnInfiniteLoop) // && gb.video.ly == 0x00) // TODO
            breakpoint = true;
    }
    
    @Override
    public void onStop()
    {
        if (breakOnStop)
            breakpoint = true;
    }
};
