package cn.go.app;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.PipedWriter;

public class WebAppender extends AppenderSkeleton {

    private PipedWriter out = new PipedWriter();
    protected String target;




    public PipedWriter getOut() {
        return out;
    }

    public void setOut(PipedWriter out) {
        this.out = out;
    }

    @Override
    protected void append(LoggingEvent event) {

        System.out.println("[web]" + this.layout.format(event));
        try {
            out.write(this.layout.format(event));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public synchronized void close() {
        if (this.closed)
            return;
        this.closed = true;
    }

    @Override
    public boolean requiresLayout() {
        // TODO Auto-generated method stub  
        return false;
    }

}



