package games.alejandrocoria.mapfrontiers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import journeymap.client.api.IClientAPI;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class FrontiersOverlayManager {
    public static FrontiersOverlayManager instance;

    private IClientAPI jmAPI;
    private HashMap<Integer, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private HashMap<Integer, Integer> frontiersSelected;

    public FrontiersOverlayManager(IClientAPI jmAPI) {
        instance = this;

        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierOverlay>>();
        frontiersSelected = new HashMap<Integer, Integer>();
    }

    public void addFrontier(FrontierData data) {
        if (jmAPI == null) {
            return;
        }

        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());
        frontiers.add(new FrontierOverlay(data, jmAPI));
    }

    public void createNewfrontier(int dimension) {
        // @Incomplete: send packet with creation and first vertex
    }

    public void deleteFrontier(int dimension, int index) {
        // @Incomplete: send packet with deletion
    }

    public Map<Integer, ArrayList<FrontierOverlay>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(int dimension) {
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<FrontierOverlay>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        return frontiers;
    }

    public void updateAllOverlays() {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.updateOverlay();
            }
        }
    }

    public int getFrontierIndexSelected(int dimension) {
        Integer selected = frontiersSelected.get(Integer.valueOf(dimension));
        if (selected == null)
            return -1;
        return selected;
    }

    public void setFrontierIndexSelected(int dimension, int frontier) {
        Integer dim = Integer.valueOf(dimension);
        int prevSelected = frontiersSelected.getOrDefault(dim, -1);
        List<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);

        if (prevSelected >= 0 && prevSelected < frontiers.size()) {
            FrontierOverlay f = frontiers.get(prevSelected);
            f.selected = false;
            f.updateOverlay();
        }

        frontiersSelected.put(dim, Integer.valueOf(frontier));

        if (frontier >= 0) {
            FrontierOverlay f = frontiers.get(frontier);
            f.selected = true;
            f.updateOverlay();
        }
    }
}
