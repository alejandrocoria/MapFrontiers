package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
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
    private static final Minecraft mc = Minecraft.getInstance();

    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientCollectionRuntime collectionRuntime;
    private final ClientLocalPersonalFrontierStore localPersonalStore;
    private boolean localPersonalFrontiersLoaded = false;

    public ClientFrontierSyncService(FrontiersOverlayManager globalManager,
                                     FrontiersOverlayManager personalManager,
                                     ClientCollectionRuntime collectionRuntime,
                                     ClientLocalPersonalFrontierStore localPersonalStore) {
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.collectionRuntime = collectionRuntime;
        this.localPersonalStore = localPersonalStore;
    }

    public void loadLocalPersonalFrontiers() {
        if (localPersonalFrontiersLoaded || mc.isLocalServer()) {
            return;
        }

        for (FrontierData frontier : localPersonalStore.loadFrontiers()) {
            if (!frontier.isPersistent()) {
                continue;
            }
            personalManager.addFrontier(frontier);
        }

        localPersonalFrontiersLoaded = true;
    }

    public void applyServerSnapshot(List<FrontierData> globalFrontiers,
                                    List<FrontierData> personalFrontiers,
                                    List<CollectionData> globalCollections,
                                    List<CollectionData> personalCollections) {
        loadLocalPersonalFrontiers();

        globalManager.replaceFrontiers(globalFrontiers);
        collectionRuntime.replaceCollections(globalCollections, personalCollections);
        if (mc.isLocalServer()) {
            personalManager.replaceFrontiers(personalFrontiers);
            collectionRuntime.refreshFromFrontiers(globalManager, personalManager);
            return;
        }

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
        if (mc.player != null) {
            SettingsUser currentPlayer = new SettingsUser(mc.player);
            for (FrontierOverlay localFrontier : existingLocalPersonal) {
                if (!serverFrontierIds.contains(localFrontier.getId()) && localFrontier.getOwner().equals(currentPlayer)
                        && localFrontier.isPersistent()) {
                    localOnlyOwnedFrontiers.add(localFrontier);
                }
            }
        }

        for (FrontierOverlay frontier : localOnlyOwnedFrontiers) {
            frontier.removeAllUserShared();
            PacketHandler.sendToServer(new PacketPersonalFrontier(frontier));
        }
        collectionRuntime.refreshFromFrontiers(globalManager, personalManager);
        persistOwnedPersonalFrontiers();
    }

    public void close() {
        localPersonalFrontiersLoaded = false;
        collectionRuntime.clear();
    }

    private void persistOwnedPersonalFrontiers() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        localPersonalStore.saveOwnedFrontierMirror(getAllPersonalFrontiers(), new SettingsUser(mc.player));
    }

    private Collection<FrontierOverlay> getAllPersonalFrontiers() {
        return personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
