package games.alejandrocoria.mapfrontiers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketNewFrontier;
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

    public FrontierOverlay addFrontier(FrontierData data) {
        if (jmAPI == null) {
            return null;
        }

        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());
        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        frontiers.add(frontierOverlay);

        return frontierOverlay;
    }

    public void clientCreateNewfrontier(int dimension) {
        PacketHandler.INSTANCE
                .sendToServer(new PacketNewFrontier(dimension, ConfigData.addVertexToNewFrontier, ConfigData.snapDistance));
    }

    public void clientDeleteFrontier(int dimension, int index) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);
        if (frontiers == null) {
            return;
        }

        FrontierOverlay frontier = frontiers.get(index);

        if (frontier != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketDeleteFrontier(dimension, frontier.getId()));
        }
    }

    public int deleteFrontier(int dimension, int id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);
        if (frontiers == null) {
            return -1;
        }

        int index = IntStream.range(0, frontiers.size()).filter(i -> frontiers.get(i).getId() == id).findFirst().orElse(-1);

        if (index == -1) {
            return -1;
        }

        frontiers.remove(index);
        return index;
    }

    public int getFrontierIndex(FrontierOverlay frontierOverlay) {
        List<FrontierOverlay> frontiers = getAllFrontiers(frontierOverlay.getDimension());
        if (frontiers == null) {
            return -1;
        }

        return frontiers.lastIndexOf(frontierOverlay);
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
