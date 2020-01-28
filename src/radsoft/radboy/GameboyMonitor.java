package radsoft.radboy;

import radsoft.radboy.core.Gameboy;

class GameboyMonitor implements Gameboy.Monitor
{
    private final Gameboy gb;
    private boolean breakpoint = false;
    
    GameboyMonitor(Gameboy gb)
    {
        this.gb = gb;
    }
    
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
    
    private final Runnable r = new Runnable() {
        @Override
        public void run()
        {
            breakpoint = false;
            gb.run(null, GameboyMonitor.this);
        }
    };
    
    private Thread t = null;
    
    boolean isPaused()
    {
        return t == null;
    }
    
    void start(Thread.UncaughtExceptionHandler eh)
    {
        //System.out.println("GameboyMonitor start");
        if (t != null)
            throw new IllegalStateException();
        t = new Thread(r);
        t.setUncaughtExceptionHandler(eh);
        t.start();
    }
    
    void stop() throws InterruptedException
    {
        breakpoint = true;
        if (t != null)  // TODO AG Need to tell it to stop somehow
            t.join();
        t = null;
    }
};
