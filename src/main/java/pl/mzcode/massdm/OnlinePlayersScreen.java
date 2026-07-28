package pl.mzcode.massdm;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class OnlinePlayersScreen extends Screen {
    private final Screen parent;
    private PlayerListWidget listWidget;
    private TextFieldWidget searchField;
    private List<PlayerListEntryWidget> allWidgets = new ArrayList<>();

    protected OnlinePlayersScreen(Screen parent) {
        super(Text.literal(MassDMMod.translate("screen_online_players").replace(":", "")));
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

        this.allWidgets.clear();
        if (this.client != null && this.client.getNetworkHandler() != null) {
            Collection<PlayerListEntry> entries = this.client.getNetworkHandler().getPlayerList();

            for (PlayerListEntry entry : entries) {
                String nick = entry.getProfile().getName();
                
                if (this.client.player != null && nick.equals(this.client.player.getGameProfile().getName())) {
                    continue;
                }
                
                // UUID v2 is for NPCs
                if (entry.getProfile().getId() != null && entry.getProfile().getId().version() == 2) {
                    continue;
                }

                // Spacers / fake players filter
                if (nick.isEmpty() || !nick.matches(".*[a-zA-Z0-9].*") || nick.startsWith("!") || nick.startsWith(" ")) {
                    continue;
                }

                Text baseName = entry.getDisplayName() != null ? entry.getDisplayName() : Text.literal(nick);
                Team team = entry.getScoreboardTeam();
                Text displayName = team != null ? Team.decorateName(team, baseName) : baseName;

                String rawName = displayName.getString().trim();
                if (rawName.isEmpty() || !rawName.matches(".*[a-zA-Z0-9].*")) {
                    continue;
                }

                allWidgets.add(new PlayerListEntryWidget(nick, displayName));
            }

            allWidgets.sort(Comparator.comparing(w -> w.nick.toLowerCase()));
            filterEntries(this.searchField != null ? this.searchField.getText() : "");
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_back")), button -> this.close())
                .dimensions(center - 100, this.height - 28, 200, 20).build());
    }

    private void filterEntries(String query) {
        if (this.listWidget == null) return;
        this.listWidget.children().clear();
        String q = query.trim().toLowerCase();
        for (PlayerListEntryWidget w : allWidgets) {
            if (q.isEmpty() || w.nick.toLowerCase().contains(q) || w.displayName.getString().toLowerCase().contains(q)) {
                this.listWidget.addEntry(w);
            }
        }
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

    class PlayerListWidget extends ElementListWidget<PlayerListEntryWidget> {
        public PlayerListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public int addEntry(PlayerListEntryWidget entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    class PlayerListEntryWidget extends ElementListWidget.Entry<PlayerListEntryWidget> {
        public final String nick;
        private final Text displayName;
        private final ButtonWidget excludeButton;

        public PlayerListEntryWidget(String nick, Text displayName) {
            this.nick = nick;
            this.displayName = displayName;
            
            boolean isExcluded = MassDMMod.excludedPlayers.contains(this.nick);
            
            this.excludeButton = ButtonWidget.builder(
                    Text.literal(isExcluded ? MassDMMod.translate("screen_remove") : MassDMMod.translate("screen_exclude")), button -> {
                        if (MassDMMod.excludedPlayers.contains(this.nick)) {
                            MassDMMod.excludedPlayers.remove(this.nick);
                        } else {
                            MassDMMod.excludedPlayers.add(this.nick);
                        }
                        MassDMMod.saveConfig();
                        
                        boolean excluded = MassDMMod.excludedPlayers.contains(this.nick);
                        button.setMessage(Text.literal(excluded ? MassDMMod.translate("screen_remove") : MassDMMod.translate("screen_exclude")));
                    })
                    .dimensions(0, 0, 80, 20).build();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerListEntry entry = client.getNetworkHandler() != null ? client.getNetworkHandler().getPlayerListEntry(this.nick) : null;
            if (entry != null) {
                PlayerSkinDrawer.draw(context, entry.getSkinTextures(), x + 5, y + 2, 16);
            } else {
                context.fill(x + 5, y + 2, x + 21, y + 18, 0xFF555555);
            }

            context.drawTextWithShadow(OnlinePlayersScreen.this.textRenderer, this.displayName, x + 26, y + 5, 0xFFFFFF);
            this.excludeButton.setX(x + entryWidth - 85);
            this.excludeButton.setY(y);
            this.excludeButton.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Element> children() {
            return java.util.List.of(this.excludeButton);
        }

        @Override
        public java.util.List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return java.util.List.of(this.excludeButton);
        }
    }
}
