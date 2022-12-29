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

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract implementation of an {@link IPCMessage}.
 */
public abstract class AbstractIPCMessage implements IPCMessage {
    
    protected static final String SEPARATOR = "`|`";
    
    private final String origin;
    private final String destination;
    private final String channel;
    private final Queue<String> data;
    
    private int length;
    
    /**
     * Constructs a new {@link IPCMessage} containing no data.
     * 
     * @param origin The origin {@link IPCSocket}.
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @throws IllegalArgumentException If {@code origin}, {@code destination},
     *                                  and/or {@code channel} are blank.
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, List)
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel) throws IllegalArgumentException, IllegalStateException {
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
     * @throws IllegalArgumentException If {@code origin}, {@code destination},
     *                                  and/or {@code channel} are blank, or if
     *                                  any element in {@code data} is
     *                                  {@code null}.
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, Queue)
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final List<String> data) throws IllegalArgumentException, IllegalStateException {
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
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     */
    protected AbstractIPCMessage(@NotNull final String origin, @NotNull final String destination, @NotNull final String channel, @NotNull final Queue<String> data) throws IllegalArgumentException {
        
        if (origin.trim().isEmpty()) {
            throw new IllegalArgumentException("IPCMessage origin cannot be blank.");
        }
        if (destination.trim().isEmpty()) {
            throw new IllegalArgumentException("IPCMessage destination cannot be blank.");
        }
        if (channel.trim().isEmpty()) {
            throw new IllegalArgumentException("IPCMessage channel cannot be blank.");
        }
        
        for (final String item : data) {
            if (item == null) {
                throw new IllegalArgumentException("IPCMessage data cannot have null items.");
            }
        }
        
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
        
        this.length = this.getLength(this.origin);
        this.length += this.getLength(this.destination);
        this.length += this.getLength(this.channel);
        
        for (final String item : this.data) {
            this.length += this.addLength(item);
        }
        
        this.checkLength(0);
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
    public final void add(@NotNull final String data) throws IllegalStateException {
        
        final int length = this.addLength(data);
        this.checkLength(length);
        
        this.length += length;
        this.data.offer(data);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void add(@NotNull final List<String> data) throws IllegalStateException {
        this.add((Queue<String>) new LinkedList<String>(data));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void add(@NotNull final Queue<String> data) throws IllegalStateException {
        
        int length = 0;
        for (final String item : data) {
            length += this.addLength(item);
        }
        this.checkLength(length);
        
        this.length += length;
        for (final String item : data) {
            this.data.offer(item);
        }
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
        final String data = this.data.poll();
        if (data == null) {
            throw new NoSuchElementException();
        }
        return data;
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
     * Gets the length of the given {@link String} via the same methods as
     * {@code DataOutputStream#writeUTF(String, DataOutput)}.
     * 
     * @param string The {@link String} to get the length of.
     * @return The length of the {@link String}.
     */
    private int getLength(@NotNull final String string) {
        
        final int stringLength = string.length();
        int length = stringLength;
        
        for (int index = 0; index < stringLength; index++) {
            final char c = string.charAt(index);
            if (c >= 0x80 || c == 0) {
                length += (c >= 0x800) ? 2 : 1;
            }
        }
        
        return length;
    }
    
    /**
     * Gets the length of the given {@link String} added to the length of
     * {@link AbstractIPCMessage#SEPARATOR}. This uses the same methods as
     * {@code DataOutputStream#writeUTF(String, DataOutput)} to calculate the
     * length.
     * 
     * @param string The {@link String} to get the length of.
     * @return The length of the {@link String} combined with the length of
     *         {@link AbstractIPCMessage#SEPARATOR}.
     */
    
    private int addLength(@NotNull final String string) {
        
        final String fullString = AbstractIPCMessage.SEPARATOR + string;
        final int stringLength = fullString.length();
        int length = stringLength;
        
        for (int index = 0; index < stringLength; index++) {
            final char c = fullString.charAt(index);
            if (c >= 0x80 || c == 0) {
                length += (c >= 0x800) ? 2 : 1;
            }
        }
        
        return length;
    }
    
    /**
     * Checks the length of this {@link IPCMessage} added to the given length.
     * If the total is too long (greater than 65535), then it will throw an
     * {@link IllegalStateException}, as this {@link IPCMessage} cannot be sent
     * via a {@link DataOutputStream} when its length is greater than that.
     * 
     * @param length The additional length to check.
     * @throws IllegalStateException If the current length of this
     *                               {@link IPCMessage} added to the given
     *                               length is greater than 65535.
     */
    private void checkLength(final int length) throws IllegalStateException {
        if (this.length + length > 65535) {
            throw new IllegalStateException("IPCMessage is too long.");
        }
    }
}
