package games.alejandrocoria.mapfrontiers.client.network;

public class ClientRequestIdSequence {
    private long current;

    public ClientRequestIdSequence() {
    }

    ClientRequestIdSequence(long current) {
        this.current = current;
    }

    public long next() {
        ++current;
        if (current == 0L) {
            ++current;
        }
        return current;
    }

    public void reset() {
        current = 0L;
    }
}
