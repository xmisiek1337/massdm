package pl.mzcode.massdm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MassDMScreen extends Screen {
    private static final Identifier LOGO = Identifier.of("massdm", "textures/gui/logo.png");
    private TextFieldWidget messageField;
    private TextFieldWidget excludeField;
    private TextFieldWidget cmdFormatField;
    private SliderWidget delaySlider;
    private List<String> players = new ArrayList<>();
    private String status = "";
    private double currentDelay = 1.585;
    public static String savedMessage = "Mod created by mzcode";
    public static String savedCmdFormat = "msg {player} {message}";

    public MassDMScreen() {
        super(Text.literal("ᴍᴀssᴅᴍ"));
    }

    @Override
    protected void init() {
        if (status.isEmpty()) status = "§f" + MassDMMod.translate("screen_ready");
        int center = this.width / 2;
        int btnWidth = 200;
        int btnHalf = 98;
        int y = 65; // Below the logo (logo ends at 55)

        // === Message & Delay ===
        this.messageField = new TextFieldWidget(
                textRenderer, center - 100, y, btnWidth - 25, 20, Text.literal(MassDMMod.translate("screen_message_placeholder")));
        this.messageField.setMaxLength(256);
        this.messageField.setText(savedMessage);
        this.messageField.setChangedListener(text -> savedMessage = text);
        addDrawableChild(this.messageField);

        addDrawableChild(ButtonWidget.builder(
                Text.literal("★"), button -> this.client.setScreen(new PresetsScreen(this)))
                .dimensions(center + 77, y, 23, 20).build());
        y += 22;

        this.delaySlider = new SliderWidget(center - 100, y, btnWidth, 20,
                Text.literal(MassDMMod.translate("screen_delay", 1.5)), 0.15) {
            @Override
            protected void updateMessage() {
                double val = 0.1 + (this.value * 9.9);
                this.setMessage(Text.literal(MassDMMod.translate("screen_delay", val)));
            }

            @Override
            protected void applyValue() {
                MassDMScreen.this.currentDelay = 0.1 + (this.value * 9.9);
            }
        };
        addDrawableChild(this.delaySlider);
        y += 22;

        // Start / Stop
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_start")), button -> startMassDM())
                .dimensions(center - 100, y, btnHalf, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_stop")), button -> stopMassDM())
                .dimensions(center + 2, y, btnHalf, 20).build());
        y += 22;

        // Repeat Last
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_start_repeat")), button -> {
                    if (!MassDMMod.isRunning()) {
                        MassDMMod.startMassDM(savedMessage, currentDelay);
                        status = "§f" + MassDMMod.translate("screen_started");
                    }
                })
                .dimensions(center - 100, y, btnHalf, 20).build());

        // Autopilot
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_autopilot_title")), button -> this.client.setScreen(new AutopilotScreen(this)))
                .dimensions(center + 2, y, btnHalf, 20).build());
        y += 28; // spacer for Exclude section

        // === Management ===
        this.excludeField = new TextFieldWidget(
                textRenderer, center - 100, y, btnWidth - 65, 20, Text.literal(MassDMMod.translate("screen_player_nick")));
        this.excludeField.setMaxLength(16);
        addDrawableChild(this.excludeField);

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_exclude")), button -> {
                    String nick = excludeField.getText().trim();
                    if (!nick.isEmpty()) {
                        MassDMMod.excludedPlayers.add(nick);
                        MassDMMod.saveConfig();
                        excludeField.setText("");
                        status = "§f" + MassDMMod.translate("msg_player_removed", nick);
                    }
                })
                .dimensions(center + 40, y, 60, 20).build());
        y += 22;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_view_list")), button -> this.client.setScreen(new ExcludedPlayersScreen(this)))
                .dimensions(center - 100, y, btnHalf, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_online_players")), button -> this.client.setScreen(new OnlinePlayersScreen(this)))
                .dimensions(center + 2, y, btnHalf, 20).build());
        y += 22;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("🔄 " + MassDMMod.translate("screen_refresh")), button -> {
                    refreshPlayerList();
                })
                .dimensions(center - 100, y, btnHalf, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("📋 " + MassDMMod.translate("screen_copy_list")), button -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (!players.isEmpty() && mc != null) {
                        String list = String.join(", ", players);
                        mc.keyboard.setClipboard(list);
                        status = "§f" + MassDMMod.translate("screen_copied");
                    }
                })
                .dimensions(center + 2, y, btnHalf, 20).build());
        y += 22;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_discord_title")), button -> this.client.setScreen(new DiscordScreen(this)))
                .dimensions(center - 100, y, btnWidth, 20).build());
        y += 22;
        
        // Close button at the end of the stack
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_close")), button -> this.close())
                .dimensions(center - 100, y, btnWidth, 20).build());

        // Language toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_lang")), button -> cycleLanguage())
                .dimensions(this.width - 110, 10, 100, 20).build());

        // Version / Changelog button (bottom left corner)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("v1.1"), button -> this.client.setScreen(new ChangelogScreen(this)))
                .dimensions(10, this.height - 30, 45, 20).build());

        // Placeholders button (next to version)
        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_placeholders_btn")), button -> this.client.setScreen(new PlaceholdersScreen(this)))
                .dimensions(60, this.height - 30, 100, 20).build());

        refreshPlayerList();
    }

    private void cycleLanguage() {
        int nextOrdinal = (MassDMMod.currentLanguage.ordinal() + 1) % MassDMMod.Language.values().length;
        MassDMMod.currentLanguage = MassDMMod.Language.values()[nextOrdinal];
        status = "§f" + MassDMMod.translate("screen_ready");
        this.clearChildren();
        this.init();
    }

    private void refreshPlayerList() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            status = "§d" + MassDMMod.translate("msg_not_connected");
            return;
        }

        this.players = client.getNetworkHandler().getPlayerList().stream()
                .filter(e -> {
                    String name = e.getProfile().getName();
                    if (client.player != null && name.equals(client.player.getGameProfile().getName())) return false;
                    if (e.getProfile().getId() != null && e.getProfile().getId().version() == 2) return false;
                    if (name.isEmpty() || !name.matches(".*[a-zA-Z0-9].*") || name.startsWith("!") || name.startsWith(" ")) return false;
                    
                    net.minecraft.scoreboard.Team team = e.getScoreboardTeam();
                    net.minecraft.text.Text baseName = e.getDisplayName() != null ? e.getDisplayName() : net.minecraft.text.Text.literal(name);
                    net.minecraft.text.Text displayName = team != null ? net.minecraft.scoreboard.Team.decorateName(team, baseName) : baseName;
                    String rawName = displayName.getString().trim();
                    if (rawName.isEmpty() || !rawName.matches(".*[a-zA-Z0-9].*")) return false;
                    
                    return true;
                })
                .map(e -> e.getProfile().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        status = "§f" + MassDMMod.translate("screen_found_online", players.size());
        MassDMMod.LOGGER.info("[MassDM] Refreshed list: {} players", players.size());
    }

    private void startMassDM() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) {
            status = "§d" + MassDMMod.translate("screen_enter_msg");
            return;
        }
        if (players.isEmpty()) {
            status = "§d" + MassDMMod.translate("msg_no_players");
            return;
        }

        MassDMMod.startMassDM(msg, this.currentDelay);
        status = "§f" + MassDMMod.translate("screen_started");
    }

    private void stopMassDM() {
        MassDMMod.stopMassDM();
        status = "§d" + MassDMMod.translate("screen_stopped");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Simple dark background to prevent blur mods from blurring it too much if they hook into renderBackground
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int center = width / 2;

        // Label above Message input (was 28, now 53, just above the input field at y=65)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(MassDMMod.translate("screen_message_content")), center, 54, 0xAAAAAA);

        // Stats in top-right: excluded count
        int ex = MassDMMod.excludedPlayers.size();
        String excludedText = "§7ᴇxᴄʟᴜᴅᴇᴅ: §d" + ex;
        int exWidth = textRenderer.getWidth(excludedText);
        context.drawTextWithShadow(textRenderer, Text.literal(excludedText), this.width - exWidth - 5, 5, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        if (MassDMMod.isRunning()) {
            status = "§d" + MassDMMod.translate("screen_in_progress");
        } else if (status.equals("§d" + MassDMMod.translate("screen_in_progress"))) {
            status = "§f" + MassDMMod.translate("screen_finished");
        }

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), center, this.height - 15, 0xFFFFFF);

        // Logo on top (centered at y=2)
        context.drawTexture(RenderLayer::getGuiTextured, LOGO, center - 100, 2, 0, 0, 200, 50, 200, 50);
    }
}

