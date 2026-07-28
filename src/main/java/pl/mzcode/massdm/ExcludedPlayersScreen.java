package pl.mzcode.massdm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.ArrayList;
import java.util.List;

public class ExcludedPlayersScreen extends Screen {
    private final Screen parent;
    private PlayerListWidget listWidget;
    private TextFieldWidget searchField;
    private List<PlayerListEntry> allEntries = new ArrayList<>();

    protected ExcludedPlayersScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("msg_removed_list").replace(":", "")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;

        this.searchField = new TextFieldWidget(
                textRenderer, center - 100, 30, 200, 18, Text.literal(MassDMMod.translate("screen_search")));
        this.searchField.setMaxLength(32);
        this.searchField.setChangedListener(this::filterEntries);
        addDrawableChild(this.searchField);

        this.listWidget = new PlayerListWidget(this.client, this.width, this.height, 52, 25);
        this.addDrawableChild(this.listWidget);

        this.allEntries.clear();
        for (String nick : MassDMMod.excludedPlayers) {
            this.allEntries.add(new PlayerListEntry(nick));
        }

        filterEntries(this.searchField != null ? this.searchField.getText() : "");

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.close())
                .dimensions(center - 100, this.height - 28, 200, 20).build());
    }

    private void filterEntries(String query) {
        if (this.listWidget == null) return;
        this.listWidget.children().clear();
        String q = query.trim().toLowerCase();
        for (PlayerListEntry entry : allEntries) {
            if (q.isEmpty() || entry.nick.toLowerCase().contains(q)) {
                this.listWidget.addEntry(entry);
            }
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(new MassDMScreen());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000); // No blur
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
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
        public PlayerListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
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
        public final String nick;
        private final ButtonWidget removeButton;

        public PlayerListEntry(String nick) {
            this.nick = nick;
            this.removeButton = ButtonWidget.builder(
                    Text.literal(MassDMMod.translate("screen_remove")), button -> {
                        MassDMMod.excludedPlayers.remove(this.nick);
                        MassDMMod.saveConfig();
                        ExcludedPlayersScreen.this.allEntries.remove(this);
                        ExcludedPlayersScreen.this.listWidget.children().remove(this);
                    })
                    .dimensions(0, 0, 80, 20).build();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = MinecraftClient.getInstance();
            net.minecraft.client.network.PlayerListEntry mcEntry = client.getNetworkHandler() != null ? client.getNetworkHandler().getPlayerListEntry(this.nick) : null;
            if (mcEntry != null) {
                PlayerSkinDrawer.draw(context, mcEntry.getSkinTextures(), x + 5, y + 2, 16);
            } else {
                context.fill(x + 5, y + 2, x + 21, y + 18, 0xFF555555);
            }

            context.drawTextWithShadow(ExcludedPlayersScreen.this.textRenderer, this.nick, x + 26, y + 5, 0xFFFFFF);
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
