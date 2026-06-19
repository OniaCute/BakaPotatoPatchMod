package cc.oniacute.bakapotatopatcher.common;

public record ClientInfoRequest(
        String requestId,
        boolean includeModList,
        boolean includeHardwareId
) {
    public ClientInfoRequest {
        requestId = requestId == null ? "" : requestId;
    }
}
