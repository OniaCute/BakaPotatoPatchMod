package cc.oniacute.bakapotatopatcher.fabric;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoUpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;

public final class FabricUpdateScreen extends Screen {
    private final Screen parent;
    private final String currentVersion;

    public FabricUpdateScreen(Screen parent, String currentVersion) {
        super(Component.literal("BakaPotatoPatch 更新检查"));
        this.parent = parent;
        this.currentVersion = currentVersion;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("前往下载"), button -> openDownload())
                .bounds(centerX - 154, this.height - 52, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("重新检查"), button ->
                BakaPotatoPatcherFabricClient.checkUpdates(false)
        ).bounds(centerX - 48, this.height - 52, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(centerX + 58, this.height - 52, 96, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BakaPotatoUpdateChecker.Result result = BakaPotatoUpdateChecker.lastResult();
        int centerX = this.width / 2;
        int y = this.height / 2 - 54;
        graphics.drawCenteredString(this.font, this.title, centerX, y, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("当前版本: " + currentVersion), centerX, y + 24, 0xCCCCCC);
        graphics.drawCenteredString(this.font, statusLine(result), centerX, y + 42, statusColor(result));
        if (result.status() == BakaPotatoUpdateChecker.Status.READY) {
            graphics.drawCenteredString(this.font, Component.literal("最新版本: " + result.latestVersion() + " / reminds=" + result.reminds()), centerX, y + 60, 0xCCCCCC);
            if (result.shouldRemind()) {
                graphics.drawCenteredString(this.font, Component.literal("建议前往下载最新版本。"), centerX, y + 78, 0xFFFF55);
            }
        }
    }

    private Component statusLine(BakaPotatoUpdateChecker.Result result) {
        return switch (result.status()) {
            case IDLE -> Component.literal("尚未检查更新");
            case CHECKING -> Component.literal("正在检查更新...");
            case FAILED -> Component.literal("检查失败: " + result.error());
            case READY -> result.latest()
                    ? Component.literal("当前已经是最新版本")
                    : Component.literal("发现新版本");
        };
    }

    private int statusColor(BakaPotatoUpdateChecker.Result result) {
        return switch (result.status()) {
            case FAILED -> 0xFF5555;
            case READY -> result.shouldRemind() ? 0xFFFF55 : 0x55FF55;
            default -> 0xCCCCCC;
        };
    }

    private void openDownload() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(BakaPotatoUpdateChecker.DOWNLOAD_URL));
            }
        } catch (Exception ignored) {
            // Ignore browser launch failures; the URL is visible through the update prompt context.
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
