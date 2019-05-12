package main;

import controllers.OfflineMapEditorController;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;

public class Chat {
    private ScrollPane scrollPane;
    private AnchorPane chatBox;
    private OfflineMapEditorController controller;
    private TextField chatField;
    private int PID;
    // height position of last
    private double lastHeight = 15;
    // information whether bar was scrolled when
    private boolean scrolled = false;
    private int fontSize = 12;
    private ArrayList<Text> chat = new ArrayList<>();
    private ArrayList<String> chatHistory = new ArrayList<>();
    private ListIterator<String> historyIterator = chatHistory.listIterator();
    private Calendar cal = Calendar.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");


    public Chat(AnchorPane chatBox, ScrollPane scrollPane, TextField chatField,
                OfflineMapEditorController controller, int PID) {
        this.chatBox = chatBox;
        this.chatField = chatField;
        this.controller = controller;
        this.PID = PID;
        this.scrollPane = scrollPane;
        chatField.setOnKeyPressed(keyEvent -> {
            KeyCode keyCode = keyEvent.getCode();
            if(keyCode.equals(KeyCode.UP)) {
                if(historyIterator.hasPrevious()) {
                    chatField.setText(historyIterator.previous());
                }
            } else if(keyCode.equals(KeyCode.DOWN)) {
                if(historyIterator.hasNext()) {
                    chatField.setText(historyIterator.next());
                }
            } else {
                if(historyIterator.hasNext()) {
                    historyIterator = chatHistory.listIterator(chatHistory.size());
                }
            }
        });
    }

    public void sendMessage(String msg) {
        if(!controller.isConnected()) {
            writeDownMessage("You're disconnected from server!");
        }
        if(properMessage(msg)) {
            controller.getClient().requestMessage(PID, msg);
            archiveMessage(msg);
            chatField.clear();
        } else {
            writeDownMessage("Message is not proper");
        }
    }

    private void archiveMessage(String msg) {
        if(chatHistory.size() == 0) {
            chatHistory.add(msg);
        } else if(!chatHistory.get(chatHistory.size() - 1).equals(msg)) {
            chatHistory.add(msg);
        }
        historyIterator = chatHistory.listIterator(chatHistory.size());
    }

    private boolean properMessage(String msg) {
        msg = msg.trim();
        if(msg.startsWith("/")) {
            return properCommand(msg);
        }
        char firstChar = msg.charAt(0);
        if(!Character.isAlphabetic(firstChar) && !Character.isDigit(firstChar)) {
            return false;
        }
        return true;
    }

    private boolean properCommand(String msg) {
        String command = msg.substring(1).split(" ")[0];
        if(command.equalsIgnoreCase("roll")) return true;
        if(command.equalsIgnoreCase("me")) return true;
        return false;
    }

    public void writeDownMessage(String msg) {
        writeDownMessage(msg, "none");
    }

    // TODO provide arguments with handling of multiple arguments
    public void writeDownMessage(String msg, String arguments) {
        scrolled = false;
        System.out.println(msg);
        Text text = new Text(msg);
        if(arguments.equalsIgnoreCase("bold")) {
            text.setFont(Font.font(null, FontWeight.BOLD, fontSize));
        } else {
            text.setFont(Font.font(fontSize));
        }
        chat.add(text);
        chatBox.getChildren().add(text);
        text.setLayoutY(lastHeight);
        text.setWrappingWidth(chatBox.getWidth() - 10);
        lastHeight += text.getLayoutBounds().getHeight();
        if(scrollPane.getVvalue() == scrollPane.getVmax()) {
            scrolled = true;
        }
        if(lastHeight > chatBox.getPrefHeight()) {
            chatBox.setPrefHeight(lastHeight);
        }
    }

    public boolean isScrolled() {
        return scrolled;
    }

    public void setFontSize(int size) {
        if(fontSize == size) {
            return;
        }
        for(Text text : chat) {
            if(text.getFont().hashCode() == Font.font(null, FontWeight.BOLD, fontSize).hashCode()) {
                text.setFont(Font.font(null, FontWeight.BOLD, size));
            } else {
                text.setFont(Font.font(size));
            }
        }
        fontSize = size;
        Text last = chat.get(chat.size() - 1);
        lastHeight = last.getLayoutY() + last.getLayoutBounds().getHeight();
        chatBox.setPrefHeight(lastHeight);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setPID(int PID) {
        this.PID = PID;
    }

    public int getPID() {
        return PID;
    }

    // SERVER SIDE METHODS //
    public void receiveMessage(String msg, int PID) {
        String nickname;
        try {
            nickname = controller.getPlayerNick(PID);
        } catch (Exception e) {
            return;
        }
        msg = msg.trim();
        if(!msg.startsWith("/")) {
            controller.broadcastMessage("[" + sdf.format(cal.getTime()) + "]"
                            + nickname + "(" + PID + "): " + msg,
                    "none");
            return;
        }
        if(msg.startsWith("/roll")) {
            controller.broadcastMessage(nickname + " " + roll(msg.split(" ")[1]), "none");
        }
        if(msg.startsWith("/me ")) {
            msg = msg.substring("/me ".length());
            controller.broadcastMessage(nickname + " " + msg, "bold");
        }
    }

    private String roll(String arguments) {
        int diceAmount = Integer.parseInt(arguments.split("d")[0]);
        int diceSize = Integer.parseInt(arguments.split("d")[1]);
        int rand;
        StringBuilder builder = new StringBuilder();
        builder.append("rolled ").append(arguments).append(": ");
        for(int i = 0; i < diceAmount; i++) {
            rand = (int)(Math.random() * diceSize) + 1;
            builder.append(rand).append(" ");
        }
        builder.deleteCharAt(builder.lastIndexOf(" "));
        return builder.toString();
    }
}
