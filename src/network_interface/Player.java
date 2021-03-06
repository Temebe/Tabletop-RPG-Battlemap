package network_interface;

// Permission groups: 0 means GM (host), 1 means player, may be expanded

// TODO permissions
public class Player {
    private String nickname = null;
    private ServerSideSocket serverSideSocket;
    private int permissionGroup = 1;
    private int PID;
    private int lastPMSender = -1;

    public Player(ServerSideSocket serverSideSocket) {
        this.serverSideSocket = serverSideSocket;
    }

    public void receiveMessage(String nickname, String message) {
        serverSideSocket.sendChatMessage(nickname, message);
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setPermissionGroup(int group) {
        permissionGroup = group;
    }

    public void setPID(int PID) {
        this.PID = PID;
    }

    public String getNickname() {
        if(nickname != null) {
            return nickname;
        } else {
            return "NONICKNAME";
        }
    }

    public int getPermissionGroup() {
        return permissionGroup;
    }

    public int getPID() {
        return PID;
    }

    public ServerSideSocket getSocket() {
        return serverSideSocket;
    }

    public void setLastPMSender(int lastPMSender) {
        this.lastPMSender = lastPMSender;
    }

    public int getLastPMSender() {
        return lastPMSender;
    }
}
