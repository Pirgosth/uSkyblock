package us.talabrek.ultimateskyblock.handler;

import static us.talabrek.ultimateskyblock.util.LogUtil.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.primesoft.asyncworldedit.api.IAsyncWorldEdit;
import org.primesoft.asyncworldedit.api.blockPlacer.IBlockPlacer;
import org.primesoft.asyncworldedit.api.blockPlacer.IBlockPlacerListener;
import org.primesoft.asyncworldedit.api.blockPlacer.IJobEntryListener;
import org.primesoft.asyncworldedit.api.blockPlacer.entries.IJobEntry;
import org.primesoft.asyncworldedit.api.blockPlacer.entries.JobStatus;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerManager;
import org.primesoft.asyncworldedit.api.utils.IFuncParamEx;
import org.primesoft.asyncworldedit.api.worldedit.IAsyncEditSessionFactory;
import org.primesoft.asyncworldedit.api.worldedit.ICancelabeEditSession;
import org.primesoft.asyncworldedit.api.worldedit.IThreadSafeEditSession;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dk.lockfuglsang.minecraft.util.VersionUtil;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.handler.asyncworldedit.AWEAdaptor;
import us.talabrek.ultimateskyblock.handler.task.WEPasteSchematic;
import us.talabrek.ultimateskyblock.island.task.CreateIslandTask.SchemValidator;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.util.LogUtil;

/**
 * Handles integration with AWE. Very HACKY and VERY unstable.
 *
 * Only kept as a cosmetic measure, to at least try to give the players some
 * feedback.
 */
public enum AsyncWorldEditHandler {
	;
	private static AWEAdaptor adaptor = null;

	public static void onEnable(uSkyBlock plugin) {
		getAWEAdaptor().onEnable(plugin);
	}

	public static void onDisable(uSkyBlock plugin) {
		getAWEAdaptor().onDisable(plugin);
		adaptor = null;
	}

	public static EditSession createEditSession(World world, int maxblocks) {
		return getAWEAdaptor().createEditSession(world, maxblocks);
	}

	public static void loadIslandSchematic(File file, Location origin, PlayerPerk playerPerk, SchemValidator schemValidator) {
		new WEPasteSchematic(file, origin, playerPerk, schemValidator).runTask(uSkyBlock.getInstance());
	}

	public static void regenerate(Region region, Runnable onCompletion) {
		getAWEAdaptor().regenerate(region, onCompletion);
	}

	public static AWEAdaptor getAWEAdaptor() {
		if (adaptor == null) {
			if (!uSkyBlock.getInstance().getConfig().getBoolean("asyncworldedit.enabled", true)) {
				return NULL_ADAPTOR;
			}
			Plugin fawe = getFAWE();
			String className = null;
			if (fawe != null) {
				VersionUtil.Version version = VersionUtil.getVersion(fawe.getDescription().getVersion());
				className = "us.talabrek.ultimateskyblock.handler.asyncworldedit.FAWEAdaptor";
				try {
					adaptor = (AWEAdaptor) Class.forName(className).<AWEAdaptor>newInstance();
					log(Level.INFO, "Hooked into FAWE " + version);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException
						| NoClassDefFoundError e) {
					log(Level.WARNING, "Unable to locate FAWE adaptor for version " + version + ": " + e);
					adaptor = NULL_ADAPTOR;
				}
			} else {
				adaptor = NULL_ADAPTOR;
			}
		}
		return adaptor;
	}

	public static boolean isAWE() {
		return getAWEAdaptor() != NULL_ADAPTOR;
	}

	public static Plugin getFAWE() {
		return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
	}

	public static IAsyncWorldEdit getAWE() {
		return (IAsyncWorldEdit) Bukkit.getPluginManager().getPlugin("AsyncWorldEdit");
	}

	public static final AWEAdaptor NULL_ADAPTOR = new AWEAdaptor() {
		private final Logger log = Logger.getLogger(WorldEditHandler.class.getName());

		@Override
		public void onEnable(Plugin plugin) {

		}

		@Override
		public void registerCompletion(Player player) {

		}

		@Override
		public void loadIslandSchematic(File file, Location origin, PlayerPerk playerPerk, SchemValidator schemValidator) {
			log.finer("Trying to load schematic " + file);
			if (file == null || !file.exists() || !file.canRead()) {
				LogUtil.log(Level.WARNING, "Unable to load schematic " + file);
			}
			IAsyncWorldEdit awe = getAWE();
			IPlayerManager pm = awe.getPlayerManager();
			final IPlayerEntry playerEntry = pm.getUnknownPlayer();
			BukkitWorld bukkitWorld = new BukkitWorld(origin.getWorld());
			IThreadSafeEditSession tsSession = ((IAsyncEditSessionFactory) ((WorldEditPlugin) Bukkit.getServer()
					.getPluginManager().getPlugin("WorldEdit")).getWorldEdit().getEditSessionFactory())
							.getThreadSafeEditSession(bukkitWorld, 9999999, null, playerEntry);
			IFuncParamEx<Integer, ICancelabeEditSession, MaxChangedBlocksException> action = new PasteAction(origin,
					file);
			IBlockPlacer bp = awe.getBlockPlacer();
			final String jobName = "loadIslandSchematic:" + origin.getWorld().getName() + "-" + origin.toString();
			final IJobEntryListener jobEntryListener = new IJobEntryListener() {

				@Override
				public void jobStateChanged(IJobEntry job) {
					if (!job.getName().equalsIgnoreCase(jobName))
						return;

					JobStatus status = job.getStatus();

					if (status == JobStatus.Done) {
						log.log(Level.INFO, "Schematic paste successfully: " + jobName + "!");
						if(schemValidator != null) {
							schemValidator.validate();
						}
					}
				}
			};
			bp.addListener(new IBlockPlacerListener() {

				@Override
				public void jobRemoved(IJobEntry job) {
					job.removeStateChangedListener(jobEntryListener);
				}

				@Override
				public void jobAdded(IJobEntry job) {
					job.addStateChangedListener(jobEntryListener);
				}
			});
			bp.performAsAsyncJob(tsSession, playerEntry, jobName, action);
		}

		class PasteAction implements IFuncParamEx<Integer, ICancelabeEditSession, MaxChangedBlocksException> {
			private final Location origin;
			private final File file;

			public PasteAction(Location origin, File file) {
				this.origin = origin;
				this.file = file;
			}

			@Override
			public Integer execute(ICancelabeEditSession editSession) throws MaxChangedBlocksException {
				try {
					ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(origin);
					if (region != null) {
						editSession.setMask(new RegionMask(WorldEditHandler.getRegion(origin.getWorld(), region)));
					}
					ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(this.file);
					try (InputStream in = new FileInputStream(this.file)) {
						editSession.enableQueue();
						Clipboard clipboard = clipboardFormat.getReader(in).read();
						Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
								.to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
								.ignoreAirBlocks(true).build();
						Operations.completeBlindly(operation);
						editSession.flushSession();
					}
				} catch (IOException e) {
					log.log(Level.INFO, "Unable to paste schematic " + file, e);
				}
				return 32768;
			}
		}

		@Override
		public void onDisable(Plugin plugin) {

		}

		@Override
		public EditSession createEditSession(World world, int maxBlocks) {
			return WorldEditHandler.createEditSession(world, maxBlocks);
		}

		@Override
		public void regenerate(final Region region, final Runnable onCompletion) {
			uSkyBlock.getInstance().sync(new Runnable() {
				@Override
				public void run() {
					try {
						final EditSession editSession = WorldEditHandler.createEditSession(region.getWorld(),
								region.getArea() * 255);
						editSession.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
						editSession.setFastMode(true);
						editSession.getWorld().regenerate(region, editSession);
						editSession.flushSession();
					} finally {
						onCompletion.run();
					}
				}
			});
		}
	};
}
