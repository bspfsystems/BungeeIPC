# BungeeIPC

BungeeIPC is a set of APIs and plugins meant for BungeeCord proxies and their backend Bukkit Minecraft Servers. It allows other plugins to use the API to send messages between the servers and proxy without relying on the built-in Minecraft/BungeeCord channels.

## Download

You can download the latest version of the plugins from [here](https://github.com/bspfsystems/BungeeIPC/releases/latest/). Please be sure to download both the Bukkit and BungeeCord `.jar` files.

The latest release is 1.0.3.<br />
The latest snapshot is 2.0.0-SNAPSHOT.

## Build from Source

BungeeIPC uses [Apache Maven](https://maven.apache.org/) to build and handle dependencies.

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

The `.jar` files will be located in the `bukkit/target/` folder for the Bukkit plugin, and the `bungeecord/target/` folder for the BungeeCord plugin.

## Installation

Simply drop the appropriate file into the `plugins/` folder for your Bukkit/BungeeCord installation, and then (re-)start the server/proxy.

The currently-supported versions of Bukkit are:
- 1.8.x thru 1.16.x

The currently-supported versions of BungeeCord are:
- 1.8.x thru 1.16.x

_Please Note: These plugins may work with other versions of Bukkit and/or BungeeCord, but they are not guaranteed to._

### Configuration

A default configuration file (`config.yml`) will be created in the respective plugin's data folder when you start the server/proxy for the first time after installing the plugin. You can then edit the configuration file as needed, and then run the reload command to reload the configuration file:
- `/ipc reload` for Bukkit
- `/ipcb reload` for BungeeCord

When new releases of the plugin are made available, the configuration file may update; however, the configuration file in the respective plugin's data folder will not be updated. While we try not to change the configuration file, sometimes it is unavoidable. You may obtain an up-to-date version of the default file from [here](https://bspfsystems.org/config-files/bungeeipc/). You can simply drop the updated file in place of the old one, updating the values to reflect your requirements and/or previous settings. You can then run the reload command in-game to load the updated configuration.

The IPCPlugins can accept alternative names for their respective configuration files, if the default `config.yml` is confusing to keep track of (all configuration files will be in the respective plugin's data folder). The Bukkit plugin will accept `bukkitipc.yml` as the configuration file name, while the BungeeCord plugin will accept `bungeeipc.yml`. More information can be found at the top of the respective plugins' default configuration file (can be viewed [here](https://bspfsystems.org/config-files/bungeeipc/)).

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

## API Usage / Downstream Dependencies

The main purpose of BungeeIPC is to facilitate sending messages between BungeeCord and Bukkit for downstream plugins. A few API modules have been created that can be used by any downstream plugins to access the capabilities in BungeeIPC.

### Adding as a Dependency

To add BungeeIPC as a dependency to your project, use one of the following common methods (you may use others that exist, these are the common ones):

**Maven:**<br />
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
    <artifactId>bungeeipc-client-api</artifactId>
    <version>1.0.3-SNAPSHOT</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
...
```

**Gradle:**<br />
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
    include implementation("org.bspfsystems.bungeeipc:bungeeipc-client-api:${project.bungeeipc_version}")
    ...
}
...
```

Also include the following in your `gradle.properties` file:<br />
```
...
bungeeipc_version = 2.0.0-SNAPSHOT
...
```

_**Please Note:** The above examples show the client-side API as a dependency (commonly used with Bukkit). For the server-side API (usually BungeeCord), replace `bungeeipc-client-api` with `bungeeipc-server-api`._

### Inside the Plugin

Inside your Plugin code, you can gain access to the common BungeeIPC plugin functions via the following means:
- Bukkit: `IPCPlugin ipcPlugin = (IPCPlugin) Bukkit.getPluginManager().getPlugin("IPCPlugin");`
- BungeeCord: `IPCPlugin ipcPlugin = (IPCPlugin) ProxyServer.getPluginManager().getPlugin("IPCPlugin");`

For Bukkit- or BungeeCord-specific API calls, you can obtain the specific type of Plugin:
- Bukkit: `IPCClientPlugin ipcClientPlugin = (IPCClientPlugin) Bukkit.getPluginManager().getPlugin("IPCPlugin");`
- BungeeCord: `IPCServerPlugin ipcServerPlugin = (IPCServerPlugin) ProxyServer.getPluginManager().getPlugin("IPCPlugin");`

### Javadocs

The API Javadocs can be found [here](https://bspfsystems.org/docs/bungeeipc/), hosted by [javadoc.io](https://javadoc.io/).

## Contributing

### Pull Requests

Contributions to the project are welcome. BungeeIPC is a free and open source software project, created in the hopes that the community would find ways to improve it. If you make any improvements or other enhancements to BungeeIPC, we ask that you submit a Pull Request to merge the changes back upstream. We would enjoy the opportunity to give those improvements back to the wider community.

Various types of contributions are welcome, including (but not limited to):
- Security updates / patches
- Bug fixes
- Feature enhancements

We reserve the right to not include a contribution in the project if the contribution does not add anything substantive or otherwise reduces the functionality of BungeeIPC in a non-desirable way. That said, the idea of having free and open source software was that contributions would be accepted, and discussions over a potential contribution are welcome.

For licensing questions, please see the Licensing section.

### Project Layout

BungeeIPC somewhat follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). This is not the definitive coding style of the project. Generally, it is best to try to copy the style of coding found in the class that you are editing.

BungeeIPC contains a few modules:
- **Common API** - The public common API used by other downstream plugins to access the functionality in the plugin. Except for a few cases, there are no implementations within the API itself, and the Bukkit and BungeeCord plugins provide the API.
- **Client API** - The public client-side API used by other downstream plugins to access client-specific functionality in the plugin. The client API depends on the core API.
- **Server API** - The public server-side API used by other downstream plugins to access server-specific functionality in the plugin. The server API depends on the core API.
- **Bukkit** - The Bukkit implementation of the API, which can be run by the Bukkit server and provides the IPC functionality of the client API.
- **BungeeCord** - The BungeeCord implementation of the API, which can be run by the BungeeCord proxy and provides the IPC functionality of the server API.

## Support / Issues

Issues can be reported [here in GitHub](https://github.com/bspfsystems/BungeeIPC/issues/).

### First Steps

Before creating an issue, please search to see if anyone else has reported the same issue. Don't forget to search the closed issues. It is much easier for us (and will get you a faster response) to handle a single issue that affects multiple users than it is to have to deal with duplicates.

There is also a chance that your issue has been resolved previously. In this case, you can (ideally) find the answer to your problem without having to ask (new version of BungeeIPC, configuration update, etc).

### Creating an Issue

If no one has reported the issue previously, or the solution is not apparent, please open a new issue. When creating the issue, please give it a descriptive title (no "It's not working", please), and put as much detail into the description as possible. The more details you add, the easier it becomes for us to solve the issue. Helpful items may include:
- A descriptive title for the issue
- The version of BungeeIPC you are using
- The version of Minecraft you are using
- The Bukkit implementation you are using (CraftBukkit / Spigot / Paper / etc.)
- The BungeeCord implementation you are using (BungeeCord / Waterfall / etc.)
- Logs and/or stack traces
- Any steps to reproducing the issue
- Anything else that might be helpful in solving your issue.

_Note:_ Please redact any Personally-Identifiable Information (PII) when you create your issue. These may appear in logs or stack traces. Examples include (but are not limited to):
- Real names of players / server administrators
- Usernames of accounts on computers (may appear in logs or stack traces)
- IP addresses / hostnames
- etc.

If you are not sure, you can always redact or otherwise change the data.

### Non-Acceptable Issues

Issues such as "I need help" or "It doesn't work" will not be addressed and/or will be closed with no assistance given. These type of issues do not have any meaningful details to properly address the problem. Other issues that will not be addressed and/or closed without help include (but are not limited to):
- How to install BungeeIPC (explained in README)
- How to configure BungeeIPC (explained in README and default configuration)
- How to use BungeeIPC as a dependency (explained in README)
- How to create plugins
- How to set up a development environment
- How to install plugins
- How to create a server
- Other issues of similar nature...

This is not a help forum for server administration or non-project-related coding issues. Other resources, such as [Google](https://www.google.com/), should have answers to most questions not related to BungeeIPC.

## Licensing

BungeeIPC uses the following licenses for the respective modules:
- Common / Client / Server APIs - [The Apache License, Version 2.0](https://apache.org/licenses/LICENSE-2.0.html)
- Bukkit / BungeeCord - [The GNU General Public License, Version 3](https://www.gnu.org/licenses/gpl-3.0.en.html)

### Contributions & Licensing

Contributions to the project will remain licensed under the respective module's license, as defined by the particular license. Copyright/ownership of the contributions shall be governed by the license. The use of an open source license in the hopes that contributions to the project will have better clarity on legal rights of those contributions.

_Please Note: This is not legal advice. If you are unsure on what your rights are, please consult a lawyer._
