package cc.oniacute.bakapotatopatcher.forge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ForgeConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 44;
    private static final int SECTION_HEIGHT = 26;
    private static final int SUBSECTION_HEIGHT = 20;
    private static final int CONTROL_WIDTH = 190;
    private static final int TOP = 46;
    private static final int BOTTOM_PADDING = 42;

    private final Screen parent;
    private BakaPotatoClientConfig draft;
    private int scrollOffset;
    private int contentHeight;
    private EditBox stringBox;
    private EditBox integerBox;
    private EditBox decimalBox;
    private EditBox customChannelsBox;
    private EditBox hudBackgroundColorBox;
    private EditBox hudBackgroundAlphaBox;
    private EditBox hudTextColorBox;
    private EditBox hudStaySecondsBox;
    private Button booleanButton;
    private Button singleChoiceButton;
    private Button applyModeButton;
    private Button autoAcceptPacksButton;
    private Button waypointEnabledButton;
    private Button dropInvalidButton;
    private Button extraBytesEnabledButton;
    private Button extraBytesModeButton;
    private Button debugLogButton;
    private Button hudDebugButton;
    private Button hudBackgroundButton;
    private Button sendModListButton;
    private Button modListModeButton;
    private Button hardwareHashButton;
    private Button saveButton;
    private final Map<String, Button> multiChoiceButtons = new LinkedHashMap<>();

    public ForgeConfigScreen(Screen parent) {
        super(Component.literal("BakaPotatoPatcher Config"));
        this.parent = parent;
        this.draft = BakaPotatoClientConfigManager.copy();
    }

    @Override
    protected void init() {
        resetWidgetReferences();
        int controlX = Math.min(this.width - CONTROL_WIDTH - 24, this.width / 2 + 78);
        int labelX = Math.max(24, this.width / 2 - 238);
        int y = TOP - scrollOffset;
        y = section(labelX, y, "服务器范围");
        y = subsection(labelX, y, "应用模式");
        addItemLabels(labelX, y, "修补应用范围 (patches.applyMode)", "所有服务器应用时，全部功能都会对所有服务器生效。");
        applyModeButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.patches.applyMode = isAllServersMode()
                    ? BakaPotatoClientConfig.PatchConfig.APPLY_MODE_BAKAPOTATO_SERVERS
                    : BakaPotatoClientConfig.PatchConfig.APPLY_MODE_ALL_SERVERS;
            draft.normalize();
            refreshButtons();
        });
        y += ROW_HEIGHT;
        y = section(labelX, y, "资源包修复");
        y = subsection(labelX, y, "服务器材质包");
        addItemLabels(labelX, y, "自动同意服务器材质包 (resourcePacks.autoAcceptServerPacks)", "进入适用服务器时自动同意服务器材质包提示。");
        autoAcceptPacksButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.resourcePacks.autoAcceptServerPacks = !draft.resourcePacks.autoAcceptServerPacks;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        y = section(labelX, y, "网络修复");
        y = subsection(labelX, y, "网络协议错误修正");
        y = subsection(labelX + 16, y, "Waypoint检查");
        addItemLabels(labelX, y, "Waypoint检查 (waypoint.enabled)", "修正接收异常 Waypoint 数据包导致的网络协议错误。");
        waypointEnabledButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.waypoint.enabled = !draft.waypoint.enabled;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "丢弃异常Waypoint包 (waypoint.dropInvalidPackets)", "空包、全零包和无法识别的数据包会被直接丢弃。");
        dropInvalidButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.waypoint.dropInvalidPackets = !draft.waypoint.dropInvalidPackets;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        y = subsection(labelX + 16, y, "过多字节包修复");
        addItemLabels(labelX, y, "过多字节包修复 (extraBytesPatch.enabled)", "修复数据包解码后仍有多余字节导致的断链。");
        extraBytesEnabledButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.extraBytesPatch.enabled = !draft.extraBytesPatch.enabled;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "过多字节处理模式 (extraBytesPatch.mode)", "强制解析会跳过多余字节；忽略会丢弃该包；断开连接为原版行为。");
        extraBytesModeButton = addControlButton(controlX, y, Component.empty(), button -> {
            cycleExtraBytesMode();
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "自定义Waypoint通道 (waypoint.customChannels)", "逗号分隔的 namespace:path 通道列表，仅启动时注册。");
        customChannelsBox = addTextBox(controlX, y, String.join(",", draft.waypoint.customChannels));
        customChannelsBox.setSuggestion("namespace:path,...");
        y += ROW_HEIGHT;
        y = section(labelX, y, "调试");
        y = subsection(labelX, y, "网络协议错误修正");
        y = subsection(labelX + 16, y, "Waypoint检查调试");
        addItemLabels(labelX, y, "丢包调试日志 (waypoint.debugLogDroppedPackets)", "输出被丢弃的 Waypoint 数据包来源，便于排查。");
        debugLogButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.waypoint.debugLogDroppedPackets = !draft.waypoint.debugLogDroppedPackets;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "游戏内HUD调试显示 (debug.showHud)", "在左上角绘制原版文字，显示当前补丁和服务器匹配状态。");
        hudDebugButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.debug.showHud = !draft.debug.showHud;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "HUD背景 (debug.hudBackgroundEnabled)", "控制调试日志文字后方是否绘制半透明背景。");
        hudBackgroundButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.debug.hudBackgroundEnabled = !draft.debug.hudBackgroundEnabled;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "HUD背景颜色 (debug.hudBackgroundColor)", "使用 #RRGGBB 格式，例如 #000000。");
        hudBackgroundColorBox = addTextBox(controlX, y, draft.debug.hudBackgroundColor);
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "HUD背景透明度 (debug.hudBackgroundAlpha)", "0-255，0 为完全透明，255 为不透明。");
        hudBackgroundAlphaBox = addTextBox(controlX, y, Integer.toString(draft.debug.hudBackgroundAlpha));
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "HUD文字颜色 (debug.hudTextColor)", "使用 #RRGGBB 格式，例如 #FFFFFF。");
        hudTextColorBox = addTextBox(controlX, y, draft.debug.hudTextColor);
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "HUD日志停留时间 (debug.hudStaySeconds)", "每条调试日志在左上角停留的秒数。");
        hudStaySecondsBox = addTextBox(controlX, y, Integer.toString(draft.debug.hudStaySeconds));
        y += ROW_HEIGHT;
        y = section(labelX, y, "隐私与上报");
        y = subsection(labelX, y, "服务器检测");
        addItemLabels(labelX, y, "向服务器发送Mod列表 (privacy.sendModList)", "开启后，仅在服务器管理员执行检查命令时响应 Mod 列表。");
        sendModListButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.privacy.sendModList = !draft.privacy.sendModList;
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "Mod列表发送模式 (privacy.modListMode)", "发送全部包含 id 和版本；仅发送必要会过滤加载器基础 Mod 且只发送名称。");
        modListModeButton = addControlButton(controlX, y, Component.empty(), button -> {
            draft.privacy.modListMode = "all".equals(draft.privacy.modListMode) ? "necessary" : "all";
            refreshButtons();
        });
        y += ROW_HEIGHT;
        addItemLabels(labelX, y, "向服务器发送硬件ID哈希 (privacy.hardwareIdHash)", "不可设置，仅在服务器管理员执行检查命令时发送 SHA-256 哈希。");
        hardwareHashButton = addControlButton(controlX, y, Component.literal("硬件ID哈希: 按需发送"), button -> {});
        hardwareHashButton.active = false;
        y += ROW_HEIGHT;
        contentHeight = y + scrollOffset - TOP + BOTTOM_PADDING;
        clampScroll();
        saveButton = addRenderableWidget(Button.builder(Component.literal("完成"), button -> saveAndClose())
                .bounds(this.width / 2 - 154, this.height - 28, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("重置"), button -> {
            draft = BakaPotatoClientConfig.defaults();
            scrollOffset = 0;
            rebuildWidgets();
        }).bounds(this.width / 2 - 48, this.height - 28, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), button -> onClose())
                .bounds(this.width / 2 + 58, this.height - 28, 96, 20).build());
        refreshButtons();
        updateSaveState();
    }

    private int section(int labelX, int y, String title) {
        addTextLabel(labelX, y + 4, Component.literal(title).withStyle(ChatFormatting.YELLOW), 220, 14);
        return y + SECTION_HEIGHT;
    }

    private int subsection(int labelX, int y, String title) {
        addTextLabel(labelX + 8, y + 2, Component.literal("- " + title).withStyle(ChatFormatting.AQUA), 220, 12);
        return y + SUBSECTION_HEIGHT;
    }

    private void addItemLabels(int labelX, int y, String title, String description) {
        addTextLabel(labelX + 16, y, Component.literal(title), 520, 12);
        addTextLabel(labelX + 16, y + 11, Component.literal(description).withStyle(ChatFormatting.GRAY), 520, 12);
    }

    private void addTextLabel(int x, int y, Component text, int width, int height) {
        StringWidget label = addRenderableOnly(new StringWidget(x, y, width, height, text, this.font));
        label.visible = isInContentViewport(y);
    }

    private Button addControlButton(int x, int y, Component message, Button.OnPress onPress) {
        Button button = addRenderableWidget(Button.builder(message, onPress).bounds(x, y, CONTROL_WIDTH, 20).build());
        button.visible = isInContentViewport(y);
        return button;
    }

    private EditBox addTextBox(int x, int y, String value) {
        EditBox box = addRenderableWidget(new EditBox(this.font, x, y, CONTROL_WIDTH, 20, Component.empty()));
        box.setMaxLength(512);
        box.setValue(value);
        box.visible = isInContentViewport(y);
        return box;
    }

    private void saveAndClose() {
        if (!applyInputs()) {
            updateSaveState();
            return;
        }
        BakaPotatoClientConfigManager.replaceAndSave(draft);
        Minecraft.getInstance().setScreen(parent);
    }

    private boolean applyInputs() {
        try {
            if (stringBox != null) draft.ui.demoString = stringBox.getValue();
            if (integerBox != null) draft.ui.demoInteger = Integer.parseInt(integerBox.getValue().trim());
            if (decimalBox != null) draft.ui.demoDecimal = Double.parseDouble(decimalBox.getValue().trim());
            if (customChannelsBox != null) {
                draft.waypoint.customChannels.clear();
                for (String channel : customChannelsBox.getValue().split(",")) {
                    String trimmed = channel.trim();
                    if (!trimmed.isEmpty()) draft.waypoint.customChannels.add(trimmed);
                }
            }
            if (hudBackgroundColorBox != null) draft.debug.hudBackgroundColor = hudBackgroundColorBox.getValue();
            if (hudBackgroundAlphaBox != null) draft.debug.hudBackgroundAlpha = Integer.parseInt(hudBackgroundAlphaBox.getValue().trim());
            if (hudTextColorBox != null) draft.debug.hudTextColor = hudTextColorBox.getValue();
            if (hudStaySecondsBox != null) draft.debug.hudStaySeconds = Integer.parseInt(hudStaySecondsBox.getValue().trim());
            draft.normalize();
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void updateSaveState() {
        boolean valid = (integerBox == null || isInteger(integerBox.getValue()))
                && (decimalBox == null || isDecimal(decimalBox.getValue()))
                && (hudBackgroundAlphaBox == null || isInteger(hudBackgroundAlphaBox.getValue()))
                && (hudStaySecondsBox == null || isInteger(hudStaySecondsBox.getValue()));
        if (integerBox != null) integerBox.setTextColor(isInteger(integerBox.getValue()) ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
        if (decimalBox != null) decimalBox.setTextColor(isDecimal(decimalBox.getValue()) ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
        if (hudBackgroundAlphaBox != null) hudBackgroundAlphaBox.setTextColor(isInteger(hudBackgroundAlphaBox.getValue()) ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
        if (hudStaySecondsBox != null) hudStaySecondsBox.setTextColor(isInteger(hudStaySecondsBox.getValue()) ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
        if (saveButton != null) saveButton.active = valid;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isDecimal(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void refreshButtons() {
        if (applyModeButton != null) applyModeButton.setMessage(Component.literal(isAllServersMode() ? "所有服务器应用" : "笨蛋土豆服务器应用"));
        if (autoAcceptPacksButton != null) autoAcceptPacksButton.setMessage(toggle("自动同意", draft.resourcePacks.autoAcceptServerPacks));
        if (waypointEnabledButton != null) waypointEnabledButton.setMessage(toggle("Waypoint检查", draft.waypoint.enabled));
        if (dropInvalidButton != null) dropInvalidButton.setMessage(toggle("丢弃异常包", draft.waypoint.dropInvalidPackets));
        if (extraBytesEnabledButton != null) extraBytesEnabledButton.setMessage(toggle("过多字节修复", draft.extraBytesPatch.enabled));
        if (extraBytesModeButton != null) extraBytesModeButton.setMessage(Component.literal("模式: " + extraBytesModeLabel(draft.extraBytesPatch.mode)));
        if (debugLogButton != null) debugLogButton.setMessage(toggle("调试日志", draft.waypoint.debugLogDroppedPackets));
        if (hudDebugButton != null) hudDebugButton.setMessage(toggle("HUD调试", draft.debug.showHud));
        if (hudBackgroundButton != null) hudBackgroundButton.setMessage(toggle("HUD背景", draft.debug.hudBackgroundEnabled));
        if (sendModListButton != null) sendModListButton.setMessage(toggle("发送Mod列表", draft.privacy.sendModList));
        if (modListModeButton != null) modListModeButton.setMessage(Component.literal("Mod列表: " + ("all".equals(draft.privacy.modListMode) ? "发送全部" : "仅发送必要")));
        if (booleanButton != null) booleanButton.setMessage(toggle("开关", draft.ui.demoBoolean));
        if (singleChoiceButton != null) singleChoiceButton.setMessage(Component.literal(draft.ui.demoSingleChoice));
        multiChoiceButtons.forEach((choice, button) -> button.setMessage(toggle(choice, draft.ui.demoMultiChoice.contains(choice))));
    }

    private boolean isAllServersMode() {
        return BakaPotatoClientConfig.PatchConfig.APPLY_MODE_ALL_SERVERS.equals(draft.patches.applyMode);
    }

    private void cycleExtraBytesMode() {
        int index = BakaPotatoClientConfig.ExtraBytesPatchConfig.MODES.indexOf(draft.extraBytesPatch.mode);
        draft.extraBytesPatch.mode = BakaPotatoClientConfig.ExtraBytesPatchConfig.MODES.get((index + 1)
                % BakaPotatoClientConfig.ExtraBytesPatchConfig.MODES.size());
    }

    private String extraBytesModeLabel(String mode) {
        return switch (mode) {
            case BakaPotatoClientConfig.ExtraBytesPatchConfig.MODE_FORCE_PARSE -> "强制解析";
            case BakaPotatoClientConfig.ExtraBytesPatchConfig.MODE_DISCONNECT -> "断开连接";
            default -> "忽略";
        };
    }

    private Component toggle(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "开启" : "关闭"));
    }

    private void resetWidgetReferences() {
        stringBox = null;
        integerBox = null;
        decimalBox = null;
        customChannelsBox = null;
        hudBackgroundColorBox = null;
        hudBackgroundAlphaBox = null;
        hudTextColorBox = null;
        hudStaySecondsBox = null;
        booleanButton = null;
        singleChoiceButton = null;
        applyModeButton = null;
        autoAcceptPacksButton = null;
        waypointEnabledButton = null;
        dropInvalidButton = null;
        extraBytesEnabledButton = null;
        extraBytesModeButton = null;
        debugLogButton = null;
        hudDebugButton = null;
        hudBackgroundButton = null;
        sendModListButton = null;
        modListModeButton = null;
        hardwareHashButton = null;
        multiChoiceButtons.clear();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!applyInputs()) return true;
        scrollOffset -= (int) Math.round(verticalAmount * 24.0D);
        clampScroll();
        rebuildWidgets();
        return true;
    }

    private void clampScroll() {
        int viewportHeight = Math.max(1, this.height - TOP - BOTTOM_PADDING);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private boolean isInContentViewport(int y) {
        return y + 20 >= TOP && y <= this.height - BOTTOM_PADDING;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        if (contentHeight > this.height - TOP - BOTTOM_PADDING) {
            graphics.drawCenteredString(this.font, Component.literal("滚轮滚动查看更多配置"), this.width / 2, this.height - 40, 0x888888);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
