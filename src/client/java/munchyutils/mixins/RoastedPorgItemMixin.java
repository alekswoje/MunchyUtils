package munchyutils.mixins;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import munchyutils.client.MunchyConfig;
import munchyutils.client.InfoHudOverlay;
import munchyutils.client.Utils;

@Mixin(Item.class)
public class RoastedPorgItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void munchyutils$preventPorgUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        String name = Utils.stripColorCodes(stack.getName().getString()).toLowerCase();
        if (name.contains("roasted porg")) {
            MunchyConfig config = MunchyConfig.get();
            if (config.isPreventPorgUseIfActive() && InfoHudOverlay.session.isPorgBuffActive()) {
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
} 