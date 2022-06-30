# Configuring BungeeIPC for Bukkit and BungeeCord

A default configuration file (`config.yml`) will be created in each of the respective plugin's data folder when you start the Bukkit server/BungeeCord proxy for the first time after installing BungeeIPC. You can then edit the configuration files as needed, and then run the reload command to reload the files:
- `/ipc reload` for Bukkit
- `/ipcb reload` for BungeeCord

When new releases of the plugin are made available, the default configuration files in the repository may be updated; however, the configuration files in the plugin data folders will not be updated. While we try not to change the configuration files, sometimes it is unavoidable. You may obtain an up-to-date version of the default file from [here (Bukkit)](https://bspfsystems.org/config-files/bungeeipc/bukkit/) or [here (BungeeCord)](https://bspfsystems.org/config-files/bungeeipc/bungeecord/). You can simply drop the updated file in place of the old one, updating the values to reflect your requirements and/or previous settings. You can then run the reload command in-game to load the updated configuration.

The BungeeIPC plugin can accept alternative names for its configuration files, if it is preferred to not use the default `config.yml` (all configuration files will be located in the respective plugin's data folder). The acceptable alternative names are `bukkitipc.yml` for the Bukkit plugin, and `bungeeipc.yml` for the BungeeCord plugin.

**A Note about SSL/TLS Encryption:**

Security is of an ever-increasing importance when it comes to computer systems, and Minecraft should be no exception. With that in mind, we have added the ability to use SSL/TLS to encrypt the IPC data while it is in transit between the proxy and the servers.

By default, the configuration files have encryption disabled. **We highly recommend changing these settings.** This can be changed by simply updating `use_ssl` to `true` on the Bukkit side, and reloading the configuration. On the BungeeCord side, some additional settings will have to be changed, including the Java KeyStore file location, as well as the password for the KeyStore. The BungeeCord BungeeIPC plugin configuration file can then be reloaded.

The default encryption protocols, ciphers, and other settings should be sufficient for most users; however, we recognize that increased security may be necessary. Java 8 supports various protocols and ciphers, which may change over time. You may change the settings to be appropriate for your needs.

**DISCLAIMER:** WHILE WE OFFER SOLUTIONS TO ENCRYPT THE IPC TRAFFIC BETWEEN THE BungeeCord PROXY AND THE Bukkit SERVERS, WE CAN NOT GUARANTEE THAT THE METHODS ARE FOOLPROOF. RESEARCHERS, HACKERS, AND OTHERS FIND NEW SECURITY VULNERABILITIES QUITE OFTEN, AND PROTOCOLS AND CIPHERS THAT WERE PREVIOUSLY DEEMED SECURE MAY BECOME DEPRECATED. IT IS UP TO THE PERSON(S) INSTALLING AND CONFIGURING THE BungeeIPC PLUGINS TO ENSURE THE SECURITY OF THEIR DATA IF IT IS SENSITIVE IN NATURE. ADDITIONALLY, ANY PLUGIN DEVELOPERS THAT USE BungeeIPC AS A DEPENDENCY SHOULD BE AWARE OF WHAT DATA THEY MAY SEND OVER THE IPC CONNECTION(S), AND BE AWARE THAT SOME DATA THAT IS SENSITIVE IN NATURE MAY REQUIRE EXTRA CARE OR ALTERNATIVE METHODS BEFORE SENDING.

The value in fee and open source software is that it can be audited by the community, and they can offer solutions to improve it. This includes updating the security aspects of the software, which further benefits the community that uses it. We ask that any improvements that strengthen the security of the plugins are made available via Pull Requests so that all may benefit from it.

## Bukkit Configuration Options

This section is for the Bukkit plugin. For BungeeCord, please check further down the page for "BungeeCord Configuration Options". The settings and their respective defaults are listed first in each section.

### General Plugin Settings

```
logging_level: "INFO"
```

- **logging_level:**
  - This is the logging level for the BungeeIPC plugin logger. It will *only* change the logging level for the plugin's logger.
  - NOTE: You may need to update `spigot.yml` to enable debugging for the trace and debug levels to display in the log files.
  - The available levels and their respective Log4j levels are:
    - Java Logger | Log4J
    - SEVERE      | ERROR
    - WARNING     | WARN
    - INFO        | INFO
    - CONFIG      | DEBUG
    - FINE        | TRACE
    - FINER       | TRACE
    - FINEST      | TRACE
  - The default value is "INFO", but a null or empty value will use the default value.

### IPC Client Configuration

```
bungeecord_ip: ""
port: -1
```

- **bungeecord_ip:**
  - This is the hostname or IP address of the BungeeCord server.
  - This should be the hostname or IP address that the BungeeIPC plugin is configured for on the BungeeCord proxy. This may or may not be the same address that players used to connect to the BungeeCord network, depending on your particular server setup.
    - Ex: BungeeCord and Bukkit on the same physical server - The IP address will most likely be a local IP, whereas the IP that players connect to will be the public IP
    - Ex: BungeeCord and Bukkit on different servers - The IP address may be different, depending on the backend networking setup (public vs. private networks for the servers).
  - If you are using SSL/TLS, it is highly recommended to use the same hostname as the server certificate, as hostname validation may occur during the SSL handshake.
  - If no hostname or IP address is specified, "localhost" will be used (127.0.0.1).
  - An invalid hostname or IP address will throw an Exception.
  - The default is blank, which translates to "localhost".
- **port:**
  - This is the port number used to connect to the BungeeCord server.
  - This should be the port number that the BungeeIPC plugins is configured for on the BungeeCord proxy. The combination of the above hostname/IP address and this port number should be unique relative to any other configured BungeeIPC instances. It also should not match any combination configured for the operation of BungeeCord and its backend Minecraft server(s) itself.
  - The port number must be between 1024 and 65535 (inclusive).
  - If no port number is specified, or a value outside the specified range is specified, an Exception will be thrown. Please note that if you leave the default port number (-1) in place, this will cause an Exception to be thrown.
  - The default is -1, which will throw an Exception if left unchanged.

### Global SSL/TLS Settings

_**IMPORTANT:** Please take note of which settings should be mirrored in the BungeeIPC configuration file for the BungeeCord plugin. Failure to ensure mirrored settings may lead to a failure to connect securely and/or a failure for the plugin to load properly._

```
use_ssl: false
```

- **use_ssl:**
  - THIS SETTING MUST BE MIRRORED WITH THE BungeeIPC CONFIGURATION ON THE BungeeCord PROXY.
  - This determines whether SSL/TLS will be used to protect the connection between the Bukkit server and the BungeeCord proxy.
  - NOTE: While this setting's label would indicate the ability to use SSL, which has been superseded by TLS, this setting is simply to enable the SSL/TLS features below.
  - DISCLAIMER: While it is much easier to not use SSL/TLS, having SSL/TLS disabled should only be used in a testing environment. It is _highly_ recommended to use encryption between the BungeeIPC plugins on the Bukkit server and BungeeCord proxy, even if they are running on the same physical machine, no matter how "unimportant" the data is that traverses the IPC connection.
  - If no value is specified, the default value ("false") will be used.
  - The default value is "false", but a null, empty, or otherwise invalid value will be treated as "false".

### Basic SSL/TLS Settings

_NOTE: These settings will be ignored if `use_ssl` is set to `false`._

```
ssl_context_protocol: "TLS"
tls_version_whitelist:
  - "TLSv1.2"
tls_cipher_suite_whitelist:
  - "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
```

- **ssl_context_protocol:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the BungeeCord proxy. If not mirrored, the side with the weaker context protocol will be used, which may be an older or otherwise-out-of-spec protocol ("as strong as the weakest link in the chain" analogy).
  - It is recommended to use the latest version of SSL/TLS that your system(s) support.
  - The default value is "TLS", but a null or empty value will use the default value.
- **tls_version_whitelist:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the BungeeCord proxy. If not mirrored, the side with the weakest "best" TLS version will be used, which may be an older or otherwise-out-of-spec TLS version ("as strong as the weakest link in the chain" analogy).
  - This is the list of TLS versions that you wish to be able to use for your IPC connection.
  - This is separate from the above setting in that the above will allow _all_ TLS versions, but this setting will restrict that to only the ones listed.
  - The default list only contains "TLSv1.2" (only TLSv1.2 will be able to be used), but a null or empty list will use the default list.
- **tls_cipher_suite_whitelist:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the BungeeCord proxy. If not mirrored, the side with the weakest "best" TLS Cipher Suite will be used, which may be an older or otherwise-out-of-spec TLS Cipher Suite ("as strong as the weakest link in the chain" analogy).
  - This is the list of TLS Cipher Suites that you wish to be able to use for your IPC connection.
  - There are over 40 Cipher Suites that the SSLSocket(s) will use by default, some of which are less-than-ideal to use. The following list allows only the specified Cipher Suite(s) to be used for BungeeIPC.
  - The default list only contains "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", but a null or empty list will use the default list.

## BungeeCord Configuration Options

This section is for the BungeeCord plugin. For Bukkit, please check further up the page for "Bukkit Configuration Options". The settings and their respective defaults are listed first in each section.

### General Plugin Settings

```
logging_level: "INFO"
```

- **logging_level:**
  - This is the logging level for the BungeeIPC plugin logger. It will *only* change the logging level for the plugin's logger.
  - NOTE: This replaces the old setting "extra_logging", as a Log4j adapter has been created within BungeeCord.
  - The available levels and their respective Log4j levels are:
    - Java Logger | Log4J
    - SEVERE      | ERROR
    - WARNING     | WARN
    - INFO        | INFO
    - CONFIG      | INFO
    - FINE        | DEBUG
    - FINER       | DEBUG
    - FINEST      | TRACE
  - The default value is "INFO", but a null or empty value will use the default value.

### IPC Servers Configuration

```
servers:
```

- **servers:**
  - This is where the IPC ServerSocket(s) (or SSLServerSocket(s)) are defined to allow the Bukkit-side BungeeIPC plugin(s) to connect.
  - This section will include the IPC Servers' names, hostnames/IP addresses, and ports.
  - The _minimum_ information required to set up an IPC Server is the port that the ServerSocket will bind to. Optionally, you may also include the hostname or IP address that the specific server is to bind to (IP address is preferred).
    - Please note that the hostname/IP address is _NOT_ the address of the Bukkit server, unlike in the BungeeCord proxy's config.yml (or bungee.yml) file. This address is the local address to the BungeeCord proxy that the BungeeIPC plugin on the Bukkit server will connect to.
    - Most of the time, this address will be the same for all defined servers. Only in complex setups will the addresses be different between servers, usually where the BungeeCord proxy has multiple IP addresses that correspond to different Bukkit servers. Again, this is a rare use case.
  - All servers defined here must have unique names, each of which must uniquely match a server name from the BungeeCord proxy's config.yml file.
  - If no hostname or IP address is defined for a particular server, "localhost" (or 127.0.0.1) will be used.
  - If a hostname or IP address is specified, it does not have to match the address that players use to connect to the network.
    - Depending on your particular setup, you may have a public address that players use to connect to the BungeeCord proxy, and then have private networking to your backend Bukkit servers.
    - In this case, you would want to specify (one of) the private address(es) to bind to for your BungeeIPC connection.
  - Port numbers must be between 1024 and 65535 (inclusive). Failure to specify a valid value will result in an Exception being thrown, and the respective IPC Server will not start.
  - If no port is specified, an Exception will be thrown when the configuration is reloaded, and the respective IPC Server will not start.
  - The hostname/IP address and port combination for each server must be unique to any other defined IPC Server(s), as well as any listeners (including the player connection address and port) defined in the BungeeCord proxy's config.yml file.
    - The address may be the same for all connections (listener(s) and IPC Server(s)), but then the port numbers must all be unique.
  - There are no default IPC Servers defined. You must define at least 1 IPC Server with a port number and reload the configuration to be able to use BungeeIPC.
  - An example of a configuration can be seen below:

```
servers:
  testserver1:
    bind_address: 127.0.0.1
    bind_port: 12345
  testserver2:
    bind_port: 55555
```

### Global SSL/TLS Settings

_**IMPORTANT:** Please take note of which settings should be mirrored in the BungeeIPC configuration file(s) for the Bukkit plugin(s). Failure to ensure mirrored settings may lead to a failure to connect securely and/or a failure for the plugin to load properly._

```
use_ssl: false
```

- **use_ssl:**
  - THIS SETTING MUST BE MIRRORED WITH THE BungeeIPC CONFIGURATION ON THE Bukkit SERVER(S).
  - This determines whether SSL/TLS will be used to protect the connection between the BungeeCord proxy and the Bukkit server(s).
  - NOTE: While this setting's label would indicate the ability to use SSL, which has been superseded by TLS, this setting is simply to enable the SSL/TLS features below.
  - DISCLAIMER: While it is much easier to not use SSL/TLS, having SSL/TLS disabled should only be used in a testing environment. It is _highly_ recommended to use encryption between the BungeeIPC plugins on the Bukkit server and BungeeCord proxy, even if they are running on the same physical machine, no matter how "unimportant" the data is that traverses the IPC connection.
  - If no value is specified, the default value ("false") will be used.
  - The default value is "false", but a null, empty, or otherwise invalid value will be treated as "false".

### Basic SSL/TLS Settings

_NOTE: These settings will be ignored if `use_ssl` is set to `false`._

```
key_store_file: ""
key_store_password: ""
ssl_context_protocol: "TLS"
tls_version_whitelist:
  - "TLSv1.2"
tls_cipher_suite_whitelist:
  - "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
```

- **key_store_file:**
  - It is HIGHLY RECOMMENDED to use the absolute path to the KeyStore file (as opposed to a relative path from the BungeeIPC data directory).
  - WHen creating the KeyStore, depending on what CA you used to generate your certificate, you may need to edit your certificate to be a full-chain certificate, as Java's trusted CAs may not include yours by default.
  - The default value is an empty String. If a null value, the default value, or an invalid path is specified, an Exception will be thrown.
- **key_store_password:**
  - It is HIGHLY RECOMMENDED to use a complex, securely-generated password. Using a simple password, or the Java KeyStore's default password falls under the umbrella of "bad security practices".
  - The default value is an empty String. If a null value or the default value is specified, an Exception will be thrown.
- **ssl_context_protocol:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the Bukkit server(s). If not mirrored, the side with the weaker context protocol will be used, which may be an older or otherwise-out-of-spec protocol ("as strong as the weakest link in the chain" analogy).
  - It is recommended to use the latest version of SSL/TLS that your system(s) support.
  - The default value is "TLS", but a null or empty value will use the default value.
- **tls_version_whitelist:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the Bukkit server(s). If not mirrored, the side with the weakest "best" TLS version will be used, which may be an older or otherwise-out-of-spec TLS version ("as strong as the weakest link in the chain" analogy).
  - This is the list of TLS versions that you wish to be able to use for your IPC connection.
  - This is separate from the above setting in that the above will allow _all_ TLS versions, but this setting will restrict that to only the ones listed.
  - The default list only contains "TLSv1.2" (only TLSv1.2 will be able to be used), but a null or empty list will use the default list.
- **tls_cipher_suite_whitelist:**
  - It is HIGHLY RECOMMENDED to mirror this setting with the BungeeIPC configuration on the Bukkit server(s). If not mirrored, the side with the weakest "best" TLS Cipher Suite will be used, which may be an older or otherwise-out-of-spec TLS Cipher Suite ("as strong as the weakest link in the chain" analogy).
  - This is the list of TLS Cipher Suites that you wish to be able to use for your IPC connection.
  - There are over 40 Cipher Suites that the SSLSocket(s) will use by default, some of which are less-than-ideal to use. The following list allows only the specified Cipher Suite(s) to be used for BungeeIPC.
  - The default list only contains "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", but a null or empty list will use the default list.

### Advanced SSL/TLS Settings

_NOTE: These settings will be ignored if `use_ssl` is set to `false`._
_NOTE: UNLESS YOU KNOW WHAT YOU ARE DOING, IT IS BEST TO LEAVE THESE SETTINGS ALONE. WE HAVE INCLUDED THEM FOR ADVANCED USERS WITH VERY SPECIFIC OR OTHERWISE SPECIAL REQUIREMENTS._

```
key_store_instance: "PKCS12"
key_manager_factory_algorithm: "NewSunX509"
trust_manager_factory_algorithm: "SunX509"
```

- **key_store_instance:**
  - This determines which instance of the KeyStore will be used.
  - The default value is "PKCS12", but a null or empty value will use the default value.
- **key_manager_factory_algorithm:**
  - This determines which instance of the KeyManagerFactory will be used.
  - The default value is "NewSunX509", but a null or empty value will use the default value.
- **trust_manager_factory_algorithm:**
  - This determines which instance of the TrustManagerFactory will be used.
  - The default value is "SunX509", but a null or empty value will use the default value.
