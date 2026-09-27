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
    private GameSession currentGame;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public boolean isUIgri() {
        return uIgri;
    }

    public void setCurrentGame(GameSession g) {
        this.currentGame = g;
    }

    public GameSession getCurrentGame() {
        return currentGame;
    }

    public void setUIgri(boolean vrijednost) {
        this.uIgri = vrijednost;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message loginMsg = (Message) in.readObject();
            username = (String) loginMsg.getPayload();
            Server.clients.put(username, this);
            System.out.println(username + " se prijavio.");
            Server.broadcast(new Message(MessageType.USER_LIST, Server.statusSvihKorisnika()));

            while (true) {
                Message msg = (Message) in.readObject();

                if (msg.getType() == MessageType.CHAT_MSG) {
                    if (currentGame != null) {
                        currentGame.handleChat(this, msg);
                    } else {
                        for (ClientHandler h : Server.clients.values()) {
                            if (h != this && h.getCurrentGame() == null) {
                                h.send(msg);
                            }
                        }
                    }

                } else if (msg.getType() == MessageType.INVITE) {
                    if (uIgri) {
                        send(new Message(MessageType.CHAT_MSG, "Vec si u partiji, ne mozes pozivati druge."));
                    } else {
                        List<String> pozvani = (List<String>) msg.getPayload();
                        pozvani.remove(username);

                        if (pozvani.isEmpty()) {
                            send(new Message(MessageType.CHAT_MSG, "Ne mozes pozvati sam sebe."));
                        } else {
                            Server.activeInvites.put(username, new ArrayList<>());
                            for (String ime : pozvani) {
                                ClientHandler h = Server.clients.get(ime);
                                if (h != null && !h.isUIgri()) {
                                    h.send(new Message(MessageType.GAME_INVITE_RECEIVED, username));
                                }
                            }
                            System.out.println(username + " je pozvao: " + pozvani);
                        }
                    }
                } else if (msg.getType() == MessageType.INVITE_ACCEPT) {
                    String kreator = (String) msg.getPayload();

                    if (uIgri) {
                        send(new Message(MessageType.CHAT_MSG, "Vec si prihvatio poziv ili si u partiji."));
                    } else {
                        List<String> prihvatili = Server.activeInvites.get(kreator);
                        if (prihvatili == null) {
                            send(new Message(MessageType.CHAT_MSG, "Taj poziv vise ne postoji."));
                        } else if (prihvatili.size() >= 5) {
                            send(new Message(MessageType.CHAT_MSG, "Partija je puna (max 6 igraca)."));
                        } else {
                            prihvatili.add(username);
                            uIgri=true;
                            System.out.println(username + " je prihvatio poziv od " + kreator);

                            ClientHandler kreatorHandler = Server.clients.get(kreator);
                            if (kreatorHandler != null) {
                                kreatorHandler.setUIgri(true);
                                kreatorHandler.send(new Message(MessageType.INVITE_ACCEPTED_NOTIFY, username));
                            }
                        }
                    }
                } else if (msg.getType() == MessageType.START_GAME) {
                    List<String> prihvatili = Server.activeInvites.get(username);
                    if (prihvatili != null && !prihvatili.isEmpty()) {
                        List<ClientHandler> igraci = new ArrayList<>();
                        igraci.add(this); //kreator
                        for (String ime : prihvatili) {
                            igraci.add(Server.clients.get(ime));
                        }

                        GameSession sesija = new GameSession(igraci, Server.asocijacije);
                        for (ClientHandler h : igraci) {
                            h.setUIgri(true);
                            Server.broadcast(new Message(MessageType.USER_LIST, Server.statusSvihKorisnika()));
                            h.setCurrentGame(sesija);
                        }
                        new Thread(sesija).start();

                        Server.activeInvites.remove(username);
                        System.out.println("Partija pokrenuta: " + username + " + " + prihvatili);
                    }

                } else if (msg.getType() == MessageType.CANCEL) {
                    List<String> prihvatili = Server.activeInvites.remove(username);
                    if (prihvatili != null) {
                        //kreator
                        for (String ime : prihvatili) {
                            ClientHandler h = Server.clients.get(ime);
                            if (h != null){
                                h.setUIgri(false);
                            }
                        }
                        uIgri=false;
                        System.out.println(username + " je otkazao svoj poziv.");
                    } else if (uIgri) {
                        //prihvatio poziv
                        for (List<String> lista : Server.activeInvites.values()) {
                            lista.remove(username);
                        }
                        uIgri = false;
                        System.out.println(username + " je otkazao svoje prihvatanje.");
                    }
                    Server.broadcast(new Message(MessageType.USER_LIST, Server.statusSvihKorisnika()));

                } else if (msg.getType() == MessageType.OPEN_FIELD
                        || msg.getType() == MessageType.GUESS_COLUMN
                        || msg.getType() == MessageType.GUESS_FINAL) {
                    if (currentGame != null) {
                        currentGame.handleMessage(this, msg);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(username + " je prekinuo konekciju.");
        } finally {
            if (username != null) {
                Server.clients.remove(username);
                Server.broadcast(new Message(MessageType.USER_LIST, Server.statusSvihKorisnika()));
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