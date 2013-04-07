package lihad.FreeRegion;

import java.util.*;
import java.util.logging.Logger;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.*;

public class FreeRegion extends JavaPlugin implements Listener{
	protected static String header = "[FreeRegion]";
	private static Logger log = null;

	public int limit = 0;
	public Location currentFreeRegion;
	public List<Location> locations_iter = new LinkedList<Location>();
	public String freeRegionWorld;
	public Long timing;
	public int locations_number = 0;
	public int protected_area = 0;
	
	List<Material> legalItems = null;

	@Override
	public void onEnable() {
		log = getLogger();
		load();
		
		while(locations_iter.size()<locations_number){
			locations_iter.add(getFreeRegion());
		}
		
		this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable(){
			public void run() {
				if(locations_iter.isEmpty()){
					warning("Region rotation empty.  Holding on last.");			
				}else{
					if(currentFreeRegion != null)locations_iter.add(currentFreeRegion);
					currentFreeRegion = locations_iter.remove(0);
					info("Changing freeregion");
				}
			}
		}, 0, timing);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	void load() {
		reloadConfig();
		ConfigurationSection config = getConfig();
		limit = config.getInt("limit");
		freeRegionWorld = config.getString("world");
		timing = config.getLong("timing");
		locations_number = config.getInt("locationsnum");
		protected_area = config.getInt("protected_area");
		protected_area *= protected_area;	// Square it to use with distanceSquared

		legalItems = loadMaterialList(config, "allowed_items");
	}
	
    public static List<Material> loadMaterialList(ConfigurationSection config, String section) {
        List<String> stringList = config.getStringList(section);
        List<Material> mats;
        if (stringList != null && stringList.size() > 0) {
            mats = new ArrayList<Material>();
            for (String s : stringList) {
                Material m = Material.matchMaterial(s);
                if (m == null) {
                    severe("Unknown material " + s);
                }
                else {
                    mats.add(m);
                }
            }
        }
        else {
            mats = new ArrayList<Material>(0);
        }
        return mats;
    }

	public Location getFreeRegion(){
		int leavecount = 0;
		World world = getServer().getWorld(freeRegionWorld);
		Block prospect = world.getBlockAt(new Random().nextInt(limit*2)-limit, 64, new Random().nextInt(limit*2)-limit);
		try{
			for(int x = -50;x<50;x++){
				for(int y = 0;y<25;y++){
					for(int z = -50;z<50;z++){
						if(prospect.getRelative(x, y, z).getTypeId() == 18) leavecount++;
					}
				}
			}
			if(leavecount > 100){
				prospect = world.getHighestBlockAt(prospect.getLocation()).getRelative(0, 3, 0);
				
				return prospect.getLocation();
			}else{
				return getFreeRegion();
			}
		}catch(Exception e){
			severe("Issue occurred that ripped the startup script apart.  Crash?  Spawn is now FR");
			return this.getServer().getWorld(freeRegionWorld).getSpawnLocation();
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player)sender;
		if(cmd.getName().equalsIgnoreCase("freeregion")) {
			if(currentFreeRegion == null)player.sendMessage("Try again later: Location not set");
			else player.teleport(currentFreeRegion);
			return true;
		}else if(cmd.getName().equalsIgnoreCase("fr") && sender.isOp()){
			if(args.length == 1){
				if(args[0].equalsIgnoreCase("new")){
					sender.sendMessage(ChatColor.GREEN+"Setting random new FreeRegion");
					currentFreeRegion = getFreeRegion();
				}else if(args[0].equalsIgnoreCase("set")){
					sender.sendMessage(ChatColor.GREEN+"Setting new FreeRegion at current location");
					currentFreeRegion = ((Player)sender).getLocation();
				}else if (args[0].equalsIgnoreCase("reload")) {
					load();
				}
			}else{
				sender.sendMessage("fr <argument>");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerDamage(EntityDamageByEntityEvent event){
		if(event.getEntity() instanceof Player){
			if(event.getEntity().getLocation().distanceSquared(currentFreeRegion) < protected_area){
				if ((event.getDamager() instanceof Player) && ((Player)event.getDamager()).isOp()) {
					// Ops can kill you
				}
				else {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Location to = event.getTo();
		
		if (to.distanceSquared(currentFreeRegion) < protected_area) {
			Player player = event.getPlayer();
			if (player.isOp() || player.hasPermission("freeregion.freeteleport")) {
				// These people can keep their shit.
				return;
			}

			// Otherwise, strip everything in the inventory that isn't on the allowed list
			Location from = event.getFrom();
			PlayerInventory inv = player.getInventory();
			World world = player.getWorld();
			
			for (ItemStack i : inv.getContents()) {
				if (i == null || i.getTypeId() == 0) {
					continue;
				}
				if (legalItems.contains(i.getType())) {
					continue;
				}

				inv.removeItem(i);
				world.dropItemNaturally(from, i);
			}
			ItemStack[] armors = inv.getArmorContents();
			for (int j = 0; j < armors.length; j++) {
				ItemStack i = armors[j];
				if (i == null || i.getTypeId() == 0) {
					continue;
				}
				if (legalItems.contains(i.getType())) {
					continue;
				}
				armors[j] = null;
				world.dropItemNaturally(from, i);
			}
			inv.setArmorContents(armors);
			
			player.sendMessage(""+ChatColor.RED + "Welcome to FreeRegion, a place for NOOBS!");
			player.sendMessage(""+ChatColor.RED + "Established players are asked to leave their advanced tools and armor behind.");
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
