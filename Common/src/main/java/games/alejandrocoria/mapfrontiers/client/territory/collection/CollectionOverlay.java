package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class CollectionOverlay {
    private final CollectionOverlayKey key;
    private @Nullable CollectionData collection;
    private List<FrontierOverlay> memberFrontiers;
    private boolean needUpdateOverlay = true;
    private boolean membershipDirty = true;
    private boolean geometryDirty = true;
    private boolean labelDirty = true;
    private @Nullable Runnable dirtyOverlayListener;

    public CollectionOverlay(CollectionOverlayKey key, CollectionData collection, List<FrontierOverlay> members) {
        this.key = key;
        this.collection = collection;
        memberFrontiers = List.copyOf(members);
    }

    public CollectionOverlayKey getKey() {
        return key;
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public List<FrontierOverlay> getMemberFrontiers() {
        return memberFrontiers;
    }

    public void refreshMembersAndCollection(CollectionData collection, List<FrontierOverlay> members) {
        boolean dirty = false;

        if (this.collection != collection) {
            this.collection = collection;
            labelDirty = true;
            dirty = true;
        }

        List<FrontierOverlay> updatedMembers = List.copyOf(members);
        if (!memberFrontiers.equals(updatedMembers)) {
            memberFrontiers = updatedMembers;
            membershipDirty = true;
            geometryDirty = true;
            labelDirty = true;
            dirty = true;
        }

        if (dirty) {
            invalidateOverlayRefresh();
        }
    }

    public void processDirtyOverlay() {
        if (needUpdateOverlay) {
            refreshOverlay();
        }
    }

    public void rebuildOverlayNow() {
        membershipDirty = true;
        geometryDirty = true;
        labelDirty = true;
        refreshOverlay();
    }

    public void deleted() {
        dirtyOverlayListener = null;
    }

    void setDirtyOverlayListener(@Nullable Runnable dirtyOverlayListener) {
        this.dirtyOverlayListener = dirtyOverlayListener;
    }

    private void refreshOverlay() {
        needUpdateOverlay = false;
        membershipDirty = false;
        geometryDirty = false;
        labelDirty = false;
    }

    private void invalidateOverlayRefresh() {
        if (!needUpdateOverlay && dirtyOverlayListener != null) {
            dirtyOverlayListener.run();
        }
        needUpdateOverlay = true;
    }
}
