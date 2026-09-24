import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class Server {
    private static final int PORT = 12345;
    public static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server pokrenut na portu " + PORT);

        List<Asocijacija> asocijacije = AsocijacijeLoader.ucitaj("asocijacije.txt");
        System.out.println("Ucitano asocijacija: " + asocijacije.size());
        System.out.println("Prva finalna: " + asocijacije.get(0).getFinalnoRjesenje());

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            new Thread(handler).start();
        }
    }

    public static void broadcast(Message message) {
        for (ClientHandler handler : clients.values()) {
            handler.send(message);
        }
    }
}