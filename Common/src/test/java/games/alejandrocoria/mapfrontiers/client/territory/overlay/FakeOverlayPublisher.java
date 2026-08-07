package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Displayable;

import java.util.ArrayList;
import java.util.List;

final class FakeOverlayPublisher implements OverlayPublisher {
    enum OperationType {
        SHOW,
        REMOVE
    }

    record Operation(OperationType type, Displayable overlay) {
    }

    private final List<Operation> operations = new ArrayList<>();
    private boolean available = true;
    private int showFailuresRemaining;
    private int removeFailuresRemaining;

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void show(Displayable overlay) {
        operations.add(new Operation(OperationType.SHOW, overlay));
        if (showFailuresRemaining > 0) {
            showFailuresRemaining--;
            throw new TestPublicationException("show failed");
        }
    }

    @Override
    public void remove(Displayable overlay) {
        operations.add(new Operation(OperationType.REMOVE, overlay));
        if (removeFailuresRemaining > 0) {
            removeFailuresRemaining--;
            throw new TestPublicationException("remove failed");
        }
    }

    void setAvailable(boolean available) {
        this.available = available;
    }

    void failNextShow() {
        showFailuresRemaining++;
    }

    void failNextRemove() {
        removeFailuresRemaining++;
    }

    List<Operation> operations() {
        return operations;
    }

    void clearOperations() {
        operations.clear();
    }

    private static final class TestPublicationException extends RuntimeException {
        private TestPublicationException(String message) {
            super(message);
        }
    }
}
