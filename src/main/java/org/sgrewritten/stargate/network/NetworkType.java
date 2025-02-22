package org.sgrewritten.stargate.network;

import org.sgrewritten.stargate.api.formatting.TranslatableMessage;
import org.sgrewritten.stargate.api.network.portal.flag.PortalFlag;
import org.sgrewritten.stargate.api.network.portal.flag.StargateFlag;
import org.sgrewritten.stargate.network.portal.formatting.HighlightingStyle;

import java.util.Set;

public enum NetworkType {

    /**
     * A network that is directly linked to players
     */
    PERSONAL(HighlightingStyle.CURLY_BRACKETS, StargateFlag.PERSONAL_NETWORK, TranslatableMessage.PERSONAL_NETWORK),

    /**
     * The default network
     */
    DEFAULT(HighlightingStyle.SQUARE_BRACKETS, StargateFlag.DEFAULT_NETWORK, TranslatableMessage.DEFAULT_NETWORK),

    /**
     * A customised network
     */
    CUSTOM(HighlightingStyle.ROUNDED_BRACKETS, StargateFlag.CUSTOM_NETWORK, TranslatableMessage.CUSTOM_NETWORK),

    /**
     * A terminal network
     */
    TERMINAL(HighlightingStyle.DOUBLE_GREATER_LESSER_THAN, StargateFlag.TERMINAL_NETWORK, TranslatableMessage.TERMINAL_NETWORK);

    private final HighlightingStyle style;
    private final StargateFlag flag;
    private final TranslatableMessage terminology;

    /**
     * The network type as determined by its given style and flags.
     *
     * @param style <p>The applicable HighlightingStyle</p>
     * @param flag  <p>The applicable PortalFlag</p>
     */
    NetworkType(HighlightingStyle style, StargateFlag flag, TranslatableMessage terminology) {
        this.style = style;
        this.flag = flag;
        this.terminology = terminology;
    }

    /**
     * The applicable highlighting style.
     *
     * @return HighlightingStyle style
     */
    public HighlightingStyle getHighlightingStyle() {
        return style;
    }

    /**
     * The applicable flags.
     *
     * @return PortalFlag flag
     */
    public StargateFlag getRelatedFlag() {
        return flag;
    }

    /**
     * @return <p> The localized word for this network type </p>
     */
    public TranslatableMessage getTerminology() {
        return terminology;
    }

    /**
     * Whether the style of the gate contains sufficient information to determine its network type.
     *
     * @param style <p>The applicable HighlightingStyle</p>.
     * @return <p>Whether or not sufficient information was available, represented as a boolean</p>
     */
    public static boolean styleGivesNetworkType(HighlightingStyle style) {
        return getNetworkTypeFromHighlight(style) != null;
    }

    /**
     * A method to remove internal flags related to the network's type.
     *
     * @param flags <p>The applicable PortalFlags</p>.
     */
    public static void removeNetworkTypeRelatedFlags(Set<PortalFlag> flags) {
        for (NetworkType type : NetworkType.values()) {
            StargateFlag flagToRemove = type.getRelatedFlag();
            if (flagToRemove == null) {
                continue;
            }
            flags.remove(type.getRelatedFlag());
        }
    }

    /**
     * A method that infers a NetworkType from a provided HighlightingStyle, if sufficient information is available.
     *
     * @param highlight <p>The applicable HighlightingStyle</p>
     * @return NetworkType type
     */
    public static NetworkType getNetworkTypeFromHighlight(HighlightingStyle highlight) {
        for (NetworkType type : NetworkType.values()) {
            if (type.getHighlightingStyle() == highlight) {
                return type;
            }
        }
        return null;
    }

    /**
     * A method that infers a NetworkType from provided flags.
     *
     * @param flags <p>A PortalFlag set</p>.
     * @return NetworkType type
     */
    public static NetworkType getNetworkTypeFromFlags(Set<PortalFlag> flags) {
        for (NetworkType type : NetworkType.values()) {
            if (flags.contains(type.getRelatedFlag())) {
                return type;
            }
        }
        return null;
    }

    /**
     * @param flag <p>A portal flag</p>
     * @return <p>True if the specified flag relates to a network flag</p>
     */
    public static boolean isNetworkTypeFlag(PortalFlag flag) {
        for (NetworkType type : NetworkType.values()) {
            if (flag == type.getRelatedFlag()) {
                return true;
            }
        }
        return false;
    }
}
