/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
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

package org.bspfsystems.bungeeipc.bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.socket.IPCSocket;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BukkitIPCSocket implements IPCSocket {
    
    private final BukkitIPCPlugin ipcPlugin;
    private final Logger logger;
    
    private final InetAddress address;
    private final int port;
    
    private final SSLSocketFactory sslSocketFactory;
    private final ArrayList<String> tlsVersionWhitelist;
    private final ArrayList<String> tlsCipherSuiteWhitelist;
    
    private DataOutputStream toBungee;
    private Socket socket;
    
    private final BukkitScheduler scheduler;
    private final AtomicBoolean running;
    private final AtomicBoolean connected;
    private final AtomicInteger taskId;
    
    BukkitIPCSocket(@NotNull final BukkitIPCPlugin ipcPlugin, @NotNull final YamlConfiguration config, @Nullable final SSLSocketFactory sslSocketFactory, @Nullable final ArrayList<String> tlsVersionWhitelist, @Nullable final ArrayList<String> tlsCipherSuiteWhitelist) {
        
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        
        final String addressValue = config.getString("ip_address", null);
        final int portValue = config.getInt("port", -1);
        
        BukkitIPCSocket.validateNotNull(addressValue);
        BukkitIPCSocket.validateNotBlank(addressValue, "IP address cannot be blank.");
        if (portValue == -1) {
            throw new IllegalArgumentException("Port must be specified in the config.");
        }
        if (portValue < 1024 || portValue > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535, inclusive.");
        }
        
        try {
            this.address = InetAddress.getByName(addressValue);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to decipher IP address from config value.", e);
        }
        this.port = portValue;
        
        
        this.sslSocketFactory = sslSocketFactory;
        this.tlsVersionWhitelist = tlsVersionWhitelist;
        this.tlsCipherSuiteWhitelist = tlsCipherSuiteWhitelist;
        
        if (this.sslSocketFactory != null) {
            if (this.tlsVersionWhitelist == null) {
                this.logger.log(Level.SEVERE, "SSL is enabled, but the TLS version whitelist is null.");
                this.logger.log(Level.SEVERE, "Unable to set up the IPC Client.");
                throw new RuntimeException("SSL is enabled, but the TLS version whitelist is null.");
            }
            if (this.tlsCipherSuiteWhitelist == null) {
                this.logger.log(Level.SEVERE, "SSL is enabled, but the TLS cipher suite whitelist is null.");
                this.logger.log(Level.SEVERE, "Unable to set up the IPC Client.");
                throw new RuntimeException("SSL is enabled, but the TLS cipher suite whitelist is null.");
            }
        }
    
        this.scheduler = this.ipcPlugin.getServer().getScheduler();
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
            
            if (this.sslSocketFactory != null) {
                this.socket = this.sslSocketFactory.createSocket(this.address, this.port);
                ((SSLSocket) this.socket).setEnabledProtocols(this.tlsVersionWhitelist.toArray(new String[] {}));
                ((SSLSocket) this.socket).setEnabledCipherSuites(this.tlsCipherSuiteWhitelist.toArray(new String[] {}));
                ((SSLSocket) this.socket).startHandshake();
            } else {
                this.socket = new Socket(this.address, this.port);
            }
            
            this.connected.set(true);
            this.logger.log(Level.INFO, "Connected to the IPC server.");
        } catch (IOException e) {
            
            this.logger.log(Level.INFO, "Unable to connect to IPC server.");
            this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.INFO, "Port Number - " + this.port);
            this.logger.log(Level.INFO, "IOException thrown.", e);
            
            this.taskId.set(this.scheduler.runTaskLaterAsynchronously(this.ipcPlugin, this, 40).getTaskId());
            return;
        }
        
        try {
            
            this.toBungee = new DataOutputStream(this.socket.getOutputStream());
            final DataInputStream fromBungee = new DataInputStream(this.socket.getInputStream());
            
            while(this.connected.get()) {
                final IPCMessage message = IPCMessage.read(fromBungee.readUTF());
                this.scheduler.runTask(this.ipcPlugin, () -> ipcPlugin.receiveMessage(message));
            }
        } catch (IOException e) {
            
            this.logger.log(Level.INFO, "IPC connection broken.");
            this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.INFO, "Port Number - " + this.port);
            this.logger.log(Level.INFO, "IOException thrown.", e);
    
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
        this.scheduler.runTaskAsynchronously(this.ipcPlugin, () -> this.send(message));
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
    
    private static void validateNotNull(@NotNull final String value) {
        // Do nothing, JetBrains annotations does the work.
    }
    
    private static void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
