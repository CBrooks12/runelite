package net.runelite.client.plugins.chatarchiver;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.SetMessage;

public class ArchivedMessage {
    public ChatMessageType type;
    public String message;
    public String name;
    public long timestamp;
    ArchivedMessage(SetMessage message){
        this.type = message.getType();
        this.message = message.getValue();
        this.name = message.getName();
        this.timestamp = System.currentTimeMillis();//new Timestamp(System.currentTimeMillis());
    }

}