package cc.oniacute.bakapotatopatcher.neoforge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoProtocol;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientTelemetry;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoHardwareId;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoModListFormatter;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoPatchApplicability;
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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod(value = BakaPotatoProtocol.MOD_ID, dist = Dist.CLIENT)
public final class BakaPotatoPatcherNeoForgeClient {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.MOD_ID, "main")
    );
    private static KeyMapping openConfigKey;
    private static boolean patcherServer;

    public BakaPotatoPatcherNeoForgeClient(IEventBus modBus) {
        BakaPotatoClientConfigManager.initialize(FMLPaths.CONFIGDIR.get());
        BakaPotatoPatchApplicability.refreshRemoteServersAsync(BakaPotatoClientConfigManager.get());
        modBus.addListener(ClientNetwork::registerPayloads);
        modBus.addListener(BakaPotatoPatcherNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(BakaPotatoPatcherNeoForgeClient::afterClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        openConfigKey = new KeyMapping(
                "key.bakapotatopatcher.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        );
        event.registerCategory(KEY_CATEGORY);
        event.register(openConfigKey);
    }

    private static void afterClientTick(ClientTickEvent.Post event) {
        updateServerState(Minecraft.getInstance());
        tickClientStats(Minecraft.getInstance());
        if (openConfigKey == null) {
            return;
        }
        while (openConfigKey.consumeClick()) {
            Minecraft.getInstance().setScreenAndShow(new NeoForgeConfigScreen(null));
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

    public static final class ClientNetwork {
        private ClientNetwork() {
        }

        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToClient(NeoForgeHandshakeQueryPayload.TYPE, NeoForgeHandshakeQueryPayload.STREAM_CODEC, ClientNetwork::handleQuery);
            registrar.playToServer(NeoForgeHandshakeResponsePayload.TYPE, NeoForgeHandshakeResponsePayload.STREAM_CODEC, (payload, context) -> {
            });
            registerWaypointPayloads(registrar);
        }

        private static void handleQuery(NeoForgeHandshakeQueryPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                try {
                    String type = BakaPotatoProtocol.readPacketType(payload.data());
                    if (BakaPotatoProtocol.TYPE_QUERY.equals(type)) {
                        BakaPotatoProtocol.decodeQuery(payload.data());
                        patcherServer = true;
                        byte[] response = ClientHandshakeFactory.responsePayload(
                                modVersion(),
                                LoaderType.NEOFORGE,
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

        private static void registerWaypointPayloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
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
                    CustomPacketPayload.Type<NeoForgeWaypointPayload> type = new CustomPacketPayload.Type<>(id);
                    registrar.playToClient(type, NeoForgeWaypointPayload.codec(type), ClientNetwork::handleWaypoint);
                }
            }
        }

        private static void handleWaypoint(NeoForgeWaypointPayload payload, IPayloadContext context) {
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

        private static String modVersion() {
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

    private static void sendToServer(byte[] payload) {
        ClientPacketDistributor.sendToServer(new NeoForgeHandshakeResponsePayload(payload));
        BakaPotatoClientTelemetry.markPacketSentToServer();
    }
}
