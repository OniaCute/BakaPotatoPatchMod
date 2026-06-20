package cc.oniacute.bakapotatopatcher.fabric;

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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BakaPotatoPatcherFabricClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.MOD_ID, "main")
    );
    private static BakaPotatoPatcherFabricClient instance;
    private static KeyMapping openConfigKey;
    private boolean patcherServer;

    @Override
    public void onInitializeClient() {
        instance = this;
        BakaPotatoClientConfigManager.initialize(FabricLoader.getInstance().getConfigDir());
        BakaPotatoPatchApplicability.refreshRemoteServersAsync(BakaPotatoClientConfigManager.get());
        checkUpdates(true);
        registerConfigKey();
        registerClientboundPayload(FabricHandshakeQueryPayload.TYPE, FabricHandshakeQueryPayload.STREAM_CODEC);
        registerServerboundPayload(FabricHandshakeResponsePayload.TYPE, FabricHandshakeResponsePayload.STREAM_CODEC);
        registerWaypointReceivers();

        ClientPlayNetworking.registerGlobalReceiver(FabricHandshakeQueryPayload.TYPE, (payload, context) -> {
            try {
                refreshCurrentServerFromClient();
                String type = BakaPotatoProtocol.readPacketType(payload.data());
                if (BakaPotatoProtocol.TYPE_QUERY.equals(type)) {
                    BakaPotatoProtocol.decodeQuery(payload.data());
                    patcherServer = true;
                    byte[] response = ClientHandshakeFactory.responsePayload(
                            currentModVersion(),
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
        try {
            openConfigKey = registerKeyMapping(new KeyMapping(
                    "key.bakapotatopatcher.open_config",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    KEY_CATEGORY
            ));
        } catch (LinkageError | RuntimeException error) {
            openConfigKey = null;
            BakaPotatoDebugInfo.log("config key skipped: " + error.getClass().getSimpleName());
            System.out.println("[BakaPotatoPatcher] Fabric key binding API is unavailable; config key is disabled.");
        }
    }

    private static KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        for (String className : List.of(
                "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
                "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper"
        )) {
            for (String methodName : List.of("registerKeyMapping", "registerKeyBinding")) {
                try {
                    Class<?> helper = Class.forName(className);
                    Method method = helper.getMethod(methodName, KeyMapping.class);
                    return (KeyMapping) method.invoke(null, keyMapping);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    // Try the next Fabric API package and method name.
                }
            }
        }
        throw new IllegalStateException("No compatible Fabric key mapping helper found");
    }

    private static void refreshCurrentServerFromClient() {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client == null ? null : client.getCurrentServer();
        BakaPotatoPatchApplicability.setCurrentServerAddress(server == null ? "" : server.ip);
    }

    public static void checkUpdates(boolean startup) {
        if (!BakaPotatoClientConfigManager.get().update.checkUpdates) {
            BakaPotatoUpdateChecker.markDisabled(LoaderType.FABRIC.id(), currentModVersion());
            return;
        }
        BakaPotatoUpdateChecker.checkAsync(LoaderType.FABRIC.id(), currentModVersion(), startup);
    }

    public static void tickClient(Minecraft client) {
        BakaPotatoPatcherFabricClient current = instance;
        if (current == null || client == null) {
            return;
        }
        current.updateServerState(client);
        current.tickClientStats(client);
        current.openStartupUpdateReminder(client);
        while (openConfigKey != null && openConfigKey.consumeClick()) {
            client.setScreen(new FabricConfigScreen(client.screen));
        }
    }

    private void openStartupUpdateReminder(Minecraft client) {
        if (BakaPotatoUpdateChecker.consumeStartupReminder()) {
            client.setScreen(new FabricUpdateScreen(client.screen, currentModVersion()));
        }
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
        registerClientboundPayload(type, FabricWaypointPayload.codec(type));
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends CustomPacketPayload> void registerClientboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<?, T> codec
    ) {
        PayloadTypeRegistry registry = payloadRegistry("clientboundPlay", "playS2C");
        registry.register(type, (StreamCodec) codec);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends CustomPacketPayload> void registerServerboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<?, T> codec
    ) {
        PayloadTypeRegistry registry = payloadRegistry("serverboundPlay", "playC2S");
        registry.register(type, (StreamCodec) codec);
    }

    private static PayloadTypeRegistry<?> payloadRegistry(String preferredMethod, String legacyMethod) {
        try {
            Method method = PayloadTypeRegistry.class.getMethod(preferredMethod);
            return (PayloadTypeRegistry<?>) method.invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            try {
                Method method = PayloadTypeRegistry.class.getMethod(legacyMethod);
                return (PayloadTypeRegistry<?>) method.invoke(null);
            } catch (ReflectiveOperationException | LinkageError error) {
                throw new IllegalStateException("No compatible Fabric payload registry method found", error);
            }
        }
    }

    private static void sendToServer(byte[] payload) {
        ClientPlayNetworking.send(new FabricHandshakeResponsePayload(payload));
        BakaPotatoClientTelemetry.markPacketSentToServer();
    }

    public static String currentModVersion() {
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
