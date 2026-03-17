package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

/**
 * Server-side frontier operations.
 * <p>
 * Methods in this service mutate authoritative server state immediately.
 */
@SuppressWarnings("unused")
public interface ServerFrontierService {
    /**
     * Creates a global frontier directly on server state.
     *
     * @param owner owner to persist in the created frontier
     * @param dimension target dimension
     * @param shape initial frontier shape
     * @return created frontier snapshot
     */
    FrontierDataView createGlobalFrontier(UserRef owner, DimensionId dimension, FrontierShape shape);

    /**
     * Updates a global frontier directly on server state.
     *
     * @param frontierId target frontier id
     * @param mutation partial update payload
     * @return updated frontier snapshot, or empty when not found or not global
     */
    Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation);

    /**
     * Deletes a global frontier directly on server state.
     *
     * @param frontierId target frontier id
     * @return true when deleted
     */
    boolean deleteGlobalFrontier(FrontierId frontierId);

    /**
     * Lists global frontier snapshots for a dimension.
     *
     * @param dimension target dimension
     * @return global frontier snapshots
     */
    List<FrontierDataView> listGlobalFrontiers(DimensionId dimension);

    /**
     * Returns a global frontier snapshot by id.
     *
     * @param frontierId target frontier id
     * @return empty when id is unknown or references a personal frontier
     */
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);
}
