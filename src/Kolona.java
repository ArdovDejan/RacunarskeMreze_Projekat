import java.io.Serializable;

public class Kolona implements Serializable {
    private String[] pojmovi;
    private String rjesenje;

    public Kolona(String[] pojmovi, String rjesenje) {
        this.pojmovi = pojmovi;
        this.rjesenje = rjesenje;
    }

    public String[] getPojmovi() {
        return pojmovi;
    }

    public String getRjesenje() {
        return rjesenje;
    }
}