package radsoft.radboy;

import java.awt.*;
import java.io.*;
import javax.swing.*;

class TraceView
{
    final JDialog dlg;
    final GameboyMonitor mon;
    final JTextArea text = new JTextArea();
    
    class StreamCapturer extends OutputStream
    {
        private StringBuilder buffer = new StringBuilder(128);

        public StreamCapturer()
        {
        }

        @Override
        public void write(int b) throws IOException
        {
            char c = (char) b;
            buffer.append(c);
            if (c == '\n')
            {
                appendText(buffer.toString());
                buffer.delete(0, buffer.length());
            }
        }
        
        public void appendText(final String s)
        {
            if (EventQueue.isDispatchThread())
            {
                text.append(s);
                text.setCaretPosition(text.getText().length());
                //System.out.println(s);
            }
            else
            {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        appendText(s);
                    }
                });
            }
        }
    }
    
    TraceView(JFrame f, GameboyMonitor mon)
    {
        this.dlg = new JDialog(f, "Trace");
        this.mon = mon;
        mon.trace = new PrintStream(new StreamCapturer());
        
        dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent)
            {
                //if ()
                mon.trace = null;
            }
        });
        
        //text.setMinimumSize(new Dimension(80, 33));
        //text.setPreferredSize(new Dimension(80, 33));
        text.setRows(25);
        text.setColumns(25);
        text.setWrapStyleWord(true);
    }
    
    void open()
    {
        text.doLayout();
        JScrollPane sp = new JScrollPane(text);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        dlg.getContentPane().add(sp);
        dlg.pack();
        dlg.setVisible(true);
    }
}
