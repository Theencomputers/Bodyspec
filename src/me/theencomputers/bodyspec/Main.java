package me.theencomputers.bodyspec;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener{
	  private Map<String, String> teams = new HashMap<String, String>(10);
	  private Map<String, String> inbodyspec = new HashMap<String, String>(10);
	  private Map<String, String> dead = new HashMap<String, String>(10);
	  static boolean deathremove = true;
	  public void onEnable(){
	 		Bukkit.getServer().getPluginManager().registerEvents((Listener)this, this);
	 		this.getCommand("bodyspec").setExecutor(this);
	}
	  @EventHandler
	  	public void playerDeath(PlayerDeathEvent e) {
		  Player p = e.getEntity().getPlayer();
		  p.sendMessage("you died!");
		  if(deathremove) {
			  p.sendMessage("you have been added to dead list");
			  dead.put(p.getName().toString(), "dead");
		  }
			  }
	  @EventHandler
	  	public void specTeleporterUse(InventoryClickEvent e) {
		  Player p = (Player) e.getWhoClicked();
//		  p.sendMessage("clicked");
		  if(p.getOpenInventory() == null && inbodyspec.containsKey(p.getUniqueId().toString())) {
			 e.setCancelled(true);
		  }
			  }
	  @EventHandler
	  	public void specTeleport(PlayerTeleportEvent e) {
		  Player p = e.getPlayer();
		  if(e.getCause().toString().equals("SPECTATE") && (inbodyspec.containsKey(p.getUniqueId().toString()))) {
			  e.setCancelled(true);
			  e.setTo(Bukkit.getPlayer(inbodyspec.get(p.getUniqueId().toString())).getLocation());
			  p.setSpectatorTarget(Bukkit.getPlayer(inbodyspec.get(p.getUniqueId().toString())));
			  }
	  }
	  @EventHandler
	  	public void leaveBodySpec(PlayerToggleSneakEvent e) {
		  Player p = e.getPlayer();
			  if(p.getGameMode().equals(GameMode.SPECTATOR) && (inbodyspec.containsKey(p.getUniqueId().toString()))) {
				  e.setCancelled(true);
				  if (p.isSneaking()) {
					  p.sendMessage(ChatColor.RED + "In order to leave Body Spectating Do /bodyspecleave");
				  }
//				  p.setSpectatorTarget(null);
	//			  p.setSpectatorTarget(inbodyspec.get(p));
			  }

		  
	  }
	  @SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
			 if (cmd.getName().equalsIgnoreCase("bodyspecadmin")) {
				 	if(args.length > 0) {
				 		if (args[0].equalsIgnoreCase("sync")) {
				 			if (args.length == 2) {
				 				OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
				 				try {teams.remove(p.getPlayer().getUniqueId().toString());}
				 				catch(Exception e) {}
					    		try{teams.put(p.getPlayer().getUniqueId().toString(), p.getPlayer().getScoreboard().getPlayerTeam(p.getPlayer()).toString());
					    		sender.sendMessage(ChatColor.GREEN + "Successifully synced player named " + p.getPlayer().getDisplayName());
					    		}
					    		catch (Exception e) {sender.sendMessage(ChatColor.RED + "Could not find that player");}
				 			}
				 			else if (args.length == 1) {
				 				teams.clear();
				 				dead.clear();
				 				for (Player loopplayer : Bukkit.getOnlinePlayers()) {
				 				try {teams.remove(loopplayer.getPlayer().getUniqueId().toString());}
				 				catch(Exception e) {}
					    		try{teams.put(loopplayer.getPlayer().getUniqueId().toString(), loopplayer.getPlayer().getScoreboard().getPlayerTeam(loopplayer.getPlayer()).toString());
					    		sender.sendMessage(ChatColor.GREEN + "Successifully synced player named " + loopplayer.getPlayer().getDisplayName());
					    		}
					    		catch (Exception e) {sender.sendMessage(ChatColor.RED + "Could not find that player");}
				 				}
				 			}	else {sender.sendMessage(ChatColor.RED + "Usage /bodyspecadmin sync <plyaer>");}
				 			}

				 		}
				 
				 	if(args.length == 2 && args[0].equalsIgnoreCase("setdead")) {
				 		OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
				 		try {dead.remove(p.getName());}
				 		catch(Exception e) {}
				 		try {dead.put(p.getName(), "dead");
				 		sender.sendMessage(ChatColor.GREEN + "Player has been set to dead");}
				 		catch(Exception e) {sender.sendMessage(ChatColor.RED + "Could not find Player");}
				 	}
				 	if(args.length == 2 && args[0].equalsIgnoreCase("setalive")) {
				 		OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
				 		try {dead.remove(p.getName());}
				 		catch(Exception e) {sender.sendMessage(ChatColor.RED + "That player is not dead.");}
		 				try {teams.remove(p.getPlayer().getUniqueId().toString());}
		 				catch(Exception e) {}
			    		try{teams.put(p.getPlayer().getUniqueId().toString(), p.getPlayer().getScoreboard().getPlayerTeam(p.getPlayer()).toString());
			    		sender.sendMessage(ChatColor.GREEN + "Added Player to alive list");
			    		
			    		}
			    		catch(Exception e) {sender.sendMessage(ChatColor.RED + "Could not find player");}
				 	}
				 	if(args.length > 0 && args[0].equalsIgnoreCase("deathremove")) {
				 		if(args.length == 2) {
				 			if(args[1].equalsIgnoreCase("true")) {
				 				deathremove = true;
				 			}
				 			else if(args[1].equalsIgnoreCase("false")) {
				 				deathremove = false;
				 			}
				 			else {sender.sendMessage(ChatColor.RED + "Error Usage: /deathremove <true|false>");}
				 		}
				 		else {sender.sendMessage(ChatColor.RED + "Error Usage: /deathremove <true|false>");}
				 	}
				 	}
				    

				    
			 if (cmd.getName().equalsIgnoreCase("bodyspecleave")) {
				if(sender instanceof Player) {
					Player p = (Player) sender;
					inbodyspec.remove(p.getUniqueId().toString());
					p.setSpectatorTarget(null);
					p.teleport(p.getBedSpawnLocation());
					p.setGameMode(GameMode.SURVIVAL);
					p.openInventory(p.getInventory());
					
				}
				    }
			 if (cmd.getName().equalsIgnoreCase("bodyspec")) {
				 if(args.length == 1) {
					 if(sender instanceof Player) {
						 Player p = (Player) sender;
							if(dead.containsKey(p.getName().toString())) {
						 String teamname;
						 if(teams.containsKey(p.getUniqueId().toString())) {
							sender.sendMessage("contains" );
							 teamname = teams.get(p.getUniqueId().toString());
				    for(Player loopplayer : Bukkit.getOnlinePlayers()){
				    	if(loopplayer.getName().equals(args[0])) {
							sender.sendMessage("is arg0 " + loopplayer.toString());
				    	if(teams.containsKey(loopplayer.getUniqueId().toString())) {
							sender.sendMessage("found target" );
				    		if(teamname.equals(teams.get(loopplayer.getUniqueId().toString()))){
								sender.sendMessage("right team" );
				    		inbodyspec.put(p.getUniqueId().toString(), loopplayer.getUniqueId().toString());
				    		p.setGameMode(GameMode.SPECTATOR);
				    		p.setSpectatorTarget(null);
				    		p.teleport(loopplayer.getLocation());
							Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
							    @Override
							    public void run() {
						    		p.setSpectatorTarget(loopplayer);							    }
							}, 5L);
				    	}
				    		else{sender.sendMessage(teams.get(loopplayer.getUniqueId().toString()) + " teamname " + teamname) ;}
				    		}
				    	}}				    }
					 else {sender.sendMessage(ChatColor.RED + "This command must be preformed in-game" );}
					 }else {sender.sendMessage("You cannot Preform this command whilist alive");}}
				 else {
					 String teamdebug;
					teamdebug = teams.toString();
					sender.sendMessage(teamdebug);
				 }
				 
			//	 else {sender.sendMessage(ChatColor.RED + "Error Usage: /bodyspectate <player>" );}
				    
			 }
			 }
		  return true;
	  

	  }}
