package cc.oniacute.bakapotatopatcher.fabric.mixin;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoHudStyle;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void bakapotatopatcher$renderDebugHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
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
            graphics.drawString(minecraft.font, line, 4, y, textColor);
        }
    }
}
