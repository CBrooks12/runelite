package net.runelite.client.plugins.chatarchiver;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;

public class ChatArchiverPanel extends PluginPanel {

    private static final int MAX_MESSAGE_ITEMS = 100;

    private static final int CONTENT_WIDTH = 148;
    private static final Color SENT_BACKGROUND = new Color(15, 15, 15);
    private static final Color RECIEVED_BACKGROUND = new Color(36, 75, 19);
    private static final Color RECIEVED_MOD_BACKGROUND = new Color(50, 30, 19);

    private ChatArchiverPlugin plugin;

    private ScheduledExecutorService executorService;

    private HashSet<String> playerList;

    private String currentPlayerSelection;

    private DefaultComboBoxModel model;
    private JComboBox comboBox;

    private JPanel messagesBox;

    private ChatArchiverFileIO fileIO;

    private ClientThread clientThread;

    ChatArchiverPanel(ChatArchiverPlugin plugin,
                      String[] boxNames,
                      ChatArchiverFileIO fileIO,
                      ClientThread clientThread,
                      ScheduledExecutorService executorService)
    {
        super(true);
        this.plugin = plugin;
        this.fileIO = fileIO;
        this.clientThread = clientThread;
        this.executorService = executorService;

        setBorder(null);
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        /*  The main container, this holds the search bar and the center panel */
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(5, 5));
        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);

        //dynamically sorts names as they get inserted
        model = new SortedComboBoxModel(boxNames);
        comboBox = new JComboBox(model);
        comboBox.addActionListener(e -> executorService.execute(() -> loadMessagesOnNameChange()));

        messagesBox = new JPanel();
        messagesBox.setLayout(new GridLayout(0, 1, 5, 5));
        messagesBox.setBackground(ColorScheme.DARK_GRAY_COLOR);

        currentPlayerSelection = (String) comboBox.getSelectedItem();

        container.add(comboBox, BorderLayout.NORTH);
        container.add(messagesBox, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);

        loadPlayerChat((String)comboBox.getSelectedItem());
    }

    private void loadMessagesOnNameChange(){

        String playerSelected = (String) comboBox.getSelectedItem();
        // if no change, do nothing
        if(playerSelected.equals(currentPlayerSelection)){
            return;
        }
        currentPlayerSelection = playerSelected;
        messagesBox.removeAll();
        System.out.println("Selected player: " + playerSelected);

        // move to client thread to lookup item composition
        clientThread.invokeLater(() -> loadPlayerChat(playerSelected));

    }

    public String getCurrentPlayer(){
        return currentPlayerSelection;
    }

    public void addPlayer(String player){
        model.addElement(player);
    }

    public void addMessage(ArchivedMessage message){
        clientThread.invokeLater(() -> SwingUtilities.invokeLater(()->{
            addItemToPanel(message);
            revalidate();
        }));
    }

    private void loadPlayerChat(String playerName){
        ArrayList<ArchivedMessage> messageList = this.fileIO.getMessages(playerName);
        if(messageList==null){
            return;
        }
        SwingUtilities.invokeLater(() -> {
            int count = 0;
            for (ArchivedMessage message : messageList) {
                if (count++ > MAX_MESSAGE_ITEMS) {
                    break;
                }
                addItemToPanel(message);
            }
            revalidate();
        });
    }

    private void addItemToPanel(ArchivedMessage item)
    {

        // TODO: margin constraint fix needed possibly?
        JPanel avatarAndRight = new JPanel(new BorderLayout());
        avatarAndRight.setPreferredSize(new Dimension(0, 56));

        switch(item.type) {

            case PRIVATE_MESSAGE_SENT:
                avatarAndRight.setBackground(SENT_BACKGROUND);
                break;

            case PRIVATE_MESSAGE_RECEIVED:
                avatarAndRight.setBackground(RECIEVED_BACKGROUND);
                break;

            case PRIVATE_MESSAGE_RECEIVED_MOD:
                avatarAndRight.setBackground(RECIEVED_MOD_BACKGROUND);
                break;

            default:
                avatarAndRight.setBackground(SENT_BACKGROUND);
                break;

        }

        JPanel upAndContent = new JPanel();
        upAndContent.setLayout(new BoxLayout(upAndContent, BoxLayout.Y_AXIS));
        upAndContent.setBorder(new EmptyBorder(4, 8, 4, 4));
        upAndContent.setBackground(null);

        JPanel timeLabelPanel = new JPanel();
        timeLabelPanel.setLayout(new BorderLayout());
        timeLabelPanel.setBackground(null);

        Color darkerForeground = UIManager.getColor("Label.foreground").darker();

        //Duration duration = Duration.between(Instant.ofEpochMilli(item.getTimestamp()), Instant.now());
        DateFormat dateTime = new SimpleDateFormat("MM/dd hh:mm:ss a");
        JLabel timeLabel = new JLabel(String.valueOf(dateTime.format(item.timestamp)));
        timeLabel.setFont(FontManager.getRunescapeSmallFont());
        timeLabel.setBackground(null);
        timeLabel.setForeground(darkerForeground);
        timeLabel.setPreferredSize(new Dimension(CONTENT_WIDTH, 0));

        timeLabelPanel.add(timeLabel, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(null);

        JLabel contentLabel = new JLabel(lineBreakText(item.message, FontManager.getRunescapeSmallFont()));
        contentLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
        contentLabel.setFont(FontManager.getRunescapeSmallFont());
        contentLabel.setForeground(darkerForeground);

        content.add(contentLabel, BorderLayout.CENTER);

        upAndContent.add(timeLabelPanel);
        upAndContent.add(content);
        upAndContent.add(new Box.Filler(new Dimension(0, 0),
                new Dimension(0, Short.MAX_VALUE),
                new Dimension(0, Short.MAX_VALUE)));

        avatarAndRight.add(upAndContent, BorderLayout.CENTER);

        //Color backgroundColor = avatarAndRight.getBackground();
        //Color hoverColor = backgroundColor.brighter().brighter();
        //Color pressedColor = hoverColor.brighter();

        messagesBox.add(avatarAndRight);
        messagesBox.repaint();
    }

    private String lineBreakText(String text, Font font)
    {
        StringBuilder newText = new StringBuilder("<html>");

        FontRenderContext fontRenderContext = new FontRenderContext(font.getTransform(),
                true, true);

        int lines = 0;
        int pos = 0;
        String[] words = text.split(" ");
        String line = "";

        while (pos < words.length)
        {
            String newLine = pos > 0 ? line + " " + words[pos] : words[pos];
            double width = font.getStringBounds(newLine, fontRenderContext).getWidth();

            if (width >= CONTENT_WIDTH)
            {
                newText.append(line);
                newText.append("<br>");
                line = "";
                lines++;
            }
            else
            {
                line = newLine;
                pos++;
            }
        }

        newText.append(line);
        newText.append("</html>");

        return newText.toString();
    }

}
