package com.tonywww.slashblade_sendims.startup;

/** Only known optional Sounds configuration previews; never quest/HUD or arbitrary mod images. */
public final class ConfigImagePolicy {
    private ConfigImagePolicy() {}
    public static boolean defer(String namespace, String path) {
        return "sounds".equals(namespace) && path.startsWith("textures/gui/")
                && (path.endsWith(".webp") || path.endsWith(".gif"));
    }
}
