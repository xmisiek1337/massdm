package pl.mzcode.massdm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PresetsScreen extends Screen {
    private final MassDMScreen parent;
    private PresetListWidget listWidget;

    public PresetsScreen(MassDMScreen parent) {
        super(Text.literal(MassDMMod.translate("screen_presets")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.listWidget = new PresetListWidget(this.client, this.width, this.height, 52, 25);
        this.addDrawableChild(this.listWidget);

        this.allEntries.clear();
        for (String preset : MassDMMod.presets) {
            PresetListEntry entry = new PresetListEntry(preset);
            this.listWidget.addEntry(entry);
            this.allEntries.add(entry);
        }

        int center = this.width / 2;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_save_preset")), button -> {
                    String msg = MassDMScreen.savedMessage;
                    if (!msg.isEmpty() && !MassDMMod.presets.contains(msg)) {
                        MassDMMod.presets.add(msg);
                        MassDMMod.savePresets();
                        PresetListEntry entry = new PresetListEntry(msg);
                        this.listWidget.addEntry(entry);
                        this.allEntries.add(entry);
                    }
                })
                .dimensions(center - 105, this.height - 35, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.client.setScreen(this.parent))
                .dimensions(center + 5, this.height - 35, 100, 20).build());
    }

    private final List<PresetListEntry> allEntries = new ArrayList<>();

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000); // No blur
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l" + MassDMMod.translate("screen_presets")), this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof ButtonWidget && ((ButtonWidget) element).isMouseOver(mouseX, mouseY)) {
                return element.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    class PresetListWidget extends ElementListWidget<PresetListEntry> {
        public PresetListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public int addEntry(PresetListEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    class PresetListEntry extends ElementListWidget.Entry<PresetListEntry> {
        public final String presetText;
        private final ButtonWidget selectButton;
        private final ButtonWidget deleteButton;

        public PresetListEntry(String presetText) {
            this.presetText = presetText;

            this.selectButton = ButtonWidget.builder(Text.literal(MassDMMod.translate("screen_select")), button -> {
                        MassDMScreen.savedMessage = this.presetText;
                        MinecraftClient.getInstance().setScreen(parent);
                    })
                    .dimensions(0, 0, 60, 20).build();

            this.deleteButton = ButtonWidget.builder(Text.literal(MassDMMod.translate("screen_delete")), button -> {
                        MassDMMod.presets.remove(this.presetText);
                        MassDMMod.savePresets();
                        MinecraftClient.getInstance().setScreen(new PresetsScreen(parent));
                    })
                    .dimensions(0, 0, 50, 20).build();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            String display = this.presetText;
            if (display.length() > 25) {
                display = display.substring(0, 22) + "...";
            }
            context.drawTextWithShadow(PresetsScreen.this.textRenderer, Text.literal(display), x + 5, y + 5, 0xFFFFFF);

            this.selectButton.setX(x + entryWidth - 115);
            this.selectButton.setY(y);
            this.selectButton.render(context, mouseX, mouseY, tickDelta);

            this.deleteButton.setX(x + entryWidth - 50);
            this.deleteButton.setY(y);
            this.deleteButton.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Element> children() {
            return java.util.List.of(this.selectButton, this.deleteButton);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return java.util.List.of(this.selectButton, this.deleteButton);
        }
    }
}
