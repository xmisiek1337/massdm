package pl.pixelcode.massdm;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class MassDMHud implements HudRenderCallback {

    public static void register() {
        HudRenderCallback.EVENT.register(new MassDMHud());
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!MassDMMod.isRunning()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) return;

        int total = MassDMMod.totalTargetCount;
        if (total <= 0) return;
        int sent = MassDMMod.currentSentCount;
        String currentTarget = MassDMMod.currentTargetPlayer;
        double delaySec = MassDMMod.currentDelaySec;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int boxWidth = 220;
        int boxHeight = 44;
        int x = screenWidth - boxWidth - 10;
        int y = screenHeight - boxHeight - 10;

        // Background card (dark transparent with purple border)
        drawContext.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, 0xFF8B5CF6);
        drawContext.fill(x, y, x + boxWidth, y + boxHeight, 0xF0111827);

        // Title: [MassDM] Broadcasting...
        String title = "§d[MassDM] §f" + MassDMMod.translate("hud_broadcasting");
        drawContext.drawTextWithShadow(client.textRenderer, title, x + 8, y + 6, 0xFFFFFF);

        // Progress count: e.g. 14/40 (35.0%)
        float percentage = ((float) sent / total) * 100f;
        String countStr = String.format("§7%d/%d §8(§d%.1f%%§8)", sent, total, percentage);
        int countWidth = client.textRenderer.getWidth(countStr.replaceAll("§.", ""));
        drawContext.drawTextWithShadow(client.textRenderer, countStr, x + boxWidth - countWidth - 8, y + 6, 0xFFFFFF);

        // Progress bar background
        int barX = x + 8;
        int barY = y + 18;
        int barWidth = boxWidth - 16;
        int barHeight = 6;
        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF374151);

        // Progress bar fill
        int fillWidth = Math.min(barWidth, (int) (barWidth * ((float) sent / total)));
        if (fillWidth > 0) {
            drawContext.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFA855F7);
        }

        // Bottom status line: -> Target (est. XXs left)
        double remainingSec = Math.max(0, (total - sent) * delaySec);
        String targetText = String.format("§8→ §f%s §7(est. %.0fs remaining)", currentTarget.isEmpty() ? "..." : currentTarget, remainingSec);
        drawContext.drawTextWithShadow(client.textRenderer, targetText, x + 8, y + 28, 0x9CA3AF);
    }
}
