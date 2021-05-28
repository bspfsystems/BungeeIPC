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

package org.bspfsystems.bungeeipc.bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.socket.IPCSocket;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

final class BukkitIPCSocket implements IPCSocket {
    
    private final BukkitIPCPlugin ipcPlugin;
    private final Logger logger;
    private final BukkitScheduler scheduler;
    
    private final InetAddress address;
    private final int port;
    
    private DataOutputStream toBungee;
    private Socket socket;
    
    private final AtomicBoolean running;
    private final AtomicBoolean connected;
    private final AtomicInteger taskId;
    
    BukkitIPCSocket(@NotNull final BukkitIPCPlugin ipcPlugin, @NotNull final YamlConfiguration config) {
        
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        this.scheduler = this.ipcPlugin.getServer().getScheduler();
        
        if (!config.contains("ip_address")) {
            throw new IllegalArgumentException("IPC Client config missing IP address.");
        }
        if (!config.contains("port")) {
            throw new IllegalArgumentException("IPC Client config missing port.");
        }
        
        try {
            this.address = InetAddress.getByName(config.getString("ip_address"));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to decipher IP address from " + config.getString("ip_address"), e);
        }
        
        this.port = config.getInt("port");
        if (this.port < 1024 || this.port > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535, inclusive.");
        }
        
        this.running = new AtomicBoolean(false);
        this.connected = new AtomicBoolean(false);
        this.taskId = new AtomicInteger(-1);
        this.toBungee = null;
    }
    
    @Override
    public boolean isRunning() {
        return this.running.get();
    }
    
    @Override
    public boolean isConnected() {
        return this.running.get() && this.connected.get();
    }
    
    @Override
    public void start() {
        this.logger.log(Level.INFO, "Starting the IPC client socket.");
        this.running.set(true);
        this.taskId.set(this.scheduler.runTaskAsynchronously(this.ipcPlugin, this).getTaskId());
    }
    
    @Override
    public void run() {
        
        try {
            this.logger.log(Level.INFO, "Attempting to connect to the IPC server...");
            this.socket = new Socket(this.address, this.port);
            this.connected.set(true);
            this.logger.log(Level.INFO, "Connected to the IPC server.");
        } catch (IOException e) {
            
            this.logger.log(Level.INFO, "Unable to connect to IPC server.");
            this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.INFO, "Port Number - " + this.port);
            this.logger.log(Level.CONFIG, "IOException thrown.", e);
            
            this.taskId.set(this.scheduler.runTaskLaterAsynchronously(this.ipcPlugin, this, 40).getTaskId());
            return;
        }
        
        try {
            
            this.toBungee = new DataOutputStream(this.socket.getOutputStream());
            final DataInputStream fromBungee = new DataInputStream(this.socket.getInputStream());
            
            while(this.connected.get()) {
                final IPCMessage message = IPCMessage.read(fromBungee.readUTF());
                this.ipcPlugin.getServer().getScheduler().runTask(this.ipcPlugin, () -> ipcPlugin.receiveMessage(message));
            }
        } catch (IOException e) {
            
            this.logger.log(Level.INFO, "IPC connection broken.");
            this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.INFO, "Port Number - " + this.port);
            this.logger.log(Level.CONFIG, "IOException thrown.", e);
    
            try {
                if (this.toBungee != null) {
                    this.toBungee.close();
                }
            } catch (IOException e1) {
                this.logger.log(Level.WARNING, "Failure for IPC client.");
                this.logger.log(Level.WARNING, "Unable to close the DataOutputStream after the IPC connection was broken.");
                this.logger.log(Level.WARNING, "IOException thrown.", e1);
            }
    
            try {
                if (this.socket != null) {
                    this.socket.close();
                }
            } catch (IOException e1) {
                this.logger.log(Level.WARNING, "Failure for IPC client.");
                this.logger.log(Level.WARNING, "Unable to close the Socket after the IPC connection was broken.");
                this.logger.log(Level.WARNING, "IOException thrown.", e1);
            }
    
            this.running.set(false);
            this.connected.set(false);
            this.toBungee = null;
            
            if (this.running.get()) {
                this.taskId.set(this.scheduler.runTaskLaterAsynchronously(this.ipcPlugin, this, 5).getTaskId());
            }
        }
    }
    
    @Override
    public void stop() {
    
        this.logger.log(Level.INFO, "Closing IPC client connection...");
        this.scheduler.cancelTask(this.taskId.get());
    
        try {
            if (this.toBungee != null) {
                this.toBungee.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC client.");
            this.logger.log(Level.WARNING, "Unable to close the DataOutputStream during shutdown.");
            this.logger.log(Level.WARNING, "IOException thrown.", e);
        }
    
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC client.");
            this.logger.log(Level.WARNING, "Unable to close the Socket during shutdown.");
            this.logger.log(Level.WARNING, "IOException thrown.", e);
        }
    
        this.running.set(false);
        this.connected.set(false);
        this.toBungee = null;
        this.logger.log(Level.INFO, "IPC client closed.");
    }
    
    @Override
    public synchronized void sendMessage(@NotNull final IPCMessage message) {
        this.scheduler.runTaskAsynchronously(this.ipcPlugin, () -> send(message));
    }
    
    private synchronized void send(@NotNull final IPCMessage message) {
        
        if (!this.connected.get()) {
            this.logger.log(Level.WARNING, "Unable to send IPC message.");
            this.logger.log(Level.WARNING, "IPC Client not connected.");
            return;
        }
        if (this.toBungee == null) {
            this.logger.log(Level.SEVERE, "Unable to send IPC message.");
            this.logger.log(Level.SEVERE, "IPC Client output to Bungee proxy is null.");
            this.logger.log(Level.SEVERE, "Client check determined that the connection is valid.");
            return;
        }
        
        try {
            this.toBungee.writeUTF(message.write());
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Cannot send IPC message to Bungee proxy.");
            this.logger.log(Level.WARNING, "IOException thrown.", e);
        }
    }
}
