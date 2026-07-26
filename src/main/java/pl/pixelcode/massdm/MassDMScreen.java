package pl.pixelcode.massdm;

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
    private SliderWidget delaySlider;
    private List<String> players = new ArrayList<>();
    private String status = "";
    private double currentDelay = 1.585;
    public static String savedMessage = "Mod created by PixelCode";

    public MassDMScreen() {
        super(Text.literal("ᴍᴀssᴅᴍ"));
    }

    @Override
    protected void init() {
        if (status.isEmpty()) status = "§f" + MassDMMod.translate("screen_ready");
        int center = this.width / 2;

        this.messageField = new TextFieldWidget(
                textRenderer, center - 100, 80, 200, 20, Text.literal(MassDMMod.translate("screen_message_placeholder")));
        this.messageField.setMaxLength(256);
        this.messageField.setText(savedMessage);
        this.messageField.setChangedListener(text -> savedMessage = text);
        addDrawableChild(this.messageField);

        this.delaySlider = new SliderWidget(center - 100, 115, 200, 20,
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

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_start")), button -> startMassDM())
                .dimensions(center - 100, 150, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_stop")), button -> stopMassDM())
                .dimensions(center + 5, 150, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_close")), button -> this.close())
                .dimensions(center - 100, 190, 200, 20).build());
                
        this.excludeField = new TextFieldWidget(
                textRenderer, center + 110, 80, 100, 20, Text.literal(MassDMMod.translate("screen_player_nick")));
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
                .dimensions(center + 110, 105, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_view_list")), button -> this.client.setScreen(new ExcludedPlayersScreen(this)))
                .dimensions(center + 110, 130, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(MassDMMod.translate("screen_lang")), button -> cycleLanguage())
                .dimensions(this.width - 110, 10, 100, 20).build());

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
                .map(e -> e.getProfile().getName())
                .filter(name -> !name.equals(client.player.getGameProfile().getName()))
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
        this.renderBackground(context, mouseX, mouseY, delta);
        
        context.drawTextWithShadow(textRenderer, Text.literal(MassDMMod.translate("screen_message_content")), width / 2 - 100, 68, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal(MassDMMod.translate("screen_player_nick")), width / 2 + 110, 68, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        if (MassDMMod.isRunning()) {
            status = "§d" + MassDMMod.translate("screen_in_progress");
        } else if (status.equals("§d" + MassDMMod.translate("screen_in_progress"))) {
            status = "§f" + MassDMMod.translate("screen_finished");
        }

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(status), width / 2, height - 30, 0xFFFFFF);
                
        // Rysowanie loga na samym końcu, by było widoczne nad blur modem
        context.drawTexture(RenderLayer::getGuiTextured, LOGO, width / 2 - 100, 10, 0, 0, 200, 50, 200, 50);
    }
}