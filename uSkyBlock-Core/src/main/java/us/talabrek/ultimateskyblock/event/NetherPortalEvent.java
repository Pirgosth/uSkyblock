package us.talabrek.ultimateskyblock.event;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.jetbrains.annotations.NotNull;

import io.papermc.lib.PaperLib;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.world.WorldManager;

public class NetherPortalEvent implements Listener {

	private uSkyBlock plugin = null;
	private WorldManager worldManager = null;
	private boolean canVisitorsUsePortals = false;

	public NetherPortalEvent() {
		this.plugin = uSkyBlock.getInstance();
		this.worldManager = this.plugin.getWorldManager();
		this.canVisitorsUsePortals = this.plugin.getConfig().getBoolean("options.protection.visitors.use-portals", false);
	}
	
	public boolean canUseOtherPortals(Player player) {
    	return this.canVisitorsUsePortals || player.hasPermission("usb.mod.bypassprotection");
    }

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onPlayerEnterNetherPortal(PlayerPortalEvent event) {
		Location from = event.getFrom();
		Player player = event.getPlayer();
		IslandInfo islandInfo = this.plugin.getIslandInfo(from);
		if (this.worldManager.isSkyWorld(from.getWorld())) {
			if (islandInfo == null) {
				player.sendMessage("You can't travel in nether portal if you're not on an active island !");
				event.setCancelled(true);
				return;
			}
			if (islandInfo.isMember(player)) {
				event.setCancelled(true);
				this.netherHomeTeleport(player);
			} else if(this.canUseOtherPortals(player)){
				event.setCancelled(true);
				PlayerInfo leaderInfo = plugin.getPlayerLogic().getPlayerInfo(islandInfo.getLeaderUniqueId());
				this.netherHomeTeleport(player, leaderInfo);
			}
		} else if(this.worldManager.isSkyNether(from.getWorld())) {
			if (islandInfo == null) {
				event.setCancelled(true);
				player.sendMessage("You can't travel in nether portal if you're not on an active island !");
				return;
			}
			if (islandInfo.isMember(player)) {
				event.setCancelled(true);
				this.homeTeleport(player);
			} else if(this.canUseOtherPortals(player)){
				event.setCancelled(true);
				PlayerInfo leaderInfo = plugin.getPlayerLogic().getPlayerInfo(islandInfo.getLeaderUniqueId());
				this.homeTeleport(player, leaderInfo);
			}
		}
	}

	@EventHandler
	private void onNetherPortalCreate(PortalCreateEvent event) {
		if ((this.worldManager.isSkyWorld(event.getWorld()) || this.worldManager.isSkyNether(event.getWorld()))
				&& event.getReason() == CreateReason.NETHER_PAIR) {
			event.setCancelled(true);
		}
	}

	private void netherHomeTeleport(@NotNull Player player, PlayerInfo pi) {
		Validate.notNull(player, "Player cannot be null");

		Location netherHomeLocation = null;
		if (pi != null) {
			netherHomeLocation = plugin.getSafeNetherHomeLocation(pi);
		}

		if (netherHomeLocation == null) {
			player.sendMessage(tr("\u00a74Unable to find a safe nether home-location on your island!"));
			if (player.isFlying()) {
				player.sendMessage(tr("\u00a7cWARNING: \u00a7eTeleporting you to mid-air."));
				PaperLib.teleportAsync(player, LocationUtil.centerOnBlock(pi.getIslandNetherLocation().clone()));
			}
			return;
		}
		PaperLib.teleportAsync(player, LocationUtil.centerOnBlock(netherHomeLocation.clone()));
	}

	private void netherHomeTeleport(@NotNull Player player) {
		PlayerInfo playerInfo = plugin.getPlayerLogic().getPlayerInfo(player);
		this.netherHomeTeleport(player, playerInfo);
	}

	private void homeTeleport(@NotNull Player player, PlayerInfo pi) {
		Validate.notNull(player, "Player cannot be null");

		Location homeLocation = null;
		if (pi != null) {
			homeLocation = plugin.getSafeHomeLocation(pi);
		}

		if (homeLocation == null) {
			player.sendMessage(tr("\u00a74Unable to find a safe home-location on your island!"));
			if (player.isFlying()) {
				player.sendMessage(tr("\u00a7cWARNING: \u00a7eTeleporting you to mid-air."));
				PaperLib.teleportAsync(player, LocationUtil.centerOnBlock(pi.getIslandLocation().clone()));
			}
			return;
		}
		PaperLib.teleportAsync(player, LocationUtil.centerOnBlock(homeLocation.clone()));
	}

	private void homeTeleport(@NotNull Player player) {
		PlayerInfo playerInfo = plugin.getPlayerLogic().getPlayerInfo(player);
		this.homeTeleport(player, playerInfo);
	}
	
}
