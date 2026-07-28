package pl.mzcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class DiscordScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget webhookField;
    private ButtonWidget toggleButton;

    public DiscordScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("screen_discord_title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int y = this.height / 2 - 40;

        // Webhook URL input
        this.webhookField = new TextFieldWidget(
                this.textRenderer, center - 120, y, 240, 20, Text.literal(MassDMMod.translate("screen_discord_url")));
        this.webhookField.setMaxLength(1024);
        this.webhookField.setText(MassDMMod.discordWebhookUrl);
        this.webhookField.setChangedListener(text -> MassDMMod.discordWebhookUrl = text);
        this.addDrawableChild(this.webhookField);
        y += 24;

        // Toggle button
        this.toggleButton = ButtonWidget.builder(
                Text.literal(MassDMMod.translate(MassDMMod.discordIntegrationEnabled ? "screen_discord_on" : "screen_discord_off")), button -> {
                    MassDMMod.discordIntegrationEnabled = !MassDMMod.discordIntegrationEnabled;
                    button.setMessage(Text.literal(MassDMMod.translate(MassDMMod.discordIntegrationEnabled ? "screen_discord_on" : "screen_discord_off")));
                })
                .dimensions(center - 120, y, 115, 20).build();
        this.addDrawableChild(this.toggleButton);

        // Test Webhook button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_discord_test")), button -> {
                    MassDMMod.sendDiscordWebhook("Test Webhook", "MassDM Discord Integration is working perfectly! 🚀", 0x00FF00);
                })
                .dimensions(center + 5, y, 115, 20).build());
        y += 35;

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.client.setScreen(this.parent))
                .dimensions(center - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        super.render(context, mouseX, mouseY, delta);

        int center = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l" + MassDMMod.translate("screen_discord_title")), center, this.height / 2 - 60, 0xFFFFFF);
    }
}
