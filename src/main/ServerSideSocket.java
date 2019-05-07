package main;

import controllers.OfflineMapEditorController;

import java.io.*;
import java.net.Socket;

//  CODES FOR MESSAGES
//  REQ_NAM:name - requests nickname "name" for player
//  DC:PID
//  MSG:PID:message - player PID send message (client to server)
//  MSG_BRD:nickname:message - broadcast message, server informs that player sent message
//  PERM:PID:PG - player PID received permission group PG
//  LOGGED:PID:NICK - give player his assigned PID and nickname

public class ServerSideSocket extends Thread {
    public final int port;
    private Socket socket;
    private InputStream in = null;
    private OutputStream out = null;
    OfflineMapEditorController controller;

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
                data = request.split(":", 2);
                if(data[0].equalsIgnoreCase("req_nam")) {
                    controller.logPlayer(data[1], this);
                }
                if(data[0].equalsIgnoreCase("msg")) {
                    data = data[1].split(":", 2);
                    controller.broadcastMessage(controller.getPlayerNick(Integer.parseInt(data[0])), data[1]);
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
}
