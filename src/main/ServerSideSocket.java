package main;

import controllers.OfflineMapEditorController;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

//  CODES FOR MESSAGES
//  REQ_NAM:name - requests nickname "name" for player
//  DC:PID
//  MSG:PID:message - player PID send message (client to server)
//  MSG_BRD:nickname:message - broadcast message, server informs that player sent message
//  PERM:PID:PG - player PID received permission group PG
//  LOGGED:PID:NICK - give player his assigned PID and nickname
//  MAP_TRA:size - information to client that map of certain size is coming
//  CLS_MAP - close map
//  DRAW_REQ:path:x:y:layer - request for drawing
//  DRAW_ACT:path:x:y:layer - broadcasted information that tile was drawn
//  CHAR_CR_REQ:path:x:y - request for creating new character
//  CHAR_CR_ACT:path:x:y:cid - broadcasted information that character was created
//  CHAR_MOV_REQ:cid:x:y - request for moving character
//  CHAR_MOV_ACT:cid:x:y - broadcasted information that character was moved
//  CHAR_SET_REQ:cid:name:color1:am1:maxAm1:color2:am2:maxAm2:color3:am3:maxAm3 - request for editing character
//  CHAR_SET_ACT:cid:name:color1:am1:maxAm1:color2:am2:maxAm2:color3:am3:maxAm3 - broadcast for setting character

public class ServerSideSocket extends Thread {
    public final int port;
    private Socket socket;
    private InputStream in = null;
    private OutputStream out = null;
    OfflineMapEditorController controller;
    private static final Logger log = Logger.getLogger(ServerSideSocket.class);

    public ServerSideSocket(int port, Socket socket, OfflineMapEditorController controller) {
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
            System.out.println("New client connected from " + socket.getInetAddress().getHostAddress());
            String request;
            String[] data;
            while ((request = br.readLine()) != null) {
                System.out.println("SERVERSIDESOCKET: " + request);
                data = request.split(":", 2);
                if(data[0].equalsIgnoreCase("REQ_NAM")) {
                    controller.logPlayer(data[1], this);
                }
                if(data[0].equalsIgnoreCase("MSG")) {
                    data = data[1].split(":", 2);
                    controller.broadcastMessage(controller.getPlayerNick(Integer.parseInt(data[0])), data[1]);
                }
                // TODO permissions check
                // TODO think about resolving duplicate code problem
                if(data[0].equalsIgnoreCase("DRAW_REQ")) {
                    data = data[1].split(":", 4);
                    String path = data[0];
                    int posX = Integer.parseInt(data[1]);
                    int posY = Integer.parseInt(data[2]);
                    int layerNum = Integer.parseInt(data[3]);
                    controller.broadcastDrawing(posX, posY, path, layerNum);
                }
            }
        } catch (IOException e) {
            System.out.println("There was some problems with streams from client.");
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setController(OfflineMapEditorController controller) {
        this.controller = controller;
    }

    public void sendPID(int PID, String nickname) {
        String msg = "LOGGED:" + PID + ":" + nickname + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String nickname, String message) {
        String msg = "MSG_BRD:" + nickname + ":" + message + '\n';
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  DRAW_ACT:path:x:y:layer - broadcasted information that tile was drawn
    public void sendDrawAct(int posX, int posY, String imagePath, int layerNum) {
        String msg = "DRAW_ACT" + ":" + imagePath + ":" + posX + ":" + posY + ":" + layerNum + '\n';
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
}
