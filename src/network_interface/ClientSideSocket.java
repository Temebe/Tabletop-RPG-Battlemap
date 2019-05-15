package network_interface;

import controllers.BattlemapController;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientSideSocket extends Thread{
    private int port;
    private String host;
    private BattlemapController controller;
    Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private static final Logger log = Logger.getLogger(ClientSideSocket.class);

    public ClientSideSocket(String host, int port) {
        this.port = port;
        this.host = host;
        try {
            System.out.println("Trying to connect at " + host + ":" + port);
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if(socket.isConnected())
                System.out.println("Connected!");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setDaemon(true);
        start();
    }

    public void run() {
        String input;
        String[] data;
        try {
            while((input = in.readLine()) != null) {
                data = input.split(":", 2);
                if(data[0].equalsIgnoreCase("LOGGED")) {
                    logged(data[1].split(":", 2));
                }
                if(data[0].equalsIgnoreCase("MSG_ACT")) {
                    msgAct(data[1]);
                }
                if(data[0].equalsIgnoreCase("MAP_TRA")) {
                    mapTra(data[1]);
                }
                if(data[0].equalsIgnoreCase("DRAW_ACT")) {
                    drawAct(data[1].split(":", 4));
                }
                if(data[0].equalsIgnoreCase("CHAR_CR_ACT")) {
                    charCrAct(data[1].split(":", 4));
                }
                if(data[0].equalsIgnoreCase("CHAR_DL_ACT")) {
                    charDlAct(data[1]);
                }
                if(data[0].equalsIgnoreCase("CHAR_MOV_ACT")) {
                    charMovAct(data[1].split(":", 4));
                }
                if(data[0].equalsIgnoreCase("CHAR_SET_ACT")) {
                    String[] characterData = data[1].split(":", 12);
                    Platform.runLater(() -> controller.setCharacter(characterData));
                }
            }
        } catch (IOException e) {
            log.error("Connection with server lost.");
        } finally {
            controller.logOut();
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException ignored) { }
        }
    }

    private void logged(String[] data) {
        controller.setPID(Integer.parseInt(data[0]));
        controller.setNickname(data[1]);
    }

    private void msgAct(String data) {
        int separator = data.lastIndexOf(":");
        if(separator == -1) {
            log.error("Message data was corrupted");
        }
        try {
            String message = data.substring(0, separator);
            String arguments = data.substring(separator + 1);
            Platform.runLater(() -> controller.getChat().writeDownMessage(message, arguments));
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Client received corrupted message acceptance data");
        }
    }

    private void mapTra(String data) {
        try {
            int mapSize = Integer.parseInt(data);
            receiveMap(mapSize);
        } catch (NumberFormatException e) {
            log.error("Client received wrong map info, unable to load map");
        }
    }

    private void drawAct(String[] data) {
        try {
            String imagePath = data[0];
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            int layerNum = Integer.parseInt(data[3]);
            Platform.runLater(() -> controller.setMapSquareGraphic(posY, posX, imagePath, layerNum));
        } catch (NumberFormatException e) {
            log.error("Client received corrupted drawing data");
        }
    }

    private void charCrAct(String[] data) {
        try {
            String imagePath = data[0];
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            int cid = Integer.parseInt(data[3]);
            Platform.runLater(() -> controller.putCharacterOnSquare(posX, posY, imagePath, cid));
        } catch (NumberFormatException e) {
            log.error("Client received corrupted character creation data");
        }
    }

    private void charDlAct(String data) {
        try {
            int cid = Integer.parseInt(data);
            Platform.runLater(() -> controller.deleteCharacter(cid));
        } catch (NumberFormatException e) {
            log.error("Client received improper character id, cannot delete character");
        }
    }

    private void charMovAct(String[] data) {
        try {
            int posX = Integer.parseInt(data[1]);
            int posY = Integer.parseInt(data[2]);
            int cid = Integer.parseInt(data[0]);
            Platform.runLater(() -> controller.moveCharacter(cid, posX, posY));
        } catch (NumberFormatException e) {
            log.error("Client received corrupted character move data, cannot move character");
        }
    }

    private void charSetAct(String[] data) {

    }

    public void setController(BattlemapController controller) {
        this.controller = controller;
    }

    public void requestNickname(String nickname) {
        out.println("REQ_NAM:" + nickname);
    }

    public void requestChatMessage(int PID, String msg) {
        out.println("MSG_REQ:" + PID + ":" + msg);
    }

    public void requestDrawing(int posX, int posY, String imagePath, int layerNum) {
        out.println("DRAW_REQ:" + imagePath + ":" + posX + ":" + posY + ":" + layerNum);
    }

    public void requestNewCharacter(int posX, int posY, String imagePath) {
        out.println("CHAR_CR_REQ:" + imagePath + ":" + posX + ":" + posY);
    }

    public void requestDelCharacter(int cid) {
        out.println("CHAR_DL_REQ:" + cid);
    }

    public void requestMoveCharacter(int cid, int posX, int posY) {
        out.println("CHAR_MOV_REQ:" + cid + ":" + posX + ":" + posY);
    }

    public void requestSetCharacter(int cid, String name, int size,
                                    String color1, double amount1, double maxAmount1,
                                    String color2, double amount2, double maxAmount2,
                                    String color3, double amount3, double maxAmount3) {
        out.println("CHAR_SET_REQ:" + cid + ":" + name + ":" + size
                + ":" + color1 + ":" + amount1 + ":" + maxAmount1
                + ":" + color2 + ":" + amount2 + ":" + maxAmount2
                + ":" + color3 + ":" + amount3 + ":" + maxAmount3);
    }

    private void receiveMap(int fileSize) {
        FileOutputStream fileOut;
        DataInputStream input;
        File map;
        try {
             input = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            controller.popUpError(e.getMessage());
            return;
        }
        try {
            map = createNewTempMap();
            fileOut = new FileOutputStream(map);
        } catch (Exception e) {
            controller.popUpError(e.getMessage());
            return;
        }
        if(fileSize == 0) {
            closeFileStream(fileOut);
            return;
        }
        int remaining = fileSize;
        int count;
        byte[] buffer = new byte[8192];
        try {
            while ((count = input.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                remaining -= count;
                fileOut.write(buffer, 0, count);
            }
        } catch (IOException e) {
            closeFileStream(fileOut);
            controller.popUpError("Problem occured while sending file");
        }
        closeFileStream(fileOut);
        Platform.runLater(() -> controller.loadMap(map));
    }

    private File createNewTempMap() throws Exception {
        File map = new File("map.map");
        if(map.exists()) {
            if(!map.delete()) {
                throw new Exception("Couldn't delete temporary file");
            }
        }
        if(!map.createNewFile()) {
            throw new Exception("Couldn't create temporary file");
        }
        return map;
    }

    private void closeFileStream(FileOutputStream out) {
        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                controller.popUpError(e.getMessage());
            }
        }
    }

    public void close() {
        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
