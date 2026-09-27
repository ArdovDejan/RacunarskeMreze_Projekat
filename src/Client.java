import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Map;

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
                        Map<String, Boolean> statusi = (Map<String, Boolean>) msg.getPayload();
                        for (var e : statusi.entrySet()) {
                            System.out.println(e.getKey() + (e.getValue() ? " (u igri)" : " (slobodan)"));
                        }
                    } else if (msg.getType() == MessageType.CHAT_MSG) {
                        System.out.println((String) msg.getPayload());
                    }else if (msg.getType() == MessageType.GAME_INVITE_RECEIVED) {
                        System.out.println((String) msg.getPayload() + " te je pozvao na igru! Ukucaj /accept " + msg.getPayload() + " da prihvatis.");
                    }else if (msg.getType() == MessageType.INVITE_ACCEPTED_NOTIFY) {
                        System.out.println((String) msg.getPayload() + " je prihvatio tvoj poziv!");
                    } else if (msg.getType() == MessageType.BOARD_STATE) {
                        System.out.println("\n=== " + msg.getPayload() + " ===");
                    } else if (msg.getType() == MessageType.FIELD_OPENED) {
                        Object[] d = (Object[]) msg.getPayload();
                        char kolonaSlovo = (char) ('A' + (int) d[0]);
                        System.out.println("" + kolonaSlovo + (((int) d[1]) + 1) + ": " + d[2]);
                    } else if (msg.getType() == MessageType.ROUND_END) {
                        Asocijacija a = (Asocijacija) msg.getPayload();
                        System.out.println("--- Vrijeme isteklo / rjesenja ---");
                        for (int k = 0; k < 4; k++) {
                            System.out.println((char)('A'+k) + ": " + a.getKolone()[k].getRjesenje());
                        }
                        System.out.println("FINALNO: " + a.getFinalnoRjesenje());
                    } else if (msg.getType() == MessageType.SCORE_UPDATE) {
                        System.out.println("Bodovi: " + msg.getPayload());
                    } else if (msg.getType() == MessageType.GAME_END) {
                        System.out.println("=== KRAJ PARTIJE: " + msg.getPayload() + " ===");
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
            }else if (text.startsWith("/open ")) {
                try {
                    String[] d = text.substring(6).trim().split(" ");
                    if (d.length < 2) {
                        System.out.println("Koristi: /open <kolona> <red>  npr. /open A 2");
                    } else {
                        int kolona = d[0].toUpperCase().charAt(0) - 'A';
                        int red = Integer.parseInt(d[1]) - 1;
                        client.send(new Message(MessageType.OPEN_FIELD, new int[]{kolona, red}));
                    }
                } catch (Exception e) {
                    System.out.println("Neispravan unos. Koristi: /open <kolona> <red>  npr. /open A 2");
                }

            } else if (text.startsWith("/guess ")) {
                try {
                    String[] d = text.substring(7).trim().split(" ", 2);
                    if (d.length < 2) {
                        System.out.println("Koristi: /guess <kolona> <rjesenje>  npr. /guess A CVIJECE");
                    } else {
                        int kolona = d[0].toUpperCase().charAt(0) - 'A';
                        client.send(new Message(MessageType.GUESS_COLUMN, new Object[]{kolona, d[1]}));
                    }
                } catch (Exception e) {
                    System.out.println("Neispravan unos. Koristi: /guess <kolona> <rjesenje>  npr. /guess A CVIJECE");
                }

            } else if (text.startsWith("/guessfinal ")) {
                String rjesenje = text.substring(12).trim();
                if (rjesenje.isEmpty()) {
                    System.out.println("Koristi: /guessfinal <rjesenje>");
                } else {
                    client.send(new Message(MessageType.GUESS_FINAL, rjesenje));
                }
            } else {
                client.send(new Message(MessageType.CHAT_MSG, username + ": " + text));
            }
        }
    }
}