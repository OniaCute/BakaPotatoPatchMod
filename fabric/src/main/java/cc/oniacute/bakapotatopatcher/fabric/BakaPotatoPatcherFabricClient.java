package cc.oniacute.bakapotatopatcher.fabric;

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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BakaPotatoPatcherFabricClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.MOD_ID, "main")
    );
    private static KeyMapping openConfigKey;
    private boolean patcherServer;

    @Override
    public void onInitializeClient() {
        BakaPotatoClientConfigManager.initialize(FabricLoader.getInstance().getConfigDir());
        BakaPotatoPatchApplicability.refreshRemoteServersAsync(BakaPotatoClientConfigManager.get());
        registerConfigKey();
        PayloadTypeRegistry.playS2C().register(FabricHandshakeQueryPayload.TYPE, FabricHandshakeQueryPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FabricHandshakeResponsePayload.TYPE, FabricHandshakeResponsePayload.STREAM_CODEC);
        registerWaypointReceivers();

        ClientPlayNetworking.registerGlobalReceiver(FabricHandshakeQueryPayload.TYPE, (payload, context) -> {
            try {
                String type = BakaPotatoProtocol.readPacketType(payload.data());
                if (BakaPotatoProtocol.TYPE_QUERY.equals(type)) {
                    BakaPotatoProtocol.decodeQuery(payload.data());
                    patcherServer = true;
                    byte[] response = ClientHandshakeFactory.responsePayload(
                            modVersion(),
                            LoaderType.FABRIC,
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

    private void registerConfigKey() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bakapotatopatcher.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateServerState(client);
            tickClientStats(client);
            while (openConfigKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new FabricConfigScreen(Minecraft.getInstance().screen));
            }
        });
    }

    private void tickClientStats(Minecraft client) {
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

    private void updateServerState(Minecraft client) {
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

    private void registerWaypointReceivers() {
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
                registerWaypointReceiver(id);
            }
        }
    }

    private void registerWaypointReceiver(Identifier id) {
        CustomPacketPayload.Type<FabricWaypointPayload> type = new CustomPacketPayload.Type<>(id);
        PayloadTypeRegistry.playS2C().register(type, FabricWaypointPayload.codec(type));
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            boolean dropped = BakaPotatoWaypointGuard.shouldDrop(payload == null ? null : payload.data(), BakaPotatoClientConfigManager.get());
            if (dropped && BakaPotatoClientConfigManager.get().waypoint.debugLogDroppedPackets) {
                System.out.println("[BakaPotatoPatcher] Dropped invalid waypoint payload from " + id);
            }
            if (dropped) {
                BakaPotatoDebugInfo.log("dropped custom waypoint payload: " + id);
            }
        });
    }

    private static void sendToServer(byte[] payload) {
        ClientPlayNetworking.send(new FabricHandshakeResponsePayload(payload));
        BakaPotatoClientTelemetry.markPacketSentToServer();
    }

    private String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(BakaPotatoProtocol.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private List<String> formattedMods(BakaPotatoClientConfig config) {
        return BakaPotatoModListFormatter.format(installedMods(), config);
    }

    private List<BakaPotatoModListFormatter.ModEntry> installedMods() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(container -> new BakaPotatoModListFormatter.ModEntry(
                        container.getMetadata().getId(),
                        container.getMetadata().getName(),
                        container.getMetadata().getVersion().getFriendlyString()
                ))
                .toList();
    }
}
