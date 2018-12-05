package net.runelite.client.plugins.chatarchiver;

import net.runelite.api.Client;
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

    private final GridBagConstraints constraints = new GridBagConstraints();

    private ChatArchiverPlugin plugin;

    private ScheduledExecutorService executorService;

    private HashSet<String> playerList;

    private String currentPlayerSelection;

    private DefaultComboBoxModel model;
    private JComboBox comboBox;

    private JPanel messagesBox;

    private ChatArchiverFileIO fileIO;

    private ClientThread clientThread;

    private JScrollPane resultsWrapper;

    ChatArchiverPanel(ChatArchiverPlugin plugin,
                      String[] boxNames,
                      ChatArchiverFileIO fileIO,
                      ClientThread clientThread,
                      Client client,
                      ScheduledExecutorService executorService)
    {
        super(true);
        this.plugin = plugin;
        this.fileIO = fileIO;
        this.clientThread = clientThread;
        this.executorService = executorService;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridx = 0;
        constraints.gridy = 0;

        /*  The main container, this holds the search bar and the center panel */
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(5, 5));
        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);

        //dynamically sorts names as they get inserted
        if (boxNames.length == 0) boxNames = new String[]{""};
        model = new SortedComboBoxModel(boxNames);
        comboBox = new JComboBox(model);

        comboBox.setPreferredSize(new Dimension(100, 30));
        comboBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        comboBox.addActionListener(e -> executorService.execute(() -> loadMessagesOnNameChange()));

        messagesBox = new JPanel(new GridBagLayout());
        messagesBox.setBackground(ColorScheme.DARK_GRAY_COLOR);

        /* This panel wraps the results panel and guarantees the scrolling behaviour */
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.add(messagesBox, BorderLayout.NORTH);

        /*  The results wrapper, this scrolling panel wraps the results container */
        resultsWrapper = new JScrollPane(wrapper);
        resultsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resultsWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        resultsWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 5, 0, 0));
        resultsWrapper.setMaximumSize(new Dimension(100, 600));
        resultsWrapper.setPreferredSize(new Dimension(100, 600));

        currentPlayerSelection = (String) comboBox.getSelectedItem();

        container.add(comboBox, BorderLayout.NORTH);
        container.add(resultsWrapper, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);

        clientThread.invokeLater(()->loadPlayerChat((String)comboBox.getSelectedItem()));
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

            messagesBox.updateUI();
        }));
    }


    private void addItemToPanel(ArchivedMessage message){
        JPanel panel = getPanelElement(message);
        if(messagesBox.getComponentCount() > 0){
            JPanel marginWrapper = new JPanel(new BorderLayout());
            marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
            marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
            marginWrapper.add(panel, BorderLayout.NORTH);
            messagesBox.add(marginWrapper, constraints);
        }
        else{
            messagesBox.add(panel, constraints);
        }
        constraints.gridy++;
    }

    private void loadPlayerChat(String playerName){
        ArrayList<ArchivedMessage> messageList = this.fileIO.getMessages(playerName);
        SwingUtilities.invokeLater(() -> {
            messagesBox.removeAll();
            if(messageList != null) {

                int count = 0;
                for (ArchivedMessage message : messageList) {
                    if (count > MAX_MESSAGE_ITEMS) {
                        break;
                    }
                    // safety check if line is null
                    if (message == null) {
                        continue;
                    }
                    addItemToPanel(message);
                }
                resultsWrapper.getVerticalScrollBar().setValue(resultsWrapper.getVerticalScrollBar().getMaximum());
            }
            messagesBox.updateUI();
        });
    }

    private JPanel getPanelElement(ArchivedMessage item)
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
        return avatarAndRight;

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
        double widthprev = 0;
        while (pos < words.length)
        {
            String newLine = pos > 0 ? line + " " + words[pos] : words[pos];
            double width = font.getStringBounds(newLine, fontRenderContext).getWidth();

            if (width >= CONTENT_WIDTH && widthprev != width)
            {
                widthprev = width;
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
