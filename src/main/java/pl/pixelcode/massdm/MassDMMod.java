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
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final List<ScheduledFuture<?>> currentTasks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public enum Language { EN, PL, DE }
    public static Language currentLanguage = Language.EN;
    private static KeyBinding guiKeyBinding;
    public static final Set<String> excludedPlayers = new java.util.concurrent.ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    
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
        addTranslation("screen_start", "▶ sᴛᴀʀᴛ", "▶ sᴛᴀʀᴛ", "▶ sᴛᴀʀᴛ");
        addTranslation("screen_close", "✕ ᴄʟᴏsᴇ", "✕ ᴢᴀᴍᴋɴɪᴊ", "✕ sᴄʜʟɪᴇssᴇɴ");
        addTranslation("screen_stop", "■ sᴛᴏᴘ", "■ sᴛᴏᴘ", "■ sᴛᴏᴘ");
        addTranslation("screen_message_content", "ᴍᴇssᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ:", "ᴛʀᴇsᴄ ᴡɪᴀᴅᴏᴍᴏsᴄɪ:", "ɴᴀᴄʜʀɪᴄʜᴛᴇɴɪɴʜᴀʟᴛ:");
        addTranslation("screen_lang", "🌐 ʟᴀɴɢ: ᴇɴ", "🌐 ᴊᴇᴢʏᴋ: ᴘʟ", "🌐 sᴘʀᴀᴄʜᴇ: ᴅᴇ");

        addTranslation("msg_player_removed", "ᴘʟᴀʏᴇʀ %s ᴀᴅᴅᴇᴅ ᴛᴏ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ!", "ɢʀᴀᴄᴢ %s ᴅᴏᴅᴀɴʏ ᴅᴏ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ!", "sᴘɪᴇʟᴇʀ %s ᴢᴜʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ʜɪɴᴢᴜɢᴇꜰᴜɢᴛ!");
        addTranslation("msg_player_restored", "ᴘʟᴀʏᴇʀ %s ʀᴇᴍᴏᴠᴇᴅ ꜰʀᴏᴍ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ!", "ɢʀᴀᴄᴢ %s ᴜsᴜɴɪᴇᴛʏ ᴢ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ!", "sᴘɪᴇʟᴇʀ %s ᴠᴏɴ ᴅᴇʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ᴇɴᴛꜰᴇʀɴᴛ!");
        addTranslation("msg_removed_list_empty", "ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ ɪs ᴇᴍᴘᴛʏ.", "ʟɪsᴛᴀ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ ᴊᴇsᴛ ᴘᴜsᴛᴀ.", "ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ɪsᴛ ʟᴇᴇʀ.");
        addTranslation("msg_removed_list", "ᴇxᴄʟᴜᴅᴇᴅ ᴘʟᴀʏᴇʀs:", "ᴡʏᴋʟᴜᴄᴢᴇɴɪ ɢʀᴀᴄᴢᴇ:", "ᴀᴜsɢᴇsᴄʜʟᴏssᴇɴᴇ sᴘɪᴇʟᴇʀ:");
        
        addTranslation("help_removed_list", "sʜᴏᴡs ᴇxᴄʟᴜᴅᴇᴅ ᴘʟᴀʏᴇʀs ʟɪsᴛ", "ᴘᴏᴋᴀᴢᴜᴊᴇ ʟɪsᴛᴇ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ ɢʀᴀᴄᴢʏ", "ᴢᴇɪɢᴛ ᴅɪᴇ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ᴀɴ");
        addTranslation("help_removed_player", "ᴀᴅᴅs ᴘʟᴀʏᴇʀ ᴛᴏ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ", "ᴅᴏᴅᴀᴊᴇ ɢʀᴀᴄᴢᴀ ᴅᴏ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ", "ꜰᴜɢᴛ sᴘɪᴇʟᴇʀ ᴢᴜʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ ʜɪɴᴢᴜ");
        addTranslation("help_restore_player", "ʀᴇᴍᴏᴠᴇs ᴘʟᴀʏᴇʀ ꜰʀᴏᴍ ᴇxᴄʟᴜsɪᴏɴ ʟɪsᴛ", "ᴜsᴜᴡᴀ ɢʀᴀᴄᴢᴀ ᴢ ʟɪsᴛʏ ᴡʏᴋʟᴜᴄᴢᴏɴʏᴄʜ", "ᴇɴᴛꜰᴇʀɴᴛ sᴘɪᴇʟᴇʀ ᴠᴏɴ ᴅᴇʀ ᴀᴜssᴄʜʟᴜssʟɪsᴛᴇ");

        addTranslation("screen_exclude", "ᴇxᴄʟᴜᴅᴇ", "ᴡʏᴋʟᴜᴄᴢ", "ᴀᴜssᴄʜʟɪᴇssᴇɴ");
        addTranslation("screen_view_list", "ʟɪsᴛ", "ʟɪsᴛᴀ", "ʟɪsᴛᴇ");
        addTranslation("screen_remove", "ʀᴇᴍᴏᴠᴇ", "ᴜsᴜɴ", "ᴇɴᴛꜰᴇʀɴᴇɴ");
        addTranslation("screen_back", "◀ ʙᴀᴄᴋ", "◀ ᴘᴏᴡʀᴏᴛ", "◀ ᴢᴜʀᴜᴄᴋ");
        addTranslation("screen_player_nick", "ᴘʟᴀʏᴇʀ ɴɪᴄᴋ", "ɴɪᴄᴋ ɢʀᴀᴄᴢᴀ", "ꜱᴘɪᴇʟᴇʀ ɴᴀᴍᴇ");
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

    public static final java.io.File configFile = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("massdm_excluded.txt").toFile();

    public static void loadConfig() {
        if (configFile.exists()) {
            try {
                excludedPlayers.addAll(java.nio.file.Files.readAllLines(configFile.toPath()));
            } catch (Exception e) {
                LOGGER.error("Failed to load massdm_excluded.txt", e);
            }
        }

        if (excludedPlayers.remove("pixelmine.pl") || excludedPlayers.remove("pixelmine.pl".toLowerCase())) {
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            java.nio.file.Files.write(configFile.toPath(), excludedPlayers);
        } catch (Exception e) {
            LOGGER.error("Failed to save massdm_excluded.txt", e);
        }
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
        
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                client.execute(() -> {
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
        });

        LOGGER.info("[MassDM] Mod loaded! Use /massdm help or keybind (default J)");
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

        if (players.isEmpty()) {
            client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_no_players")), false);
            return;
        }

        long delay = (long) (delayMs * 1000);
        client.player.sendMessage(Text.literal(
                String.format("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_sending", players.size(), delayMs))), false);

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
                    }
                });
                return;
            }

            String target = players.get(index[0]++);
            String safeMsg = message.replaceAll("[^\\x20-\\x7E\\xA1-\\xFF\\u0100-\\u017F]", "").replaceAll("§", "");
            String bypass = " " + java.util.UUID.randomUUID().toString().substring(0, 4);
            String cmd = "/msg " + target + " " + safeMsg + bypass;
            client.execute(() -> {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand(cmd.substring(1));
                    client.player.sendMessage(
                            Text.literal("§d[ᴍᴀssᴅᴍ] §f→ §d" + target + " §f(" + index[0] + "/" + players.size() + ")"), false);
                }
            });

            LOGGER.info("[MassDM] → {}: {}", target, message);
        }, 0, delay, TimeUnit.MILLISECONDS);
        currentTasks.add(taskRef[0]);
    }

    public static void stopMassDM() {
        for (ScheduledFuture<?> task : currentTasks) {
            task.cancel(true);
        }
        currentTasks.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§d[ᴍᴀssᴅᴍ] §f" + translate("msg_stopped")), false);
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