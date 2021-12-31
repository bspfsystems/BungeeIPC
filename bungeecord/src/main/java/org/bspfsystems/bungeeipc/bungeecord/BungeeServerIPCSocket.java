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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import org.bspfsystems.bungeeipc.api.common.AbstractIPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.bspfsystems.bungeeipc.api.server.ServerIPCSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the BungeeCord implementation of an {@link ServerIPCSocket}.
 */
final class BungeeServerIPCSocket implements ServerIPCSocket {
    
    private final BungeeIPCPlugin ipcPlugin;
    private final Logger logger;
    
    private final String name;
    private final InetAddress address;
    private final int port;
    
    private final InetAddress serverAddress;
    
    private final SSLServerSocketFactory sslServerSocketFactory;
    private final List<String> tlsVersionWhitelist;
    private final List<String> tlsCipherSuiteWhitelist;
    
    private DataOutputStream toBukkit;
    private ServerSocket serverSocket;
    private Socket socket;
    
    private final TaskScheduler scheduler;
    private final AtomicBoolean running;
    private final AtomicBoolean connected;
    private final AtomicInteger taskId;
    
    /**
     * Constructs a new {@link BungeeServerIPCSocket}.
     * 
     * @param ipcPlugin The {@link BungeeIPCPlugin} controlling the
     *                  {@link BungeeServerIPCSocket}.
     * @param name The name to assign to the {@link BungeeServerIPCSocket}.
     * @param config The {@link Configuration} used to configure the IP address
     *               and port to bind the server socket to.
     * @param localAddresses A {@link Collection} of IP addresses that are
     *                       available on the machine.
     * @param sslServerSocketFactory The {@link SSLServerSocketFactory} used for
     *                               SSL/TLS encryption on the connection.
     * @param tlsVersionWhitelist A {@link List} of SSL/TLS versions that the
     *                            {@link BungeeServerIPCSocket} may use.
     * @param tlsCipherSuiteWhitelist A {@link List} of SSL/TLS cipher suites
     *                                that the {@link BungeeServerIPCSocket} may
     *                                use.
     * @throws IllegalArgumentException If there is a configuration error when
     *                                  setting up the
     *                                  {@link BungeeServerIPCSocket}.
     */
    BungeeServerIPCSocket(@NotNull final BungeeIPCPlugin ipcPlugin, @NotNull final String name, @NotNull final Configuration config, @NotNull final Collection<InetAddress> localAddresses, @Nullable final SSLServerSocketFactory sslServerSocketFactory, @NotNull final List<String> tlsVersionWhitelist, @NotNull final List<String> tlsCipherSuiteWhitelist) throws IllegalArgumentException {
        
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        
        final String addressValue = config.getString("bind_address", "localhost");
        final int portValue = config.getInt("bind_port", -1);
    
        BungeeServerIPCSocket.validateNotBlank(name, "Server name cannot be blank.");
        if (name.equalsIgnoreCase(IPCMessage.PROXY_SERVER)) {
            throw new IllegalArgumentException("Server name cannot be the proxy name (" + IPCMessage.PROXY_SERVER + ").");
        }
        if (name.equalsIgnoreCase(IPCMessage.BROADCAST_SERVER)) {
            throw new IllegalArgumentException("Server name cannot be the broadcast name (" + IPCMessage.BROADCAST_SERVER + ").");
        }
        if (name.equalsIgnoreCase(IPCMessage.PLACEHOLDER_SERVER)) {
            throw new IllegalArgumentException("Server name cannot be the placeholder server name (" + IPCMessage.PLACEHOLDER_SERVER + ").");
        }
        final ServerInfo serverInfo = this.ipcPlugin.getProxy().getServerInfo(name);
        if (serverInfo == null) {
            throw new IllegalArgumentException("Server name is not a Minecraft server registered with the BungeeCord proxy.");
        }
        BungeeServerIPCSocket.validateNotBlank(addressValue, "IP address cannot be blank.");
        if (portValue == -1) {
            throw new IllegalArgumentException("Port must be specified in the config.");
        }
        if (portValue < 1024 || portValue > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535 (inclusive).");
        }
        
        this.name = name;
        try {
            this.address = InetAddress.getByName(addressValue);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to decipher IP address from config value.", e);
        }
        this.port = portValue;
        
        if (!localAddresses.contains(this.address)) {
            throw new IllegalArgumentException("Cannot use network address that is not on the local system.");
        }
        
        this.serverAddress = ((InetSocketAddress) serverInfo.getSocketAddress()).getAddress();
        
        this.sslServerSocketFactory = sslServerSocketFactory;
        this.tlsVersionWhitelist = tlsVersionWhitelist;
        this.tlsCipherSuiteWhitelist = tlsCipherSuiteWhitelist;
        
        this.scheduler = this.ipcPlugin.getProxy().getScheduler();
        this.running = new AtomicBoolean(false);
        this.connected = new AtomicBoolean(false);
        this.taskId = new AtomicInteger(-1);
        this.toBukkit = null;
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
        this.logger.log(Level.INFO, "Starting the IPC server for " + this.name + "...");
        this.running.set(true);
        this.taskId.set(this.scheduler.runAsync(this.ipcPlugin, this).getId());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        
        try {
            if (this.sslServerSocketFactory != null) {
                this.serverSocket = this.sslServerSocketFactory.createServerSocket(this.port, 2, this.address);
                ((SSLServerSocket) this.serverSocket).setEnabledProtocols(this.tlsVersionWhitelist.toArray(new String[] {}));
                ((SSLServerSocket) this.serverSocket).setEnabledCipherSuites(this.tlsCipherSuiteWhitelist.toArray(new String[] {}));
            } else {
                this.serverSocket = new ServerSocket(this.port, 2, this.address);
            }
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "IOException thrown while setting up the IPC server.", e);
            throw new RuntimeException(e.getClass().getSimpleName() + " thrown while setting up the IPC server.", e);
        }
        
        while (this.running.get()) {
            try {
                
                this.logger.log(Level.INFO, "IPC server " + this.name + " waiting for client connection...");
                this.socket = this.serverSocket.accept();
                final InetAddress remoteAddress = ((InetSocketAddress) this.socket.getRemoteSocketAddress()).getAddress();
                if (!remoteAddress.equals(this.serverAddress)) {
                    this.logger.log(Level.WARNING, "IPC server " + this.name + " unable to connect: configured address mismatch.");
                    this.logger.log(Level.WARNING, "Registered Minecraft server address: " + this.serverAddress.getHostAddress());
                    this.logger.log(Level.WARNING, "IPC server connected address: " + remoteAddress.getHostAddress());
                    this.stop();
                    return;
                }
                this.connected.set(true);
                this.logger.log(Level.INFO, "IPC server " + this.name + " connected to client.");
                
                final DataInputStream fromBukkit = new DataInputStream(this.socket.getInputStream());
                this.toBukkit = new DataOutputStream(this.socket.getOutputStream());
                
                while (this.connected.get()) {
                    
                    final IPCMessage message = SimpleServerIPCMessage.read(fromBukkit.readUTF(), this.name);
                    this.scheduler.runAsync(this.ipcPlugin, () -> ipcPlugin.receiveMessage(message));
                }
            } catch (IOException e) {
                
                this.logger.log(Level.INFO, "IPC server " + this.name + " connection broken.");
                if (this.ipcPlugin.isExtraLoggingEnabled()) {
                    this.logger.log(Level.INFO, "Server Name - " + this.name);
                    this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
                    this.logger.log(Level.INFO, "Port Number - " + this.port);
                    this.logger.log(Level.INFO, e.getClass().getSimpleName() + " thrown.", e);
                }
                
                try {
                    if (this.toBukkit != null) {
                        this.toBukkit.close();
                    }
                } catch (IOException e1) {
                    this.logger.log(Level.WARNING, "Failure for IPC server " + this.name + ".");
                    this.logger.log(Level.WARNING, "Unable to close the DataOutputStream after the IPC connection was broken.");
                    this.logger.log(Level.WARNING, e1.getClass().getSimpleName() + " thrown.", e1);
                }
                
                try {
                    if (this.socket != null) {
                        this.socket.close();
                    }
                } catch (IOException e1) {
                    this.logger.log(Level.WARNING, "Failure for IPC server " + this.name + ".");
                    this.logger.log(Level.WARNING, "Unable to close the Socket after the connection was broken.");
                    this.logger.log(Level.WARNING, e1.getClass().getSimpleName() + " thrown.", e1);
                }
                
                this.connected.set(false);
                this.toBukkit = null;
            }
        }
    }
    
    /**
     * Represents a simple extension of an {@link AbstractIPCMessage}, used when
     * reading in a serialized {@link IPCMessage}.
     */
    private static class SimpleServerIPCMessage extends AbstractIPCMessage {
        
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
        private SimpleServerIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final Queue<String> data) {
            super(origin, destination, channel, data);
        }
        
        /**
         * Reads in the given raw {@link IPCMessage} (as a {@link String}), and
         * deserializes it into an {@link IPCMessage}.
         *
         * @param message The serialized {@link IPCMessage} as a {@link String}.
         * @param serverName The name of the {@link IPCSocket} the message was
         *                   read in by.
         * @return The deserialized {@link IPCMessage}.
         * @throws IllegalArgumentException If the given message is blank.
         */
        @NotNull
        private static IPCMessage read(@NotNull String message, @NotNull final String serverName) {
            
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
            if (!origin.equals(IPCMessage.PLACEHOLDER_SERVER)) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, invalid origin: " + message);
            }
            
            final String destination = split.poll();
            if (destination == null) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing destination: " + message);
            }
            
            final String channel = split.poll();
            if (channel == null) {
                throw new IllegalArgumentException("Cannot recreate IPCMessage, missing channel: " + message);
            }
            
            return new SimpleServerIPCMessage(serverName, destination, channel, split);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        
        this.logger.log(Level.INFO, "Closing IPC server connection...");
        this.scheduler.cancel(this.taskId.get());
        
        try {
            if (this.toBukkit != null) {
                this.toBukkit.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC server " + this.name + ".");
            this.logger.log(Level.WARNING, "Unable to close the DataOutputStream during shutdown.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
        
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC server " + this.name + ".");
            this.logger.log(Level.WARNING, "Unable to close the Socket during shutdown.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
        
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failure for IPC server " + this.name + ".");
            this.logger.log(Level.WARNING, "Unable to close the ServerSocket during shutdown.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
    
        this.running.set(false);
        this.connected.set(false);
        this.toBukkit = null;
        this.logger.log(Level.INFO, "IPC server closed.");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void sendMessage(@NotNull final IPCMessage message) {
        this.scheduler.runAsync(this.ipcPlugin, () -> this.send(message));
    }
    
    /**
     * Performs the sending of the given {@link IPCMessage} to the proxy.
     *
     * @param message The {@link IPCMessage} to send to the proxy.
     */
    private synchronized void send(@NotNull final IPCMessage message) {
        
        if (!this.isConnected()) {
            this.logger.log(Level.WARNING, "Unable to send IPC message.");
            this.logger.log(Level.WARNING, "IPC server is not connected.");
            return;
        }
        if (this.toBukkit == null) {
            this.logger.log(Level.SEVERE, "Unable to send IPC message.");
            this.logger.log(Level.SEVERE, "IPC server output to Bukkit is null.");
            this.logger.log(Level.SEVERE, "Server check determined that the connection is valid.");
            return;
        }
        
        try {
            this.toBukkit.writeUTF(message.write());
        } catch(IOException e) {
            this.logger.log(Level.WARNING, "Cannot send IPC message to Bukkit server " + this.name);
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the {@link InetAddress} that this {@link BungeeServerIPCSocket} will
     * bind to.
     * 
     * @return The {@link InetAddress} that this {@link BungeeServerIPCSocket}
     *         will bind to.
     */
    @NotNull
    InetAddress getAddress() {
        return this.address;
    }
    
    /**
     * Gets the port number that this {@link BungeeServerIPCSocket} will bind
     * to.
     * 
     * @return The port number that this {@link BungeeServerIPCSocket} will bind
     *         to.
     */
    int getPort() {
        return this.port;
    }
    
    /**
     * Validates that the given {@link String value} is not empty (or only
     * whitespace).
     *
     * @param value The {@link String value} to check for being blank.
     * @param message The error message to display if the value is blank.
     * @throws IllegalArgumentException If the given value is blank.
     */
    private static void validateNotBlank(@Nullable final String value, @NotNull final String message) {
        if (value != null && value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
