package net.runelite.client.plugins.chatarchiver;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.SetMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

@PluginDescriptor(
        name = "Chat Archiver",
        description = "Retain all your chat history"
)
public class ChatArchiverPlugin extends Plugin {
    private Gson gson;

    private static HashSet<String> playerList;

    private static final Set<ChatMessageType> PRIVATE_MESSAGE_TYPES = Sets.newHashSet(
        ChatMessageType.PRIVATE_MESSAGE_RECEIVED,
        ChatMessageType.PRIVATE_MESSAGE_SENT,
        ChatMessageType.PRIVATE_MESSAGE_RECEIVED_MOD
    );

    @Inject
    private ClientThread clientThread;

    @Inject
    private ScheduledExecutorService executorService;

    @Inject
    private ClientToolbar clientToolbar;

    private ChatArchiverPanel chatArchiverPanel;
    private NavigationButton navButton;

    @Inject
    private ChatArchiverConfig archiverConfig;

    @Inject
    private ChatArchiverFileIO fileIO;

    @Provides
    ChatArchiverConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ChatArchiverConfig.class);
    }

    @Override
    protected void startUp()
    {
        gson = new Gson();

        playerList = fileIO.initGetPlayerNames();
        ArrayList<String> boxNames = new ArrayList<>(playerList);
        Collections.sort(boxNames);

        chatArchiverPanel = new ChatArchiverPanel(this,
                boxNames.toArray(new String[boxNames.size()]),
                fileIO,
                clientThread,
                executorService);

        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Private Messages")
                .icon(icon)
                .priority(9)
                .panel(chatArchiverPanel)
                .build();

        clientToolbar.addNavigation(navButton);
        //collect player names from files

    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        fileIO.closeWriter();
    }

    @Subscribe
    public void onSetMessage(SetMessage message)
    {
        if(PRIVATE_MESSAGE_TYPES.contains(message.getType())) {
            System.out.println(message.getType());
            System.out.println(message);
        }

        if(archiverConfig.isLoggingPrivateMessages() && PRIVATE_MESSAGE_TYPES.contains(message.getType())) {

            ArchivedMessage createdMessage = new ArchivedMessage(message);
            //serialize message for storage
            String outMessage = gson.toJson(createdMessage);

            String playerName = message.getName();

            //add player to list of saved names
            //TODO: add current updater / stream
            if(!this.playerList.contains(playerName)){
                System.out.println(playerName + " not part of list. Adding...");
                playerList.add(playerName);
                chatArchiverPanel.addPlayer(playerName);
                fileIO.setNameFileWriter(playerName);
                fileIO.addMessage(outMessage, true);
            }else{
                fileIO.setNameFileWriter(playerName);
                fileIO.addMessage(outMessage, false);
            }


            if(chatArchiverPanel.getCurrentPlayer().equals(playerName)){
                chatArchiverPanel.addMessage(createdMessage);
            }

            System.out.println(outMessage);
        }
    }
}
