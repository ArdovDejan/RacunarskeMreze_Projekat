import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message loginMsg = (Message) in.readObject();
            username = (String) loginMsg.getPayload();
            Server.clients.put(username, this);
            System.out.println(username + " se prijavio.");
            Server.broadcast(new Message(MessageType.USER_LIST, new java.util.ArrayList<>(Server.clients.keySet())));

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg.getType() == MessageType.CHAT_MSG) {
                    Server.broadcast(msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(username + " je prekinuo konekciju.");
        } finally {
            if (username != null) {
                Server.clients.remove(username);
                Server.broadcast(new Message(MessageType.USER_LIST, new java.util.ArrayList<>(Server.clients.keySet())));
            }
        }
    }

    public synchronized void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}