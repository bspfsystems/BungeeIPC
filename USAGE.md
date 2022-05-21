# Using the BungeeIPC Bukkit and BungeeCord Plugins

## Installation

To obtain the copies of BungeeIPC, please see the related section in [README.md](README.md). Simply drop the appropriate file into the `plugins/` folder for your Bukkit/BungeeCord installation, and then (re-)start the server/proxy.

The currently-supported versions of Bukkit are:
- 1.8.x thru 1.18.x

The currently-supported versions of BungeeCord are:
- 1.8.x thru 1.18.x

_Please Note: These plugins may work with other versions of Bukkit and/or BungeeCord, but they are not guaranteed to._

## Configuration

A default configuration file (`config.yml`) will be created in the respective plugin's data folder when you start the server/proxy for the first time after installing the plugin. You can then edit the configuration file as needed, and then run the reload command to reload the configuration file:
- `/ipc reload` for Bukkit
- `/ipcb reload` for BungeeCord

When new releases of the plugin are made available, the configuration file may update; however, the configuration file in the respective plugin's data folder will not be updated. While we try not to change the configuration file, sometimes it is unavoidable. You may obtain an up-to-date version of the default file from [here](https://bspfsystems.org/config-files/bungeeipc/). You can simply drop the updated file in place of the old one, updating the values to reflect your requirements and/or previous settings. You can then run the reload command in-game to load the updated configuration.

The IPCPlugins can accept alternative names for their respective configuration files, if the default `config.yml` is confusing to keep track of (all configuration files will be in the respective plugin's data folder). The Bukkit plugin will accept `bukkitipc.yml` as the configuration file name, while the BungeeCord plugin will accept `bungeeipc.yml`.

Information on the various configuration options can be found within the respective configuration files for [Bukkit](https://bspfsystems.org/config-files/bungeeipc/bukkit/) and [BungeeCord](https://bspfsystems.org/config-files/bungeeipc/bungeecord/).

### SSL/TLS Encryption

Security is of an ever-increasing importance when it comes to computer systems, and Minecraft should be no exception. With that in mind, we have added the ability to use SSL/TLS to encrypt the data while it is in transit between the proxy and the servers.

By default, the configuration files have encryption disabled. **We highly recommend changing this.** This can be changed by simply changing `use_ssl` to `true` on the Bukkit side, and reloading the configuration. On the BungeeCord side, some additional settings will have to be changed, including adding the Java KeyStore file location, as well as the password for the KeyStore, and then reloading.

The default encryption protocols, ciphers, and other settings should be sufficient for most users; however, we recognize that increased security may be necessary. Java 8 supports various protocols and ciphers, which may change over time. You may change the settings to be appropriate for your needs.

**DISCLAIMER:** WHILE WE OFFER SOLUTIONS TO ENCRYPT THE IPC TRAFFIC BETWEEN THE BungeeCord PROXY AND THE Bukkit SERVERS, WE CAN NOT GUARANTEE THAT THE METHODS ARE FOOLPROOF. RESEARCHERS, HACKERS, AND OTHERS FIND NEW SECURITY VULNERABILITIES QUITE OFTEN, AND PROTOCOLS AND CIPHERS THAT WERE PREVIOUSLY DEEMED SECURE MAY BECOME DEPRECATED. IT IS UP TO THE PERSON(S) IMPLEMENTING THE PLUGINS TO ENSURE THE SECURITY OF THEIR DATA IF IT IS SENSITIVE IN NATURE.

The value in free and open source software is that it can be audited by the community, and they can offer solutions to improve it. This includes updating the security aspects of the software, which further benefits the community that uses it. We ask that any improvements that strengthen the security of the plugins are available via Pull Requests so that all may benefit from it.

## In-Game Usage / Commands & Permissions

The main purpose of BungeeIPC is to facilitate BungeeCord-Bukkit server communications in downstream plugins.<br />
There are some standalone features that can be used in-game, in the form of commands. The list of commands and their respective descriptions and permission nodes can be seen below.

### Bukkit vs. BungeeCord

Some commands have (nearly-)identical functionality between the Bukkit and BungeeCord plugins. While running the commands from a console session will not require differentiation, an in-game player will require that:
- `/ipc <command>` will run the command on the Bukkit server that the player is currently playing on.
- `/ipcb <command>` will run the command on the BungeeCord proxy.

Additionally, the permissions for the commands differ in the same way. Bukkit's permission nodes will be `bungeeipc.command.ipc[.<node>]`, whereas BungeeCord will use `bungeeipc.command.ipcb[.<node>]`.

Only the Bukkit-based commands, descriptions, and permission nodes will be shown in the following section. If any command differs between Bukkit and BungeeCord, the BungeeCord information will be appended in _italics_. Wherever you see a command or permission node below in the below section, you can replace `/ipc` and `.ipc.` with `/ipcb` and `.ipcb.`, respectively.

### Common Commands

**Base IPC Command:** The base IPC command for all BungeeIPC commands. If this command has no arguments, a list of all subcommands that the sender has permission to use, and their respective syntax, will be displayed. **Please Note:** This permission **MUST** be granted to all that wish to use any IPC subcommand.
- `/ipc` - `bungeeipc.command.ipc`

**Command Command:** The ability to send a command to the opposing IPCPlugin, to be executed by a specified CommandSender on the opposing system (Sent from Bukkit -> Executes in BungeeCord). Specifying a different CommandSender requires additional permission nodes (see below).
- `/ipc command <sender> <command> [args...]` - `bungeeipc.command.ipc.command` _(BungeeCord's version of this command specifies a required `<server>` argument before the CommandSender to direct the command to the specified Bukkit server.)_
- Specify the `console` to execute the BungeeCord command - `bungeeipc.command.ipc.command.player.console`
- Specify another player to execute the BungeeCord command - `bungeeipc.command.ipc.command.player.other`

**Status Command:** Gives an overview of the IPC connection status (if the IPCClient is (not) enabled and/or if it is connected to its respective IPCServer or not). _(BungeeCord's version of this command has more information embedded in the status colors. Please see the `/server` command in the BungeeCord-Specific Commands subsection for more information.)_
- `/ipc status` - `bungeeipc.command.ipc.status` _(BungeeCord's version of this command takes an optional `<server>` argument at the end to query a specific server.)_

**Reconnect Command:** Disconnects and re-connects the IPCClient from its respective IPCServer.
- `/ipc reconnect` - `bungeeipc.command.ipc.reconnect` _(BungeeCord's version of this command takes a required `<server>` argument at the end to specify a specific IPCServer to disconnect and reconnect.)_

**Reload Command:** Reloads the configuration file, re-creating the IPCClient and reconnecting it to its respective IPCServer. This can be used if any of the configuration information has changed, such as IP address, port, SSL/TLS settings, etc. It can also be used if there was a general configuration file update.
- `/ipc reload` - `bungeeipc.command.ipc.reload`

**Help Command:** Displays all available (sub-)commands that the CommandSender has permission to use, if any.
- `/ipc help` - `bungeeipc.command.ipc.help`

### Bukkit-Specific Commands

These commands only exist with the Bukkit plugin, and do not have an equivalent command in the BungeeCord plugin.

_There are no Bukkit-specific commands at this time._

### BungeeCord-Specific Commands

These commands only exist with the BungeeCord plugin, and do not have an equivalent command in the Bukkit plugin.

**Server Command:** This replaces the `/server` command that comes with BungeeCord. It retains the functionality of listing out the servers defined in BungeeCord's `config.yml` file, and allowing players to teleport between servers with it. It adds to that functionality by listing the servers on their own lines, increasing readability. Additionally, it colors the server names based on their online status and IPC connection status.
- `/server [server name]` - `bungeeipc.command.server`
- The colors of the server names can be seen below:
    - Gray: No information on the server
    - Red: Defined in BungeeCord's `config.yml`, not connected/online
    - Blue: Defined in BungeeCord's `config.yml`, online, not defined in the IPCPlugin's `config.yml`
    - Gold/Orange: Defined in both places, but the IPC service has not started yet (rare to see)
    - Yellow: Defined in both places, but the IPC service has not yet connected to the opposing IPC service (rare to see in BungeeCord, can be seen in Bukkit if BungeeCord is down)
    - Green: Defined in both places, the IPC service has connected successfully and is ready to transfer data
