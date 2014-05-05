
import org.slf4j.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zuzana Melsova
 * This class handles communication through sockets.
 */
public class Sockets {

    static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Sockets.class);

    private Map<Integer, Integer> ports; // Ports associated with participants IDs.
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private int myPort;
    private Thread listener;
    private InputStream in;
    private OutputStream out;
    private MessageListener messageListener;
    private volatile boolean running = true;

    /**
     * Constructor
     * Starts a new thread listening on given port.
     * @param myPort
     */
    public Sockets(final Integer myPort) {
        this.myPort = myPort;
        ports = new HashMap<Integer, Integer>();
        listener = new Thread() {
            @Override
            public void run() {
                LOGGER.info("Starting listener on port " + myPort);
                try {
                    serverSocket = new ServerSocket(myPort);
                    serverSocket.setSoTimeout(500);
                } catch (IOException e) {
                    LOGGER.error("Error occurred when creating socket.", e);
                    return;
                }

                while (running) {
                    try {
                        clientSocket = serverSocket.accept();
                        in = clientSocket.getInputStream();
                        Message.Msg msg = Message.Msg.parseFrom(in);
                        LOGGER.info(myPort + " received message from: " + msg.getSenderId() + " " + msg.getType());
                        if (msg != null) {
                            getMessageListener().messageReceived(msg);
                        }
                    } catch (java.net.SocketTimeoutException e) {}
                    catch (Exception e) {
                        LOGGER.error("Error occurred in the socket during listening to incoming messages.", e);
                    } finally {
                        if ((clientSocket != null) && !clientSocket.isClosed()) {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                LOGGER.error("Error occurred when closing socket.", e);
                            }
                        }
                    }
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                    if ((serverSocket != null) && (!serverSocket.isClosed())) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("Error occurred when closing socket.", e);
                }
            }
        };
        listener.start();
    }

    /**
     * Sends given message to all the other participants.
     * @param msg
     */
    public void sendMsgToEveryone(Message.Msg msg) {
        for (Integer id : ports.keySet()) {
            sendMsgTo(msg, id);
        }
    }

    /**
     * Sends the message to the participant with given id.
     * @param msg
     * @param id - receiver's id
     */
    public void sendMsgTo(Message.Msg msg, Integer id) {
        if (ports.containsKey(id)) {
            int port = ports.get(id);
            Socket socket = null;

            try {
                // Open socket
                socket = new Socket(InetAddress.getByName("localhost"), port);
                out = socket.getOutputStream();

                // Write message to socket
                msg.writeTo(out);
                out.flush();
                out.close();
                LOGGER.info("Message has been sent to : " + port + ": " + msg.getType());
            } catch (IOException e) {
                LOGGER.error("Error occurred when sending message to " + port, e);
            }
            finally
            {
                if ((socket != null) && !socket.isClosed())
                {
                    try
                    {
                        socket.close();
                    }
                    catch (Exception ex)
                    {
                        LOGGER.error("Error occurred when closing socket", ex);
                    }
                }
            }
        }
    }

    /**
     * Stops listening to incoming messages.
     */
    public void stop() {
        this.running = false;
        LOGGER.info("Listener has been stopped on port" + myPort);
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setPorts(Map<Integer, Integer> ports) {
        this.ports = ports;
    }

    public Map<Integer, Integer> getPorts() {
        return ports;
    }

    public int getMyPort() {
        return myPort;
    }
}
