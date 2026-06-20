package cc.oniacute.bakapotatopatcher.neoforge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoUpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;

public final class NeoForgeUpdateScreen extends Screen {
    private final Screen parent;
    private final String currentVersion;

    public NeoForgeUpdateScreen(Screen parent, String currentVersion) {
        super(Component.literal("BakaPotatoPatch 更新检查"));
        this.parent = parent;
        this.currentVersion = currentVersion;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("打开下载页"), button -> openDownload())
                .bounds(centerX - 166, this.height - 52, 108, 20).build());
        addRenderableWidget(Button.builder(Component.literal("重新检查"), button ->
                BakaPotatoPatcherNeoForgeClient.checkUpdates(false)
        ).bounds(centerX - 48, this.height - 52, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(centerX + 58, this.height - 52, 108, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        BakaPotatoUpdateChecker.Result result = BakaPotatoUpdateChecker.lastResult();
        int centerX = this.width / 2;
        int y = this.height / 2 - 78;
        graphics.centeredText(this.font, this.title, centerX, y, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("此页面用于确认当前加载器对应的 BakaPotatoPatchMod 是否需要更新."), centerX, y + 22, 0xFFCCCCCC);
        graphics.centeredText(this.font, Component.literal("当版本不是最新,或 API 返回 reminds=1 时会显示提醒."), centerX, y + 38, 0xFFCCCCCC);
        graphics.centeredText(this.font, Component.literal("下载地址: " + BakaPotatoUpdateChecker.DOWNLOAD_URL), centerX, y + 54, 0xFF888888);
        graphics.centeredText(this.font, Component.literal("Loader: neoforge / 当前版本: " + currentVersion), centerX, y + 76, 0xFFCCCCCC);
        graphics.centeredText(this.font, statusLine(result), centerX, y + 94, statusColor(result));
        if (result.status() == BakaPotatoUpdateChecker.Status.READY) {
            graphics.centeredText(this.font, Component.literal("最新版本: " + result.latestVersion() + " / reminds=" + result.reminds()), centerX, y + 112, 0xFFCCCCCC);
            if (result.shouldRemind()) {
                graphics.centeredText(this.font, reminderLine(result), centerX, y + 130, 0xFFFFFF55);
            }
        }
    }

    private Component statusLine(BakaPotatoUpdateChecker.Result result) {
        return switch (result.status()) {
            case IDLE -> Component.literal("尚未检查更新");
            case DISABLED -> Component.literal("更新检查已关闭");
            case CHECKING -> Component.literal("正在检查更新...");
            case FAILED -> Component.literal("检查失败: " + result.error());
            case READY -> {
                if (!result.latest()) {
                    yield Component.literal("发现新版本");
                }
                if (result.reminds() == 1) {
                    yield Component.literal("当前版本为最新,但服务器发布了提醒");
                }
                yield Component.literal("当前已经是最新版本");
            }
        };
    }

    private Component reminderLine(BakaPotatoUpdateChecker.Result result) {
        return result.latest()
                ? Component.literal("请查看下载页或公告确认是否需要处理.")
                : Component.literal("建议前往下载页获取最新版本.");
    }

    private int statusColor(BakaPotatoUpdateChecker.Result result) {
        return switch (result.status()) {
            case FAILED -> 0xFFFF5555;
            case READY -> result.shouldRemind() ? 0xFFFFFF55 : 0xFF55FF55;
            default -> 0xFFCCCCCC;
        };
    }

    private void openDownload() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(BakaPotatoUpdateChecker.DOWNLOAD_URL));
                return;
            }
        } catch (Exception ignored) {
            // Fall back to clipboard below.
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(BakaPotatoUpdateChecker.DOWNLOAD_URL);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
