package lihad.FreeRegion;

import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FreeRegion extends JavaPlugin implements Listener{
	public static FileConfiguration config;
	protected static String PLUGIN_NAME = "FreeRegion";
	protected static String header = "[" + PLUGIN_NAME + "] ";
	private static Logger log = Logger.getLogger("Minecraft");
	public static int limit = 0;
	public static Location location;
	public static String world;
	public static Long timing;
	public static boolean useTiming;

	@Override
	public void onEnable() {
		config = getConfig();
		limit = config.getInt("limit");
		world = config.getString("world");
		timing = config.getLong("timing");
		useTiming = config.getBoolean("useTiming");
		
		info("Finding new region....");
		location = getFreeRegion();
		info(".... New Region Found!!!");
		
		if(useTiming)this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable(){
			public void run() {

			}
		}, 0, timing);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	public Location getFreeRegion(){
		int leavecount = 0;
		Block prospect = this.getServer().getWorld(world).getBlockAt(new Random().nextInt(limit*2)-limit, 64, new Random().nextInt(limit*2)-limit);
		try{
			for(int x = -25;x<25;x++){
				System.out.println((x+25)+"%");
				for(int y = 0;y<25;y++){
					for(int z = -25;z<25;z++){
						if(prospect.getRelative(x, y, z).getTypeId() == 18) leavecount++;
					}
				}
			}

			if(leavecount > 100){
				while(prospect.getTypeId() != 0 && prospect.getRelative(0, 1, 0).getTypeId() != 0){
					prospect = prospect.getRelative(0, 1, 0);
				}
				return prospect.getLocation();
			}else{
				return getFreeRegion();
			}
		}catch(Exception e){
			severe("Issue occurred that ripped the timer apart.  Crash?  Spawn is now FR");
			return this.getServer().getWorld(world).getSpawnLocation();
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player)sender;
		if(cmd.getName().equalsIgnoreCase("freeregion")) {
			if(location == null)player.sendMessage("Try again later: Location not set");
			else player.teleport(location);
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event){
		if(event.getEntity() instanceof Player){
			if(event.getEntity().getLocation().distance(location) <20){
				event.setCancelled(true);
			}
		}
	}
	
	public static void info(String message){ 
		log.info(header + ChatColor.WHITE + message);
	}
	public static void severe(String message){
		log.severe(header + ChatColor.RED + message);
	}
	public static void warning(String message){
		log.warning(header + ChatColor.YELLOW + message);
	}
	public static void log(java.util.logging.Level level, String message){
		log.log(level, header + message);
	}
}
