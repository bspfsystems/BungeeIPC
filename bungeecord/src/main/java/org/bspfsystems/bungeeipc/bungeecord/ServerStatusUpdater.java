/*
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
 *
 * Copyright (C) 2020-2021 BSPF Systems, LLC (https://bspfsystems.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bspfsystems.bungeeipc.bungeecord;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a utility that periodically updates the online status of the
 * connected {@link ServerInfo Server}s.
 */
final class ServerStatusUpdater implements Runnable {
    
    private final BungeeIPCPlugin ipcPlugin;
    private final Logger logger;
    private final TaskScheduler scheduler;
    private final Collection<ServerInfo> servers;
    private final int taskId;
    
    /**
     * Constructs a new {@link ServerStatusUpdater}, and schedules a repeating
     * task to update the {@link ServerInfo Server} statuses.
     * 
     * @param ipcPlugin The {@link BungeeIPCPlugin}.
     */
    ServerStatusUpdater(@NotNull final BungeeIPCPlugin ipcPlugin) {
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        this.scheduler = this.ipcPlugin.getProxy().getScheduler();
        this.servers = this.ipcPlugin.getProxy().getServers().values();
        this.taskId = this.scheduler.schedule(this.ipcPlugin, this, 15, 15, TimeUnit.SECONDS).getId();
    }
    
    /**
     * Runs through the {@link Collection} of {@link ServerInfo Server}s,
     * performing updates on their online statuses.
     */
    @Override
    public void run() {
        for (final ServerInfo server : this.servers) {
            this.scheduler.runAsync(this.ipcPlugin, () -> this.updateStatus(server));
        }
    }
    
    /**
     * Cancels the scheduled task.
     */
    void stop() {
        this.scheduler.cancel(this.taskId);
    }
    
    /**
     * Updates the online status for the given {@link ServerInfo Server}.
     * <p>
     * This works by attempting to open a connection (via the configured
     * information in BungeeCord's "config.yml" file), with a timeout of 500
     * milliseconds. If that connection opens, then the
     * {@link ServerInfo Server} is considered online. If an {@link IOException}
     * is thrown (due to reaching the timeout, unable to connect, etc), then the
     * {@link ServerInfo Server} will be considered offline.
     * 
     * @param server The {@link ServerInfo Server} to get the updated online
     *               status on.
     */
    private synchronized void updateStatus(@NotNull final ServerInfo server) {
        
        final Socket socket = new Socket();
        try {
            if (ipcPlugin.isExtraLoggingEnabled()) {
                this.logger.log(Level.INFO, "Updating server status for " + server.getSocketAddress().toString());
            }
            socket.connect(server.getSocketAddress(), 500);
            socket.close();
            
            this.scheduler.runAsync(this.ipcPlugin, () -> this.ipcPlugin.setOnlineStatus(server.getName(), true));
            return;
        } catch (IOException e) {
            if (this.ipcPlugin.isExtraLoggingEnabled()) {
                this.logger.log(Level.INFO, e.getClass().getSimpleName() + " thrown while updating status.", e);
            }
        }
        this.scheduler.runAsync(this.ipcPlugin, () -> this.ipcPlugin.setOnlineStatus(server.getName(), false));
    }
}
