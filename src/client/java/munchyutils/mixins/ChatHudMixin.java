package munchyutils.mixins;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void munchyutils$onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        // Call the new handler for all chat logic
        munchyutils.client.MunchyUtilsClient.handleChatHudMessage(message);
        // Suppress unwanted messages
        String msg = message.getString();
        munchyutils.client.MunchyConfig config = munchyutils.client.MunchyConfig.get();
        if (config.isHideInventoryFullMessage() && msg.contains("Your inventory is full! Click here to sell your items, or type /sell!")) {
            ci.cancel();
            return;
        }
        if (config.isHideSellSuccessMessage() && msg.matches("Successfully sold \\d+ items for \\$[\\d,]+.*")) {
            ci.cancel();
            return;
        }
        // Otherwise, let the message through
    }
} 