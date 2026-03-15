package team.kitemc.verifymc.security;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import team.kitemc.verifymc.core.PluginContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminAccessManager {
    private final PluginContext ctx;
    private volatile boolean missingVaultLogged;

    public AdminAccessManager(PluginContext ctx) {
        this.ctx = ctx;
    }

    public boolean canAccess(CommandSender sender, AdminAction action) {
        if (sender == null || action == null) {
            return false;
        }

        if (isConsoleLike(sender)) {
            return true;
        }

        return switch (ctx.getConfigManager().getAdminAuthMode()) {
            case OP -> sender.isOp();
            case PERMISSION -> hasPermission(sender, action);
        };
    }

    public boolean canAccess(String username, AdminAction action) {
        if (username == null || username.isBlank() || action == null) {
            return false;
        }

        return switch (ctx.getConfigManager().getAdminAuthMode()) {
            case OP -> ctx.getOpsManager() != null && ctx.getOpsManager().isOp(username);
            case PERMISSION -> hasPermission(username, action);
        };
    }

    public boolean hasAnyAdminAccess(String username) {
        return !getGrantedActions(username).isEmpty();
    }

    public boolean hasAnyAdminAccess(CommandSender sender) {
        return !getGrantedActions(sender).isEmpty();
    }

    public List<AdminAction> getGrantedActions(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        return switch (ctx.getConfigManager().getAdminAuthMode()) {
            case OP -> ctx.getOpsManager() != null && ctx.getOpsManager().isOp(username)
                    ? AdminAction.VALUES
                    : Collections.emptyList();
            case PERMISSION -> {
                if (hasRootPermission(username)) {
                    yield AdminAction.VALUES;
                }
                yield AdminAction.VALUES.stream()
                        .filter(action -> hasPermissionNode(username, action.permissionNode()))
                        .collect(Collectors.toList());
            }
        };
    }

    public List<AdminAction> getGrantedActions(CommandSender sender) {
        if (sender == null) {
            return Collections.emptyList();
        }

        if (isConsoleLike(sender)) {
            return AdminAction.VALUES;
        }

        return switch (ctx.getConfigManager().getAdminAuthMode()) {
            case OP -> sender.isOp() ? AdminAction.VALUES : Collections.emptyList();
            case PERMISSION -> {
                if (sender.hasPermission(AdminAction.ROOT_PERMISSION)) {
                    yield AdminAction.VALUES;
                }
                yield AdminAction.VALUES.stream()
                        .filter(action -> sender.hasPermission(action.permissionNode()))
                        .collect(Collectors.toList());
            }
        };
    }

    private boolean hasPermission(CommandSender sender, AdminAction action) {
        return sender.hasPermission(AdminAction.ROOT_PERMISSION) || sender.hasPermission(action.permissionNode());
    }

    private boolean hasPermission(String username, AdminAction action) {
        return hasRootPermission(username) || hasPermissionNode(username, action.permissionNode());
    }

    private boolean hasRootPermission(String username) {
        return hasPermissionNode(username, AdminAction.ROOT_PERMISSION);
    }

    private boolean hasPermissionNode(String username, String permissionNode) {
        Player onlinePlayer = findOnlinePlayer(username);
        if (onlinePlayer != null) {
            return onlinePlayer.hasPermission(permissionNode);
        }

        Object vaultPermission = resolveVaultPermission();
        if (vaultPermission == null) {
            logMissingVault(permissionNode, username);
            return false;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        try {
            return (boolean) vaultPermission.getClass()
                    .getMethod("playerHas", String.class, OfflinePlayer.class, String.class)
                    .invoke(vaultPermission, null, offlinePlayer, permissionNode);
        } catch (ReflectiveOperationException e) {
            logMissingVault(permissionNode, username);
            return false;
        }
    }

    private Player findOnlinePlayer(String username) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveVaultPermission() {
        try {
            Class permissionClass = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration(permissionClass);
            return registration != null ? registration.getProvider() : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private boolean isConsoleLike(CommandSender sender) {
        return !(sender instanceof Player);
    }

    private void logMissingVault(String permissionNode, String username) {
        if (missingVaultLogged) {
            return;
        }
        missingVaultLogged = true;
        ctx.getPlugin().getLogger().warning(
                "[VerifyMC] admin_auth.mode=permission requires Vault for offline web permission checks. " +
                        "Permission lookup failed for user '" + username + "' and node '" + permissionNode + "'.");
    }
}
