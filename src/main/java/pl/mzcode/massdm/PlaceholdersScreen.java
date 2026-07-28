package pl.mzcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class PlaceholdersScreen extends Screen {
    private final Screen parent;
    
    private List<String> getPlaceholders() {
        return List.of(
            "§d{player} / {target} §f- " + MassDMMod.translate("placeholder_player"),
            "§d{me} §f- " + MassDMMod.translate("placeholder_me"),
            "§d{online} §f- " + MassDMMod.translate("placeholder_online"),
            "§d{server} §f- " + MassDMMod.translate("placeholder_server"),
            "§d{time} §f- " + MassDMMod.translate("placeholder_time"),
            "§d{date} §f- " + MassDMMod.translate("placeholder_date"),
            "§d{ping} §f- " + MassDMMod.translate("placeholder_ping"),
            "§d{uuid} §f- " + MassDMMod.translate("placeholder_uuid"),
            "§d{random:X} §f- " + MassDMMod.translate("placeholder_random")
        );
    }

    public PlaceholdersScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("screen_placeholders_btn")));
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
        
        int boxW = 340;
        int boxH = 170;
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - boxH) / 2 - 15;
        
        context.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF8B5CF6);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0111827);
        
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l" + MassDMMod.translate("screen_placeholders_btn")), this.width / 2, boxY + 10, 0xFFFFFF);
        
        int textY = boxY + 30;
        for (String line : getPlaceholders()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(line), boxX + 15, textY, 0xFFFFFF);
            textY += 14;
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
