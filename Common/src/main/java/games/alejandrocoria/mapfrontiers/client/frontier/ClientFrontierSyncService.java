package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ClientFrontierSyncService {
    private static final Minecraft minecraft = Minecraft.getInstance();

    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientLocalPersonalFrontierStore localPersonalStore;
    private boolean localPersonalFrontiersLoaded = false;

    public ClientFrontierSyncService(FrontiersOverlayManager globalManager,
                                     FrontiersOverlayManager personalManager,
                                     ClientLocalPersonalFrontierStore localPersonalStore) {
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.localPersonalStore = localPersonalStore;
    }

    public void loadLocalPersonalFrontiers() {
        if (localPersonalFrontiersLoaded || minecraft.isLocalServer()) {
            return;
        }

        for (FrontierData frontier : localPersonalStore.loadFrontiers()) {
            personalManager.addFrontier(frontier);
        }

        localPersonalFrontiersLoaded = true;
    }

    public void applyServerSnapshot(List<FrontierData> globalFrontiers, List<FrontierData> personalFrontiers) {
        loadLocalPersonalFrontiers();

        globalManager.replaceFrontiers(globalFrontiers);

        List<FrontierOverlay> existingLocalPersonal = personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
        Set<UUID> serverFrontierIds = new HashSet<>();

        for (FrontierData data : personalFrontiers) {
            serverFrontierIds.add(data.getId());
            FrontierOverlay existing = personalManager.getFrontier(data.getId());
            if (existing != null) {
                existing.updateFromData(data);
            } else {
                personalManager.addFrontier(data);
            }
        }

        List<FrontierOverlay> localOnlyOwnedFrontiers = new ArrayList<>();
        if (minecraft.player != null) {
            SettingsUser currentPlayer = new SettingsUser(minecraft.player);
            for (FrontierOverlay localFrontier : existingLocalPersonal) {
                if (!serverFrontierIds.contains(localFrontier.getId()) && localFrontier.getOwner().equals(currentPlayer)) {
                    localOnlyOwnedFrontiers.add(localFrontier);
                }
            }
        }

        for (FrontierOverlay frontier : localOnlyOwnedFrontiers) {
            frontier.removeAllUserShared();
            PacketHandler.sendToServer(new PacketPersonalFrontier(frontier));
        }
        persistOwnedPersonalFrontiers();
    }

    public void close() {
        localPersonalFrontiersLoaded = false;
    }

    private void persistOwnedPersonalFrontiers() {
        if (minecraft.isLocalServer() || minecraft.player == null) {
            return;
        }

        localPersonalStore.saveOwnedFrontierMirror(getAllPersonalFrontiers(), new SettingsUser(minecraft.player));
    }

    private Collection<FrontierOverlay> getAllPersonalFrontiers() {
        return personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
