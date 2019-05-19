package network_interface;

import controllers.BattlemapController;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

//  CODES FOR MESSAGES
//  PWD:password - client tells password to server
//  PWD_ACT - password accepted
//  PWD_REJ - password rejected
//  BANNED - client has correct password but his ip is banned
//  REQ_NAM:name - requests nickname "name" for player
//  DC:PID
//  MSG_REQ:PID:message:arguments - player PID send message (client to server)
//  MSG_ACT:message:arguments - broadcast message, server informs that player sent message
//  PERM:PID:PG - player PID received permission group PG
//  LOGGED:PID:NICK - give player his assigned PID and nickname
//  MAP_TRA:size - information to client that map of certain size is coming
//  CLS_MAP - close map
//  DRAW_REQ:path:x:y:layer - request for drawing
//  DRAW_ACT:path:x:y:layer - broadcast information that tile was drawn
//  CHAR_CR_REQ:path:x:y - request for creating new character
//  CHAR_CR_ACT:path:x:y:cid - broadcast information that character was created
//  CHAR_DL_REQ:cid - request for character's deletion
//  CHAR_DL_ACT:cid - broadcast information that character was deleted
//  CHAR_MOV_REQ:cid:x:y - request for moving character
//  CHAR_MOV_ACT:cid:x:y - broadcast information that character was moved
//  CHAR_SET_REQ:cid:name:size:color1:am1:maxAm1:color2:am2:maxAm2:color3:am3:maxAm3 - request for editing character
//  CHAR_SET_ACT:cid:name:size:color1:am1:maxAm1:color2:am2:maxAm2:color3:am3:maxAm3 - broadcast for setting character

public class ServerSideSocket extends Thread {
    public final int port;
    private Socket socket;
    private InputStream in = null;
    private OutputStream out = null;
    BattlemapController controller;
    private String nickname = "";
    private static final Logger log = Logger.getLogger(ServerSideSocket.class);

    public ServerSideSocket(int port, Socket socket, BattlemapController controller) {
        this.port = port;
        this.socket = socket;
        this.controller = controller;
        setDaemon(true);
        start();
    }

    public void run() {
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            log.info("New client connected from " + socket.getInetAddress().getHostAddress());
            String request;
            String[] data;
            while ((request = br.readLine()) != null) {
                data = request.split(":", 2);
                if(data[0].equalsIgnoreCase("REQ_NAM")) {
                    controller.logInPlayer(data[1], this);
                }
                if(data[0].equalsIgnoreCase("MSG_REQ")) {
                    msgReq(data[1].split(":", 2));
                }
                if(data[0].equalsIgnoreCase("DRAW_REQ")) {
                    drawReq(data[1].split(":", 4));
                }
                if(data[0].equalsIgnoreCase("CHAR_CR_REQ")) {
                    charCrReq(data[1].split(":", 3));
                }
                if(data[0].equalsIgnoreCase("CHAR_DL_REQ")) {
                    charDlReq(data[1]);
                }
                if(data[0].equalsIgnoreCase("CHAR_MOV_REQ")) {
                    charMovReq(data[1].split(":", 3));
                }
                if(data[0].equalsIgnoreCase("CHAR_SET_REQ")) {
                    data = data[1].split(":", 12);
                    charSetAct(data);
                }
                if(data[0].equalsIgnoreCase("PWD")) {
                    pwd(data[1]);
                }
            }
        } catch (IOException e) {
            log.error("Lost connection with player");
        } finally {
            Platform.runLater(() -> controller.logOutPlayer(this));
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException ignored) { }
        }
    }

    private void msgReq(String[] data) {
        try {
            int PID = Integer.parseInt(data[0]);
            controller.getChat().receiveMessage(data[1], PID);
        } catch (NumberFormatException e) {
            log.error("Server received improper PID for message request");
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Server received corrupted data for message request");
        }
    }

    private void drawReq(String[] data) {
        if(!permissionGranted(PlayerGroup.drawingPerm)) {
            return;
        }
        try {
            String path = data[0];
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            int layerNum = Integer.parseInt(data[3]);
            controller.broadcastDrawing(posX, posY, path, layerNum);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("There was problem with given data for drawing tiles.");
        } catch (NumberFormatException e) {
            log.error("Server received improper data for drawing request.");
        }
    }

    private void charCrReq(String[] data) {
        if(!permissionGranted(PlayerGroup.creatingPerm)) {
            return;
        }
        try {
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            String imagePath = data[0];
            controller.broadcastCreatingCharacter(posX, posY, imagePath);
        } catch (NumberFormatException e) {
            log.error("Server received improper position numbers for character creation request");
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Server received improper data for character creation request");
        }
    }

    private void charDlReq(String data) {
        if(!permissionGranted(PlayerGroup.deletingPerm)) {
            return;
        }
        try {
            int cid = Integer.parseInt(data);
            controller.broadcastDeletingCharacter(cid);
        } catch (NumberFormatException e) {
            log.error("Server received improper character id for character deletion request");
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Server received corrupted data for character deletion request");
        }
    }

    private void charSetAct(String[] data) {
        if(!permissionGranted(PlayerGroup.editingPerm)) {
            return;
        }
        try {
            int cid = Integer.parseInt(data[0]);
            String name = data[1];
            int size = Integer.parseInt(data[2]);
            String color1 = data[3];
            double amount1 = Double.parseDouble(data[4]);
            double maxAmount1 = Double.parseDouble(data[5]);
            String color2 = data[6];
            double amount2 = Double.parseDouble(data[7]);
            double maxAmount2 = Double.parseDouble(data[8]);
            String color3 = data[9];
            double amount3 = Double.parseDouble(data[10]);
            double maxAmount3 = Double.parseDouble(data[11]);
            controller.broadcastSettingCharacter(cid, name, size,
                    color1, amount1, maxAmount1,
                    color2, amount2, maxAmount2,
                    color3, amount3, maxAmount3);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("There was problem with given data for character settings.");
        } catch (NumberFormatException e) {
            log.error("Server received improper data for character settings.");
        }
    }

    private void charMovReq(String[] data) {
        if(!permissionGranted(PlayerGroup.movingPerm)) {
            return;
        }
        try {
            int cid = Integer.parseInt(data[0]);
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            controller.broadcastMovingCharacter(cid, posX, posY);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Server received corrupted data for character move request");
        } catch (NumberFormatException e) {
            log.error("Server received improper character id and/or positions");
        }
    }

    private void pwd(String data) {
        String msg;
        if(controller.passwordMatches(data)) {
            if(controller.isBanned(socket.getInetAddress().getHostAddress())) {
                msg = "BANNED" + '\n';
            } else {
                msg = "PWD_ACT" + '\n';
            }
        } else {
            msg = "PWD_REJ" + '\n';
        }
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setController(BattlemapController controller) {
        this.controller = controller;
    }

    public void sendPID(int PID, String nickname) {
        String msg = "LOGGED:" + PID + ":" + nickname + '\n';
        this.nickname = nickname;
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendChatMessage(String message, String arguments) {
        String msg = "MSG_ACT:" + message + ":" + arguments + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDrawAct(int posX, int posY, String imagePath, int layerNum) {
        String msg = "DRAW_ACT:" + imagePath + ":" + posX + ":" + posY + ":" + layerNum + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCharCreateAct(int posX, int posY, String imagePath, int cid) {
        String msg = "CHAR_CR_ACT:" + imagePath + ":" + posX + ":" + posY + ":" + cid + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCharDeleteAct(int cid) {
        String msg = "CHAR_DL_ACT:" + cid + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCharMoveAct(int cid, int posX, int posY) {
        String msg = "CHAR_MOV_ACT:" + cid + ":" + posX + ":" + posY +'\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCharSetAct(int cid, String name, int size,
                               String color1, double amount1, double maxAmount1,
                               String color2, double amount2, double maxAmount2,
                               String color3, double amount3, double maxAmount3) {
        String msg = "CHAR_SET_ACT:" + cid + ":" + name + ":" + size
                + ":" + color1 + ":" + amount1 + ":" + maxAmount1
                + ":" + color2 + ":" + amount2 + ":" + maxAmount2
                + ":" + color3 + ":" + amount3 + ":" + maxAmount3 + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMap(File map) {
        if(log.isDebugEnabled()) log.debug("sendMap started");
        FileInputStream fileIn;
        try {
            fileIn = new FileInputStream(map);
        } catch (FileNotFoundException e) {
            controller.popUpError("Unable to send map to the player");
            return;
        }
        long fileSize = map.length();
        if(log.isDebugEnabled()) log.debug("Map size: " + fileSize);
        if(fileSize > Integer.MAX_VALUE) {
            controller.popUpError("Map is too large");
        }
        String msg = "MAP_TRA:" + fileSize + '\n';
        if(log.isDebugEnabled()) log.debug(msg);
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            controller.popUpError("Connection with client failed");
            return;
        }
        byte[] buffer = new byte[8192];
        int count;
        try {
            while((count = fileIn.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        } catch (IOException e) {
            controller.popUpError("Problem while sending map occurred");
        }
        if(log.isDebugEnabled()) log.debug("End of sending map");
        try {
            fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCloseMap() {
        String msg = "CLS_MAP" + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kick() {
        kick("No reason was given");
    }

    public void kick(String reason) {
        if(reason.trim().isEmpty())
            reason = "No reason was given";
        sendChatMessage("You were kicked from server: " + reason, "error");
        controller.logOutPlayer(this);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException ignored) { }
        controller.broadcastChatMessage(nickname + " was kicked from server (" + reason + ")",
                "error");
    }

    public void ban(String reason) {
        if(reason.trim().isEmpty()) {
            reason = "No reason was given";
        }
        sendChatMessage("You were banned from this server: " + reason, "error");
        controller.logOutPlayer(this);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException ignored) { }
        controller.addBannedIp(socket.getInetAddress().getHostAddress());
        controller.broadcastChatMessage(nickname + " was banned from this server (" + reason + ")",
                "error");
    }

    private boolean permissionGranted(int permission) {
        if(!controller.getPlayerGroup(nickname).getPermission(permission)) {
            sendChatMessage("You don't have permission for this action!", "error");
            return false;
        }
        return true;
    }
}
