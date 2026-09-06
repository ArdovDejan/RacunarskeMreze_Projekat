import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private ObjectOutputStream out;

    public Client(String serverAddress, int port, String username) throws IOException {
        Socket socket = new Socket(serverAddress, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        send(new Message(MessageType.LOGIN, username));

        new Thread(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    if (msg.getType() == MessageType.USER_LIST) {
                        System.out.println("Korisnici: " + msg.getPayload());
                    } else if (msg.getType() == MessageType.CHAT_MSG) {
                        System.out.println((String) msg.getPayload());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Veza prekinuta.");
            }
        }).start();
    }

    public void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Unesi ime: ");
        String username = scanner.nextLine();

        Client client = new Client("localhost", 12345, username);

        while (true) {
            String text = scanner.nextLine();
            client.send(new Message(MessageType.CHAT_MSG, username + ": " + text));
        }
    }
}