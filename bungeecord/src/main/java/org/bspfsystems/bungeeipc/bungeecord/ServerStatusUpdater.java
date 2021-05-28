/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 *
 * Copyright (C) 2020  Matt Ciolkosz (https://github.com/mciolkosz)
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

final class ServerStatusUpdater implements Runnable {
    
    private final BungeeIPCPlugin ipcPlugin;
    private final Logger logger;
    private final TaskScheduler scheduler;
    private final Collection<ServerInfo> servers;
    private final int taskId;
    
    ServerStatusUpdater(@NotNull final BungeeIPCPlugin ipcPlugin) {
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        this.scheduler = this.ipcPlugin.getProxy().getScheduler();
        this.servers = this.ipcPlugin.getProxy().getServers().values();
        this.taskId = this.scheduler.schedule(this.ipcPlugin, this, 15, 15, TimeUnit.SECONDS).getId();
    }
    
    @Override
    public void run() {
        for (final ServerInfo server : this.servers) {
            this.scheduler.runAsync(this.ipcPlugin, () -> updateStatus(server));
        }
    }
    
    void stop() {
        this.scheduler.cancel(this.taskId);
    }
    
    private synchronized void updateStatus(@NotNull final ServerInfo server) {
        
        final Socket socket = new Socket();
        try {
            this.logger.log(Level.CONFIG, "Updating server status for " + server.getSocketAddress().toString());
            socket.connect(server.getSocketAddress(), 500);
            socket.close();
            
            this.scheduler.runAsync(this.ipcPlugin, () -> ipcPlugin.setOnlineStatus(server.getName(), true));
            return;
        } catch (IOException e) {
            this.logger.log(Level.CONFIG, "IOException while updating status.", e);
        }
        this.scheduler.runAsync(this.ipcPlugin, () -> ipcPlugin.setOnlineStatus(server.getName(), false));
    }
}
