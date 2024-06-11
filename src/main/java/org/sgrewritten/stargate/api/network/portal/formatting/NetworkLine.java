package org.sgrewritten.stargate.api.network.portal.formatting;

import net.md_5.bungee.api.ChatColor;
import org.sgrewritten.stargate.api.network.Network;
import org.sgrewritten.stargate.network.portal.formatting.HighlightingStyle;

import java.util.ArrayList;
import java.util.List;

public class NetworkLine extends AbstractSignLine {


    private final Network network;

    public NetworkLine(List<StargateComponent> components, Network network) {
        super(new ArrayList<>(components));
        this.network = network;
    }

    public NetworkLine(HighlightingStyle style, Network network, ChatColor textColor, ChatColor pointerColor) {
        super(new ArrayList<>());
        List<StargateComponent> components = getComponents();
        components.add(new StargateComponent(pointerColor + style.getPrefix()));
        components.add(new StargateComponent(textColor + (network == null ? "null" : network.getName())));
        components.add(new StargateComponent(pointerColor + style.getSuffix()));
        this.network = network;
    }

    /**
     * @return <p>The network this line relates to</p>
     */
    public Network getNetwork() {
        return this.network;
    }

    @Override
    public SignLineType getType() {
        return SignLineType.NETWORK;
    }
}
