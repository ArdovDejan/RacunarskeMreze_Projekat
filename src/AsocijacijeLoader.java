import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AsocijacijeLoader {

    public static List<Asocijacija> ucitaj(String putanja) throws IOException {
        List<Asocijacija> sve = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(putanja))) {
            List<String> blok = new ArrayList<>();
            String linija;

            while ((linija = reader.readLine()) != null) {
                if (linija.trim().isEmpty()) {
                    if (!blok.isEmpty()) {
                        sve.add(parsirajBlok(blok));
                        blok.clear();
                    }
                } else {
                    blok.add(linija);
                }
            }
            // ako fajl ne zavrsava praznim redom, obradi i poslednji blok
            if (!blok.isEmpty()) {
                sve.add(parsirajBlok(blok));
            }
        }

        return sve;
    }

    private static Asocijacija parsirajBlok(List<String> blok) {
        // blok ima tačno 5 linija: 4 kolone + finalno rješenje
        Kolona[] kolone = new Kolona[4];

        for (int i = 0; i < 4; i++) {
            String[] dijelovi = blok.get(i).split(";");
            String[] pojmovi = new String[]{dijelovi[0], dijelovi[1], dijelovi[2], dijelovi[3]};
            String rjesenjeKolone = dijelovi[4];
            kolone[i] = new Kolona(pojmovi, rjesenjeKolone);
        }

        String finalnoRjesenje = blok.get(4).trim();
        return new Asocijacija(kolone, finalnoRjesenje);
    }
}