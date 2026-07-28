package pl.mzcode.massdm.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerEntryMixin {
    @Shadow public abstract ServerInfo getServer();

    @Inject(method = "render", at = @At("TAIL"))
    private void renderPixelmineStar(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        ServerInfo info = this.getServer();
        if (info != null && info.address.equalsIgnoreCase("pixelmine.pl")) {
            context.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, "★", x - 15, y + (entryHeight / 2) - 4, 0xFFD700);
        }
    }
}
