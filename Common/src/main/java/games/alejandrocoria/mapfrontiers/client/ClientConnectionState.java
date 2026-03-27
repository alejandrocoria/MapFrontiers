package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class ClientConnectionState {
    public enum HandshakeOutcome {
        NONE,
        RESOLVED,
        UPGRADED
    }

    private boolean handshakeSent = false;
    private long handshakeNonce = 0L;
    private long handshakeStartedAtMs = 0L;
    private long lastHandshakeSentAtMs = 0L;
    private boolean handshakeResolved = false;
    private boolean modOnServer = false;
    private boolean initialSettingsProfileReceived = false;
    private boolean initialFrontiersReceived = false;
    private boolean clientApiPublished = false;
    private @Nullable SettingsProfile settingsProfile;

    public boolean isHandshakeSent() {
        return handshakeSent;
    }

    public boolean isHandshakeResolved() {
        return handshakeResolved;
    }

    public boolean isModOnServer() {
        return modOnServer;
    }

    public long getHandshakeStartedAtMs() {
        return handshakeStartedAtMs;
    }

    public boolean isInitialSettingsProfileReceived() {
        return initialSettingsProfileReceived;
    }

    public boolean isInitialFrontiersReceived() {
        return initialFrontiersReceived;
    }

    public @Nullable SettingsProfile getSettingsProfile() {
        return settingsProfile;
    }

    public boolean restartHandshakeIfUnresolved() {
        if (handshakeResolved) {
            return false;
        }

        restartHandshake();
        return true;
    }

    public void restartHandshake() {
        handshakeSent = false;
        handshakeResolved = false;
        modOnServer = false;
        handshakeNonce = 0L;
        handshakeStartedAtMs = 0L;
        lastHandshakeSentAtMs = 0L;
        initialSettingsProfileReceived = false;
        initialFrontiersReceived = false;
        clientApiPublished = false;
        settingsProfile = null;
    }

    public boolean shouldSendHandshake(long now, long retryMs) {
        return !handshakeResolved && (!handshakeSent || now - lastHandshakeSentAtMs >= retryMs);
    }

    public long markHandshakeSent(long now) {
        if (!handshakeSent) {
            handshakeNonce = now;
            handshakeStartedAtMs = now;
        }

        handshakeSent = true;
        lastHandshakeSentAtMs = now;
        return handshakeNonce;
    }

    public boolean hasHandshakeTimedOut(long now, long timeoutMs) {
        return !handshakeResolved && handshakeStartedAtMs > 0L && now - handshakeStartedAtMs >= timeoutMs;
    }

    public HandshakeOutcome onHandshakeAck(long nonce) {
        if (!handshakeResolved && nonce == handshakeNonce) {
            return resolveHandshake(true);
        }

        if (handshakeResolved && !modOnServer && nonce == handshakeNonce) {
            modOnServer = true;
            return HandshakeOutcome.UPGRADED;
        }

        return HandshakeOutcome.NONE;
    }

    public boolean updateSettingsProfile(SettingsProfile profile) {
        if (Objects.equals(settingsProfile, profile)) {
            return false;
        }

        settingsProfile = profile;
        initialSettingsProfileReceived = true;
        return true;
    }

    public void markInitialFrontiersReceived() {
        initialFrontiersReceived = true;
    }

    public HandshakeOutcome resolveHandshake(boolean hasModOnServer) {
        if (handshakeResolved) {
            if (!modOnServer && hasModOnServer) {
                modOnServer = true;
                return HandshakeOutcome.UPGRADED;
            }
            return HandshakeOutcome.NONE;
        }

        handshakeResolved = true;
        modOnServer = hasModOnServer;
        return HandshakeOutcome.RESOLVED;
    }

    public boolean shouldPublishClientApi() {
        if (!handshakeResolved || clientApiPublished) {
            return false;
        }

        return !modOnServer || (initialSettingsProfileReceived && initialFrontiersReceived);
    }

    public void markClientApiPublished() {
        clientApiPublished = true;
    }
}
