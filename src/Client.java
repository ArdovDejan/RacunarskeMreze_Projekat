import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Arrays;

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
                    }else if (msg.getType() == MessageType.GAME_INVITE_RECEIVED) {
                        System.out.println((String) msg.getPayload() + " te je pozvao na igru! Ukucaj /accept " + msg.getPayload() + " da prihvatis.");
                    }else if (msg.getType() == MessageType.INVITE_ACCEPTED_NOTIFY) {
                        System.out.println((String) msg.getPayload() + " je prihvatio tvoj poziv!");
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
            System.out.print(username + ": ");
            String text = scanner.nextLine();

            if (text.startsWith("/invite ")) {
                String[] imena = text.substring(8).split(",");
                client.send(new Message(MessageType.INVITE, Arrays.asList(imena)));
            } else if (text.startsWith("/accept ")) {
                String kreator = text.substring(8).trim();
                client.send(new Message(MessageType.INVITE_ACCEPT, kreator));
            } else if (text.equals("/start")) {
                client.send(new Message(MessageType.START_GAME, null));
            } else {
                client.send(new Message(MessageType.CHAT_MSG, username + ": " + text));
            }
        }
    }
}