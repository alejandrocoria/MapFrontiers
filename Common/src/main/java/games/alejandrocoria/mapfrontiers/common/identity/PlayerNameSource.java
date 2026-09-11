package games.alejandrocoria.mapfrontiers.common.identity;

public enum PlayerNameSource {
    HINT(10),
    MINECRAFT_CACHE(20),
    SERVER_SYNC(30),
    CONNECTED_PROFILE(40);

    private final int priority;

    PlayerNameSource(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
