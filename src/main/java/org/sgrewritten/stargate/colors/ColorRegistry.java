package org.sgrewritten.stargate.colors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.DyeColor;
import org.sgrewritten.stargate.Stargate;
import org.sgrewritten.stargate.api.network.portal.PortalFlag;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ColorRegistry {
    private ColorRegistry(){
        throw new IllegalStateException("Utility class");
    }

    public static final Map<DyeColor, ChatColor> TEXT_COLORS = loadColors(false,"/colors/colorTable.json");
    public static final Map<DyeColor, ChatColor> POINTER_COLORS = loadColors(true, "/colors/colorTable.json");
    public static final Map<PortalFlag, ChatColor> FLAG_COLORS = loadFlagColors("/colors/flagColorTable.json");

    private static Map<DyeColor, ChatColor> loadColors(boolean isPointer, String fileName) {
        try (InputStream inputStream = Stargate.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("Could not find internal file: " + fileName);
            }
            try (Reader reader = new InputStreamReader(inputStream)) {
                JsonElement jsonData = JsonParser.parseReader(reader);
                Map<DyeColor, ChatColor> output = new EnumMap<>(DyeColor.class);
                for (JsonElement jsonElement : jsonData.getAsJsonArray()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    DyeColor dyeColor = DyeColor.valueOf(jsonObject.get("color").getAsString());
                    String hexColor;
                    if (isPointer) {
                        hexColor = jsonObject.get("pointer").getAsString();
                    } else {
                        hexColor = jsonObject.get("text").getAsString();
                    }
                    output.put(dyeColor, ChatColor.of("#" + hexColor));
                }
                return Collections.unmodifiableMap(output);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<PortalFlag, ChatColor> loadFlagColors(String fileName) {
        try (InputStream inputStream = Stargate.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("Could not find internal file: " + fileName);
            }
            try (Reader reader = new InputStreamReader(inputStream)) {
                JsonElement jsonData = JsonParser.parseReader(reader);
                Map<PortalFlag, ChatColor> output = new EnumMap<>(PortalFlag.class);
                for (JsonElement jsonElement : jsonData.getAsJsonArray()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    DyeColor dyeColor = DyeColor.valueOf(jsonObject.get("dyeColor").getAsString());
                    PortalFlag portalFlag = PortalFlag.valueOf(jsonObject.get("flag").getAsString());
                    output.put(portalFlag, POINTER_COLORS.get(dyeColor));
                }
                return Collections.unmodifiableMap(output);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
