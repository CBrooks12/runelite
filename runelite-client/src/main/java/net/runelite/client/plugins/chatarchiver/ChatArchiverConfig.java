package net.runelite.client.plugins.chatarchiver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chatarchiverplugin")
public interface ChatArchiverConfig extends Config {
    @ConfigItem(
            position = 1,
            keyName = "isLogging",
            name = "Enable Archiving",
            description = "Starts archiving messages"
    )
    default boolean isLogging()
    {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "isArchivingPMs",
            name = "Private Messages",
            description = "Enable to log private messages"
    )
    default boolean isLoggingPrivateMessages()
    {
        return false;
    }

}


