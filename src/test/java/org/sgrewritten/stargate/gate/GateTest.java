package org.sgrewritten.stargate.gate;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sgrewritten.stargate.FakeStargateLogger;
import org.sgrewritten.stargate.exception.GateConflictException;
import org.sgrewritten.stargate.exception.InvalidStructureException;
import org.sgrewritten.stargate.network.StargateRegistry;
import org.sgrewritten.stargate.network.portal.PortalBlockGenerator;
import org.sgrewritten.stargate.network.portal.PortalData;
import org.sgrewritten.stargate.util.FakeStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;

class GateTest {

    private static Gate loadGate;
    private static ServerMock server;
    private static WorldMock world;
    private static Gate createdGate;
    private static Block signBlock;
    private static GateFormat format;
    private static PortalData portalData;
    private static final File testGatesDir = new File("src/test/resources/gates");

    @BeforeAll
    public static void setUp() throws InvalidStructureException, GateConflictException {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        portalData = new PortalData();
        portalData.topLeft = new Location(world,0,6,0);
        portalData.facing = BlockFace.SOUTH;
        portalData.gateFileName = "nether.gate";
        signBlock = PortalBlockGenerator.generatePortal(portalData.topLeft.clone().subtract(new Vector(0,4,0)));
        GateFormatHandler.setFormats(GateFormatHandler.loadGateFormats(testGatesDir, new FakeStargateLogger()));
        
        loadGate = new Gate(portalData,new StargateRegistry(new FakeStorage()));
        format = GateFormatHandler.getFormat(portalData.gateFileName);
        // Note that this is created in a different registry, so as to avoid any conflicts
        createdGate = new Gate(format,signBlock.getLocation(),portalData.facing,false,new StargateRegistry(new FakeStorage()));
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }
    
    @Test
    public void isValid_LoadedGate() throws GateConflictException {
        Assertions.assertTrue(loadGate.isValid(false));
    }
    
    @Test
    public void isValid_CreatedGate() throws GateConflictException {
        Assertions.assertTrue(createdGate.isValid(false));
    }
}
