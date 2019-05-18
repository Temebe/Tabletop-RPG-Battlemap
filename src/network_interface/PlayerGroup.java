package network_interface;

public class PlayerGroup {
    private String groupName;
    private int groupId;
    private boolean[] permissionTable = {false, false, false, false, false};
    public static final int drawingPerm = 0;
    public static final int deletingPerm = 1;
    public static final int creatingPerm = 2;
    public static final int editingPerm = 3;
    public static final int movingPerm = 4;

    public PlayerGroup(int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getGroupId() {
        return groupId;
    }

    // TODO do some checks for permission type
    public void setPermission(int permissionType, boolean permission) {
        permissionTable[permissionType] = permission;
    }

    public boolean getPermission(int permissionType) {
        return permissionTable[permissionType];
    }

}
