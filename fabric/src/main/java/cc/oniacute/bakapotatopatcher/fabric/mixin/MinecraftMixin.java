package cc.oniacute.bakapotatopatcher.fabric.mixin;

import cc.oniacute.bakapotatopatcher.fabric.BakaPotatoPatcherFabricClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = {"tick", "runTick"}, at = @At("TAIL"), require = 0)
    private void bakapotatopatcher$tickClient(CallbackInfo ci) {
        BakaPotatoPatcherFabricClient.tickClient((Minecraft) (Object) this);
    }
}
