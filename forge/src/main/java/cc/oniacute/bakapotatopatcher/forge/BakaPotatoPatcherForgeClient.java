package cc.oniacute.bakapotatopatcher.forge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoProtocol;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientTelemetry;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoHardwareId;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoModListFormatter;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoPatchApplicability;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoUpdateChecker;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoWaypointGuard;
import cc.oniacute.bakapotatopatcher.common.ClientInfoRequest;
import cc.oniacute.bakapotatopatcher.common.ClientInfoResponse;
import cc.oniacute.bakapotatopatcher.common.ClientHandshakeFactory;
import cc.oniacute.bakapotatopatcher.common.ClientStatsPacket;
import cc.oniacute.bakapotatopatcher.common.LoaderType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod(BakaPotatoProtocol.MOD_ID)
public final class BakaPotatoPatcherForgeClient {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.MOD_ID, "main")
    );
    private static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath(
            BakaPotatoProtocol.CHANNEL_NAMESPACE,
            BakaPotatoProtocol.CHANNEL_PATH
    );
    private static final Channel<CustomPacketPayload> CHANNEL = ChannelBuilder.named(CHANNEL_ID)
            .networkProtocolVersion(BakaPotatoProtocol.PROTOCOL_VERSION)
            .optional()
            .payloadChannel()
            .play()
            .flow(PacketFlow.CLIENTBOUND)
            .add(ForgeHandshakeQueryPayload.TYPE, ForgeHandshakeQueryPayload.STREAM_CODEC, BakaPotatoPatcherForgeClient::handleQuery)
            .flow(PacketFlow.SERVERBOUND)
            .add(ForgeHandshakeResponsePayload.TYPE, ForgeHandshakeResponsePayload.STREAM_CODEC, (payload, context) -> {
            })
            .build();
    private static final List<Channel<CustomPacketPayload>> WAYPOINT_CHANNELS = new ArrayList<>();
    private static KeyMapping openConfigKey;
    private static boolean patcherServer;

    public BakaPotatoPatcherForgeClient() {
        BakaPotatoClientConfigManager.initialize(FMLPaths.CONFIGDIR.get());
        BakaPotatoPatchApplicability.refreshRemoteServersAsync(BakaPotatoClientConfigManager.get());
        checkUpdates(true);
        CHANNEL.getName();
        registerWaypointChannels();
        RegisterKeyMappingsEvent.BUS.addListener(BakaPotatoPatcherForgeClient::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(BakaPotatoPatcherForgeClient::afterClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        openConfigKey = new KeyMapping(
                "key.bakapotatopatcher.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        );
        event.register(openConfigKey);
    }

    private static void afterClientTick(TickEvent.ClientTickEvent.Post event) {
        updateServerState(Minecraft.getInstance());
        tickClientStats(Minecraft.getInstance());
        openStartupUpdateReminder(Minecraft.getInstance());
        if (openConfigKey == null) {
            return;
        }
        while (openConfigKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new ForgeConfigScreen(Minecraft.getInstance().screen));
        }
    }

    public static void checkUpdates(boolean startup) {
        if (!BakaPotatoClientConfigManager.get().update.checkUpdates) {
            BakaPotatoUpdateChecker.markDisabled(LoaderType.FORGE.id(), currentModVersion());
            return;
        }
        BakaPotatoUpdateChecker.checkAsync(LoaderType.FORGE.id(), currentModVersion(), startup);
    }

    private static void openStartupUpdateReminder(Minecraft client) {
        if (BakaPotatoUpdateChecker.consumeStartupReminder()) {
            client.setScreen(new ForgeUpdateScreen(client.screen, currentModVersion()));
        }
    }

    private static void tickClientStats(Minecraft client) {
        String serverAddress = BakaPotatoPatchApplicability.currentServer();
        if (!patcherServer) {
            return;
        }
        if (BakaPotatoClientTelemetry.shouldSendStats(client.level == null ? 0L : client.level.getGameTime(), serverAddress)) {
            ClientStatsPacket stats = BakaPotatoClientTelemetry.createStats(serverAddress);
            sendToServer(BakaPotatoProtocol.encodeClientStats(stats));
            BakaPotatoDebugInfo.log("sent client stats to server");
        }
    }

    private static void updateServerState(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server == null) {
            BakaPotatoPatchApplicability.setCurrentServerAddress("");
            patcherServer = false;
            return;
        }
        BakaPotatoPatchApplicability.setCurrentServerAddress(server.ip);
        BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
        if (config.resourcePacks.autoAcceptServerPacks && BakaPotatoPatchApplicability.isEnabledForCurrentServer(config)) {
            server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        }
    }

    private static void handleQuery(ForgeHandshakeQueryPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            try {
                String type = BakaPotatoProtocol.readPacketType(payload.data());
                if (BakaPotatoProtocol.TYPE_QUERY.equals(type)) {
                    BakaPotatoProtocol.decodeQuery(payload.data());
                    patcherServer = true;
                    byte[] response = ClientHandshakeFactory.responsePayload(
                            currentModVersion(),
                            LoaderType.FORGE,
                            SharedConstants.getCurrentVersion().name()
                    );
                    sendToServer(response);
                    BakaPotatoDebugInfo.log("sent handshake response to server");
                    return;
                }
                if (BakaPotatoProtocol.TYPE_CLIENT_INFO_QUERY.equals(type)) {
                    ClientInfoRequest request = BakaPotatoProtocol.decodeClientInfoQuery(payload.data());
                    BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
                    ClientInfoResponse response = new ClientInfoResponse(
                            request.requestId(),
                            request.includeModList() ? formattedMods(config) : List.of(),
                            request.includeHardwareId() ? BakaPotatoHardwareId.sha256() : ""
                    );
                    sendToServer(BakaPotatoProtocol.encodeClientInfoResponse(response));
                    BakaPotatoDebugInfo.log("sent requested client info to server");
                }
            } catch (IOException ignored) {
                // Ignore invalid server queries; this channel is used only for lightweight detection.
            }
        });
    }

    private static void registerWaypointChannels() {
        BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
        Set<String> channels = new LinkedHashSet<>();
        if (BakaPotatoWaypointGuard.isBuiltinProfileEnabled(config, "bakapotato")) {
            channels.add(BakaPotatoWaypointGuard.WAYPOINT_CHANNEL);
        }
        if (BakaPotatoWaypointGuard.isBuiltinProfileEnabled(config, "custom")) {
            channels.addAll(config.waypoint.customChannels);
        }
        for (String channel : channels) {
            Identifier id = Identifier.tryParse(channel);
            if (id != null) {
                WAYPOINT_CHANNELS.add(createWaypointChannel(id));
            }
        }
    }

    private static Channel<CustomPacketPayload> createWaypointChannel(Identifier id) {
        CustomPacketPayload.Type<ForgeWaypointPayload> type = new CustomPacketPayload.Type<>(id);
        return ChannelBuilder.named(id)
                .networkProtocolVersion(BakaPotatoProtocol.PROTOCOL_VERSION)
                .optional()
                .payloadChannel()
                .play()
                .flow(PacketFlow.CLIENTBOUND)
                .add(type, ForgeWaypointPayload.codec(type), BakaPotatoPatcherForgeClient::handleWaypoint)
                .build();
    }

    private static void handleWaypoint(ForgeWaypointPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            boolean dropped = BakaPotatoWaypointGuard.shouldDrop(payload == null ? null : payload.data(), BakaPotatoClientConfigManager.get());
            if (dropped && BakaPotatoClientConfigManager.get().waypoint.debugLogDroppedPackets) {
                System.out.println("[BakaPotatoPatcher] Dropped invalid waypoint payload from " + (payload == null ? "unknown" : payload.type().id()));
            }
            if (dropped) {
                BakaPotatoDebugInfo.log("dropped custom waypoint payload: " + (payload == null ? "unknown" : payload.type().id()));
            }
        });
    }

    private static void sendToServer(byte[] payload) {
        CHANNEL.send(new ForgeHandshakeResponsePayload(payload), PacketDistributor.SERVER.noArg());
        BakaPotatoClientTelemetry.markPacketSentToServer();
    }

    public static String currentModVersion() {
        return ModList.get()
                .getModContainerById(BakaPotatoProtocol.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private static List<String> formattedMods(BakaPotatoClientConfig config) {
        return BakaPotatoModListFormatter.format(installedMods(), config);
    }

    private static List<BakaPotatoModListFormatter.ModEntry> installedMods() {
        return ModList.get().getMods().stream()
                .map(info -> new BakaPotatoModListFormatter.ModEntry(
                        info.getModId(),
                        info.getDisplayName(),
                        info.getVersion().toString()
                ))
                .toList();
    }
}
