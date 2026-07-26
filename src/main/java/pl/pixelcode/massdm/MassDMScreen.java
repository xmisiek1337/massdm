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
    private SliderWidget delaySlider;
    private List<String> players = new ArrayList<>();
    private String status = "§fʀᴇᴀᴅʏ.";
    private double currentDelay = 1.585;

    protected MassDMScreen() {
        super(Text.literal("ᴍᴀssᴅᴍ"));
    }

    @Override
    protected void init() {
        int center = this.width / 2;

        this.messageField = new TextFieldWidget(
                textRenderer, center - 100, 80, 200, 20, Text.literal("ᴍᴇssᴀɢᴇ"));
        this.messageField.setMaxLength(256);
        this.messageField.setText("ᴍᴏᴅ ᴄʀᴇᴀᴛᴇᴅ ʙʏ ᴘɪxᴇʟᴄᴏᴅᴇ");
        addDrawableChild(this.messageField);

        this.delaySlider = new SliderWidget(center - 100, 115, 200, 20,
                Text.literal("ᴅᴇʟᴀʏ: 1.5 s"), 0.15) {
            @Override
            protected void updateMessage() {
                double val = 0.1 + (this.value * 9.9);
                this.setMessage(Text.literal(String.format("ᴅᴇʟᴀʏ: %.1f s", val)));
            }

            @Override
            protected void applyValue() {
                MassDMScreen.this.currentDelay = 0.1 + (this.value * 9.9);
            }
        };
        addDrawableChild(this.delaySlider);

        addDrawableChild(ButtonWidget.builder(
                Text.literal("📨 sᴛᴀʀᴛ"), button -> startMassDM())
                .dimensions(center - 100, 150, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("⏹ sᴛᴏᴘ"), button -> stopMassDM())
                .dimensions(center + 5, 150, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("✕ ᴄʟᴏsᴇ"), button -> this.close())
                .dimensions(center - 100, 190, 200, 20).build());

        refreshPlayerList();
    }

    private void refreshPlayerList() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            status = "§dʏᴏᴜ ᴀʀᴇ ɴᴏᴛ ᴄᴏɴɴᴇᴄᴛᴇᴅ ᴛᴏ ᴀ sᴇʀᴠᴇʀ!";
            return;
        }

        this.players = client.getNetworkHandler().getPlayerList().stream()
                .map(e -> e.getProfile().getName())
                .filter(name -> !name.equals(client.player.getGameProfile().getName()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        status = "§fꜰᴏᴜɴᴅ §d" + players.size() + " §fᴘʟᴀʏᴇʀs ᴏɴʟɪɴᴇ";
        MassDMMod.LOGGER.info("[MassDM] Refreshed list: {} players", players.size());
    }

    private void startMassDM() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) {
            status = "§dᴇɴᴛᴇʀ ᴍᴇssᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ!";
            return;
        }
        if (players.isEmpty()) {
            status = "§dɴᴏ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀs ᴏɴ sᴇʀᴠᴇʀ!";
            return;
        }

        MassDMMod.startMassDM(msg, this.currentDelay);
        status = "§fsᴛᴀʀᴛᴇᴅ ᴍᴀss sᴇɴᴅɪɴɢ!";
    }

    private void stopMassDM() {
        MassDMMod.stopMassDM();
        status = "§dsᴛᴏᴘᴘᴇᴅ!";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        context.drawTextWithShadow(textRenderer, Text.literal("ᴍᴇssᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ:"), width / 2 - 100, 68, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        if (MassDMMod.isRunning()) {
            status = "§dsᴇɴᴅɪɴɢ ɪɴ ᴘʀᴏɢʀᴇss...";
        } else if (status.equals("§dsᴇɴᴅɪɴɢ ɪɴ ᴘʀᴏɢʀᴇss...")) {
            status = "§fsᴇɴᴅɪɴɢ ꜰɪɴɪsʜᴇᴅ!";
        }

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(status), width / 2, height - 30, 0xFFFFFF);
                
        // Rysowanie loga na samym końcu, by było widoczne nad blur modem
        context.drawTexture(RenderLayer::getGuiTextured, LOGO, width / 2 - 100, 10, 0, 0, 200, 50, 200, 50);
    }
}