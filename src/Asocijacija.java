import java.io.Serializable;

public class Asocijacija implements Serializable {
    private Kolona[] kolone;
    private String finalnoRjesenje;

    public Asocijacija(Kolona[] kolone, String finalnoRjesenje) {
        this.kolone = kolone;
        this.finalnoRjesenje = finalnoRjesenje;
    }

    public Kolona[] getKolone() {
        return kolone;
    }

    public String getFinalnoRjesenje() {
        return finalnoRjesenje;
    }
}