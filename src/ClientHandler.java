import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private boolean uIgri = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public boolean isUIgri() {
        return uIgri;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message loginMsg = (Message) in.readObject();
            username = (String) loginMsg.getPayload();
            Server.clients.put(username, this);
            System.out.println(username + " se prijavio.");
            Server.broadcast(new Message(MessageType.USER_LIST, new ArrayList<>(Server.clients.keySet())));

            while (true) {
                Message msg = (Message) in.readObject();

                if (msg.getType() == MessageType.CHAT_MSG) {
                    for (ClientHandler h : Server.clients.values()) {
                        if (h != this) h.send(msg);
                    }

                } else if (msg.getType() == MessageType.INVITE) {
                    List<String> pozvani = (List<String>) msg.getPayload();
                    Server.activeInvites.putIfAbsent(username, new ArrayList<>());
                    //System.out.println(Server.activeInvites.toString());
                    for (String ime : pozvani) {
                        ClientHandler h = Server.clients.get(ime);
                        if (h != null && !h.isUIgri()) {
                            h.send(new Message(MessageType.GAME_INVITE_RECEIVED, username));
                        }
                    }
                    System.out.println(username + " je pozvao: " + pozvani);

                } else if (msg.getType() == MessageType.INVITE_ACCEPT) {
                    String kreator = (String) msg.getPayload();
                    List<String> prihvatili = Server.activeInvites.get(kreator);
                    if (prihvatili != null) {
                        prihvatili.add(username);
                        System.out.println(username + " je prihvatio poziv od " + kreator);

                        ClientHandler kreatorHandler = Server.clients.get(kreator);
                        if (kreatorHandler != null) {
                            kreatorHandler.send(new Message(MessageType.INVITE_ACCEPTED_NOTIFY, username));
                        }

                    }

                } else if (msg.getType() == MessageType.START_GAME) {
                    List<String> prihvatili = Server.activeInvites.get(username);
                    if (prihvatili != null && !prihvatili.isEmpty()) {
                        System.out.println("Partija kreće! Kreator: " + username + ", igraci: " + prihvatili);

                        Server.activeInvites.remove(username);
                    } else {
                        System.out.println(username + " je pokusao da pokrene partiju bez prihvacenih igraca.");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(username + " je prekinuo konekciju.");
        } finally {
            if (username != null) {
                Server.clients.remove(username);
                Server.broadcast(new Message(MessageType.USER_LIST, new ArrayList<>(Server.clients.keySet())));
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