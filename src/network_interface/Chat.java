package network_interface;

import controllers.BattlemapController;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;

public class Chat {
    private ScrollPane scrollPane;
    private AnchorPane chatBox;
    private BattlemapController controller;
    private TextField chatField;
    private int PID;
    private int lastPMPID = -1;
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


    // TODO I should use here more string format
    public Chat(AnchorPane chatBox, ScrollPane scrollPane, TextField chatField,
                BattlemapController controller, int PID) {
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
            if(isLocalCommand(msg)) {
                chatField.clear();
                return;
            }
            controller.getClient().requestChatMessage(PID, msg);
            archiveMessage(msg);
            chatField.clear();
        } else {
            writeDownMessage("Message is not proper", "error");
        }
    }

    private boolean isLocalCommand(String msg) {
        if(msg.startsWith("/h") || msg.startsWith("/help")) {
            printHelp();
        }
        return false;
    }

    private void printHelp() {
        writeDownMessage("List of commands");
        writeDownMessage("/h(elp) - print available commands");
        writeDownMessage("/me <message> - write an action you or your character does");
        writeDownMessage("/roll <int>d<int> - roll few dices with few sides");
        writeDownMessage("/r <message> - fast repeat for PM message");
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
        return true;
    }

    private boolean properCommand(String msg) {
        String command = msg.substring(1).split(" ")[0];
        if(command.equals("roll")) return true;
        if(command.equals("me")) return true;
        if(command.equals("pm")) return true;
        if(command.equals("r")) return true;
        if(command.equals("h")) return true;
        if(command.equals("help")) return true;
        return false;
    }

    public void writeDownMessage(String msg) {
        writeDownMessage(msg, "none");
    }

    public void writeDownMessage(String msg, String arguments) {
        scrolled = false;
        Text text = formatText(msg, arguments);
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

    // TODO add multiple arguments handling
    private Text formatText(String msg, String arguments) {
        Text text = new Text(msg);
        if(arguments.equalsIgnoreCase("bold")) {
            text.setFont(Font.font(null, FontWeight.BOLD, fontSize));
        } else if(arguments.equalsIgnoreCase("italic")) {
            text.setFont(Font.font(null, FontPosture.ITALIC, fontSize));
        } else {
            text.setFont(Font.font(fontSize));
        }
        if(arguments.equalsIgnoreCase("error")) {
            text.setFill(Color.RED);
        }
        return text;
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
        if(msg.length() > 1000) {
            controller.sendMessage("Message is too long!", "error", PID);
            return;
        }
        msg = msg.trim();
        if(!msg.startsWith("/")) {
            controller.broadcastChatMessage("[" + sdf.format(cal.getTime()) + "]"
                            + nickname + "(" + PID + "): " + msg,
                    "none");
            return;
        }
        if(msg.startsWith("/roll ")) {
            try {
                controller.broadcastChatMessage(nickname + " " + roll(msg.split(" ")[1]), "none");
            } catch (IllegalArgumentException e) {
                controller.sendMessage("Improper arguments for command roll!", "error", PID);
            }
        }
        if(msg.startsWith("/me ")) {
            msg = msg.substring("/me ".length());
            controller.broadcastChatMessage(nickname + " " + msg, "bold");
        }
        if(msg.startsWith("/pm ")) {
            privateMessage(PID, msg.substring("/pm ".length()));
        }
        if(msg.startsWith("/r ")) {
            fastRepeat(PID, msg.substring("/r ".length()));
        }
    }

    private String roll(String arguments) throws IllegalArgumentException {
        int diceAmount;
        int diceSize;
        try {
            diceAmount = Integer.parseInt(arguments.split("d")[0]);
            diceSize = Integer.parseInt(arguments.split("d")[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Improper arguments");
        }
        if(diceAmount < 0 || diceSize < 0) {
            throw new IllegalArgumentException("Negative values, expected equal or greater than 0");
        }
        int rand;
        int sum = 0;
        if(diceAmount > 10) {
            diceAmount = 10;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("rolled ").append(arguments).append(": ");
        for(int i = 0; i < diceAmount; i++) {
            rand = (int)(Math.random() * diceSize) + 1;
            sum += rand;
            builder.append(rand).append(" ");
        }
        builder.append("Sum of rolls: ").append(sum);
        return builder.toString();
    }

    // TODO split private message so you just give PID and not build String in order to split it again
    private void fastRepeat(int sourcePID, String message) {
        Player source = controller.getPlayer(sourcePID);
        privateMessage(sourcePID, source.getLastPMSender() + " " + message);
    }

    private void privateMessage(int sourcePID, String data) {
        Player source = controller.getPlayer(sourcePID);
        Player destination;
        data = data.trim();
        String[] dataArray = data.split(" ", 2);
        if(isInteger(dataArray[0])) {
            destination = controller.getPlayer(Integer.parseInt(dataArray[0]));
        } else {
            destination = controller.getPlayer(dataArray[0]);
        }
        if(destination == null) {
            controller.sendMessage("Couldn't reach player!", "error", sourcePID);
            return;
        }
        String msgToSource = String.format("[%s]To %s(%d): %s", sdf.format(cal.getTime()), destination.getNickname(),
                destination.getPID(), dataArray[1]);
        String msgToDestination = String.format("[%s]From %s(%d): %s", sdf.format(cal.getTime()), source.getNickname(),
                sourcePID, dataArray[1]);
        controller.sendMessage(msgToSource, "italic", sourcePID);
        controller.sendMessage(msgToDestination, "italic", destination.getPID());
        destination.setLastPMSender(sourcePID);
    }

    private boolean isInteger(String string) {
        for(char character : string.toCharArray()) {
            if(!Character.isDigit(character)) {
                return false;
            }
        }
        return true;
    }
}
