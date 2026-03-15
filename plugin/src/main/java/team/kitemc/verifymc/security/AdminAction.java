package team.kitemc.verifymc.security;

import java.util.List;

public enum AdminAction {
    RELOAD("reload", "verifymc.admin.reload"),
    APPROVE("approve", "verifymc.admin.approve"),
    REJECT("reject", "verifymc.admin.reject"),
    DELETE("delete", "verifymc.admin.delete"),
    BAN("ban", "verifymc.admin.ban"),
    UNBAN("unban", "verifymc.admin.unban"),
    LIST("list", "verifymc.admin.list"),
    INFO("info", "verifymc.admin.info"),
    AUDIT("audit", "verifymc.admin.audit"),
    SYNC("sync", "verifymc.admin.sync"),
    PASSWORD("password", "verifymc.admin.password"),
    UNLINK("unlink", "verifymc.admin.unlink");

    public static final String ROOT_PERMISSION = "verifymc.admin";
    public static final List<AdminAction> VALUES = List.of(values());

    private final String key;
    private final String permissionNode;

    AdminAction(String key, String permissionNode) {
        this.key = key;
        this.permissionNode = permissionNode;
    }

    public String key() {
        return key;
    }

    public String permissionNode() {
        return permissionNode;
    }
}
