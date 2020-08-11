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

package org.bspfsystems.bungeeipc.bungeeproxy;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.TaskScheduler;

final class ServerStatusUpdater implements Runnable {
	
	private final BungeeIPCPlugin ipcPlugin;
	private final TaskScheduler scheduler;
	private final Collection<ServerInfo> servers;
	
	private final int taskId;
	
	ServerStatusUpdater(final BungeeIPCPlugin ipcPlugin) {
		
		this.ipcPlugin = ipcPlugin;
		this.scheduler = ipcPlugin.getProxy().getScheduler();
		this.servers = ipcPlugin.getProxy().getServers().values();
		
		this.taskId = scheduler.schedule(ipcPlugin, this, 15, 15, TimeUnit.SECONDS).getId();
	}
	
	@Override
	public void run() {
		
		for(final ServerInfo serverInfo : servers) {
			scheduler.runAsync(ipcPlugin, new Runnable() {
				
				@Override
				public void run() {
					updateStatus(serverInfo);
				}
			});
		}
	}
	
	void stop() {
		scheduler.cancel(taskId);
	}
	
	private void updateStatus(final ServerInfo serverInfo) {
		
		final Socket socket = new Socket();
		try {
			socket.connect(serverInfo.getSocketAddress(), 500);
			socket.close();
			ipcPlugin.setOnlineStatus(serverInfo.getName(), true);
		}
		catch(IOException e) {
			// Ignored.
		}
		ipcPlugin.setOnlineStatus(serverInfo.getName(), false);
	}
}
