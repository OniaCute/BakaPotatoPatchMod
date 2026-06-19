package cc.oniacute.bakapotatopatcher.common;

import java.util.List;

public record ClientInfoResponse(
        String requestId,
        List<String> modList,
        String hardwareIdHash
) {
    public ClientInfoResponse {
        requestId = requestId == null ? "" : requestId;
        modList = List.copyOf(modList);
        hardwareIdHash = hardwareIdHash == null ? "" : hardwareIdHash;
    }
}
