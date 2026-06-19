package cc.oniacute.bakapotatopatcher.neoforge.mixin;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoHudStyle;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void bakapotatopatcher$renderDebugHud(DeltaTracker deltaTracker, boolean renderHud, boolean renderScreens, CallbackInfo ci, Object profiler, int mouseX, int mouseY, GuiGraphicsExtractor graphics) {
        if (!renderHud || graphics == null) {
            return;
        }
        BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
        List<String> lines = BakaPotatoDebugInfo.hudLines(config);
        Minecraft minecraft = Minecraft.getInstance();
        int textColor = BakaPotatoHudStyle.textColor(config);
        int backgroundColor = BakaPotatoHudStyle.backgroundColor(config);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int y = 4 + index * 10;
            if (backgroundColor != 0) {
                graphics.fill(2, y - 1, 8 + minecraft.font.width(line), y + 10, backgroundColor);
            }
            graphics.text(minecraft.font, line, 4, y, textColor);
        }
    }
}
