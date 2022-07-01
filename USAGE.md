# Using the BungeeIPC Bukkit & BungeeCord Plugins

## Installation

To obtain the copies of BungeeIPC, please see the related section in [README.md](README.md). Simply drop the appropriate file into the `plugins/` folder for your Bukkit/BungeeCord installation, and then (re-)start the server/proxy.

The currently-supported versions of Bukkit are:
- 1.8.x thru 1.18.x

The currently-supported versions of BungeeCord are:
- 1.8.x thru 1.18.x

_Please Note: These plugins may work with other versions of Bukkit and/or BungeeCord, but they are not guaranteed to._

## Configuration

Please see [CONFIGURATION.md](CONFIGURATION.md) for information on configuring the plugins.

## In-Game Usage / Commands & Permissions

The main purpose of BungeeIPC is to facilitate BungeeCord-Bukkit server communications in downstream plugins.<br />
There are some standalone features that can be used in-game, in the form of commands. The list of commands and their respective descriptions and permission nodes can be seen below.

### Bukkit vs. BungeeCord

Some commands have identical or nearly-identical functionality between the Bukkit and BungeeCord plugins. While a console session will only run the appropriate command for the specific server/proxy, a player in-game will need to verify they run the appropriate command:
- `/ipc <command>` will run the command on the Bukkit server that the player is currently connected to.
- `/ipcb <command>` will run the command on the BungeeCord proxy.
Additionally, if running the Bukkit command, the player may need to verify they are connected to the appropriate server to receive their desired result.

The permissions for the commands differ in the same way. Bukkit's permission nodes will be `bungeeipc.command.ipc[.<node>]`, whereas BungeeCord will use `bungeeipc.command.ipcb[.<node>]`. If required by the permission management system, the Bukkit permissions may need to be duplicated to each server.

Only the Bukkit-based commands, descriptions, and permission nodes will be shown in the following section. If any command differs between Bukkit and BungeeCord, the BungeeCord information will be appended in _italics_. Wherever you see a command or permission node below in the below section, you can replace `/ipc` and `.ipc.` with `/ipcb` and `.ipcb.`, respectively.

### Common Commands

**Base IPC Command:** The base IPC command for all BungeeIPC commands. If this command has no arguments, a list of all subcommands that the sender has permission to use, and their respective syntax, will be displayed. **Please Note:** This permission **MUST** be granted to all that wish to use any IPC subcommand.
- `/ipc` - `bungeeipc.command.ipc`

**Command Command:** The ability to send a command to the opposing IPCPlugin, to be executed by a specified CommandSender on the opposing system (Sent from Bukkit -> Executes in BungeeCord). Specifying a different CommandSender requires additional permission nodes (see below).
- `/ipc command <sender> <command> [args...]` - `bungeeipc.command.ipc.command` _(BungeeCord's version of this command specifies a required `<server>` argument before the CommandSender to direct the command to the specified Bukkit server.)_
- Specify the `console` to execute the Bukkit/BungeeCord command - `bungeeipc.command.ipc.command.player.console`
- Specify another player to execute the Bukkit/BungeeCord command - `bungeeipc.command.ipc.command.player.other`

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
