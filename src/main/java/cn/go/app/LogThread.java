package cn.go.app;

import org.fusesource.jansi.AnsiString;
import sun.nio.cs.StreamDecoder;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.websocket.Session;

/**
 * log process thread
 */
public class LogThread extends Thread {
    private BufferedReader reader;
    private BufferedInputStream bufferedInputStream;
    private Session session;

    private PrintStream systemOut;

    public LogThread(PipedReader pipedReader, Session session) {
        this.reader = new BufferedReader(pipedReader);
        this.session = session;
    }
    public LogThread(PrintStream systemOut, PipedInputStream pipedInputStream, Session session) {
        this.bufferedInputStream = new BufferedInputStream(pipedInputStream);
        this.session = session;
        this.systemOut = systemOut;
    }


    @Override
    public void run() {
        try {
            String line = null;
            byte[] bytes = new byte[10*1024];
            InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//            while ( bufferedInputStream.read(bytes) != -1) {
            while ( (line = bufferedReader.readLine()) != null) {
                //line =new String(bytes, StandardCharsets.UTF_8);
                this.systemOut.println(line);
                AnsiString ansiString = new AnsiString(line);
                session.getBasicRemote().sendText(ansiString.getPlain() + "\n<br>");
                java.util.Arrays.fill(bytes, 0, bytes.length ,(byte)0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}