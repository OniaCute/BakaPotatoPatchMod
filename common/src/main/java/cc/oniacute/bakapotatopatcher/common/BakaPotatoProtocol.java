package cc.oniacute.bakapotatopatcher.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BakaPotatoProtocol {
    public static final String MOD_ID = "bakapotatopatcher";
    public static final String CHANNEL_NAMESPACE = MOD_ID;
    public static final String CHANNEL_PATH = "handshake";
    public static final String CHANNEL = CHANNEL_NAMESPACE + ":" + CHANNEL_PATH;
    public static final int PROTOCOL_VERSION = 1;

    public static final String TYPE_QUERY = "query";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_CLIENT_INFO_QUERY = "client_info_query";
    public static final String TYPE_CLIENT_INFO_RESPONSE = "client_info_response";
    public static final String TYPE_CLIENT_STATS = "client_stats";

    private static final int MAX_STRING_BYTES = 32767;

    private BakaPotatoProtocol() {
    }

    public static byte[] encodeQuery(String serverNonce) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            writeString(output, TYPE_QUERY);
            output.writeInt(PROTOCOL_VERSION);
            writeString(output, serverNonce);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode BakaPotatoPatch query.", exception);
        }
    }

    public static HandshakeQuery decodeQuery(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String type = readString(input);
        if (!TYPE_QUERY.equals(type)) {
            throw new IOException("Unexpected handshake packet type: " + type);
        }
        int protocolVersion = input.readInt();
        String serverNonce = readString(input);
        return new HandshakeQuery(protocolVersion, serverNonce);
    }

    public static String readPacketType(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        return readString(input);
    }

    public static byte[] encodeResponse(HandshakeResponse response) {
        Objects.requireNonNull(response, "response");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            writeString(output, TYPE_RESPONSE);
            output.writeInt(response.protocolVersion());
            writeString(output, response.modVersion());
            writeString(output, response.loader());
            writeString(output, response.minecraftVersion());
            output.writeInt(response.capabilities().size());
            for (String capability : response.capabilities()) {
                writeString(output, capability);
            }
            writeString(output, response.serverAddress());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode BakaPotatoPatch response.", exception);
        }
    }

    public static HandshakeResponse decodeResponse(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String type = readString(input);
        if (!TYPE_RESPONSE.equals(type)) {
            throw new IOException("Unexpected handshake packet type: " + type);
        }
        int protocolVersion = input.readInt();
        String modVersion = readString(input);
        String loader = readString(input);
        String minecraftVersion = readString(input);
        int capabilityCount = input.readInt();
        if (capabilityCount < 0 || capabilityCount > 256) {
            throw new IOException("Invalid capability count: " + capabilityCount);
        }
        List<String> capabilities = new ArrayList<>(capabilityCount);
        for (int index = 0; index < capabilityCount; index++) {
            capabilities.add(readString(input));
        }
        String serverAddress = "";
        if (input.available() > 0) {
            serverAddress = readString(input);
        }
        return new HandshakeResponse(
                protocolVersion,
                modVersion,
                loader,
                minecraftVersion,
                List.copyOf(capabilities),
                serverAddress,
                List.of(),
                ""
        );
    }

    public static byte[] encodeClientInfoQuery(ClientInfoRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            writeString(output, TYPE_CLIENT_INFO_QUERY);
            output.writeInt(PROTOCOL_VERSION);
            writeString(output, request.requestId());
            output.writeBoolean(request.includeModList());
            output.writeBoolean(request.includeHardwareId());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode BakaPotatoPatch client info query.", exception);
        }
    }

    public static ClientInfoRequest decodeClientInfoQuery(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String type = readString(input);
        if (!TYPE_CLIENT_INFO_QUERY.equals(type)) {
            throw new IOException("Unexpected client info query packet type: " + type);
        }
        int protocolVersion = input.readInt();
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IOException("Unsupported client info query protocol: " + protocolVersion);
        }
        return new ClientInfoRequest(readString(input), input.readBoolean(), input.readBoolean());
    }

    public static byte[] encodeClientInfoResponse(ClientInfoResponse response) {
        Objects.requireNonNull(response, "response");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            writeString(output, TYPE_CLIENT_INFO_RESPONSE);
            output.writeInt(PROTOCOL_VERSION);
            writeString(output, response.requestId());
            output.writeInt(response.modList().size());
            for (String mod : response.modList()) {
                writeString(output, mod);
            }
            writeString(output, response.hardwareIdHash());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode BakaPotatoPatch client info response.", exception);
        }
    }

    public static ClientInfoResponse decodeClientInfoResponse(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String type = readString(input);
        if (!TYPE_CLIENT_INFO_RESPONSE.equals(type)) {
            throw new IOException("Unexpected client info response packet type: " + type);
        }
        int protocolVersion = input.readInt();
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IOException("Unsupported client info response protocol: " + protocolVersion);
        }
        String requestId = readString(input);
        int modCount = input.readInt();
        if (modCount < 0 || modCount > 4096) {
            throw new IOException("Invalid mod list count: " + modCount);
        }
        List<String> modList = new ArrayList<>(modCount);
        for (int index = 0; index < modCount; index++) {
            modList.add(readString(input));
        }
        return new ClientInfoResponse(requestId, List.copyOf(modList), readString(input));
    }

    public static byte[] encodeClientStats(ClientStatsPacket stats) {
        Objects.requireNonNull(stats, "stats");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            writeString(output, TYPE_CLIENT_STATS);
            output.writeInt(PROTOCOL_VERSION);
            writeString(output, stats.serverAddress());
            output.writeLong(stats.packetsSentToServer());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode BakaPotatoPatch client stats.", exception);
        }
    }

    public static ClientStatsPacket decodeClientStats(byte[] payload) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String type = readString(input);
        if (!TYPE_CLIENT_STATS.equals(type)) {
            throw new IOException("Unexpected client stats packet type: " + type);
        }
        int protocolVersion = input.readInt();
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IOException("Unsupported client stats protocol: " + protocolVersion);
        }
        return new ClientStatsPacket(readString(input), input.readLong());
    }

    public static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("String is too large for the handshake payload.");
        }
        output.writeShort(encoded.length);
        output.write(encoded);
    }

    public static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        byte[] encoded = input.readNBytes(length);
        if (encoded.length != length) {
            throw new IOException("Unexpected end of handshake payload.");
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
