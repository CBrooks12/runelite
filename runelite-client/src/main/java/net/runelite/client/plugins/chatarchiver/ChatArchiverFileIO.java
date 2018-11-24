package net.runelite.client.plugins.chatarchiver;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class ChatArchiverFileIO {
    private static final String path = "./runelite-client/src/main/resources/net/runelite/client/plugins/chatarchiver/players/";

    private static Gson gson;
    private FileWriter fw;
    private BufferedWriter bw;
    private String player;
    private ArrayList<ArchivedMessage> playerMessages;

    ChatArchiverFileIO() {
        this.gson = new Gson();
    }
    public HashSet<String> initGetPlayerNames()
    {
        HashSet<String> playerNames = new HashSet<String>();
        File folder = new File(path);
        try {
            for (File file : folder.listFiles()) {
                if (file.isFile() && file.getAbsolutePath().toLowerCase().endsWith(".txt")) {
                    String playerName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    playerNames.add(playerName);
                    System.out.println(playerName);
                }
            }
        }catch(Exception e){

        }
        return playerNames;
    }

    public void setNameFileWriter(String playerName)
    {
        //if new player selection
        if(!playerName.equals(this.player))
        {
            //close filewriter
            System.out.println("Closing writers");
            closeWriter();
            this.player = playerName;
            System.out.println("setting new writer: " + playerName);
            try
            {
                fw = new FileWriter(path + playerName + ".txt",true);
                bw = new BufferedWriter(fw);

            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void addMessage(String outMessage, boolean isFirstMessage){
        System.out.println("Writing message: "+ outMessage);
        if(fw == null || bw == null){
            return;
        }
        try {
            // TODO: rewrite for cleaner practice
            if(isFirstMessage){
                bw.write(outMessage);
            }
            else {
                bw.write(System.lineSeparator() + outMessage);
            }
            bw.flush(); //TODO: check for performance issues
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ArchivedMessage> getMessages(String playerSelected){
        ArrayList<ArchivedMessage> messageList = new ArrayList<>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(path + playerSelected + ".txt");
            br = new BufferedReader(fr);

            String curLine;
            while ((curLine = br.readLine()) != null) {
                System.out.println(curLine);
                messageList.add(gson.fromJson(curLine, ArchivedMessage.class));
            }
        } catch(IOException e){
            e.printStackTrace();
        }finally {
            //close readers
            try{
                if(br != null){
                    br.close();
                }
                if(fr != null){
                    fr.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return messageList;
    }

    public void closeWriter(){
        try
        {
            if(bw != null) {
                bw.close();
            }
            if(fw != null) {
                fw.close();
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
