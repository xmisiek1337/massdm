package pl.pixelcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ChangelogScreen extends Screen {
    private final Screen parent;
    
    // Add changelog entries here!
    private List<String> getChangelog() {
        return List.of(
            MassDMMod.translate("changelog_1"),
            MassDMMod.translate("changelog_2"),
            MassDMMod.translate("changelog_3"),
            MassDMMod.translate("changelog_4"),
            MassDMMod.translate("changelog_5"),
            MassDMMod.translate("changelog_6"),
            MassDMMod.translate("changelog_7"),
            MassDMMod.translate("changelog_8"),
            "",
            MassDMMod.translate("changelog_thx")
        );
    }

    public ChangelogScreen(Screen parent) {
        super(Text.literal("Changelog"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.close())
                .dimensions(center - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000); // No blur
        
        // Draw elegant dark box in center
        int boxW = 320;
        int boxH = 200;
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - boxH) / 2 - 15;
        
        context.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF8B5CF6); // Purple border
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0111827); // Dark background
        
        int textY = boxY + 15;
        context.drawTextWithShadow(this.textRenderer, Text.literal("§lMassDM v1.1 Changelog:"), boxX + 15, textY, 0xFFFFFF);
        textY += 20;

        for (String line : getChangelog()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(line), boxX + 15, textY, 0xFFFFFF);
            textY += 12;
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
