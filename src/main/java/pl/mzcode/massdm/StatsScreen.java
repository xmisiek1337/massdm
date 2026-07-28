package pl.mzcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StatsScreen extends Screen {
    private final Screen parent;

    public StatsScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("screen_stats_title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int y = this.height / 2 + 35;
        
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_close")), button -> this.client.setScreen(this.parent))
                .dimensions(center - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        super.render(context, mouseX, mouseY, delta);

        int center = this.width / 2;
        int boxY = this.height / 2 - 50;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(MassDMMod.translate("screen_stats_title")), center, boxY, 0xFFFFFF);

        long durationMs = System.currentTimeMillis() - MassDMMod.broadcastStartTime;
        long mins = durationMs / 60000;
        long secs = (durationMs % 60000) / 1000;
        String timeStr = mins > 0 ? mins + "m " + secs + "s" : secs + "s";

        String sentText = MassDMMod.translate("screen_stats_sent", MassDMMod.lastBroadcastTargetCount);
        String timeText = MassDMMod.translate("screen_stats_time", timeStr);
        String skippedText = MassDMMod.translate("screen_stats_skipped", MassDMMod.skippedPlayersCount);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§a" + sentText), center, boxY + 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e" + timeText), center, boxY + 35, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§c" + skippedText), center, boxY + 50, 0xFFFFFF);
    }
}
