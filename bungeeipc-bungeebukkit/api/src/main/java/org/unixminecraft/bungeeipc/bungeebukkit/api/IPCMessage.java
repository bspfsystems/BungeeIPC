/*
 * BungeeIPC BungeeCord/Bukkit plugin for Minecraft
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

package org.unixminecraft.bungeeipc.bungeebukkit.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class IPCMessage {
	
	public static final String BROADCAST_CHANNEL = "BROADCAST";
	public static final String SEPARATOR = "`";
	
	private final String channel;
	private final List<String> messages;
	
	public IPCMessage(final String channel) {
		
		if(channel == null) {
			throw new IllegalArgumentException("Channel cannot be null.");
		}
		if(channel.isEmpty()) {
			throw new IllegalArgumentException("Channel cannot be blank.");
		}
		
		this.channel = channel;
		this.messages = new ArrayList<String>();
	}
	
	public IPCMessage(final String channel, final List<String> messages) {
		
		this(channel);
		
		if(messages == null) {
			return;
		}
		
		for(final String message : messages) {
			this.messages.add(message);
		}
	}
	
	public String getChannel() {
		return channel;
	}
	
	public List<String> getMessages() {
		return new ArrayList<String>(messages);
	}
	
	public boolean add(final String message) {
		
		if(message == null) {
			return false;
		}
		return messages.add(message);
	}
	
	public void add(final int index, final String message) {
		
		if(message == null) {
			return;
		}
		messages.add(index, message);
	}
	
	public String set(final int index, final String message) {
		
		if(message == null) {
			return null;
		}
		return messages.set(index, message);
	}
	
	public boolean addAll(final Collection<String> messages) {
		
		if(messages == null) {
			return false;
		}
		
		boolean added = false;
		for(final String message : messages) {
			
			if(message == null) {
				continue;
			}
			added = this.messages.add(message);
		}
		
		return added;
	}
	
	public boolean addAll(final int startAt, final List<String> messages) {
		
		if(messages == null) {
			return false;
		}
		
		boolean added = false;
		int index = startAt;
		for(final String message : messages) {
			
			if(message == null) {
				continue;
			}
			this.messages.add(index, message);
			added = true;
			index++;
		}
		
		return added;
	}
	
	@Override
	public String toString() {
		
		if(messages.isEmpty()) {
			return channel;
		}
		
		String fullMessage = messages.get(0);
		for(int index = 1; index < messages.size(); index++) {
			fullMessage += SEPARATOR + messages.get(index);
		}
		
		return channel + SEPARATOR + fullMessage;
	}
	
	public static IPCMessage fromString(final String rawMessage) {
		
		final List<String> messageParts = new ArrayList<String>(Arrays.asList(rawMessage.split(SEPARATOR)));
		if(messageParts.size() < 1) {
			throw new IllegalArgumentException("Raw IPCMessage is missing the channel.");
		}
		
		final String channel = messageParts.remove(0);
		return new IPCMessage(channel, messageParts);
	}
}
