package us.talabrek.ultimateskyblock.island.task;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.player.PlayerPerk;

/**
 * Task instead of anonymous runnable - so we get some info on /timings paste
 */
public class CreateIslandTask extends BukkitRunnable {
    private final uSkyBlock plugin;
    private final Player player;
    private final PlayerPerk playerPerk;
    private final Location next;
    private final String cSchem;
    private SchemValidator schemValidator;

    public CreateIslandTask(uSkyBlock plugin, Player player, PlayerPerk playerPerk, Location next, String cSchem) {
        this.plugin = plugin;
        this.player = player;
        this.playerPerk = playerPerk;
        this.next = next;
        this.cSchem = cSchem;
    }

    @Override
    public void run() {
    	this.schemValidator = new SchemValidator();
        if (!plugin.getIslandGenerator().createIsland(playerPerk, next, cSchem, this.schemValidator)) {
            player.sendMessage(tr("Unable to locate schematic {0}, contact a server-admin", cSchem));
        }
        GenerateTask generateTask = new GenerateTask(plugin, player, playerPerk.getPlayerInfo(), next, playerPerk, cSchem);
        final BukkitRunnable completionWatchDog = new LocateChestTask(plugin, player, next, generateTask, this.schemValidator);
    }
    
    public static class SchemValidator{
    	private boolean isValidated = false;

    	public boolean isValidated() {
    		return this.isValidated;
    	}

    	public void validate() {
    		this.isValidated = true;
    	}
    }	    
}
