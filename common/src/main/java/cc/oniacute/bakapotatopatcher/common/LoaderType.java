package cc.oniacute.bakapotatopatcher.common;

public enum LoaderType {
    FABRIC("fabric"),
    FORGE("forge"),
    NEOFORGE("neoforge");

    private final String id;

    LoaderType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
