package munchyutils.mixins;

import munchyutils.client.MunchyConfig;
import munchyutils.client.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void munchyutils$hideNearbyGroundItems(
        ItemEntityRenderState state,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        MunchyConfig config = MunchyConfig.get();
        if (!Utils.isPlayerMining(client)) return;
        double dist = client.player.getPos().distanceTo(new net.minecraft.util.math.Vec3d(state.x, state.y, state.z));
        if (dist > 1.5) return;
        if (config.isHideNearbyGroundItemsWhenMining()) {
            ci.cancel();
        }
    }
} 