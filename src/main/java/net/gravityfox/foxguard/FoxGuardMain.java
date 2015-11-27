/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.gravityfox.foxguard;

import com.google.inject.Inject;
import net.gravityfox.foxguard.commands.*;
import net.gravityfox.foxguard.commands.flagsets.CommandPriority;
import net.gravityfox.foxguard.listener.BlockEventListener;
import net.gravityfox.foxguard.listener.InteractListener;
import net.gravityfox.foxguard.listener.PlayerEventListener;
import net.gravityfox.foxguard.listener.SpawnEntityEventListener;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.living.player.TargetPlayerEvent;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.event.EventManager;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.service.user.UserStorage;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;

import static net.gravityfox.foxguard.util.Aliases.flagSetsAliases;

/**
 * Created by Fox on 8/16/2015.
 * Project: foxguard
 */
@Plugin(id = "foxguard", name = "FoxGuard", version = FoxGuardMain.PLUGIN_VERSION)
public class FoxGuardMain {

    public static final String PLUGIN_VERSION = "0.8.3";

    private static FoxGuardMain instance;


    @Inject
    private Logger logger;
    @Inject
    private Game game;
    @Inject
    private EventManager eventManager;
    @Inject
    @ConfigDir(sharedRoot = true)
    private File configDirectory;

    private SqlService sql;
    private UserStorage userStorage;

    private boolean loaded = false;

    public static FoxGuardMain getInstance() {
        return instance;
    }

    //my uuid - f275f223-1643-4fac-9fb8-44aaf5b4b371

    @Listener
    public void gamePreInit(GamePreInitializationEvent event) {
        instance = this;
        new FGConfigManager();
        FGConfigManager.getInstance().save();
    }

    @Listener
    public void gameInit(GameInitializationEvent event) {
        userStorage = game.getServiceManager().provide(UserStorage.class).get();
        new FGManager(this, game.getServer());

        registerCommands();
        registerListeners();
        configurePermissions();
    }

    @Listener
    public void serverStarted(GameStartedServerEvent event) {
        FGStorageManager.getInstance().initFlagSets();
        FGStorageManager.getInstance().loadFlagSets();
        loaded = true;
        FGStorageManager.getInstance().loadLinks();
        if (FGConfigManager.getInstance().forceLoad)
            FGStorageManager.getInstance().resolveDeferredObjects();
        FGStorageManager.getInstance().writeFlagSets();
        for (World world : game.getServer().getWorlds()) {
            FGStorageManager.getInstance().writeWorld(world);
        }
    }

    @Listener
    public void serverStopping(GameStoppingServerEvent event) {
        FGStorageManager.getInstance().writeFlagSets();
    }

    @Listener
    public void serverStopped(GameStoppedServerEvent event) {
        FGStorageManager.getInstance().purgeDatabases();
        FGConfigManager.getInstance().save();
    }

    @Listener
    public void worldUnload(UnloadWorldEvent event) {
        FGStorageManager.getInstance().writeWorld(event.getTargetWorld());
    }

    @Listener
    public void worldLoad(LoadWorldEvent event) {
        FGManager.getInstance().populateWorld(event.getTargetWorld());
        FGStorageManager.getInstance().initWorld(event.getTargetWorld());
        FGStorageManager.getInstance().loadWorldRegions(event.getTargetWorld());
        if (loaded) {
            if (FGConfigManager.getInstance().forceLoad)
                FGStorageManager.getInstance().resolveDeferredObjects();
            FGStorageManager.getInstance().loadWorldLinks(event.getTargetWorld());
        }
    }

    public DataSource getDataSource(String jdbcUrl) throws SQLException {
        if (sql == null) {
            sql = game.getServiceManager().provide(SqlService.class).get();
        }
        return sql.getDataSource(jdbcUrl);
    }

    private void registerCommands() {
        FGCommandMainDispatcher fgDispatcher = new FGCommandMainDispatcher("/foxguard");
        FGCommandDispatcher fgRegionDispatcher = new FGCommandDispatcher("/foxguard regions");
        FGCommandDispatcher fgFlagSetDispatcher = new FGCommandDispatcher("/foxguard flagsets",
                "Commands spcifically meant for managing FlagSets.");

        fgDispatcher.register(new CommandCreate(), "create", "construct", "new", "make", "define", "mk");
        fgDispatcher.register(new CommandDelete(), "delete", "del", "remove", "rem", "rm", "destroy");
        fgDispatcher.register(new CommandModify(), "modify", "mod", "change", "edit", "update");
        fgDispatcher.register(new CommandLink(), "link", "connect", "attach");
        fgDispatcher.register(new CommandUnlink(), "unlink", "disconnect", "detach");
        fgDispatcher.register(new CommandList(), "list", "ls");
        fgDispatcher.register(new CommandDetail(), "detail", "det", "show");
        fgDispatcher.register(new CommandState(), "state", "current", "cur");
        fgDispatcher.register(new CommandPosition(), "position", "pos", "p");
        fgDispatcher.register(new CommandAdd(), "add", "push");
        fgDispatcher.register(new CommandSubtract(), "subtract", "sub", "pop");
        fgDispatcher.register(new CommandFlush(), "flush", "clear");
        fgDispatcher.register(new CommandAbout(), "about", "info");
        fgDispatcher.register(new CommandTest(), "test");
        fgDispatcher.register(new CommandSave(), "save", "saveall", "save-all");

        fgFlagSetDispatcher.register(new CommandPriority(), "priority", "prio", "level", "rank");

        fgDispatcher.register(fgFlagSetDispatcher, flagSetsAliases);

        game.getCommandDispatcher().register(this, fgDispatcher, "foxguard", "foxg", "fguard", "fg");
    }

    private void registerListeners() {
        eventManager.registerListener(this, TargetPlayerEvent.class, new PlayerEventListener());
        eventManager.registerListener(this, ChangeBlockEvent.class, new BlockEventListener());
        eventManager.registerListener(this, InteractBlockEvent.class, new InteractListener());
        eventManager.registerListener(this, SpawnEntityEvent.class, new SpawnEntityEventListener());
    }

    private void configurePermissions() {
        getPermissionService().getDefaultData().setPermission(SubjectData.GLOBAL_CONTEXT, "foxguard.command.info", Tristate.TRUE);
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

    public UserStorage getUserStorage() {
        return userStorage;
    }

    public File getConfigDirectory() {
        return configDirectory;
    }

    public PermissionService getPermissionService() {
        return game.getServiceManager().provide(PermissionService.class).get();
    }

    public boolean isLoaded() {
        return loaded;
    }
}
