package pl.pixelcode.massdm.mixin;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Shadow private List<ServerInfo> servers;

    @Inject(method = "loadFile", at = @At("TAIL"))
    private void addPermanentServer(CallbackInfo ci) {
        servers.removeIf(s -> s.address.equalsIgnoreCase("pixelmine.pl"));
        ServerInfo info = new ServerInfo("Serwer PixelCode", "pixelmine.pl", ServerInfo.ServerType.OTHER);
        servers.add(0, info);
    }
}
