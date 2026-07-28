package pl.mzcode.massdm.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.mzcode.massdm.MassDMScreen;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void addMassDMButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ᴍᴀssᴅᴍ ᴍᴇɴᴜ"), button -> {
            if (this.client != null) {
                this.client.setScreen(new MassDMScreen());
            }
        }).dimensions(this.width / 2 - 102, this.height / 4 + 144, 204, 20).build());
    }
}
