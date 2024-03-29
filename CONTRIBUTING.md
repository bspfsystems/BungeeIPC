# Contributions, Support, and Issues for BungeeIPC

Contributions to the project are welcome. BungeeIPC is a free and open source software project, made open source with the hopes that the community would find ways to improve it.

## Contributing

### Pull Requests

If you make any improvements or other enhancements to BungeeIPC, we ask that you submit a Pull Request to merge the changes back upstream. We would enjoy the opportunity to give those improvements back to the wider community.

Various types of contributions are welcome, including (but not limited to):
- Security updates / patches
- Bug fixes
- Feature enhancements

We reserve the right to not include a contribution in the project if the contribution does not add anything substantive or otherwise reduces the functionality of BungeeIPC in a non-desirable way. That said, the idea of having free and open source software was that contributions would be accepted, and discussions over a potential contribution are welcome.

For licensing questions, please see the Licensing section in [README.md](README.md).

### Project Layout

BungeeIPC somewhat follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). This is not the definitive coding style of the project. Generally, it is best to try to copy the style of coding found in the class that you are editing.

BungeeIPC contains several modules:
- **Common API** - The public common API used by other downstream plugins to access the functionality in the plugin. Except for a few cases, there are no implementations within the API itself, and the Bukkit and BungeeCord plugins provide the API.
- **Client API** - The public client-side API used by other downstream plugins to access client-specific functionality in the plugin. The client API depends on the core API.
- **Server API** - The public server-side API used by other downstream plugins to access server-specific functionality in the plugin. The server API depends on the core API.
- **Bukkit** - The Bukkit implementation of the API, which can be run by the Bukkit server and provides the IPC functionality of the client API.
- **BungeeCord** - The BungeeCord implementation of the API, which can be run by the BungeeCord proxy and provides the IPC functionality of the server API.

## Support / Issues

Issues can be reported [here in GitHub](https://github.com/bspfsystems/BungeeIPC/issues/).

### First Steps

Before creating an issue, please search to see if anyone else has reported the same issue. Don't forget to search through the closed issues. It is much easier for us (and will get you a faster response) to handle a single issue that affects multiple users than it is to have to deal with duplicates.

There is also a chance that your issue has been resolved previously. In this case, you can (ideally) find the answer to your problem without having to ask (new version of BungeeIPC, configuration update, etc).

### Creating an Issue

If no one has reported the issue previously, or the solution is not apparent, please open a new issue. When creating the issue, please give it a descriptive title (no "It's not working", please), and put as much detail into the description as possible. The more details you add, the easier it becomes for us to solve the issue. Helpful items may include:
- A descriptive title for the issue
- The version of BungeeIPC you are using
- The version of Minecraft you are using
- The Bukkit implementation you are using (CraftBukkit / Spigot / Paper / etc.) AND/OR...
- The BungeeCord implementation you are using (BungeeCord / Waterfall / etc.)
- Logs and/or stack traces
- Any steps to reproducing the issue
- Anything else that might be helpful in solving your issue

_Note:_ Please redact any Personally-Identifiable Information (PII) when you create your issue. These may appear in logs or stack traces. Examples include (but are not limited to):
- Real names of players / server administrators
- Usernames of accounts on computers (may appear in logs or stack traces)
- IP addresses / hostnames
- etc.

If you are not sure, you can always redact or otherwise change the data.

### Non-Acceptable Issues

Issues such as "I need help" or "It doesn't work" will not be addressed and/or will be closed with no assistance given. These types of issues do not have any meaningful details to properly address the problem. Other issues that will not be addressed and/or will be closed without help include (but are not limited to):
- How to install BungeeIPC (explained in [USAGE.md](USAGE.md))
- How to configure BungeeIPC (explained in [CONFIGURATION.md](CONFIGURATION.md))
- How to use BungeeIPC as a dependency (explained in [README.md](README.md))
- How to create plugins
- How to set up a development environment
- How to install plugins
- How to create a server and/or a server network
- Other issues of similar nature...

This is not a help forum for server administration or non-project-related coding issues. Other resources, such as [Google](https://www.google.com/), should have answers to most questions not related to BungeeIPC.
