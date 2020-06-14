package cn.go.app;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.*;

/**
 */

@Component
@ServerEndpoint("/api/log")
public class WebSocketLogHandler {

    private PipedReader reader;
    private Writer writer;
    private PrintStream systemOut;

    /**
     * WebSocket请求开启
     */
    @OnOpen
    public void onOpen(Session session) {

        try {
            systemOut = System.out;
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream pipedOutputStream = new PipedOutputStream(in);
            PrintStream printStream = new PrintStream(pipedOutputStream);
            System.setOut(printStream);
            LogThread thread = new LogThread(systemOut, in, session);
            thread.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    /**
     * WebSocket请求关闭，关闭请求时调用此方法，关闭流
     */
    @OnClose
    public void onClose() {
        System.setOut(systemOut);
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @OnError
    public void onError(Throwable thr) {
        thr.printStackTrace();
    }
}