package pl.mzcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AutopilotScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget messageField;
    private SliderWidget intervalSlider;
    private ButtonWidget toggleButton;

    public AutopilotScreen(Screen parent) {
        super(Text.literal(pl.mzcode.massdm.MassDMMod.translate("screen_autopilot_title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int y = this.height / 2 - 50;

        // Message input
        this.messageField = new TextFieldWidget(
                this.textRenderer, center - 100, y, 200, 20, Text.literal(MassDMMod.translate("screen_message_placeholder")));
        this.messageField.setMaxLength(256);
        this.messageField.setText(MassDMMod.autopilotMessage);
        this.messageField.setChangedListener(text -> MassDMMod.autopilotMessage = text);
        this.addDrawableChild(this.messageField);
        y += 24;

        // Interval slider
        this.intervalSlider = new SliderWidget(center - 100, y, 200, 20,
                Text.literal(MassDMMod.translate("screen_autopilot_interval", MassDMMod.autopilotIntervalMinutes)), 
                (MassDMMod.autopilotIntervalMinutes - 1) / 59.0) {
            @Override
            protected void updateMessage() {
                int mins = (int) (1 + (this.value * 59));
                this.setMessage(Text.literal(MassDMMod.translate("screen_autopilot_interval", mins)));
            }

            @Override
            protected void applyValue() {
                MassDMMod.autopilotIntervalMinutes = (int) (1 + (this.value * 59));
            }
        };
        this.addDrawableChild(this.intervalSlider);
        y += 24;

        // Toggle button
        this.toggleButton = ButtonWidget.builder(
                Text.literal(MassDMMod.translate(MassDMMod.autopilotEnabled ? "screen_autopilot_on" : "screen_autopilot_off")), button -> {
                    MassDMMod.autopilotEnabled = !MassDMMod.autopilotEnabled;
                    button.setMessage(Text.literal(MassDMMod.translate(MassDMMod.autopilotEnabled ? "screen_autopilot_on" : "screen_autopilot_off")));
                    if (MassDMMod.autopilotEnabled) {
                        MassDMMod.lastAutopilotSendTime = System.currentTimeMillis();
                        MassDMMod.autopilotSentPlayers.clear();
                    }
                })
                .dimensions(center - 100, y, 200, 20).build();
        this.addDrawableChild(this.toggleButton);
        y += 35;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.client.setScreen(this.parent))
                .dimensions(center - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        super.render(context, mouseX, mouseY, delta);

        int center = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l" + MassDMMod.translate("screen_autopilot_title")), center, this.height / 2 - 70, 0xFFFFFF);
    }
}
