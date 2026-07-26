package pl.pixelcode.massdm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MassDMMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("massdm");
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> currentTask;
    private static boolean running = false;
    private static KeyBinding guiKeyBinding;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("massdm")
                    .then(literal("send")
                            .then(argument("message", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        String message = StringArgumentType.getString(ctx, "message");
                                        MassDMMod.startMassDM(message, 1.5);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("send")
                            .then(argument("delay", DoubleArgumentType.doubleArg(0.1, 10.0))
                                    .then(argument("message", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                double delay = DoubleArgumentType.getDouble(ctx, "delay");
                                                String message = StringArgumentType.getString(ctx, "message");
                                                MassDMMod.startMassDM(message, delay);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(literal("list")
                            .executes(ctx -> {
                                listPlayers(ctx.getSource());
                                return 1;
                            })
                    )
                    .then(literal("stop")
                            .executes(ctx -> {
                                MassDMMod.stopMassDM();
                                return 1;
                            })
                    )
                    .then(literal("help")
                            .executes(context -> {
                                sendHelp(context.getSource());
                                return 1;
                            })
                    )
            );
            // Ukryte komendy wewnętrzne (nie pokazują się pod /massdm)
            dispatcher.register(literal("_massdm_gui")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.execute(() -> client.setScreen(new MassDMScreen()));
                        return 1;
                    })
            );

            dispatcher.register(literal("_massdm_keybinds")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.execute(() -> client.setScreen(new KeybindsScreen(null, client.options)));
                        return 1;
                    })
            );
        });

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open the MassDM GUI",
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_J, 
                "MassDM"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKeyBinding.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new MassDMScreen());
                }
            }
        });

        LOGGER.info("[MassDM] Mod loaded! Use /massdm help or keybind (default J)");
    }

    private void listPlayers(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            source.sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §fʏᴏᴜ ᴀʀᴇ ɴᴏᴛ ᴄᴏɴɴᴇᴄᴛᴇᴅ ᴛᴏ ᴀ sᴇʀᴠᴇʀ!"));
            return;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        List<String> players = entries.stream()
                .map(e -> e.getProfile().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        source.sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §fꜰᴏᴜɴᴅ §d" + players.size() + " §fᴘʟᴀʏᴇʀs:"));
        StringBuilder sb = new StringBuilder("§f");
        for (int i = 0; i < players.size(); i++) {
            sb.append(players.get(i));
            if (i < players.size() - 1) sb.append("§8, §f");
            if (sb.length() > 80) {
                source.sendFeedback(Text.literal(sb.toString()));
                sb = new StringBuilder("§f");
            }
        }
        if (sb.length() > 2) source.sendFeedback(Text.literal(sb.toString()));
    }

    public static void startMassDM(String message, double delayMs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (running) {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("§d[ᴍᴀssᴅᴍ] §fᴀʟʀᴇᴀᴅʏ sᴇɴᴅɪɴɢ! ᴜsᴇ /massdm stop"), false);
            }
            return;
        }


        if (client.player == null || client.getNetworkHandler() == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §fʏᴏᴜ ᴀʀᴇ ɴᴏᴛ ᴄᴏɴɴᴇᴄᴛᴇᴅ ᴛᴏ ᴀ sᴇʀᴠᴇʀ!"), false);
            }
            return;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        List<String> players = entries.stream()
                .map(e -> e.getProfile().getName())
                .filter(name -> !name.equals(client.player.getGameProfile().getName()))
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §fɴᴏ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀs ᴏɴ sᴇʀᴠᴇʀ!"), false);
            return;
        }

        running = true;
        long delay = (long) (delayMs * 1000);
        client.player.sendMessage(Text.literal(
                String.format("§d[ᴍᴀssᴅᴍ] §fsᴇɴᴅɪɴɢ ᴛᴏ §d%d §fᴘʟᴀʏᴇʀs ᴡɪᴛʜ ᴅᴇʟᴀʏ §d%.1fs§f...",
                        players.size(), delayMs)), false);

        final int[] index = {0};
        currentTask = executor.scheduleAtFixedRate(() -> {
            if (!running || index[0] >= players.size()) {
                running = false;
                if (index[0] >= players.size()) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("§d[ᴍᴀssᴅᴍ] §fsᴇɴᴛ ᴛᴏ ᴀʟʟ §d" + players.size() + " §fᴘʟᴀʏᴇʀs!"), false);
                        }
                    });
                }
                if (currentTask != null) {
                    currentTask.cancel(false);
                    currentTask = null;
                }
                return;
            }

            String target = players.get(index[0]++);
            String cmd = "/msg " + target + " " + message;
            client.execute(() -> {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand(cmd.substring(1));
                    client.player.sendMessage(
                            Text.literal("§d[ᴍᴀssᴅᴍ] §f→ §d" + target + " §f(" + index[0] + "/" + players.size() + ")"), false);
                }
            });

            LOGGER.info("[MassDM] → {}: {}", target, message);
        }, 0, delay, TimeUnit.MILLISECONDS);
    }

    public static void stopMassDM() {
        running = false;
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §fsᴇɴᴅɪɴɢ sᴛᴏᴘᴘᴇᴅ!"), false);
        }
    }

    private void sendHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("                §8× §fᴍᴀss§dᴅᴍ §fʜᴇʟᴘ §8×"));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʜᴇʟᴘ §7- §dꜱʜᴏᴡꜱ ʜᴇʟᴘ ᴍᴇɴᴜ"));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʟɪꜱᴛ §7- §dꜱʜᴏᴡs ᴘʟᴀʏᴇʀ ʟɪsᴛ"));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ꜱᴇɴᴅ <msg> §7- §dsᴇɴᴅs ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴀʟʟ ᴘʟᴀʏᴇʀs"));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ꜱᴇɴᴅ <delay> <msg> §7- §dsᴇɴᴅs ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴀʟʟ ᴘʟᴀʏᴇʀs ᴡɪᴛʜ ᴀ ᴅᴇʟᴀʏ"));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ꜱᴛᴏᴘ §7- §dsᴛᴏᴘs ᴛʜᴇ sᴇɴᴅɪɴɢ ᴘʀᴏᴄᴇss"));
        source.sendFeedback(Text.literal(""));
        String keyName = guiKeyBinding.getBoundKeyLocalizedText().getString();
        source.sendFeedback(Text.literal("§fᴏᴘᴇɴ ɢᴜɪ ᴋᴇʏʙɪɴᴅ§7: §d" + toSmallCaps(keyName) + " §7- ")
                .append(Text.literal("§aᴘʀᴇss").styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/_massdm_gui"))))
                .append(Text.literal(" §fᴛᴏ ᴏᴘᴇɴ ɢᴜɪ")));
        source.sendFeedback(Text.literal("§7ᴋᴇʏʙɪɴᴅ ɪꜱ ᴄʜᴀɴɢᴇᴀʙʟᴇ ɪɴ ᴏᴘᴛɪᴏɴꜱ/ᴋᴇʏʙɪɴᴅꜱ ꜱᴇᴛᴛɪɴɢꜱ! §8(")
                .append(Text.literal("§aᴘʀᴇꜱꜱ").styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/_massdm_keybinds"))))
                .append(Text.literal(" §7ᴛᴏ ᴏᴘᴇɴ ꜱᴇᴛᴛɪɴɢꜱ§8)")));
    }

    private static String toSmallCaps(String text) {
        String normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String smallcaps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toUpperCase().toCharArray()) {
            int idx = normal.indexOf(c);
            if (idx != -1) {
                sb.append(smallcaps.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}