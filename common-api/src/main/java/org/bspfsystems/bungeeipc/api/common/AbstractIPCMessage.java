/* 
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 * 
 * Copyright 2020-2022 BSPF Systems, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bspfsystems.bungeeipc.api.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract implementation of an {@link IPCMessage}.
 */
public abstract class AbstractIPCMessage implements IPCMessage {
    
    protected static final String SEPARATOR = "`|`";
    
    private final String origin;
    private final String destination;
    private final String channel;
    private final Queue<String> data;
    
    /**
     * Constructs a new {@link IPCMessage} containing no data.
     * 
     * @param origin The origin {@link IPCSocket}.
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, List)
     * @throws IllegalArgumentException If {@code origin}, {@code destination},
     *                                  and/or {@code channel} are blank.
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel) throws IllegalArgumentException {
        this(origin, destination, channel, new ArrayList<String>());
    }
    
    /**
     * Constructs a new {@link IPCMessage} containing the initial data in the
     * given {@link List}. Order of the items in the {@link List} will be
     * maintained.
     * 
     * @param origin The origin {@link IPCSocket}.
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @param data The initial data as a {@link List}. Order will be maintained.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, Queue)
     * @throws IllegalArgumentException If {@code origin}, {@code destination},
     *                                  and/or {@code channel} are blank, or if
     *                                  any element in {@code data} is
     *                                  {@code null}.
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final List<String> data) throws IllegalArgumentException {
        this(origin, destination, channel, (Queue<String>) new LinkedList<String>(data));
    }
    
    /**
     * Constructs a new {@link IPCMessage} containing the initial data in the
     * given {@link Queue}. Order of the items in the {@link Queue} will be
     * maintained.
     * 
     * @param origin The origin {@link IPCSocket}.
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @param data The initial data as a {@link Queue}. Order will be
     *             maintained.
     * @throws IllegalArgumentException If {@code origin}, {@code destination},
     *                                  and/or {@code channel} are blank, or if
     *                                  any element in {@code data} is
     *                                  {@code null}.
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final Queue<String> data) throws IllegalArgumentException {
        
        AbstractIPCMessage.validateNotBlank(origin, "IPCMessage origin cannot be blank.");
        AbstractIPCMessage.validateNotBlank(destination, "IPCMessage destination cannot be blank.");
        AbstractIPCMessage.validateNotBlank(channel, "IPCMessage channel cannot be blank.");
        AbstractIPCMessage.validateNotNull(data, "IPC data cannot have null entries: " + data);
        
        if (origin.equals(IPCMessage.BROADCAST_SERVER)) {
            throw new IllegalArgumentException("IPCMessage origin cannot be the broadcast server.");
        }
        if (destination.equals(IPCMessage.PLACEHOLDER_SERVER)) {
            throw new IllegalArgumentException("IPCMessage destination cannot be the placeholder server.");
        }
        
        this.origin = origin;
        this.destination = destination;
        this.channel = channel;
        this.data = data;
        
        this.validateDataLength(null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String getOrigin() {
        return this.origin;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String getDestination() {
        return this.destination;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String getChannel() {
        return this.channel;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void add(@NotNull final String message) throws IllegalArgumentException {
        this.validateDataLength(message);
        this.data.offer(message);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean hasNext() {
        return this.data.peek() != null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String next() throws NoSuchElementException {
        final String message = this.data.poll();
        if (message == null) {
            throw new NoSuchElementException();
        }
        return message;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String write() {
        
        final StringBuilder builder = new StringBuilder();
        builder.append(this.origin);
        builder.append(AbstractIPCMessage.SEPARATOR).append(this.destination);
        builder.append(AbstractIPCMessage.SEPARATOR).append(this.channel);
        
        for (final String item : this.data) {
            builder.append(AbstractIPCMessage.SEPARATOR).append(item);
        }
        
        return builder.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String toString() {
        return this.write();
    }
    
    /**
     * Checks the length of the data contained {@link String} generated by the
     * {@link IPCMessage#write()} method. If the length is larger than 65535,
     * this {@link IPCMessage} will not be able to be sent.
     * 
     * @param message If not {@code null}, the message will be calculated along
     *                with the data already in this {@link IPCMessage}, as if
     *                the message was part of this {@link IPCMessage}. If it is
     *                {@code null}, only the data currently in this
     *                {@link IPCMessage} will be checked.
     * @throws IllegalArgumentException If the length of the data contained in
     *                                  this {@link IPCMessage} is too long.
     */
    private void validateDataLength(@Nullable final String message) throws IllegalArgumentException {
        
        final StringBuilder builder = new StringBuilder();
        builder.append(this.write());
        if (message != null) {
            builder.append(AbstractIPCMessage.SEPARATOR).append(message);
        }
        
        final String data = builder.toString();
        
        // This is from DataOutputStream#writeUTF(String, DataOutput)
        final int length = data.length();
        int checkLength = 0;
        int c = 0;
        
        for (int index = 0; index < length; index++) {
            c = data.charAt(index);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                checkLength++;
            } else if (c > 0x07FF) {
                checkLength += 3;
            } else {
                checkLength += 2;
            }
        }
        
        if (checkLength > 65535) {
            throw new IllegalArgumentException("Encoded IPCMessage is too long: " + checkLength + " bytes.");
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
    protected static void validateNotBlank(@NotNull final String value, @NotNull final String message) throws IllegalArgumentException {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Validates that the given {@link Queue} has no data in it that is
     * {@code null}.
     * 
     * @param data The {@link Queue} of data to check.
     * @param message The error message to display if the given {@link Queue}
     *                has {@code null} data in it.
     * @throws IllegalArgumentException If the given {@link Queue} has
     *                                  {@code null} data in it.
     */
    private static void validateNotNull(@NotNull final Queue<String> data, @NotNull final String message) throws IllegalArgumentException {
        for (final String item : data) {
            if (item == null) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
