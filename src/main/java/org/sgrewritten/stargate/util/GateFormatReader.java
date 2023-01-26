package org.sgrewritten.stargate.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.sgrewritten.stargate.exception.ParsingErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Helper class for reading gate files
 */
public final class GateFormatReader {

    private static final String TAG_IDENTIFIER = "#";
    private static final String SPLIT_IDENTIFIER = ",";
    private static final Material[] allAirTypes = new Material[]{
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
    };
    private static final Map<Material, Material> materialEdgecases = loadMaterialEdgecases();

    private GateFormatReader() {

    }

    /**
     * Reads a gate file
     *
     * @param scanner              <p>The scanner to read from</p>
     * @param characterMaterialMap <p>The map of characters to store valid symbols in</p>
     * @param design               <p>The list to store the loaded design/layout to</p>
     * @param config               <p>The map of config values to store to</p>
     * @return <p>The column count/width of the loaded gate</p>
     * @throws ParsingErrorException <p>If the gate file cannot be parsed</p>
     */
    public static int readGateFile(Scanner scanner, Map<Character, Set<Material>> characterMaterialMap,
                                   List<List<Character>> design, Map<String, String> config) throws ParsingErrorException {
        int columns;
        try {
            columns = readGateFileContents(scanner, characterMaterialMap, design, config);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return columns;
    }

    /**
     * Parses a material string
     *
     * <p>Just parses all the materials based from tags or material id (when a string starts with #,
     * it should be a tag). "," resembles a split in items</p>
     *
     * @param materialString <p>The string describing a material/material class to parse</p>
     * @param line           <p>The line currently parsed</p>
     * @return <p>The parsed material</p>
     * @throws ParsingErrorException <p>If unable to parse the given material</p>
     */
    public static Set<Material> parseMaterial(String materialString, String line) throws ParsingErrorException {
        Set<Material> foundIDs = new HashSet<>();
        String[] individualIDs = materialString.split(SPLIT_IDENTIFIER);
        for (String stringId : individualIDs) {

            //Parse a tag
            if (stringId.startsWith(TAG_IDENTIFIER)) {
                foundIDs.addAll(parseMaterialTag(stringId.trim(), line));
                continue;
            }

            //Parse a normal material
            Material id = Material.getMaterial(stringId.toUpperCase().trim());

            if (id == Material.AIR) {
                foundIDs.addAll(Arrays.asList(allAirTypes));
                continue;
            }

            if (id == null) {
                id = parseMaterialFromLegacyName(stringId);
                if (id == null) {
                    throw new ParsingErrorException("Invalid material ''" + stringId + "''");
                }
            }
            if (materialEdgecases.containsKey(id)) {
                foundIDs.add(materialEdgecases.get(id));
            }
            if (id.isBlock()) {
                foundIDs.add(id);
            }
        }
        if (foundIDs.size() == 0) {
            throw new ParsingErrorException("Invalid field''" + materialString +
                    "'': Field must include at least one block");
        }
        return foundIDs;
    }

    /**
     * Reads a gate file's contents
     *
     * @param scanner              <p>The scanner to read from</p>
     * @param characterMaterialMap <p>The map of characters to store valid symbols in</p>
     * @param design               <p>The list to store the loaded design/layout to</p>
     * @param config               <p>The map of config values to store to</p>
     * @return <p>The column count/width of the loaded gate</p>
     * @throws ParsingErrorException <p>If the gate file cannot be parsed</p>
     */
    private static int readGateFileContents(Scanner scanner, Map<Character, Set<Material>> characterMaterialMap,
                                            List<List<Character>> design, Map<String, String> config) throws ParsingErrorException {
        String line;
        boolean designing = false;
        int columns = 0;

        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (designing) {
                //If we have reached the gate's layout/design, read it
                columns = readGateDesignLine(line, columns, design);
                if (columns < 0) {
                    return -1;
                }
            } else {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (!line.contains("=")) {
                    designing = true;
                    //If we have reached the gate's layout/design, read it
                    columns = readGateDesignLine(line, columns, design);
                    if (columns < 0) {
                        return -1;
                    }
                } else {
                    //Read a normal config value
                    readGateConfigValue(line, characterMaterialMap, config);
                }
            }
        }
        return columns;
    }

    /**
     * Reads one design line of the gate layout file
     *
     * <p>The max columns value is sent through this method in such a way that when the last gate design line is read,
     * the max columns value contains the largest amount of columns (character) found in any of the design's lines.</p>
     *
     * @param line       <p>The line to read</p>
     * @param maxColumns <p>The current max columns value of the design</p>
     * @param design     <p>The two-dimensional list to store the loaded design to</p>
     * @return <p>The new max columns value of the design</p>
     */
    private static int readGateDesignLine(String line, int maxColumns, List<List<Character>> design) {
        List<Character> row = new ArrayList<>();

        //Update the max columns number if this line has more columns
        if (line.length() > maxColumns) {
            maxColumns = line.length();
        }

        for (Character symbol : line.toCharArray()) {
            //Add the read character to the row
            row.add(symbol);
        }

        //Add this row of the gate's design to the two-dimensional design list
        design.add(row);
        return maxColumns;
    }

    /**
     * Reads one config value from the gate layout file
     *
     * @param line                 <p>The line to read</p>
     * @param characterMaterialMap <p>The character to material map to store to</p>
     * @param config               <p>The config value map to store to</p>
     * @throws ParsingErrorException <p>If an invalid material is encountered</p>
     */
    private static void readGateConfigValue(String line, Map<Character, Set<Material>> characterMaterialMap,
                                            Map<String, String> config) throws ParsingErrorException {
        String[] split = line.split("=");
        String key = split[0].trim();
        String value = split[1].trim();

        if (key.length() == 1) {
            //Parse a gate frame material
            Character symbol = key.charAt(0);
            Set<Material> material = parseMaterial(value, line);
            //Register the map between the read symbol and the corresponding materials
            characterMaterialMap.put(symbol, material);
        } else {
            //Read a normal config value
            config.put(key, value);
        }
    }

    /**
     * Parses a material assuming it's defined using a legacy name
     *
     * @param stringId <p>The legacy material name to parse</p>
     * @return <p>The resulting material, or null if no such legacy material exists</p>
     */
    private static Material parseMaterialFromLegacyName(String stringId) {
        return Material.getMaterial(XMaterial.matchXMaterial(stringId).toString());
    }

    /**
     * Parses a material tag
     *
     * @param stringId <p>A string denoting a tag</p>
     * @param line     <p>The line currently parsed</p>
     * @throws ParsingErrorException <p>If unable to parse the tag</p>
     */
    private static Set<Material> parseMaterialTag(String stringId, String line) throws ParsingErrorException {
        Set<Material> foundIDs = EnumSet.noneOf(Material.class);
        String tagString = stringId.replace(TAG_IDENTIFIER, "");
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS,
                NamespacedKey.minecraft(tagString.toLowerCase()), Material.class);
        if (tag == null) {
            throw new ParsingErrorException("Invalid tag in line: " + line);
        }
        for (Material materialInTag : tag.getValues()) {
            if (materialInTag.isBlock()) {
                foundIDs.add(materialInTag);
            }
        }
        return foundIDs;
    }

    private static Map<Material, Material> loadMaterialEdgecases() {
        Map<Material, Material> materialEdgecases = new EnumMap<>(Material.class);
        Map<String, String> temp = new HashMap<>();
        FileHelper.readInternalFileToMap("/util/materialEdgecases.properties", temp);
        for (Material material : Material.values()) {
            for (String edgecase : temp.keySet()) {
                String type = material.toString().replaceAll(edgecase, "");
                if (type.equals(material.toString())) {
                    continue;
                }
                String replacement = temp.get(edgecase).replaceAll("\\*", type);
                materialEdgecases.put(material, Material.valueOf(replacement));
                materialEdgecases.put(Material.valueOf(replacement), material);
            }
        }
        return materialEdgecases;
    }

}
