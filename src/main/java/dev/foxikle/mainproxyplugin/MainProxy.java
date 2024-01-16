package dev.foxikle.mainproxyplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.foxikle.mainproxyplugin.commands.*;
import dev.foxikle.mainproxyplugin.data.Database;
import dev.foxikle.mainproxyplugin.listeners.*;
import dev.foxikle.mainproxyplugin.managers.ChatManager;
import dev.foxikle.mainproxyplugin.managers.FriendManager;
import dev.foxikle.mainproxyplugin.managers.PartyManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

@Plugin(
        id = "webnetproxycore",
        name = "WebNet Proxy Core",
        version = BuildConstants.VERSION,
        url = "https://foxikle.dev",
        authors = {"Foxikle"},
        dependencies = {
                @Dependency(id = "foxrankvelocity"),
                @Dependency(id = "unsignedvelocity")
        }
)
public class MainProxy {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private YamlDocument config;

    // managers
    private final FriendManager friendManager;
    private final ChatManager chatManager;
    private final PartyManager partyManager;

    private Database database;

    // constants
    public static boolean MAINTENANCE_MODE = false;

    public static final MinecraftChannelIdentifier MAIN_CHANNEL = MinecraftChannelIdentifier.from("webnetproxy:main");
    public static final MinecraftChannelIdentifier LOBBY_REQUEST = MinecraftChannelIdentifier.from("webnetproxy:lobby_request");

    @Inject
    public MainProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        try {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );
            config.update();
            config.save();
        } catch (IOException e) {
            logger.error("Config couldn't be loaded! Shutting down plugin.");
            server.shutdown();
        }
        friendManager = new FriendManager(this);
        chatManager = new ChatManager(this);
        partyManager = new PartyManager(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initializeConstants();
        // database
        database = new Database(this);
        try {
            database.connect();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Invalid Database Credentials!");
            server.shutdown();
        }
        database.createFriendsTable();
        database.createNameTable();

        //plugin message listeners
        server.getChannelRegistrar().register(LOBBY_REQUEST);
        server.getChannelRegistrar().register(MAIN_CHANNEL);


        // commands
        CommandManager cm = server.getCommandManager();
        cm.register(cm.metaBuilder("lobby").aliases("l").plugin(this).build(), new LobbyCommand(server, this));
        cm.register(cm.metaBuilder("maintenancemode").aliases("mm").plugin(this).build(), new MaintenanceModeCommand(server, this));
        cm.register(cm.metaBuilder(FriendCommand.createBrigadierCommand(server, this)).aliases("f").plugin(this).build(), FriendCommand.createBrigadierCommand(server, this));
        cm.register(cm.metaBuilder(ChatChannelCommand.createBrigadierCommand(server, this)).plugin(this).build(), ChatChannelCommand.createBrigadierCommand(server, this));
        cm.register(cm.metaBuilder(PartyCommand.createBrigadierCommand(server, this)).aliases("p").plugin(this).build(), PartyCommand.createBrigadierCommand(server, this));
        cm.register(cm.metaBuilder(FindCommand.createBrigadierCommand(server, this)).plugin(this).build(), FindCommand.createBrigadierCommand(server, this));


        // events
        server.getEventManager().register(this, new JoinListener(this));
        server.getEventManager().register(this, new ChatListener(this));
        server.getEventManager().register(this, new LeaveListener(this));
        server.getEventManager().register(this, new PluginChannelListener(this));
        server.getEventManager().register(this, new ProxyPingListener());

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        database.disconnect();
        server.getChannelRegistrar().unregister(LOBBY_REQUEST);
        server.getChannelRegistrar().unregister(MAIN_CHANNEL);
    }

    public ProxyServer getProxy() {
        return server;
    }

    public void initializeConstants(){
        MAINTENANCE_MODE = config.getBoolean("maintenance_mode");
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public YamlDocument getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public Database getDatabase() {
        return database;
    }
}
