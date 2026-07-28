package pl.mzcode.massdm.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin {
    @Shadow protected MultiplayerServerListWidget serverListWidget;
    @Shadow private ButtonWidget buttonEdit;
    @Shadow private ButtonWidget buttonDelete;
    
    @Inject(method = "updateButtonActivationStates", at = @At("TAIL"))
    private void disableEditDeleteForPixelmine(CallbackInfo ci) {
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
            ServerInfo info = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
            if (info != null && info.address.equalsIgnoreCase("pixelmine.pl")) {
                if (this.buttonEdit != null) this.buttonEdit.active = false;
                if (this.buttonDelete != null) this.buttonDelete.active = false;
            }
        }
    }
}
