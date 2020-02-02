package radsoft.radboy;

import radsoft.radboy.core.Gameboy;

class GameboyMonitor implements Gameboy.Monitor
{
    private final Gameboy gb;
    private boolean breakpoint = false;
    private long start;
    private int startCycle;
    java.io.PrintStream trace = null;
    
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
        throttle();
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
            start = System.currentTimeMillis();
            startCycle = gb.cpu.cycle();
            gb.run(trace, GameboyMonitor.this);
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
    
    void step()
    {
        gb.step(trace, this);
    }
    
    void throttle()
    {
        final long now = System.currentTimeMillis();
        final int m = (int) (now - start);
        final int clockspeed = 4194304; // hz

        final long c = gb.cpu.cycle() - startCycle;
        final long r = (c * 1000) / (clockspeed * 2);
        
        if ((r - m) > 100)
        {
            try
            {
                Thread.sleep(r - m);
            }
            catch (InterruptedException e)
            {
            }
        }
    }
};
