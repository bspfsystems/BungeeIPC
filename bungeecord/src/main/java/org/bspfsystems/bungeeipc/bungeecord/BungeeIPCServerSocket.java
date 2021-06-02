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

package org.bspfsystems.bungeeipc.bungeecord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.socket.IPCServerSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BungeeIPCServerSocket implements IPCServerSocket {
    
    private final BungeeIPCPlugin ipcPlugin;
    private final Logger logger;
    
    private final String name;
    private final InetAddress address;
    private final int port;
    
    private final SSLServerSocketFactory sslServerSocketFactory;
    private final ArrayList<String> tlsVersionWhitelist;
    private final ArrayList<String> tlsCipherSuiteWhitelist;
    
    private DataOutputStream toBukkit;
    private ServerSocket serverSocket;
    private Socket socket;
    
    private final TaskScheduler scheduler;
    private final AtomicBoolean running;
    private final AtomicBoolean connected;
    private final AtomicInteger taskId;
    
    BungeeIPCServerSocket(@NotNull final BungeeIPCPlugin ipcPlugin, @NotNull final Configuration config, @NotNull final Collection<InetAddress> localAddresses, @Nullable final SSLServerSocketFactory sslServerSocketFactory, @Nullable final ArrayList<String> tlsVersionWhitelist, @Nullable final ArrayList<String> tlsCipherSuiteWhitelist) {
        
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
        
        final String nameValue = config.getString("server_name", null);
        final String addressValue = config.getString("ip_address", null);
        final int portValue = config.getInt("port", -1);
    
        BungeeIPCServerSocket.validateNotNull(nameValue);
        BungeeIPCServerSocket.validateNotNull(addressValue);
        BungeeIPCServerSocket.validateNotBlank(nameValue, "Server name cannot be blank.");
        BungeeIPCServerSocket.validateNotBlank(addressValue, "IP address cannot be blank.");
        if (portValue == -1) {
            throw new IllegalArgumentException("Port must be specified in the config.");
        }
        if (portValue < 1024 || portValue > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535 (inclusive).");
        }
        
        this.name = nameValue;
        try {
            this.address = InetAddress.getByName(addressValue);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to decipher IP address from config value.", e);
        }
        this.port = portValue;
        
        if (!localAddresses.contains(this.address)) {
            throw new IllegalArgumentException("Cannot use network address that is not on the local system.");
        }
        
        this.sslServerSocketFactory = sslServerSocketFactory;
        this.tlsVersionWhitelist = tlsVersionWhitelist;
        this.tlsCipherSuiteWhitelist = tlsCipherSuiteWhitelist;
    
        if (this.sslServerSocketFactory != null) {
            if (this.tlsVersionWhitelist == null) {
                this.logger.log(Level.SEVERE, "SSL is enabled, but the TLS version whitelist is null.");
                this.logger.log(Level.SEVERE, "Unable to set up the IPC Server.");
                throw new RuntimeException("SSL is enabled, but the TLS version whitelist is null.");
            }
            if (this.tlsCipherSuiteWhitelist == null) {
                this.logger.log(Level.SEVERE, "SSL is enabled, but the TLS cipher suite whitelist is null.");
                this.logger.log(Level.SEVERE, "Unable to set up the IPC Server.");
                throw new RuntimeException("SSL is enabled, but the TLS cipher suite whitelist is null.");
            }
        }
        
        this.scheduler = this.ipcPlugin.getProxy().getScheduler();
        this.running = new AtomicBoolean(false);
        this.connected = new AtomicBoolean(false);
        this.taskId = new AtomicInteger(-1);
        this.toBukkit = null;
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
        this.logger.log(Level.INFO, "Starting the IPC server for " + this.name + "...");
        this.running.set(true);
        this.taskId.set(this.scheduler.runAsync(this.ipcPlugin, this).getId());
    }
    
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
                this.connected.set(true);
                this.logger.log(Level.INFO, "IPC server " + this.name + " connected to client.");
                
                final DataInputStream fromBukkit = new DataInputStream(this.socket.getInputStream());
                this.toBukkit = new DataOutputStream(this.socket.getOutputStream());
                
                while(this.connected.get()) {
                    
                    final IPCMessage message = IPCMessage.read(fromBukkit.readUTF());
                    this.scheduler.runAsync(this.ipcPlugin, () -> ipcPlugin.receiveMessage(message));
                }
            } catch(IOException e) {
                
                this.logger.log(Level.INFO, "IPC connection broken.");
                this.logger.log(Level.INFO, "Server Name - " + this.name);
                this.logger.log(Level.INFO, "IP Address  - " + this.address.getHostAddress());
                this.logger.log(Level.INFO, "Port Number - " + this.port);
                this.logger.log(Level.INFO, e.getClass().getSimpleName() + " thrown.", e);
                
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
    
    @Override
    public synchronized void sendMessage(@NotNull final IPCMessage message) {
        this.scheduler.runAsync(this.ipcPlugin, () -> send(message));
    }
    
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
    
    @NotNull
    @Override
    public String getName() {
        return this.name;
    }
    
    @NotNull
    InetAddress getAddress() {
        return this.address;
    }
    
    int getPort() {
        return this.port;
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
