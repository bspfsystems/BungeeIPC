# BungeeIPC

BungeeIPC is a set of APIs and plugins meant for BungeeCord proxies and their backend Bukkit Minecraft Servers. It allows other plugins to use the API to send messages between the servers and proxy without relying on the built-in Minecraft/BungeeCord channels.

## Obtaining BungeeIPC

You can obtain a copy of BungeeIPC via the following methods:
- Download a pre-built copy from the [Releases page](https://github.com/bspfsystems/BungeeIPC/releases/latest/). The latest version is release 3.0.2.
- Build from source (see below).

If you need to use BungeeIPC as a dependency for your project, please see the Development API section below.

### Build from Source

BungeeIPC uses [Apache Maven](https://maven.apache.org/) to build and handle dependencies.

#### Requirements

- Java Development Kit (JDK) 8 or higher
- Git
- Apache Maven

#### Compile / Build

Run the following commands to build the plugins:
```
git clone https://github.com/bspfsystems/BungeeIPC.git
cd BungeeIPC/
mvn clean install
```

The `.jar` files will be located in the `bukkit/target/` folder for the Bukkit plugin, and the `bungeecord/target/` folder for the BungeeCord plugin.

## Usage

Please see [USAGE.md](USAGE.md) for more information on installation and in-game usage.

## Developer API

The main purpose of BungeeIPC is to facilitate sending messages between BungeeCord and Bukkit for downstream plugins. Several API modules have been created that can be used by any downstream plugins to access the capabilities in BungeeIPC.

### Add BungeeIPC as a Dependency

To add BungeeIPC as a dependency to your project, use one of the following common methods (you may use others that exist, these are the common ones):

**Maven:**<br />
Include the following in your `pom.xml` file:<br />
```
<repositories>
  <repository>
    <id>sonatype-repo</id>
    <url>https://oss.sonatype.org/content/repositories/releases/</url>
  </repository>
</repositories>

<!-- For both Bukkit and BungeeCord -->
<dependencies>
  <dependency>
    <groupId>org.bspfsystems.bungeeipc</groupId>
    <artifactId>bungeeipc-common-api</artifactId>
    <version>3.0.2</version>
    <scope>provided</scope>
  </dependency>
</dependencies>

<!-- For Bukkit -->
<dependencies>
  <dependency>
    <groupId>org.bspfsystems.bungeeipc</groupId>
    <artifactId>bungeeipc-client-api</artifactId>
    <version>3.0.2</version>
    <scope>provided</scope>
  </dependency>
</dependencies>

<!-- For BungeeCord -->
<dependencies>
  <dependency>
    <groupId>org.bspfsystems.bungeeipc</groupId>
    <artifactId>bungeeipc-server-api</artifactId>
    <version>3.0.2</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

**Gradle:**<br />
Include the following in your `build.gradle` file:<br />
```
repositories {
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/releases/"
    }
}

// For both Bukkit and BungeeCord
dependencies {
    compileOnly "org.bspfsystems.bungeeipc:bungeeipc-common-api:3.0.2"
}

// For Bukkit
dependencies {
     compileOnly "org.bspfsystems.bungeeipc:bungeeipc-client-api:3.0.2"
}

// For BungeeCord
dependencies {
     compileOnly "org.bspfsystems.bungeeipc:bungeeipc-server-api:3.0.2"
}
```

_**Please Note:** The above examples show both the client-side and server-side APIs as dependencies for their common sides (Bukkit or BungeeCord). You will only need the one specific to the side you are using. You will also need `bungee-common-api` regardless of which side you are developing on._

### API Examples

These are some basic usages of BungeeIPC; for a full scope of what the plugins offer, please see the Javadocs section below.
```
// Obtain the common instance of BungeeIPC
IPCPlugin ipcPlugin = (IPCPlugin) Bukkit.getPluginManager().getPlugin("BungeeIPC");      // For Bukkit
IPCPlugin ipcPlugin = (IPCPlugin) ProxyServer.getPluginManager().getPlugin("BungeeIPC"); // For BungeeCord

// Add an implementation of an IPCReader, registered to the channel "example_channel"
ipcPlugin.addReader("example_channel", exampleIPCReader);

// Send a previously-create IPCMessage
ipcPlugin.sendMessage(ipcMessage);

////////////////////////////////////////////////////////////////

// Obtain the Bukkit-specific instance of BungeeIPC (usually the client)
IPCClientPlugin ipcClientPlugin = (IPCClientPlugin) Bukkit.getPluginManager().getPlugin("BungeeIPC");

// Check if the client is connected, and attempts to restart it if it is not
if (!ipcClientPlugin.isConnected()) {
    ipcClientPlugin.restartClient();
}

////////////////////////////////////////////////////////////////

// Obtain the BungeeCord-specific instance of BungeeIPC (usually the server)
IPCServerPlugin ipcServerPlugin = (IPCServerPlugin) ProxyServer.getPluginManager().getPlugin("BungeeIPC");

// Check if a server with the name "exampleserver" is registered and connected, and attempts to restart it if it is not
if (ipcServerPlugin.isRegisteredServer("exampleserver") && !ipcServerPlugin.isServerConnected("exampleserver")) {
    ipcServerPlugin.restartServer("exampleserver");
}
```

### Javadocs

The API Javadocs can be found [here](https://bspfsystems.org/docs/bungeeipc/), kindly hosted by [javadoc.io](https://javadoc.io/).

## Contributing, Support, and Issues

Please check out [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## Licensing

BungeeIPC uses the following licenses for the respective modules:
- Common / Client / Server APIs - [The Apache License, Version 2.0](https://apache.org/licenses/LICENSE-2.0.html)
- Bukkit / BungeeCord - [The GNU General Public License, Version 3](https://www.gnu.org/licenses/gpl-3.0.en.html)

### Contributions & Licensing

Contributions to the project will remain licensed under the respective module's license, as defined by the particular license. Copyright/ownership of the contributions shall be governed by the license. The use of an open source license in the hopes that contributions to the project will have better clarity on legal rights of those contributions.

_Please Note: This is not legal advice. If you are unsure on what your rights are, please consult a lawyer._
