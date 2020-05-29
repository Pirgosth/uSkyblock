package us.talabrek.ultimateskyblock.island.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.island.task.CreateIslandTask.SchemValidator;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;

/**
 * A task that looks for a chest at an island location.
 */
public class LocateChestTask extends BukkitRunnable {
    private final uSkyBlock plugin;
    private final Player player;
    private final Location islandLocation;
    private final GenerateTask onCompletion;
    private final long timeout;
    private final SchemValidator schemValidator;

    private long tStart;

    public LocateChestTask(uSkyBlock plugin, Player player, Location islandLocation, GenerateTask onCompletion, SchemValidator schemValidator) {
        this.plugin = plugin;
        this.player = player;
        this.islandLocation = islandLocation;
        this.onCompletion = onCompletion;
        this.schemValidator = schemValidator;
        timeout = System.currentTimeMillis() + TimeUtil.stringAsMillis(plugin.getConfig().getString("asyncworldedit.watchDog.timeout", "5m"));
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (tStart == 0) {
            tStart = now;
        }
        
        if (!this.schemValidator.isValidated()) {
            // Just run again
        } else {
            cancel();
            new BukkitRunnable() {
				@Override
				public void run() {
					Location chestLocation = LocationUtil.findChestLocation(islandLocation);
			        if (chestLocation == null && player != null && player.isOnline()) {
			            player.sendMessage(I18nUtil.tr("\u00a7cWatchdog!\u00a79 Unable to locate a chest within {0}, bailing out.", TimeUtil.millisAsString(timeout-tStart)));
			        }
			        if (onCompletion != null) {
			            onCompletion.setChestLocation(chestLocation);
			            plugin.sync(onCompletion);
			        }
				}
			}.runTaskLater(plugin, 10);
		        
        }
    }
}