package pl.mzcode.massdm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final List<ScheduledFuture<?>> currentTasks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public enum Language { EN, PL, DE }
    public static Language currentLanguage = Language.EN;
    private static KeyBinding guiKeyBinding;
    public static final Set<String> excludedPlayers = new java.util.concurrent.ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);

    // HUD tracking state
    public static int currentSentCount = 0;
    public static int totalTargetCount = 0;
    public static String currentTargetPlayer = "";
    public static double currentDelaySec = 0;

    // Auto-Responder & DM Tracker
    public static boolean autoResponderEnabled = false;
    public static String autoResponderMessage = "Jestem AFK, odpisze pozniej!";
    private static final Map<String, Long> lastAutoReplyMap = new ConcurrentHashMap<>();

    public static final List<String> presets = new ArrayList<>();

    // Stats variables
    public static long broadcastStartTime = 0;
    public static int skippedPlayersCount = 0;
    public static int lastBroadcastTargetCount = 0;

    // Autopilot variables
    public static boolean autopilotEnabled = false;
    public static int autopilotIntervalMinutes = 10;
    public static String autopilotMessage = "";
    public static long lastAutopilotSendTime = 0;
    public static final Set<String> autopilotSentPlayers = new HashSet<>();

    // Discord Integration
    public static String discordWebhookUrl = "";
    public static boolean discordIntegrationEnabled = false;

    private static final Map<String, Map<Language, String>> translations = new HashMap<>();

    static {
        addTranslation("msg_not_connected", "ʏᴏᴜ ᴀʀᴇ ɴᴏᴛ ᴄᴏɴɴᴇᴄᴛᴇᴅ ᴛᴏ ᴀ sᴇʀᴠᴇʀ!", "ɴɪᴇ ᴊᴇsᴛᴇs ᴘᴏʟᴀᴄᴢᴏɴʏ ᴢ sᴇʀᴡᴇʀᴇᴍ!", "ᴅᴜ ʙɪsᴛ ɴɪᴄʜᴛ ᴍɪᴛ ᴇɪɴᴇᴍ sᴇʀᴠᴇʀ ᴠᴇʀʙᴜɴᴅᴇɴ!");
        addTranslation("msg_found_players", "ꜰᴏᴜɴᴅ %d ᴘʟᴀʏᴇʀs:", "ᴢɴᴀʟᴇᴢɪᴏɴᴏ %d ɢʀᴀᴄᴢʏ:", "ᴇs ᴡᴜʀᴅᴇɴ %d sᴘɪᴇʟᴇʀ ɢᴇꜰᴜɴᴅᴇɴ:");
        addTranslation("msg_already_sending", "ᴀʟʀᴇᴀᴅʏ sᴇɴᴅɪɴɢ! ᴜsᴇ /massdm stop", "ᴡʏsʏʟᴀɴɪᴇ ᴛʀᴡᴀ! ᴜᴢʏᴊ /massdm stop", "ᴇs ᴡɪʀᴅ ʙᴇʀᴇɪᴛs ɢᴇsᴇɴᴅᴇᴛ! ɴᴜᴛᴢᴇ /massdm stop");
        addTranslation("msg_no_players", "ɴᴏ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀs ᴏɴ sᴇʀᴠᴇʀ!", "ʙʀᴀᴋ ɪɴɴʏᴄʜ ɢʀᴀᴄᴢʏ ɴᴀ sᴇʀᴡᴇʀᴢᴇ!", "ᴋᴇɪɴᴇ ᴀɴᴅᴇʀᴇɴ sᴘɪᴇʟᴇʀ ᴀᴜꜰ ᴅᴇᴍ sᴇʀᴠᴇʀ!");
        addTranslation("msg_sending", "sᴇɴᴅɪɴɢ ᴛᴏ %d ᴘʟᴀʏᴇʀs ᴡɪᴛʜ ᴅᴇʟᴀʏ %.1fs...", "ᴡʏsʏʟᴀɴɪᴇ ᴅᴏ %d ɢʀᴀᴄᴢʏ ᴢ ᴏᴘᴏᴢɴɪᴇɴɪᴇᴍ %.1fs...", "sᴇɴᴅᴇɴ ᴀɴ %d sᴘɪᴇʟᴇʀ ᴍɪᴛ %.1fs ᴅᴇʟᴀʏ...");
        addTranslation("msg_sent", "sᴇɴᴛ ᴛᴏ ᴀʟʟ %d ᴘʟᴀʏᴇʀs!", "ᴡʏsʟᴀɴᴏ ᴅᴏ ᴡsᴢʏsᴛᴋɪᴄʜ %d ɢʀᴀᴄᴢʏ!", "ᴀɴ ᴀʟʟᴇ %d sᴘɪᴇʟᴇʀ ɢᴇsᴇɴᴅᴇᴛ!");
        addTranslation("msg_stopped", "sᴇɴᴅɪɴɢ sᴛᴏᴘᴘᴇᴅ!", "ᴡʏsʏʟᴀɴɪᴇ ᴢᴀᴛʀᴢʏᴍᴀɴᴇ!", "sᴇɴᴅᴇɴ ɢᴇsᴛᴏᴘᴘᴛ!");
        
        addTranslation("help_menu", "sʜᴏᴡs ʜᴇʟᴘ ᴍᴇɴᴜ", "ᴘᴏᴋᴀᴢᴜᴊᴇ ᴍᴇɴᴜ ᴘᴏᴍᴏᴄʏ", "ᴢᴇɪɢᴛ ᴅᴀs ʜɪʟꜰᴇᴍᴇɴᴜ ᴀɴ");
        addTranslation("help_list", "sʜᴏᴡs ᴘʟᴀʏᴇʀ ʟɪsᴛ", "ᴘᴏᴋᴀᴢᴜᴊᴇ ʟɪsᴛᴇ ɢʀᴀᴄᴢʏ", "ᴢᴇɪɢᴛ ᴅɪᴇ sᴘɪᴇʟᴇʀʟɪsᴛᴇ ᴀɴ");
        addTranslation("help_send", "sᴇɴᴅs ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴀʟʟ ᴘʟᴀʏᴇʀs", "ᴡʏsʏʟᴀ ᴡɪᴀᴅᴏᴍᴏsᴄ ᴅᴏ ᴡsᴢʏsᴛᴋɪᴄʜ", "sᴇɴᴅᴇᴛ ᴇɪɴᴇ ɴᴀᴄʜʀɪᴄʜᴛ ᴀɴ ᴀʟʟᴇ sᴘɪᴇʟᴇʀ");
        addTranslation("help_send_delay", "sᴇɴᴅs ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴀʟʟ ᴘʟᴀʏᴇʀs ᴡɪᴛʜ ᴀ ᴅᴇʟᴀʏ", "ᴡʏsʏʟᴀ ᴡɪᴀᴅᴏᴍᴏsᴄ ᴅᴏ ᴡsᴢʏsᴛᴋɪᴄʜ ᴢ ᴏᴘᴏᴢɴɪᴇɴɪᴇᴍ", "sᴇɴᴅᴇᴛ ᴇɪɴᴇ ɴᴀᴄʜʀɪᴄʜᴛ ᴀɴ ᴀʟʟᴇ sᴘɪᴇʟᴇʀ ᴍɪᴛ ᴅᴇʟᴀʏ");
        addTranslation("help_stop", "sᴛᴏᴘs ᴛʜᴇ sᴇɴᴅɪɴɢ ᴘʀᴏᴄᴇss", "ᴢᴀᴛʀᴢʏᴍᴜᴊᴇ ᴡʏsʏʟᴀɴɪᴇ", "sᴛᴏᴘᴘᴛ ᴅᴇɴ sᴇɴᴅᴇᴠᴏʀɢᴀɴɢ");
        addTranslation("help_gui_keybind", "ᴏᴘᴇɴ ɢᴜɪ ᴋᴇʏʙɪɴᴅ", "sᴋʀᴏᴛ ᴋʟᴀᴡɪsᴢᴏᴡʏ ɢᴜɪ", "ɢᴜɪ-ᴛᴀsᴛᴇɴʙᴇʟᴇɢᴜɴɢ");
        addTranslation("help_gui_current", "ᴄᴜʀʀᴇɴᴛ", "ᴀᴋᴛᴜᴀʟɴʏ", "ᴀᴋᴛᴜᴇʟʟ");
        addTranslation("help_gui_press", "ᴘʀᴇss", "ᴋʟɪᴋɴɪᴊ", "ʜɪᴇʀ ᴋʟɪᴄᴋᴇɴ");
        addTranslation("help_gui_open", "ᴛᴏ ᴏᴘᴇɴ ɢᴜɪ", "ᴀʙʏ ᴏᴛᴡᴏʀᴢʏᴄ ɢᴜɪ", "ᴜᴍ ɢᴜɪ ᴢᴜ ᴏꜰꜰɴᴇɴ");
        addTranslation("help_gui_settings", "ᴋᴇʏʙɪɴᴅ ɪs ᴄʜᴀɴɢᴇᴀʙʟᴇ ɪɴ ᴏᴘᴛɪᴏɴs/ᴋᴇʏʙɪɴᴅs sᴇᴛᴛɪɴɢs!", "sᴋʀᴏᴛ ᴍᴏᴢɴᴀ ᴢᴍɪᴇɴɪᴄ ᴡ ᴏᴘᴄᴊᴀᴄʜ sᴛᴇʀᴏᴡᴀɴɪᴀ!", "ᴛᴀsᴛᴇɴʙᴇʟᴇɢᴜɴɢ ᴋᴀɴɴ ɪɴ ᴅᴇɴ ᴇɪɴsᴛᴇʟʟᴜɴɢᴇɴ ɢᴇᴀɴᴅᴇʀᴛ ᴡᴇʀᴅᴇɴ!");
        addTranslation("help_gui_open_settings", "ᴛᴏ ᴏᴘᴇɴ sᴇᴛᴛɪɴɢs", "ᴀʙʏ ᴏᴛᴡᴏʀᴢʏᴄ ᴜsᴛᴀᴡɪᴇɴɪᴀ", "ᴜᴍ ᴇɪɴsᴛᴇʟʟᴜɴɢᴇɴ ᴢᴜ ᴏꜰꜰɴᴇɴ");
        
        addTranslation("join_loaded", "MassDM loaded successfully.", "MassDM zaladowano pomyslnie.", "MassDM erfolgreich geladen.");
        addTranslation("join_discord", "Join our discord", "Dolacz na nasz discord", "Tritt unserem Discord bei");
        addTranslation("join_commands", "Commands", "Komendy", "Befehle");
        addTranslation("join_gui_keybind", "Open GUI Keybind", "Skrot Klawiszowy GUI", "GUI-Tastenbelegung");
        
        addTranslation("screen_ready", "ʀᴇᴀᴅʏ.", "ɢᴏᴛᴏᴡʏ.", "ʙᴇʀᴇɪᴛ.");
        addTranslation("screen_found_online", "ꜰᴏᴜɴᴅ %d ᴘʟᴀʏᴇʀs ᴏɴʟɪɴᴇ", "ᴢɴᴀʟᴇᴢɪᴏɴᴏ %d ɢʀᴀᴄᴢʏ ᴏɴʟɪɴᴇ", "%d sᴘɪᴇʟᴇʀ ᴏɴʟɪɴᴇ ɢᴇꜰᴜɴᴅᴇɴ");
        addTranslation("screen_enter_msg", "ᴇɴᴛᴇʀ ᴍᴇssᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ!", "ᴡᴘɪsᴢ ᴛʀᴇsᴄ ᴡɪᴀᴅᴏᴍᴏsᴄɪ!", "ɴᴀᴄʜʀɪᴄʜᴛᴇɴɪɴʜᴀʟᴛ ᴇɪɴɢᴇʙᴇɴ!");
        addTranslation("screen_started", "sᴛᴀʀᴛᴇᴅ ᴍᴀss sᴇɴᴅɪɴɢ!", "ᴜʀᴜᴄʜᴏᴍɪᴏɴᴏ ᴍᴀsᴏᴡᴇ ᴡʏsʏʟᴀɴɪᴇ!", "ᴍᴀssᴇɴsᴇɴᴅᴜɴɢ ɢᴇsᴛᴀʀᴛᴇᴛ!");
        addTranslation("screen_stopped", "sᴛᴏᴘᴘᴇᴅ!", "ᴢᴀᴛʀᴢʏᴍᴀɴᴏ!", "ɢᴇsᴛᴏᴘᴘᴛ!");
        addTranslation("screen_in_progress", "sᴇɴᴅɪɴɢ ɪɴ ᴘʀᴏɢʀᴇss...", "ᴡʏsʏʟᴀɴɪᴇ ᴡ ᴛᴏᴋᴜ...", "sᴇɴᴅᴇɴ ʟᴀᴜꜰᴛ...");
        addTranslation("screen_finished", "sᴇɴᴅɪɴɢ ꜰɪɴɪsʜᴇᴅ!", "ᴢᴀᴋᴏɴᴄᴢᴏɴᴏ ᴡʏsʏʟᴀɴɪᴇ!", "sᴇɴᴅᴇɴ ᴀʙɢᴇsᴄʜʟᴏssᴇɴ!");
        addTranslation("screen_message_placeholder", "ᴍᴇssᴀɢᴇ", "ᴡɪᴀᴅᴏᴍᴏsᴄ", "ɴᴀᴄʜʀɪᴄʜᴛ");
        addTranslation("screen_delay", "ᴅᴇʟᴀʏ: %.1f s", "ᴏᴘᴏᴢɴɪᴇɴɪᴇ: %.1f s", "ᴅᴇʟᴀʏ: %.1f s");
        addTranslation("screen_start", "§a§l▶ §2§l sᴛᴀʀᴛ", "§a§l▶ §2§l sᴛᴀʀᴛ", "§a§l▶ §2§l sᴛᴀʀᴛ");
        addTranslation("screen_start_repeat", "§a§l▶ §2§l sᴛᴀʀᴛ ʟᴀsᴛ", "§a§l▶ §2§l sᴛᴀʀᴛ ᴏsᴛᴀᴛɴɪᴇ", "§a§l▶ §2§l ᴡɪᴇᴅᴇʀʜᴏʟᴇɴ");
        addTranslation("screen_close", "§c§l✕ §4§l ᴄʟᴏsᴇ", "§c§l✕ §4§l ᴢᴀᴍᴋɴɪᴊ", "§c§l✕ §4§l sᴄʜʟɪᴇssᴇɴ");
        addTranslation("screen_stop", "§c§l■ §4§l sᴛᴏᴘ", "§c§l■ §4§l sᴛᴏᴘ", "§c§l■ §4§l sᴛᴏᴘ");
        addTranslation("screen_message_content", "§7ᴍᴇssᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ:", "§7ᴛʀᴇsᴄ ᴡɪᴀᴅᴏᴍᴏsᴄɪ:", "§7ɴᴀᴄʜʀɪᴄʜᴛᴇɴɪɴʜᴀʟᴛ:");
        addTranslation("screen_lang", "🌐 ʟᴀɴɢ: ᴇɴ", "🌐 ᴊᴇᴢʏᴋ: ᴘʟ", "🌐 sᴘʀᴀᴄʜᴇ: ᴅᴇ");

        addTranslation("msg_player_removed", "§a§lᴘʟᴀʏᴇʀ %s ᴀᴅᴅᴇᴅ ᴛᴏ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ!", "§a§lɢʀᴀᴄᴢ %s ᴅᴏᴅᴀɴʏ ᴅᴏ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ!", "§a§lsᴘɪᴇʟᴇʀ %s ᴢᴜʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ʜɪɴᴢᴜɢᴇꜰᴜɢᴛ!");
        addTranslation("msg_player_restored", "§a§lᴘʟᴀʏᴇʀ %s ʀᴇᴍᴏᴠᴇᴅ ꜰʀᴏᴍ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ!", "§a§lɢʀᴀᴄᴢ %s ᴜsᴜɴɪᴇᴛʏ ᴢ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ!", "§a§lsᴘɪᴇʟᴇʀ %s ᴠᴏɴ ᴅᴇʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ᴇɴᴛꜰᴇʀɴᴛ!");
        addTranslation("msg_removed_list_empty", "§7ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ ɪs ᴇᴍᴘᴛʏ.", "§7ʟɪsᴛᴀ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ ᴊᴇsᴛ ᴘᴜsᴛᴀ.", "§7ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ɪsᴛ ʟᴇᴇʀ.");
        addTranslation("msg_removed_list", "§7ᴇxᴄʟᴜᴅᴇᴅ ᴘʟᴀʏᴇʀs:", "§7ᴡʏᴋʟᴜᴄᴢᴇɴɪ ɢʀᴀᴄᴢᴇ:", "§7ᴀᴜsɢᴇsᴄʜʟᴏssᴇɴᴇ sᴘɪᴇʟᴇʀ:");
        
        addTranslation("help_removed_list", "sʜᴏᴡs ᴇxᴄʟᴜᴅᴇᴅ ᴘʟᴀʏᴇʀs ʟɪsᴛ", "ᴘᴏᴋᴀᴢᴜᴊᴇ ʟɪsᴛᴇ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ ɢʀᴀᴄᴢʏ", "ᴢᴇɪɢᴛ ᴅɪᴇ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ᴀɴ");
        addTranslation("help_removed_player", "ᴀᴅᴅs ᴘʟᴀʏᴇʀ ᴛᴏ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ", "ᴅᴏᴅᴀᴊᴇ ɢʀᴀᴄᴢᴀ ᴅᴏ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ", "ꜰᴜɢᴛ sᴘɪᴇʟᴇʀ ᴢᴜʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ʜɪɴᴢᴜ");
        addTranslation("help_restore_player", "ʀᴇᴍᴏᴠᴇs ᴘʟᴀʏᴇʀ ꜰʀᴏᴍ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ", "ᴜsᴜᴡᴀ ɢʀᴀᴄᴢᴀ ᴢ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ", "ᴇɴᴛꜰᴇʀɴᴛ sᴘɪᴇʟᴇʀ ᴠᴏɴ ᴅᴇʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ");

        addTranslation("screen_exclude", "§c§lᴇxᴄʟᴜᴅᴇ", "§c§lᴡʏᴋʟᴜᴄᴢ", "§c§lᴀᴜssᴄʜʟɪᴇssᴇɴ");
        addTranslation("screen_view_list", "§8§lʟɪsᴛ", "§8§lʟɪsᴛᴀ", "§8§lʟɪsᴛᴇ");
        addTranslation("screen_online_players", "§a§lᴘʟᴀʏᴇʀs ʟɪsᴛ", "§a§lʟɪsᴛᴀ ɢʀᴀᴄᴢʏ", "§a§lsᴘɪᴇʟᴇʀʟɪsᴛᴇ");
        addTranslation("screen_remove", "§c§lʀᴇᴍᴏᴠᴇ", "§c§lᴜsᴜɴ", "§c§lᴇɴᴛꜰᴇʀɴᴇɴ");
        addTranslation("screen_back", "§7§l◀ ʙᴀᴄᴋ", "§7§l◀ ᴘᴏᴡʀᴏᴛ", "§7§l◀ ᴢᴜʀᴜᴄᴋ");
        addTranslation("screen_player_nick", "§7ᴘʟᴀʏᴇʀ ɴɪᴄᴋ", "§7ɴɪᴄᴋ ɢʀᴀᴄᴢᴀ", "§7ꜱᴘɪᴇʟᴇʀ ɴᴀᴍᴇ");

        addTranslation("placeholder_player", "Target player's nick", "Nick gracza docelowego", "Zielspieler-Name");
        addTranslation("placeholder_me", "Your nick", "Twój nick", "Dein Name");
        addTranslation("placeholder_online", "Online players count", "Ilość graczy online", "Anzahl der Spieler online");
        addTranslation("placeholder_server", "Current server IP", "Aktualny serwer (IP)", "Aktuelle Server-IP");
        addTranslation("placeholder_time", "Current time (e.g. 15:30)", "Aktualna godzina (np. 15:30)", "Aktuelle Zeit (z.B. 15:30)");
        addTranslation("placeholder_date", "Current date (e.g. 28.07.2026)", "Aktualna data (np. 28.07.2026)", "Aktuelles Datum (z.B. 28.07.2026)");
        addTranslation("placeholder_ping", "Your ping (ms)", "Twój ping (ms)", "Dein Ping (ms)");
        addTranslation("placeholder_uuid", "Target player's UUID", "UUID gracza docelowego", "UUID des Zielspielers");
        addTranslation("placeholder_random", "Random string of length X", "Losowy ciąg znaków o dług. X", "Zufälliger String (Länge X)");

        addTranslation("changelog_1", "§a+ New, fully redesigned GUI layout", "§a+ Nowy, całkowicie przeprojektowany interfejs", "§a+ Neues, komplett überarbeitetes GUI-Design");
        addTranslation("changelog_2", "§a+ Better HUD in bottom right corner", "§a+ Lepszy interfejs HUD w prawym dolnym rogu", "§a+ Besseres HUD unten rechts");
        addTranslation("changelog_3", "§a+ Added custom chat message formats", "§a+ Dodano zaawansowane zmienne do wiadomości", "§a+ Benutzerdefiniertes Nachrichtenformat");
        addTranslation("changelog_4", "§a+ Interactive Exclusion List (GUI)", "§a+ Interaktywna lista wykluczonych graczy (GUI)", "§a+ Interaktive Ausschlussliste (GUI)");
        addTranslation("changelog_5", "§a+ Favorite Message Presets (★)", "§a+ Ulubione szablony wiadomości (★)", "§a+ Bevorzugte Nachrichtenvorlagen (★)");
        addTranslation("changelog_6", "§a+ Audio notifications on Start/Stop", "§a+ Powiadomienia dźwiękowe Start/Stop", "§a+ Audiobenachrichtigungen Start/Stopp");
        addTranslation("changelog_7", "§a+ Server-specific settings profiles", "§a+ Profile ustawień przypisane do serwera", "§a+ Serverspezifische Einstellungsprofile");
        addTranslation("changelog_8", "§a+ Quick actions & Multilingual support", "§a+ Szybkie akcje i pełne wsparcie 3 języków", "§a+ Schnellaktionen & Mehrsprachigkeit");
        addTranslation("changelog_9", "§a+ Removed empty gaps between players", "§a+ Usunięto puste luki między graczami na liście", "§a+ Leere Lücken zwischen Spielern entfernt");
        addTranslation("changelog_10", "§c- Removed Auto-Responder for performance", "§c- Usunięto Auto-Respondera (optymalizacja)", "§c- Auto-Responder entfernt (Leistung)");
        addTranslation("changelog_thx", "§7Thank you for using §fMassDM§7!", "§7Dziękujemy za korzystanie z §fMassDM§7!", "§7Danke, dass du §fMassDM§7 benutzt!");
        addTranslation("screen_auto_responder", "🤖 ᴀᴜᴛᴏ-ʀᴇsᴘ: %s", "🤖 ᴀᴜᴛᴏ-ʀᴇsᴘ: %s", "🤖 ᴀᴜᴛᴏ-ʀᴇsᴘ: %s");
        addTranslation("dm_received", "ᴅᴍ ꜰʀᴏᴍ %s!", "ᴡɪᴀᴅᴏᴍᴏsᴄ ᴏᴅ %s!", "ɴᴀᴄʜʀɪᴄʜᴛ ᴠᴏɴ %s!");
        addTranslation("screen_search", "🔍 sᴇᴀʀᴄʜ...", "🔍 sᴢᴜᴋᴀᴊ...", "🔍 sᴜᴄʜᴇɴ...");
        addTranslation("screen_placeholders", "ᴠᴀʀs: {player}, {online}, {server}, {random:4}", "ᴢᴍɪᴇɴɴᴇ: {player}, {online}, {server}, {random:4}", "ᴠᴀʀs: {player}, {online}, {server}, {random:4}");
        addTranslation("screen_placeholders_btn", "ᴘʟᴀᴄᴇʜᴏʟᴅᴇʀs", "ᴢᴍɪᴇɴɴᴇ", "ᴘʟᴀᴄᴇʜᴏʟᴅᴇʀs");
        addTranslation("screen_refresh", "§b§lʀᴇꜰʀᴇsʜ", "§b§lᴏᴅsᴡɪᴇᴢ", "§b§lᴀᴋᴛᴜᴀʟɪsɪᴇʀᴇɴ");
        addTranslation("screen_copy_list", "§d§lᴄᴏᴘʏ ʟɪsᴛ", "§d§lᴋᴏᴘɪᴜᴊ ʟɪsᴛᴇ", "§d§lʟɪsᴛᴇ ᴋᴏᴘɪᴇʀᴇɴ");
        addTranslation("screen_copied", "§a§l✔ ʟɪsᴛ ᴄᴏᴘɪᴇᴅ!", "§a§l✔ sᴋᴏᴘɪᴏᴡᴀɴᴏ!", "§a§l✔ ᴋᴏᴘɪᴇʀᴛ!");
        addTranslation("screen_presets", "§e§l★ ᴘʀᴇsᴇᴛs", "§e§l★ sᴢᴀʙʟᴏɴʏ", "§e§l★ ᴠᴏʀʟᴀɢᴇɴ");
        addTranslation("screen_save_preset", "§a§l+ sᴀᴠᴇ ᴄᴜʀʀᴇɴᴛ", "§a§l+ ᴢᴀᴘɪsᴢ ᴏʙᴇᴄɴᴀ", "§a§l+ ᴀᴋᴛᴜᴇʟʟᴇ sᴘᴇɪᴄʜᴇʀɴ");
        addTranslation("screen_select", "§2§lsᴇʟᴇᴄᴛ", "§2§lᴡʏʙɪᴇʀᴢ", "§2§lᴀᴜsᴡᴀʜʟᴇɴ");
        addTranslation("screen_delete", "§c§lᴅᴇʟᴇᴛᴇ", "§c§lᴜsᴜɴ", "§c§lʟᴏsᴄʜᴇɴ");
        
        // V1.2 Stats & Autopilot
        addTranslation("screen_stats_title", "📊 ʙʀᴏᴀᴅᴄᴀsᴛ ᴄᴏᴍᴘʟᴇᴛᴇᴅ!", "📊 ᴢᴀᴋᴏɴᴄᴢᴏɴᴏ ᴡʏsʏʟᴀɴɪᴇ!", "📊 UBERTRAGUNG ABGESCHLOSSEN!");
        addTranslation("screen_stats_sent", "sᴜᴄᴄᴇss: sᴇɴᴛ ᴛᴏ %d ᴘʟᴀʏᴇʀs.", "sᴜᴋᴄᴇs: ᴡʏsʟᴀɴᴏ ᴅᴏ %d ɢʀᴀᴄᴢʏ.", "ERFOLG: AN %d SPIELER GESENDET.");
        addTranslation("screen_stats_skipped", "sᴋɪᴘᴘᴇᴅ (ɴᴘᴄs/ᴇxᴄʟᴜᴅᴇᴅ): %d", "ᴘᴏᴍɪɴɪᴇᴄɪ (ɴᴘᴄ/ᴡʏᴋʟᴜᴄᴢᴇɴɪ): %d", "UBERSPRUNGEN (NPC/AUSGESCHLOSSEN): %d");
        addTranslation("screen_stats_time", "ᴅᴜʀᴀᴛɪᴏɴ: %s", "ᴄᴢᴀs ᴛʀᴡᴀɴɪᴀ: %s", "DAUER: %s");
        
        addTranslation("screen_autopilot_title", "§6§l🚀 ᴀᴜᴛᴏᴘɪʟᴏᴛ", "§6§l🚀 ᴀᴜᴛᴏᴘɪʟᴏᴛ", "§6§l🚀 AUTOPILOT");
        addTranslation("screen_autopilot_interval", "§7ɪɴᴛᴇʀᴠᴀʟ: §f%d ᴍɪɴ", "§7ɪɴᴛᴇʀᴡᴀʟ: §f%d ᴍɪɴ", "§7INTERVALL: §f%d MIN");
        addTranslation("screen_autopilot_on", "§2§lᴀᴜᴛᴏᴘɪʟᴏᴛ: ᴏɴ", "§2§lᴀᴜᴛᴏᴘɪʟᴏᴛ: ᴡʟ", "§2§lAUTOPILOT: AN");
        addTranslation("screen_autopilot_off", "§c§lᴀᴜᴛᴏᴘɪʟᴏᴛ: ᴏꜰꜰ", "§c§lᴀᴜᴛᴏᴘɪʟᴏᴛ: ᴡʏʟ", "§c§lAUTOPILOT: AUS");
        
        // V1.3 Discord Integration
        addTranslation("screen_discord_title", "§9§l📱 ᴅɪsᴄᴏʀᴅ ɪɴᴛᴇɢʀᴀᴛɪᴏɴ", "§9§l📱 ɪɴᴛᴇɢʀᴀᴄᴊᴀ ᴅɪsᴄᴏʀᴅ", "§9§l📱 DISCORD INTEGRATION");
        addTranslation("screen_discord_on", "§2§lᴅɪsᴄᴏʀᴅ: ᴏɴ", "§2§lᴅɪsᴄᴏʀᴅ: ᴡʟ", "§2§lDISCORD: AN");
        addTranslation("screen_discord_off", "§c§lᴅɪsᴄᴏʀᴅ: ᴏꜰꜰ", "§c§lᴅɪsᴄᴏʀᴅ: ᴡʏʟ", "§c§lDISCORD: AUS");
        addTranslation("screen_discord_test", "§e§lᴛᴇsᴛ ᴡᴇʙʜᴏᴏᴋ", "§e§lᴛᴇsᴛ ᴡᴇʙʜᴏᴏᴋᴀ", "§e§lTEST WEBHOOK");
        addTranslation("screen_discord_url", "§7ᴡᴇʙʜᴏᴏᴋ ᴜʀʟ...", "§7ᴡᴇʙʜᴏᴏᴋ ᴜʀʟ...", "§7WEBHOOK URL...");
    }

    private static void addTranslation(String key, String en, String pl, String de) {
        Map<Language, String> map = new HashMap<>();
        map.put(Language.EN, en);
        map.put(Language.PL, pl);
        map.put(Language.DE, de);
        translations.put(key, map);
    }

    public static String translate(String key, Object... args) {
        String template = translations.getOrDefault(key, new HashMap<>()).getOrDefault(currentLanguage, key);
        if (args != null && args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }

    public static boolean isRunning() {
        return !currentTasks.isEmpty();
    }

    public static String getCurrentServerKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            String addr = client.getCurrentServerEntry().address.toLowerCase().trim();
            return addr.replaceAll("[^a-z0-9._-]", "_");
        }
        return "global";
    }

    public static java.io.File getConfigFile() {
        String serverKey = getCurrentServerKey();
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("massdm_excluded_" + serverKey + ".txt").toFile();
    }

    public static java.io.File getPresetsFile() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("massdm_presets.txt").toFile();
    }

    public static void loadConfig() {
        excludedPlayers.clear();
        java.io.File file = getConfigFile();
        if (!file.exists()) {
            return;
        }
        try {
            excludedPlayers.addAll(java.nio.file.Files.readAllLines(file.toPath()));
        } catch (Exception e) {
            LOGGER.error("Failed to read massdm config", e);
        }

        if (excludedPlayers.remove("pixelmine.pl") || excludedPlayers.remove("pixelmine.pl".toLowerCase())) {
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            java.io.File file = getConfigFile();
            java.nio.file.Files.write(file.toPath(), excludedPlayers);
        } catch (Exception e) {
            LOGGER.error("Failed to save massdm config", e);
        }
    }

    public static void loadPresets() {
        presets.clear();
        java.io.File file = getPresetsFile();
        if (!file.exists()) return;
        try {
            presets.addAll(java.nio.file.Files.readAllLines(file.toPath()));
        } catch (Exception e) {
            LOGGER.error("Failed to read massdm presets", e);
        }
    }

    public static void savePresets() {
        try {
            java.io.File file = getPresetsFile();
            java.nio.file.Files.write(file.toPath(), presets);
        } catch (Exception e) {
            LOGGER.error("Failed to save massdm presets", e);
        }
    }

    public static String formatMessage(String template, String targetPlayer, int onlineCount) {
        if (template == null) return "";
        String result = template;
        
        // Spintax {Option1|Option2}
        java.util.regex.Pattern spintaxPattern = java.util.regex.Pattern.compile("\\{([^{}]+?)\\}");
        java.util.regex.Matcher spintaxMatcher = spintaxPattern.matcher(result);
        java.util.Random rnd = new java.util.Random();
        while (spintaxMatcher.find()) {
            String group = spintaxMatcher.group(1);
            if (group.contains("|")) {
                String[] options = group.split("\\|");
                String chosen = options[rnd.nextInt(options.length)];
                // Escape chosen string to prevent regex matcher from treating $ or \ specially
                String safeChosen = java.util.regex.Matcher.quoteReplacement(chosen);
                result = result.replaceFirst("\\{" + java.util.regex.Pattern.quote(group) + "\\}", safeChosen);
                spintaxMatcher = spintaxPattern.matcher(result);
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();

        result = result.replace("{player}", targetPlayer);
        result = result.replace("{target}", targetPlayer);
        result = result.replace("{online}", String.valueOf(onlineCount));
        
        if (client.player != null) {
            result = result.replace("{me}", client.player.getGameProfile().getName());
            result = result.replace("{ping}", client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()) != null ? 
                    String.valueOf(client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()).getLatency()) : "0");
        } else {
            result = result.replace("{me}", "Player");
            result = result.replace("{ping}", "0");
        }
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        result = result.replace("{time}", String.format("%02d:%02d", now.getHour(), now.getMinute()));
        result = result.replace("{date}", String.format("%02d.%02d.%d", now.getDayOfMonth(), now.getMonthValue(), now.getYear()));

        String serverName = "Singleplayer";
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            serverName = client.getCurrentServerEntry().address;
        }
        result = result.replace("{server}", serverName);
        
        try {
            if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(targetPlayer) != null) {
                result = result.replace("{uuid}", client.getNetworkHandler().getPlayerListEntry(targetPlayer).getProfile().getId().toString());
            } else {
                result = result.replace("{uuid}", java.util.UUID.nameUUIDFromBytes(targetPlayer.getBytes()).toString());
            }
        } catch (Exception e) {
            result = result.replace("{uuid}", "unknown");
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{random:(\\d+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int length = Integer.parseInt(matcher.group(1));
                length = Math.min(Math.max(1, length), 32);
                matcher.appendReplacement(sb, generateRandomAlphaNumeric(length));
            } catch (Exception ignored) {
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String generateRandomAlphaNumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void onInitializeClient() {
        MassDMHud.register();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleIncomingMessage(message.getString());
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                client.execute(() -> {
                    // Block pixelmine.pl
                    net.minecraft.client.network.ServerInfo currentServer = client.getCurrentServerEntry();
                    if (currentServer != null && currentServer.address != null
                            && currentServer.address.toLowerCase().contains("pixelmine.pl")) {
                        handler.getConnection().disconnect(
                            net.minecraft.text.Text.literal("§4Hej! Na §fPIXELMINE.PL§4 nie możesz uzywać §cMassDM")
                        );
                        return;
                    }

                    loadConfig();

                    client.player.sendMessage(Text.literal("§8[§dᴍᴀssᴅᴍ§8] §f" + translate("join_loaded")), false);
                    
                    net.minecraft.text.MutableText discordMsg = Text.literal("§8- §f" + translate("join_discord") + ": §dcode.pixelmine.pl")
                        .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://code.pixelmine.pl")));
                    client.player.sendMessage(discordMsg, false);
                    
                    client.player.sendMessage(Text.literal("§8- §f" + translate("join_commands") + ": §d/massdm help"), false);
                    String keyName = guiKeyBinding.getBoundKeyLocalizedText().getString();
                    client.player.sendMessage(Text.literal("§8- §f" + translate("join_gui_keybind") + "§7: §d" + toSmallCaps(keyName)), false);
                    client.player.sendMessage(Text.literal(""), false);
                });
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("massdm")
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
                    .then(literal("removed-list")
                            .executes(ctx -> {
                                if (excludedPlayers.isEmpty()) {
                                    ctx.getSource().sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_removed_list_empty")));
                                } else {
                                    ctx.getSource().sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_removed_list")));
                                    for (String nick : excludedPlayers) {
                                        ctx.getSource().sendFeedback(Text.literal("§8- §f" + nick));
                                    }
                                }
                                return 1;
                            })
                    )
                    .then(literal("remove-player")
                            .then(argument("nick", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String nick = StringArgumentType.getString(ctx, "nick");
                                        excludedPlayers.add(nick);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_player_removed", nick)));
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("restore-player")
                            .then(argument("nick", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String nick = StringArgumentType.getString(ctx, "nick");
                                        excludedPlayers.remove(nick);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_player_restored", nick)));
                                        return 1;
                                    })
                            )
                    )
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
            
            // V1.2 Autopilot Check
            if (autopilotEnabled && !autopilotMessage.isEmpty() && client.player != null && client.getNetworkHandler() != null) {
                long now = System.currentTimeMillis();
                if (now - lastAutopilotSendTime >= autopilotIntervalMinutes * 60 * 1000L) {
                    List<String> players = client.getNetworkHandler().getPlayerList().stream()
                            .map(e -> e.getProfile().getName())
                            .filter(name -> !excludedPlayers.contains(name))
                            .filter(name -> !autopilotSentPlayers.contains(name))
                            .filter(name -> !name.equals(client.player.getGameProfile().getName()))
                            .collect(Collectors.toList());
                            
                    if (!players.isEmpty()) {
                        String target = players.get(new java.util.Random().nextInt(players.size()));
                        autopilotSentPlayers.add(target);
                        
                        String formattedMsg = formatMessage(autopilotMessage, target, client.getNetworkHandler().getPlayerList().size());
                        String safeMsg = formattedMsg.replaceAll("[^\\x20-\\x7E\\xA1-\\xFF\\u0100-\\u017F]", "").replaceAll("§", "");
                        String bypass = " " + java.util.UUID.randomUUID().toString().substring(0, 4);
                        String cmd = "/msg " + target + " " + safeMsg + bypass;
                        
                        client.player.networkHandler.sendChatCommand(cmd.substring(1));
                        lastAutopilotSendTime = now;
                        LOGGER.info("[MassDM-Autopilot] → {}: {}", target, formattedMsg);
                        
                        sendDiscordWebhook("MassDM Autopilot", "Sent message to **" + target + "**\n\n```" + formattedMsg + "```", 0x00FF00);
                    } else {
                        autopilotSentPlayers.clear(); // Reset if we've messaged everyone online
                    }
                }
            }
        });
        loadPresets();

        LOGGER.info("[MassDM] Mod loaded! Use /massdm help or keybind (default J)");
    }
    
    public static void sendDiscordWebhook(String title, String description, int color) {
        if (!discordIntegrationEnabled || discordWebhookUrl.isEmpty()) return;
        
        executor.submit(() -> {
            try {
                String json = String.format("{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d}]}", 
                        title.replace("\"", "\\\""), 
                        description.replace("\"", "\\\"").replace("\n", "\\n"), 
                        color);
                        
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                        .build();
                        
                java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                LOGGER.error("Failed to send Discord webhook", e);
            }
        });
    }

    private static void handleIncomingMessage(String text) {
        if (text == null || text.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        String myName = client.getSession().getUsername();

        java.util.regex.Pattern dmPattern = java.util.regex.Pattern.compile("(?i)(?:\\[?([a-zA-Z0-9_]{3,16})\\s*(?:->|whispers to|szepta do|od|from)\\s*(?:ja|me|ty|you|mnie)\\]?:?)\\s*(.*)");
        java.util.regex.Matcher matcher = dmPattern.matcher(text.replaceAll("§.", ""));

        if (matcher.find()) {
            String sender = matcher.group(1);
            if (!sender.equalsIgnoreCase(myName)) {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("dm_received", sender)), false);

                        if (autoResponderEnabled) {
                            long now = System.currentTimeMillis();
                            Long lastReply = lastAutoReplyMap.get(sender.toLowerCase());
                            if (lastReply == null || (now - lastReply) > 30000) {
                                lastAutoReplyMap.put(sender.toLowerCase(), now);
                                String replyCmd = "msg " + sender + " " + autoResponderMessage;
                                if (client.player.networkHandler != null) {
                                    client.player.networkHandler.sendChatCommand(replyCmd);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private void listPlayers(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            source.sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_not_connected")));
            return;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        List<String> players = entries.stream()
                .map(e -> e.getProfile().getName())
                .filter(name -> !excludedPlayers.contains(name))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        source.sendFeedback(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_found_players", players.size())));
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

        if (client.player == null || client.getNetworkHandler() == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_not_connected")), false);
            }
            return;
        }

        List<String> players = new ArrayList<>();
        if (client.getNetworkHandler() != null) {
            for (net.minecraft.client.network.PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                String rawName = entry.getProfile().getName();
                String name = rawName.replaceAll("§.", "");
                name = name.replaceAll("[^a-zA-Z0-9_\\-\\.*]", "");
                
                if (!name.isEmpty() && !excludedPlayers.contains(name.toLowerCase()) && !name.equals(client.getSession().getUsername())) {
                    players.add(name);
                }
            }
        }

        skippedPlayersCount = client.getNetworkHandler().getPlayerList().size() - players.size();
        broadcastStartTime = System.currentTimeMillis();
        lastBroadcastTargetCount = players.size();

        if (players.isEmpty()) {
            client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_no_players")), false);
            return;
        }

        long delay = (long) (delayMs * 1000);
        currentSentCount = 0;
        totalTargetCount = players.size();
        currentTargetPlayer = players.isEmpty() ? "" : players.get(0);
        currentDelaySec = delayMs;

        if (client.player != null) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
            client.player.sendMessage(Text.literal(
                    String.format("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_sending", players.size(), delayMs))), false);
        }

        final int[] index = {0};
        final ScheduledFuture<?>[] taskRef = new ScheduledFuture<?>[1];
        taskRef[0] = executor.scheduleAtFixedRate(() -> {
            if (index[0] >= players.size()) {
                if (taskRef[0] != null) {
                    currentTasks.remove(taskRef[0]);
                    taskRef[0].cancel(false);
                }
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_sent", players.size())), false);
                        client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                        
                        long durationMs = System.currentTimeMillis() - broadcastStartTime;
                        long mins = durationMs / 60000;
                        long secs = (durationMs % 60000) / 1000;
                        String timeStr = mins > 0 ? mins + "m " + secs + "s" : secs + "s";
                        
                        sendDiscordWebhook("Broadcast Completed", 
                                "**Success:** Sent to " + players.size() + " players.\n" +
                                "**Skipped (NPC/Excluded):** " + skippedPlayersCount + "\n" +
                                "**Duration:** " + timeStr, 0x00FF00);
                        
                        if (client.currentScreen == null || client.currentScreen instanceof MassDMScreen || client.currentScreen instanceof StatsScreen) {
                            client.setScreen(new StatsScreen(client.currentScreen));
                        }
                    }
                });
                return;
            }

            String target = players.get(index[0]++);
            currentSentCount = index[0];
            currentTargetPlayer = target;

            String formattedMsg = formatMessage(message, target, players.size());
            String safeMsg = formattedMsg.replaceAll("[^\\x20-\\x7E\\xA1-\\xFF\\u0100-\\u017F]", "").replaceAll("§", "");
            String bypass = " " + java.util.UUID.randomUUID().toString().substring(0, 4);
            String cmd = "/msg " + target + " " + safeMsg + bypass;
            client.execute(() -> {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand(cmd.substring(1));
                    client.player.sendMessage(
                            Text.literal("§d[ᴍᴀssᴅᴍ] §f→ §d" + target + " §f(" + index[0] + "/" + players.size() + ")"), false);
                }
            });

            LOGGER.info("[MassDM] → {}: {}", target, formattedMsg);
        }, 0, delay, TimeUnit.MILLISECONDS);
        currentTasks.add(taskRef[0]);
    }

    public static void stopMassDM() {
        for (ScheduledFuture<?> task : currentTasks) {
            task.cancel(true);
        }
        currentTasks.clear();
        currentSentCount = 0;
        totalTargetCount = 0;
        currentTargetPlayer = "";
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
            mc.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_stopped")), false);
        }
    }

    private void sendHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("                §8× §fᴍᴀss§dᴅᴍ §fʜᴇʟᴘ §8×"));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʜᴇʟᴘ §7- §d" + translate("help_menu")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʟɪꜱᴛ §7- §d" + translate("help_list")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ꜱᴇɴᴅ <delay> <msg> §7- §d" + translate("help_send_delay")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ꜱᴛᴏᴘ §7- §d" + translate("help_stop")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʀᴇᴍᴏᴠᴇᴅ-ʟɪꜱᴛ §7- §d" + translate("help_removed_list")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʀᴇᴍᴏᴠᴇ-ᴘʟᴀʏᴇʀ <ɴɪᴄᴋ> §7- §d" + translate("help_removed_player")));
        source.sendFeedback(Text.literal("§f/ᴍᴀꜱꜱᴅᴍ ʀᴇꜱᴛᴏʀᴇ-ᴘʟᴀʏᴇʀ <ɴɪᴄᴋ> §7- §d" + translate("help_restore_player")));
        source.sendFeedback(Text.literal(""));
        String keyName = guiKeyBinding.getBoundKeyLocalizedText().getString();
        source.sendFeedback(Text.literal("§f" + translate("help_gui_keybind") + "§7: §d" + toSmallCaps(keyName)));
    }

    private static String toSmallCaps(String text) {
        String normal = "AĄBCĆDEĘFGHIJKLŁMNOÓPQRSŚTUVWXYZŹŻ";
        String smallcaps = "ᴀᴀʙᴄᴄᴅᴇᴇꜰɢʜɪᴊᴋʟʟᴍɴᴏᴏᴘǫʀꜱꜱᴛᴜᴠᴡxʏᴢᴢᴢ";
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
