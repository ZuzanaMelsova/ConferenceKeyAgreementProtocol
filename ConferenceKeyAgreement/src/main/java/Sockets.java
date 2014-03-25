import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


public class Sockets {

    private List<Integer> ports;
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private Thread listener;
    private InputStream in;
    private OutputStream out;
    private MessageListener messageListener;


    public Sockets(final Integer myPort) {


        listener = new Thread() {
            @Override
            public void run() {

                while (true) {
                    try {
                        serverSocket = new ServerSocket(myPort);
                        clientSocket = serverSocket.accept();
                        in = clientSocket.getInputStream();
                        Message.Msg msg = Message.Msg.parseFrom(in);

                        if (msg != null)
                        {
                            getMessageListener().MessageReceived(msg);
                        }


                        clientSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            }
        };
        listener.start();
    }


    public void stopListener() {
        listener.interrupt();
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void sendMsg(Message.Msg msg) {
        for (Integer port : ports) {
            try {
                Socket peer = new Socket(InetAddress.getByName("localhost"), port);
                out = peer.getOutputStream();
                msg.writeTo(out);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public MessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }
    public Thread getListener() {
        return listener;
    }
}
