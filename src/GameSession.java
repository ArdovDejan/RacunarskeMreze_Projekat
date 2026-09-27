import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameSession implements Runnable {
    private List<ClientHandler> igraci;
    private List<Asocijacija> asocijacije;
    private Asocijacija trenutna;
    private boolean[][] otvoreno; //[kolona][red]
    private boolean[] kolonaPogodjena;
    private boolean finalnoPogodjeno;
    private Map<String, Integer> bodovi = new ConcurrentHashMap<>();

    public GameSession(List<ClientHandler> igraci, List<Asocijacija> asocijacije) {
        this.igraci = igraci;
        this.asocijacije = asocijacije;
        for (ClientHandler h : igraci) {
            bodovi.put(h.getUsername(), 0);
        }
    }

    public void run() {
        for (Asocijacija a : asocijacije) {
            igrajRundu(a);
        }
        objaviPobjednika();

        for (ClientHandler h : igraci) {
            h.setUIgri(false);
            h.setCurrentGame(null);
        }
        Server.broadcast(new Message(MessageType.USER_LIST, Server.statusSvihKorisnika()));
    }

    private void igrajRundu(Asocijacija a) {
        trenutna = a;
        otvoreno = new boolean[4][4];
        kolonaPogodjena = new boolean[4];
        finalnoPogodjeno = false;

        posaljiSvima(new Message(MessageType.BOARD_STATE, "Nova asocijacija!"));

        long krajVremena = System.currentTimeMillis() + 90_000;
        synchronized (this) {
            while (!finalnoPogodjeno && System.currentTimeMillis() < krajVremena) {
                long preostalo = krajVremena - System.currentTimeMillis();
                try {
                    wait(Math.min(preostalo, 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        posaljiSvima(new Message(MessageType.ROUND_END, trenutna));
        posaljiSvima(new Message(MessageType.SCORE_UPDATE, new HashMap<>(bodovi)));
    }

    public synchronized void handleMessage(ClientHandler posiljalac, Message msg) {
        if (msg.getType() == MessageType.OPEN_FIELD) {
            int[] pozicija = (int[]) msg.getPayload(); //{kolona, red}
            int k = pozicija[0], r = pozicija[1];
            if (kolonaPogodjena[k]) {
                return;
            }
            if (!otvoreno[k][r]) {
                otvoreno[k][r] = true;
                String pojam = trenutna.getKolone()[k].getPojmovi()[r];
                posaljiSvima(new Message(MessageType.FIELD_OPENED, new Object[]{k, r, pojam}));
            }

        } else if (msg.getType() == MessageType.GUESS_COLUMN) {
            Object[] podaci = (Object[]) msg.getPayload(); //{kolona, tekst}
            int k = (int) podaci[0];
            String pokusaj = ((String) podaci[1]).trim();

            if (!kolonaPogodjena[k] && pokusaj.equalsIgnoreCase(trenutna.getKolone()[k].getRjesenje())) {
                kolonaPogodjena[k] = true;
                int otvorenaPolja = brojOtvorenih(k);
                int poeni = Math.max(4 - otvorenaPolja, 1);
                dodajBodove(posiljalac.getUsername(), poeni);
                posaljiSvima(new Message(MessageType.CHAT_MSG,posiljalac.getUsername() + " je pogodio kolonu (+" + poeni + " poena)"));
            }

        } else if (msg.getType() == MessageType.GUESS_FINAL) {
            String pokusaj = ((String) msg.getPayload()).trim();

            if (!finalnoPogodjeno && pokusaj.equalsIgnoreCase(trenutna.getFinalnoRjesenje())) {
                int ukupnoOtvorenih = ukupnoOtvorenihPolja();
                int poeni = Math.max(10 - ukupnoOtvorenih, 2);
                dodajBodove(posiljalac.getUsername(), poeni);
                posaljiSvima(new Message(MessageType.CHAT_MSG,posiljalac.getUsername() + " je pogodio FINALNO RJESENJE (+" + poeni + " poena)"));
                finalnoPogodjeno = true;
                notifyAll(); //timer, zavrsavamo rundu odma
            }
        }
    }

    private int brojOtvorenih(int kolona) {
        int broj = 0;
        for (int r = 0; r < 4; r++) if (otvoreno[kolona][r]) broj++;
        return broj;
    }

    private int ukupnoOtvorenihPolja() {
        int broj = 0;
        for (int k = 0; k < 4; k++) broj += brojOtvorenih(k);
        return broj;
    }

    private void dodajBodove(String igrac, int poeni) {
        bodovi.merge(igrac, poeni, Integer::sum);
    }

    private void posaljiSvima(Message m) {
        for (ClientHandler h : igraci) {
            h.send(m);
        }
    }

    public void handleChat(ClientHandler posiljalac, Message msg) {
        for (ClientHandler h : igraci) {
            if (h != posiljalac) {
                h.send(msg);
            }
        }
    }


    private void objaviPobjednika() {
        String pobjednik = null;
        int max = -1;
        for (var e : bodovi.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                pobjednik = e.getKey();
            }
        }
        posaljiSvima(new Message(MessageType.GAME_END, pobjednik + " je pobjednik sa " + max + " poena!"));
    }
}