import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 12345;
    public static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, List<String>> activeInvites = new ConcurrentHashMap<>();

    public static List<Asocijacija> asocijacije;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server pokrenut na portu " + PORT);

        asocijacije = AsocijacijeLoader.ucitaj("asocijacije.txt");
        System.out.println("Ucitano asocijacija: " + asocijacije.size());

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    public static void broadcast(Message message) {
        for (ClientHandler handler : clients.values()) {
            handler.send(message);
        }
    }

    public static Map<String, Boolean> statusSvihKorisnika() {
        Map<String, Boolean> rezultat = new LinkedHashMap<>();
        for (ClientHandler h : clients.values()) {
            rezultat.put(h.getUsername(), h.isUIgri());
        }
        return rezultat;
    }
}