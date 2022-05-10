/* 
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
 * 
 * Copyright (C) 2020-2022 BSPF Systems, LLC (https://bspfsystems.org/)
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.bspfsystems.bungeeipc.api.common.AbstractIPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.client.ClientIPCSocket;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the Bukkit implementation of an {@link ClientIPCSocket}.
 */
final class BukkitClientIPCSocket implements ClientIPCSocket {
    
    private final BukkitIPCPlugin ipcPlugin;
    private final Logger logger;
    
    private final InetAddress address;
    private final int port;
    
    private final SSLSocketFactory sslSocketFactory;
    private final List<String> tlsVersionWhitelist;
    private final List<String> tlsCipherSuiteWhitelist;
    
    private DataOutputStream toBungee;
    private Socket socket;
    
    private final BukkitScheduler scheduler;
    private final AtomicBoolean running;
    private final AtomicBoolean connected;
    private final AtomicInteger taskId;
    
    /**
     * Constructs a new {@link BukkitClientIPCSocket}.
     * 
     * @param ipcPlugin The {@link BukkitIPCPlugin} controlling the
     *                  {@link BukkitClientIPCSocket}.
     * @param config The {@link YamlConfiguration} used to configure the IP
     *               address and port to connect to.
     * @param sslSocketFactory The {@link SSLSocketFactory} used for SSL/TLS
     *                         encryption on the connection.
     * @param tlsVersionWhitelist A {@link List} of SSL/TLS versions that the
     *                            {@link BukkitClientIPCSocket} may use.
     * @param tlsCipherSuiteWhitelist A {@link List} of SSL/TLS cipher suites
     *                                that the {@link BukkitClientIPCSocket} may
     *                                use.
     * @throws IllegalArgumentException If there is a configuration error when
     *                                  setting up the
     *                                  {@link BukkitClientIPCSocket}.
     */
    BukkitClientIPCSocket(@NotNull final BukkitIPCPlugin ipcPlugin, @NotNull final YamlConfiguration config, @Nullable final SSLSocketFactory sslSocketFactory, @NotNull final List<String> tlsVersionWhitelist, @NotNull final List<String> tlsCipherSuiteWhitelist) throws IllegalArgumentException {
        
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        
        final String addressValue = config.getString("bungeecord_ip", "localhost");
        final int portValue = config.getInt("port", -1);
        
        BukkitClientIPCSocket.validateNotBlank(addressValue, "IP address cannot be blank.");
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
        
        this.scheduler = this.ipcPlugin.getServer().getScheduler();
        this.running = new AtomicBoolean(false);
        this.connected = new AtomicBoolean(false);
        this.taskId = new AtomicInteger(-1);
        this.toBungee = null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return this.running.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return this.running.get() && this.connected.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        this.logger.log(Level.INFO, "Starting the IPC client socket.");
        this.running.set(true);
        this.taskId.set(this.scheduler.runTaskAsynchronously(this.ipcPlugin, this).getTaskId());
    }
    
    /**
     * {@inheritDoc}
     */
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
            this.logger.log(Level.CONFIG, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.CONFIG, "Port Number - " + this.port);
            this.logger.log(Level.CONFIG, e.getClass().getSimpleName() + " thrown.", e);
            
            this.taskId.set(this.scheduler.runTaskLaterAsynchronously(this.ipcPlugin, this, 40).getTaskId());
            return;
        }
        
        try {
            
            this.toBungee = new DataOutputStream(this.socket.getOutputStream());
            final DataInputStream fromBungee = new DataInputStream(this.socket.getInputStream());
            
            while(this.connected.get()) {
                final IPCMessage message = SimpleClientIPCMessage.read(fromBungee.readUTF());
                this.scheduler.runTask(this.ipcPlugin, () -> ipcPlugin.receiveMessage(message));
            }
        } catch (IOException e) {
            
            this.logger.log(Level.INFO, "IPC connection broken.");
            this.logger.log(Level.CONFIG, "IP Address  - " + this.address.getHostAddress());
            this.logger.log(Level.CONFIG, "Port Number - " + this.port);
            this.logger.log(Level.CONFIG, e.getClass().getSimpleName() + " thrown.", e);
            
            try {
                if (this.toBungee != null) {
                    this.toBungee.close();
                }
            } catch (IOException e1) {
                this.logger.log(Level.WARNING, "Failure for IPC client.");
                this.logger.log(Level.WARNING, "Unable to close the DataOutputStream after the IPC connection was broken.");
                this.logger.log(Level.WARNING, e1.getClass().getSimpleName() + " thrown.", e1);
            }
            
            try {
                if (this.socket != null) {
                    this.socket.close();
                }
            } catch (IOException e1) {
                this.logger.log(Level.WARNING, "Failure for IPC client.");
                this.logger.log(Level.WARNING, "Unable to close the Socket after the IPC connection was broken.");
                this.logger.log(Level.WARNING, e1.getClass().getSimpleName() + " thrown.", e1);
            }
            
            this.connected.set(false);
            this.toBungee = null;
            
            if (this.running.get()) {
                this.taskId.set(this.scheduler.runTaskLaterAsynchronously(this.ipcPlugin, this, 5).getTaskId());
            }
        }
    }
    
    /**
     * Represents a simple extension of an {@link AbstractIPCMessage}, used when
     * reading in a serialized {@link IPCMessage}.
     */
    private static class SimpleClientIPCMessage extends AbstractIPCMessage {
        
        /**
         * Constructs a new {@link IPCMessage}.
         *
         * @param origin The origin {@link IPCSocket}.
         * @param destination The destination {@link IPCSocket}.
         * @param channel The channel the {@link IPCMessage} will be read by.
         * @param data The initial data as a {@link Queue}. Order will be
         *             maintained.
         * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, Queue)
         * @throws IllegalArgumentException If {@code origin},
         *                                  {@code destination}, and/or
         *                                  {@code channel} are blank, or if any
         *                                  element in {@code data} is
         *                                  {@code null}.
         */
        private SimpleClientIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final Queue<String> data) {
            super(origin, destination, channel, data);
        }
        
        /**
         * Reads in the given raw {@link IPCMessage} (as a {@link String}), and
         * deserializes it into an {@link IPCMessage}.
         * 
         * @param message The serialized {@link IPCMessage} as a {@link String}.
         * @return The deserialized {@link IPCMessage}.
         * @throws IllegalArgumentException If the given message is blank.
         */
        @NotNull
        private static IPCMessage read(@NotNull String message) {
            
            AbstractIPCMessage.validateNotBlank(message, "IPCMessage data cannot be blank, cannot recreate IPCMessage: " + message);
            final Queue<String> split = new LinkedList<String>();
            
            int index = message.indexOf(AbstractIPCMessage.SEPARATOR);
            while (index != -1) {
                split.add(message.substring(0, index));
                message = message.substring(index + AbstractIPCMessage.SEPARATOR.length());
                index = message.indexOf(AbstractIPCMessage.SEPARATOR);
            }
            split.add(message);
            
            if (split.size() < 3) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing some combination of origin, destination, and/or channel (data not required): " + message);
            }
            
            final String origin = split.poll();
            if (origin == null) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing origin: " + message);
            }
            
            final String destination = split.poll();
            if (destination == null) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing destination: " + message);
            }
            
            final String channel = split.poll();
            if (channel == null) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing channel: " + message);
            }
            
            return new SimpleClientIPCMessage(origin, destination, channel, split);
        }
    }
    
    /**
     * {@inheritDoc}
     */
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
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
        
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC client.");
            this.logger.log(Level.WARNING, "Unable to close the Socket during shutdown.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
        
        this.running.set(false);
        this.connected.set(false);
        this.toBungee = null;
        this.logger.log(Level.INFO, "IPC client closed.");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void sendMessage(@NotNull final IPCMessage message) {
        this.scheduler.runTaskAsynchronously(this.ipcPlugin, () -> this.send(message));
    }
    
    /**
     * Performs the sending of the given {@link IPCMessage} to the proxy.
     * 
     * @param message The {@link IPCMessage} to send to the proxy.
     */
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
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
    }
    
    /**
     * Validates that the given {@link String value} is not empty (or only
     * whitespace).
     * 
     * @param value The {@link String value} to check for being blank.
     * @param message The error message to display if the value is blank.
     * @throws IllegalArgumentException If the given value is blank.
     */
    private static void validateNotBlank(@Nullable final String value, @NotNull final String message) throws IllegalArgumentException {
        if (value != null && value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
