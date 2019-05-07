package main;

import controllers.OfflineMapEditorController;

import java.io.IOException;
import java.net.ServerSocket;

public class Server extends Thread {
    private ServerSocket server = null;
    private OfflineMapEditorController controller;
    private final int port;

    public Server(int port) {
        this.port = port;
        setDaemon(true);
        start();
    }

    public void run() {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started at " + server.getInetAddress() + " and port " + port);
            while(true) {
                    new ServerSideSocket(port, server.accept(), controller);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setController(OfflineMapEditorController controller) {
        this.controller = controller;
    }

    public ServerSocket getServer() {
        return server;
    }

    public boolean isControllerSet() {
        return !(controller == null);
    }
}
