package pl.pixelcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ElementListWidget;

public class ExcludedPlayersScreen extends Screen {
    private final Screen parent;
    private PlayerListWidget listWidget;

    protected ExcludedPlayersScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("msg_removed_list").replace(":", "")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;

        this.listWidget = new PlayerListWidget(this.client, this.width, this.height, 40, this.height - 40, 25);
        this.addDrawableChild(this.listWidget);

        for (String nick : MassDMMod.excludedPlayers) {
            this.listWidget.addEntry(new PlayerListEntry(nick));
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.close())
                .dimensions(center - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(new MassDMScreen());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
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

    class PlayerListWidget extends ElementListWidget<PlayerListEntry> {
        public PlayerListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        public int addEntry(PlayerListEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

    }

    class PlayerListEntry extends ElementListWidget.Entry<PlayerListEntry> {
        private final String nick;
        private final ButtonWidget removeButton;

        public PlayerListEntry(String nick) {
            this.nick = nick;
            this.removeButton = ButtonWidget.builder(
                    Text.literal(MassDMMod.translate("screen_remove")), button -> {
                        MassDMMod.excludedPlayers.remove(this.nick);
                        MassDMMod.saveConfig();
                        ExcludedPlayersScreen.this.listWidget.children().remove(this);
                    })
                    .dimensions(0, 0, 80, 20).build();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawTextWithShadow(ExcludedPlayersScreen.this.textRenderer, this.nick, x + 10, y + 5, 0xFFFFFF);
            this.removeButton.setX(x + entryWidth - 85);
            this.removeButton.setY(y);
            this.removeButton.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Element> children() {
            return java.util.List.of(this.removeButton);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return java.util.List.of(this.removeButton);
        }
    }
}
