package us.talabrek.ultimateskyblock.handler.task;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.island.task.CreateIslandTask.SchemValidator;
import us.talabrek.ultimateskyblock.player.PlayerPerk;

/**
 * BukkitRunnable - to single it out on the timings page
 */
public class WEPasteSchematic extends BukkitRunnable {
    private final File file;
    private final Location origin;
    private final PlayerPerk playerPerk;
    private final SchemValidator schemValidator;

    public WEPasteSchematic(File file, Location origin, PlayerPerk playerPerk, SchemValidator schemValidator) {
        this.file = file;
        this.origin = origin;
        this.playerPerk = playerPerk;
        this.schemValidator = schemValidator;
    }

    @Override
    public void run() {
        AsyncWorldEditHandler.getAWEAdaptor().loadIslandSchematic(file, origin, playerPerk, this.schemValidator);
    }
}
