package org.sgrewritten.stargate.network.portal;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.HorseMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.entity.PoweredMinecartMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sgrewritten.stargate.api.gate.GateFormatRegistry;
import org.sgrewritten.stargate.api.network.Network;
import org.sgrewritten.stargate.api.network.portal.RealPortal;
import org.sgrewritten.stargate.economy.StargateEconomyManagerMock;
import org.sgrewritten.stargate.exception.InvalidStructureException;
import org.sgrewritten.stargate.exception.TranslatableException;
import org.sgrewritten.stargate.gate.GateFormatHandler;
import org.sgrewritten.stargate.network.NetworkType;
import org.sgrewritten.stargate.network.StargateNetwork;
import org.sgrewritten.stargate.network.StorageType;
import org.sgrewritten.stargate.thread.SynchronousPopulator;
import org.sgrewritten.stargate.util.LanguageManagerMock;
import org.sgrewritten.stargate.util.StargateTestHelper;

import java.io.File;
import java.util.Objects;

class TeleporterTest {

    private HorseMock horse;
    private static Teleporter teleporter;
    private static SynchronousPopulator populator;
    private static PoweredMinecartMock furnaceMinecart;
    private ServerMock server;
    private BukkitSchedulerMock scheduler;

    @BeforeEach
    public void setup() throws TranslatableException, InvalidStructureException {
        this.server = StargateTestHelper.setup();
        this.scheduler = server.getScheduler();
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        PortalFactory fakePortalGenerator = new PortalFactory("Portal", "iPortal");


        horse = (HorseMock) world.spawnEntity(new Location(world, 0, 0, 0), EntityType.HORSE);
        horse.addPassenger(player);
        Network network = new StargateNetwork("custom", NetworkType.CUSTOM, StorageType.LOCAL);
        RealPortal origin = fakePortalGenerator.generateFakePortal(world, network, "origin", false);
        RealPortal destination = fakePortalGenerator.generateFakePortal(world, network, "destination", false);
        populator = new SynchronousPopulator();
        teleporter = new Teleporter(destination, origin, destination.getGate().getFacing(),
                origin.getGate().getFacing(), 0, "empty", new LanguageManagerMock(), new StargateEconomyManagerMock());
        furnaceMinecart = (PoweredMinecartMock) world.spawnEntity(new Location(world, 0, 0, 0), EntityType.MINECART_FURNACE);

    }

    @AfterEach
    public void tearDown() {
        StargateTestHelper.tearDown();
    }

    @Test
    void teleport() {
        teleporter.teleport(horse);
        StargateTestHelper.runAllTasks();
        Assertions.assertTrue(horse.hasTeleported());
    }

    @Test
    void teleport_FurnaceMinecart() {
        teleporter.teleport(furnaceMinecart);
        StargateTestHelper.runAllTasks();
        Assertions.assertTrue(furnaceMinecart.hasTeleported());
    }
}
