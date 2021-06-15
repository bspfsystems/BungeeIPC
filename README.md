# BungeeIPC

BungeeIPC is an API and set of plugins meant for BungeeCord proxies and their backend Bukkit Minecraft Servers. It allows other plugins to use the API to send messages between the BungeeCord proxy and the Bukkit Minecraft servers without relying on the built-in Minecraft/BungeeCord channels.

## Download

You can download the latest version of the plugins from [here](https://github.com/bspfsystems/BungeeIPC/releases/latest/). Please be sure to grab both the BungeeCord and Bukkit `.jar` files.

The latest release is 1.0.2.<br />
The latest snapshot is 1.0.2-SNAPSHOT.

## Build from Source

BungeeIPC uses Apache Maven to build and handle dependencies.

### Requirements

- Java Development Kit (JDK) 8 or higher
- Git
- Apache Maven

### Compile / Build

Run the following commands to build the plugins:

```
git clone https://github.com/bspfsystems/BungeeIPC.git
cd BungeeIPC/
mvn clean install
```

The `.jar` files will be located in `bukkit/target/` for Bukkit and `bungeecord/target` for BungeeCord.

## Installation

Simply drop the appropriate file into the `plugins/` folder for your BungeeCord/Bukkit installation, and then start the proxy or server.

### Configuration

A default configuration file (`config.yml`) will be created when you start the proxy/server for the first time after installing the plugin. You can then edit the configuration file as needed, and then run the reload command to reload the configuration file:<br />
- `/ipc reload` for Bukkit
- `/ipcb reload` for BungeeCord

When new releases of the plugin are made available, the configuration file may be updated. While we try not to change the configuration file, sometimes it is unavoidable. The configuration file will not be automatically updated to reflect those changes; however, you may obtain an up-to-date version of the default file from [here](https://bspfsystems.org/config-files/bungeeipc/). You can simply drop the updated file in place of your old one, updating the values to reflect your requirements, and run the reload command to load the new configuration.

The IPCPlugins can accept alternative names for their respective configuration files, if the default `config.yml` is confusing to keep track of (all configuration files will be in the respective plugin's data folder). The Bukkit plugin will accept `bukkitipc.yml`, while the BungeeCord plugin will accept `bungeeipc.yml`. More information can be found at the top of the respective plugins' default configuration file (can be viewed [here](https://bspfsystems.org/config-files/bungeeipc/)).

### SSL/TLS Encryption

Security is of an ever-increasing importance when it comes to computer systems, and Minecraft should be no exception. With that in mind, we have added the ability to use SSL/TLS to encrypt the data while it is in transit between the proxy and the servers.

By default, encryption is turned off in the configuration files. <strong>We highly recommend changing this.</strong><br />
This can be changed by simply changing `use_ssl` to `true` on the Bukkit side, and reloading the configuration. On the BungeeCord side, some additional settings will have to be changed, including adding the Java KeyStore file location, as well as the password for the KeyStore, and then reloading.

The default encryption protocols, ciphers, and other settings should be sufficient for most users; however, we recognize that increased security may be necessary. Java 8 supports various protocols and ciphers, which may change over time. You may change the settings to be appropriate for your needs.

<strong>DISCLAIMER:</strong> WHILE WE OFFER SOLUTIONS TO ENCRYPT THE IPC TRAFFIC BETWEEN THE BungeeCord PROXY AND THE Bukkit SERVERS, WE CAN NOT GUARANTEE THAT THE METHODS ARE FOOLPROOF. SECURITY VULNERABILITIES ARE FOUND EVERY DAY, AND PROTOCOLS AND CIPHERS THAT WERE PREVIOUSLY DEEMED SECURE MAY BECOME DEPRECATED. IT IS UP TO THE PERSON(S) IMPLEMENTING THE PLUGINS TO ENSURE THE SECURITY OF THEIR DATA IF IT IS SENSITIVE IN NATURE.<br />
The value in free and open source software is that it can be audited by the community, and they can offer solutions to improve the software, including the security aspects of it, which further benefits the community that uses the software. We ask that any contributions that strengthen the security of the plugins be made available via pull requests so that all may benefit from it.

## In-Game Usage / Commands & Permissions

The main purpose of BungeeIPC is to facilitate BungeeCord-Bukkit server communications in downstream plugins.<br />
However, there are some standalone features that can be used in-game, in the form of commands. The list of commands and their respective descriptions and permission nodes are listed below.

### BungeeCord vs. Bukkit

Some of the commands are extremely similar between the BungeeCord and Bukkit plugins. While running the commands from the respective console does not require differentiation between the proxy and the server, a logged-in player needs a way to differentiate the commands:

- `/ipc <command>` will run the command on the Bukkit server that the player is currently attached to
- `/ipcb <command>` will run the command on the BungeeCord proxy

Additionally, the permissions for the commands differ in the same way. Bukkit's permission nodes will be `bungeeipc.command.ipc[.<node>]`, whereas BungeeCord will have `bungeeipc.command.ipcb[.<node>]`.

Only the Bukkit-based commands and permissions will be shown in the Common Commands to save explaining the same thing twice. If any command description differs from Bukkit to BungeeCord, the BungeeCord information will be appended in <em>italics</em>. Wherever you see a command or permission node below in the Command Commands subsection, you can replace `/ipc` and `.ipc.` with `/ipcb` and `.ipcb.`, respectively.

### Common Commands

<strong>Base IPC Command:</strong> The base IPC command for all of the common commands. If this command is used with no arguments, a list of all subcommands (that the sender has permission to use) and their syntax's will be displayed. <strong>Please Note:</strong> This permission must be grated to all that wish to use any IPC subcommand.
- `/ipc` - `bungeeipc.command.ipc`

<strong>Command Command:</strong> The ability to send a command to the opposing IPCPlugin, to be executed by a specified CommandSender on the opposing system (Sent from Bukkit -> Executes in BungeeCord). Additional permissions are required if the specified CommandSender is not the player sending the command.
- `/ipc command <sender> <command> [args...]` - `bungeeipc.command.ipc.command` <em>(BungeeCord's version of this command specifies a required `<server>` argument before the CommandSender to direct the command to the specified Bukkit server.)</em>
- Specify the `console` to execute the BungeeCord command - `bungeeipc.command.ipc.command.player.console`
- Specify another player to execute the BungeeCord command - `bungeeipc.command.ipc.command.player.other`

<strong>Status Command:</strong> Gives an overview of the IPC connection status (if the IPCClient is (not) enabled and/or if it is connected to its respective IPCServer or not). <em>(BungeeCord's version of this command has more information embedded in the status colors. Please see the `/server` command in the BungeeCord-Specific Commands subsection for more information.)</em>
- `/ipc status` - `bungeeipc.command.ipc.status` <em>(BungeeCord's version of this command takes an optional `<server>` argument at the end to query a specific server.)</em>

<strong>Reconnect Command:</strong> Disconnects and re-connects the IPCClient from its respective IPCServer.
- `/ipc reconnect` - `bungeeipc.command.ipc.reconnect` <em>(BungeeCord's version of this command takes a required `<server>` argument at the end to specify a specific IPCServer to disconnect and reconnect.)</em>

<strong>Reload Command:</strong> Reloads the configuration file, re-creating the IPCClient and reconnecting it to its respective IPCServer. This can be used if any of the connection information (such as IP address or port) has changed, or other uses, such as a SSL/TLS configuration change, or a general configuration file update.
- `/ipc reload` - `bungeeipc.command.ipc.reload`

### Bukkit-Specific Commands

These commands only exist with the Bukkit plugin, and do not have an equivalent command in the BungeeCord plugin.

<em>There are no Bukkit-specific commands at this time.</em>

### BungeeCord-Specific Commands

These commands only exist with the BungeeCord plugin, and do not have an equivalent command in the Bukkit plugin.

<strong>Server Command:</strong> This replaces the `/server` command that comes with BungeeCord. It retains the functionality of listing out the servers defined in BungeeCord's `config.yml` file, and allowing players to teleport between servers with it. It adds to that functionality by listing the servers on their own lines, increasing readability. Additionally, it colors the server names based on their online status and IPC connection status.
- `/server [server name]` - `bungeeipc.command.server`
- The colors of the server names are defined below:
    - Gray: No information on the server
    - Red: Defined in BungeeCord's `config.yml`, not connected/online
    - Blue: Defined in BungeeCord's `config.yml`, online, not defined in the IPCPlugin's `config.yml`
    - Gold/Orange: Defined in both places, but the IPC service has not started yet (rare to see)
    - Yellow: Defined in both places, but the IPC service has not yet connected to the opposing IPC service (rare to see in BungeeCord, can be seen in Bukkit if BungeeCord is down)
    - Green: Defined in both places, the IPC service has connected successfully and is ready to transfer data

## API Usage / Downstream Dependencies

The main purpose of BungeeIPC is to facilitate sending messages between BungeeCord and Bukkit for downstream plugins. An API has been created that can be used by the downstream plugins to access the capabilities in BungeeIPC.

### Adding as a Dependency

To add BungeeIPC as a dependency to your project, use one of the following common methods (you can use others if they exist, these are the common ones):

<strong>Maven:</strong><br />
Include the following in your `pom.xml` file:<br />
```
...
<repositories>
  <repository>
    <id>sonatype-repo</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
  </repository>
  ...
</repositories>

...

<dependencies>
  <dependency>
    <groupId>org.bspfsystems.bungeeipc</groupId>
    <artifactId>bungeeipc-api</artifactId>
    <version>1.0.2-SNAPSHOT</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
...
```

<strong>Gradle:</strong><br />
Include the following in your `build.gradle` file:<br />
```
...
repositories {
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
        ...
    }
    ...
}

...

dependencies {
    include implementation("org.bspfsystems.bungeeipc:bungee-api:${project.bungeeipc_version}")
    ...
}
...
```

Also include the following in your `gradle.properties` file:<br />
```
...
bungeeipc_version = 1.0.2-SNAPSHOT
...
```

### Inside the Plugin

Inside your Plugin code, you can gain access to the basic BungeeIPC plugin functionality via the following means:
- Bukkit: `IPCPlugin ipcPlugin = (IPCPlugin) Bukkit.getPluginManager().getPlugin("IPCPlugin");`
- BungeeCord: `IPCPlugin ipcPlugin = (IPCPlugin) ProxyServer.getPluginManager().getPlugin("IPCPlugin");`

For client- or server-specific API calls, you can obtain the specific type of Plugin:
- Bukkit: `IPCClientPlugin ipcClientPlugin = (IPCClientPlugin) Bukkit.getPluginManager().getPlugin("IPCPlugin");`
- BungeeCord: `IPCServerPlugin ipcServerPlugin = (IPCServerPlugin) ProxyServer.getPluginManager().getPlugin("IPCPlugin");`

### Javadocs

The API Javadocs can be found [here](https://javadoc.io/doc/org.bspfsystems.bungeeipc/bungeeipc-api/), hosted by [javadoc.io](https://javadoc.io/).

## Contributing

### Pull Requests

Contributions are welcome to the project. BungeeIPC was made as free and open source software in the hopes that the community would find ways to improve the project. If you make any changes or improvements to BungeeIPC that you believe would help or otherwise benefit others that use it, we ask that you submit a Pull Request to merge the changes back upstream.

Various types of contributions are welcome, including (but not limited to):
- Security updates / patches
- Bug fixes
- Feature enhancements
- and more...


We do reserve the right to not include a contribution in the project, if the contribution does not add anything substantive or otherwise reduces the functionality of BungeeIPC in a non-desirable way. That said, the idea of having free and open source software was that contributions would be accepted, and discussions over a potential contribution are welcome.

For licensing questions, please see the Licensing section.

### Project Layout

BungeeIPC somewhat follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). This is not the definitive coding style of the project. Generally, it is best to try to copy the style of coding found in the class that you are editing.

BungeeIPC is split up into a few modules:
- <strong>API</strong> - The public API used by other downstream plugins to access the functionality in the plugin. Except for a few cases, there are no implementations within the API itself, and the API is provided by the plugins.
- <strong>Bukkit</strong> - The Bukkit implementation of the API, which can be run by the Bukkit server and provides the IPC functionality of the API.
- <strong>BungeeCord</strong> - The BungeeCord implementation of the API, which can be run by the BungeeCord proxy and provides the IPC functionality of the API.

## Support / Issues

Issues can be reported [here in GitHub](https://github.com/bspfsystems/BungeeIPC/issues/).

### First Steps

Before creating an issue, please search to see if anyone else has reported the same issue. It is much easier to handle a single issue that affects multiple users than to determine that multiple submitted issues have the same root cause and spend time closing those issues, only to have more of the same ones appear.

There is also a chance that the issue may have been resolved, in which case, you can (ideally) find the answer to your problem without having to ask (new version of BungeeIPC, configuration update, etc).

### Creating an Issue

If no one has reported the issue previously, or the solution is not apparent, please describe your issue in detail.

### Non-Acceptable Issues

Issues such as "I need help" and "It doesn't work" will not be addressed, as they do not have any meaningful details to properly address the problem.

Additionally, issues asking how to include BungeeIPC (or other plugins) as a dependency, how to set up your development environment, how to run a server, how to use plugins, and others of similar nature will be closed. This is not a help forum for general coding or server administration. Other resources, such as [Google](https://www.google.com/), should have answers to most questions not related to BungeeIPC.

## Licensing

BungeeIPC is licensed under 2 open source licenses: the Apache License, Version 2.0 [(link)](https://apache.org/licenses/LICENSE-2.0.html) and the GNU General Public License, Version 3 [(link)](https://www.gnu.org/licenses/gpl-3.0.en.html). Specifically:

- The BungeeIPC API is licensed under the Apache 2.0 license, allowing others to implement the API under the terms of that license.
- The Bukkit and BungeeCord implementations of the API are licenses under the GPL v3 license, to keep these particular implementations as open-source as possible.

Copies of the respective licenses are linked. In the event that the links break, you may find the licenses on the respective sites.

### Contributions & Licensing

Contributions to the project will remain licensed under their respective license(s), as defined in the license. Copyright/ownership of the contribution(s) shall be governed by the respective license for the contribution. The project is licensed under open source licenses in the hopes that contributions to the project will have better clarity on legal rights of those contributions.

THIS IS NOT LEGAL ADVICE. IF YOU ARE UNSURE OF YOUR RIGHTS WHEN CONTRIBUTING TO THE PROJECT, PLEASE CONSULT A LAWYER.