package org.sgrewritten.stargate.util;

import net.md_5.bungee.api.ChatColor;
import org.sgrewritten.stargate.config.ConfigurationHelper;
import org.sgrewritten.stargate.config.ConfigurationOption;

/**
 * A helper class for dealing with portal and network names
 */
public final class NameHelper {

    private NameHelper() {

    }

    /**
     * Trims a name and replaces whitespace with normal spaces
     *
     * @param name <p>The name to check</p>
     * @return <p>The trimmed name</p>
     */
    public static String getTrimmedName(String name) {
        if(name == null) {
            return null;
        }
        name = name.replaceAll("\\s\\s+", " ");
        return name.trim();
    }

    /**
     * Gets the normalized version of the given name
     *
     * <p>This basically just lower-cases the name, and strips color if enabled. This is to make names
     * case-agnostic and optionally color-agnostic.</p>
     *
     * @param name <p>The name to "hash"</p>
     * @return <p>The "hashed" name</p>
     */
    public static String getNormalizedName(String name) {
        String normalizedName = name.toLowerCase();
        if (ConfigurationHelper.getBoolean(ConfigurationOption.DISABLE_CUSTOM_COLORED_NAMES)) {
            normalizedName = ChatColor.stripColor(normalizedName);
        }
        return normalizedName;
    }

}
