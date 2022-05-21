/* 
	Title: bodyspec
	Author: Theencomputers
	Last Updated: 05-20-2022
	Version: 0.0.1
*/


package me.theencomputers.bodyspec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import io.netty.channel.*;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;

public class main extends JavaPlugin implements Listener{
		//Honestly there are probably better ways to do this but this is the bes I know how
	private Map<String, String> playersToTeamName = new HashMap<String, String>(10);			//Hashmap stores all the players as keys and all the team names as values
	private Map<String, String> inBodySpecToTarget = new HashMap<String, String>(10);		//Hashmap stores everyone in bodyspec mode as key and the person they are bodyspectating as value
	private Map<String, String> targetToInBodySpec = new HashMap<String, String>(10);		//Hashmap sames as inBodySpecToTarget but reversed: target as key and spectator as value
	ArrayList<String> optOutList= new ArrayList<String>();
	ArrayList<String> deadList= new ArrayList<String>();


	static boolean isDeathRemoveOn = true;	//bool that is true if players are auto removed after death
	static boolean isBodyspecEnabled = true;

private void removeFromBodyspec(String player){		//function that removes a player from bodyspectating
	OfflinePlayer oPlayer = Bukkit.getServer().getOfflinePlayer(player.toString());
	if(oPlayer.isOnline()){
	try {
		Player p = Bukkit.getServer().getPlayer(player);
		if(inBodySpecToTarget.containsKey(p.getName().toString())){
		try {
			targetToInBodySpec.remove(inBodySpecToTarget.get(p.getName().toString()));
		} catch (Exception e) {
		}
			inBodySpecToTarget.remove(p.getName().toString());
			p.setSpectatorTarget(null);
			p.performCommand("spawn");
			p.setGameMode(GameMode.SURVIVAL);
			p.setFlySpeed(0.3F);
			p.setFlying(false);
		}
	} catch (Exception e) {
		//TODO: handle exception
	}
}
	else{
		try {
			oPlayer.getPlayer().setSpectatorTarget(null);
			//handle offline player
			oPlayer.getPlayer().setGameMode(GameMode.SURVIVAL);
			inBodySpecToTarget.remove(oPlayer.getName().toString());
		} catch (Exception e) {
			//TODO: handle exception
		}
	}
}
private void putInBodySpec(String spectator, String target){
	try {
		Player p = Bukkit.getServer().getPlayer(spectator);
		Player tPlayer = Bukkit.getServer().getPlayer(target);


		tPlayer.sendMessage(ChatColor.RED + "You are being body spectated by " + p.getName().toString() + " if you do not wish to be body spectated please do /bodyspecoptout!");
		tPlayer.playSound(p.getLocation(), Sound.LEVEL_UP, 2F, 1F);		//play sound and send a message to alert the player that they are being bodyspecced		NEW

		inBodySpecToTarget.put(p.getName().toString(), tPlayer.getName().toString());		//add player to hashmap with value target
		targetToInBodySpec.put(tPlayer.getName().toString(), p.getName().toString());		//add target to hashmapa with value player

		p.setGameMode(GameMode.ADVENTURE);
		//put them in bodyspec
		new BukkitRunnable() {
			int iteration = 1;
			public void run() {
				if(iteration == 1){
					blockPackets(p);
					p.setGameMode(GameMode.SPECTATOR);
					p.setFlySpeed(0);
					p.setSpectatorTarget(null);
					p.teleport(tPlayer.getLocation());
				}
				if(iteration == 2){
					p.setSpectatorTarget(tPlayer);
					cancel();
				}
				iteration++;
			}
		}.runTaskTimer(this, 1, 5);
	} catch (Exception e) {
		//TODO: handle exception
	}
}


	public void onEnable(){
		Bukkit.getServer().getConsoleSender().sendMessage("sanity test"); //debug message feel free to remove
	 	Bukkit.getServer().getPluginManager().registerEvents((Listener)this, this); //set event listener to this class
	 	this.getCommand("bodyspec").setExecutor(this);	//set command executor to this class
	    
	 	
	}

    private void blockPackets(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
                if(packet instanceof PacketPlayOutPlayerInfo){	
					stopBlockingPacket(player);
					return;
				}
                super.write(channelHandlerContext, packet, channelPromise);
            }

        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);
    }
	private void stopBlockingPacket(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
            return null;
        });
    }
	@EventHandler
	public void playerClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(inBodySpecToTarget.containsKey(e.getPlayer().getName().toString())){
			e.setCancelled(true);
			//putInBodySpec(e.getPlayer().getName(), inBodySpecToTarget.get(e.getPlayer().getName().toString()));
			Player target = Bukkit.getPlayer(inBodySpecToTarget.get(e.getPlayer().getName().toString()));
			new BukkitRunnable() {
				int iteration = 1;
				public void run() {
					if(iteration == 1){
						p.setSpectatorTarget(null);
						p.teleport(target.getLocation());
					}
					if(iteration == 2){
						p.setSpectatorTarget(target);
						cancel();
					}
					iteration++;
				}
			}.runTaskTimer(this, 1, 5);
		}

	}
	@EventHandler
	public void playerDeath(PlayerDeathEvent e) {	//fired when player dies and adds them to the death list if 
		Player p = e.getEntity().getPlayer();
		if(targetToInBodySpec.containsKey(p.getName().toString())){
			removeFromBodyspec(targetToInBodySpec.get(p.getName().toString()));
			Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString())).sendMessage(ChatColor.RED + "The person you were body spectating died.");

		}
		if(isDeathRemoveOn) {
			deadList.add(p.getName().toString());
		  	}

		}


	@EventHandler
	public void playerJoin(PlayerJoinEvent e) {	//fired on joining to handle a spectator that relogged
		//Bukkit.getServer().getConsoleSender().sendMessage("In Join event");	//debug message
		Player p = e.getPlayer();

		if(inBodySpecToTarget.containsKey(p.getName().toString())) {	//check if player is in bodyspec			ADD CHECK FOR TARGET LOGGING OUT
			Player target = Bukkit.getServer().getPlayer(inBodySpecToTarget.get(p.getName().toString()));
			if(isBodyspecEnabled && target.isOnline()){
			  //if the spectator logs back in this will reset them
			p.sendMessage(p.getName().toString() + target.getName().toString());
			putInBodySpec(p.getName().toString(), target.getName().toString());
			}
			else{		//handle bodyspec being disabled when a spec logs out
				removeFromBodyspec(p.getName().toString());
	}
}


	}
	@EventHandler
	public void playerLeave(PlayerQuitEvent e) {	//handle target logging out
		//Bukkit.getServer().getConsoleSender().sendMessage("In Quit event");	//debug message
		OfflinePlayer p = e.getPlayer();

		if(targetToInBodySpec.containsKey(p.getName().toString())) {	//see if the player that logged out was being specced
			OfflinePlayer spectator = Bukkit.getOfflinePlayer(targetToInBodySpec.get(p.getName()));	//offline players in case the player they typed doesnt exist
			if(spectator.isOnline()){
	    		Player onlineSpectator = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName()));	//if so bring them back to spawn
				removeFromBodyspec(spectator.getName().toString());
	    		onlineSpectator.sendMessage(ChatColor.RED + "The player you where spectating logged out");	//tell the player what happened
			}
		}
		else if(inBodySpecToTarget.containsKey(p.getName().toString())){
			removeFromBodyspec(p.getName());

		}

	}


	@EventHandler
	public void teleportEvent(PlayerTeleportEvent e) {		//prevent the player from using spec shortcuts	also handles nether and ender pearl tp delay 		KINDA WORKING!
		Player p = e.getPlayer();


		if(e.getCause().equals(TeleportCause.NETHER_PORTAL)){			//sketchy fix for nether woosh glitch
			if(targetToInBodySpec.containsKey(p.getName().toString())){
				Player spec = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString()));
				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

					@Override
						public void run() {
							spec.teleport(p.getLocation());
						}
					}, 40L);
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

						@Override
							public void run() {
								spec.setSpectatorTarget(null);
								spec.setSpectatorTarget(p);
							}
						}, 45L);
			}
		}
		else if(e.getCause().toString().equals("SPECTATE") && (inBodySpecToTarget.containsKey(p.getName().toString()))) {	//issue lies here spec tp doesnt always return SPECTATE
			e.setCancelled(true);
			e.setTo(Bukkit.getPlayer(inBodySpecToTarget.get(p.getName().toString())).getLocation());
			p.setSpectatorTarget(Bukkit.getPlayer(inBodySpecToTarget.get(p.getName().toString())));
		}

		else if (targetToInBodySpec.containsKey(p.getName().toString())) {		//this accounts for nether and other teleport delay glitch

			Player spec = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString()));
			spec.setSpectatorTarget(null);		//set null to reset their spectator
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

				@Override
					public void run() {
						spec.teleport(p.getLocation());		//tp them to their target
					}
				}, 5L);		//wait 5 ticks to tp reset them as spectator
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			    public void run() {
					spec.setSpectatorTarget(null);		//set null to reset their spectator
		    		spec.setSpectatorTarget(p);
				}
			}, 10L);		//wait 5 ticks to tp reset them as spectator
			
		}
		
	}

	@EventHandler
	public void clickEvent(PlayerInteractEvent e){		//prevent the player from using spec shortcuts	also handles nether and ender pearl tp delay 		KINDA WORKING!
		Player p = e.getPlayer();

		if(inBodySpecToTarget.containsKey(p.getUniqueId().toString()) && p.getGameMode().equals(GameMode.SPECTATOR)){
			p.setSpectatorTarget(null);
			p.setSpectatorTarget(Bukkit.getServer().getPlayer(inBodySpecToTarget.get(p.getUniqueId().toString())));
		}
		
	}

	@EventHandler
	public void leaveBodySpec(PlayerToggleSneakEvent e) {		//this prevents them from leaving bodyspec by canceling sneak
		Player p = e.getPlayer();

			if(p.getGameMode().equals(GameMode.SPECTATOR) && (inBodySpecToTarget.containsKey(p.getName().toString()))) {
				e.setCancelled(true);	//cancel the sneak event
				if (p.isSneaking()) {
					  p.sendMessage(ChatColor.RED + "In order to leave Body Spectating Do /bodyspecleave");		//lets them know that is not how it works
				}
//				  p.setSpectatorTarget(null);		//old code delete
	//			  p.setSpectatorTarget(inbodyspec.get(p));		//old code delete
			}		  
	  }

	@SuppressWarnings("deprecation")		//bukkit doesnt like getPlayer I will use uuids in the future but names are easier for testing
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("bodyspecadmin")) {	//bodyspec admin command
			if(!sender.isOp()){
				if(sender instanceof Player){
					Player sPlayer = (Player) sender;
					if(!sPlayer.hasPermission("bodyspec.admin"))
						sender.sendMessage(ChatColor.RED + "Sorry! you do not have permission to use this command");
						return true;
				}
			}

			if(args.length > 0) {	//check if they typed anything more than just /bodyspecadmin 

				if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on")) {			//enable bodyspec
					if(sender.isOp()){																//FIXME	remove once permissions are fixed
						isBodyspecEnabled = true;
						sender.sendMessage(ChatColor.GREEN + "Bodyspec has been is enabled");
					}
					else{
						sender.sendMessage(ChatColor.RED + "You do not have permission to do this");
					}
				}
				if (args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off")) {		//enable bodyspec
					if(sender.isOp()){																//FIXME	remove once permissions are fixed
						isBodyspecEnabled = false;
						sender.sendMessage(ChatColor.RED + "Bodyspec has been is disabled");
						for (Player iPlayer : Bukkit.getOnlinePlayers()) {		//loop through all players and see if they are in bodyspec if yes remove them
							if(inBodySpecToTarget.containsKey(iPlayer.getName().toString())){
								removeFromBodyspec(iPlayer.getName().toString());
								iPlayer.sendMessage(ChatColor.RED + "Sorry! Body Spectating has been disabled");
							}

						}

					}
					else{
						sender.sendMessage(ChatColor.RED + "You do not have permission to do this");
					}
				}
				if (args[0].equalsIgnoreCase("sync")) {	//for sync

					if (args.length == 2) { //if we are syncing an individual player eg latescatter /bodyspecadmin theencomputers

				 		OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);	//offline players in case the player they typed doesnt exist
				 		try {	//try to remove them if they already exist eg team change
							playersToTeamName.remove(p.getPlayer().getName().toString());
						}
				 		catch(Exception e) {}

					    try{	//try to add player
							playersToTeamName.put(p.getPlayer().getName().toString(), p.getPlayer().getScoreboard().getPlayerTeam(p.getPlayer()).toString());
					    	sender.sendMessage(ChatColor.GREEN + "Successfully synced player named " + p.getPlayer().getDisplayName());
					    }
					    catch (Exception e) {	//if it fails then the player doesnt exist
					    	if(p.isOnline()) {
					    		sender.sendMessage(ChatColor.RED + p.getName() + " is a solo");
									}	//handles solos
					    	else {sender.sendMessage(ChatColor.RED + "Error: could not find player named " + args[1]);}	
							
					    }
				 	}

				 	else if (args.length == 1) {		//in case the command is /bodyspecadmin sync this syncs all players
						playersToTeamName.clear();	//clear hasmap
						deadList.clear();			//clear list		NEW actually not new
		 				for (Player iPlayer : Bukkit.getOnlinePlayers()) {	//iterates through all players setting them to iPlayer
						//			 				try {playersToTeamName.remove(iPlayer.getPlayer().getName().toString());}	//commented as this is likely not needed
						//			 				catch(Exception e) {}

					    	try{		//try and catch incase player is a solo
								playersToTeamName.put(iPlayer.getPlayer().getName().toString(), iPlayer.getPlayer().getScoreboard().getPlayerTeam(iPlayer.getPlayer()).toString());
					    		sender.sendMessage(ChatColor.GREEN + "Successfully synced player named " + iPlayer.getPlayer().getDisplayName());		//debug message
					    	}
					    	catch (Exception e) {
								sender.sendMessage(ChatColor.RED + iPlayer.getName() + " is a solo");	//debug message
							}
				 		}
				 	}
					else {
						sender.sendMessage(ChatColor.RED + "Usage /bodyspecadmin sync <plyaer>");
					}	//invalid amount of args
				}
			}
				 
			if(args.length == 2 && args[0].equalsIgnoreCase("setdead")) {	//for /bodyspecadmin setdead <player>
				OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);		//offline player incase the player doesnt exist
				try {	//remove incase player is already dead
					deadList.remove(p.getName().toString());	//NEW
				}
				catch(Exception e) {}

				try {		//put in dead list
					//deadList.put(p.getName(), "dead");		//OLD
					deadList.add(p.getName().toString());		//NEW

				 	sender.sendMessage(ChatColor.GREEN + "Player has been set to dead");}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Could not find Player");	//player doesnt exist
				}

			}

			if(args.length == 2 && args[0].equalsIgnoreCase("setalive")) {		//bodyspecadmin setalive <player>
				OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);

				try {		//remove from deadlist
					deadList.remove(p.getName());
				}	//catch in case they are already alive
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "That player is not dead.");
				}
		 		//remove player from hashmap		
				try {
					playersToTeamName.remove(p.getPlayer().getName().toString());
				}
		 		catch(Exception e) {}	//catch in case player is not in the hasmap yet

			    try{	//add player to hashmap
					playersToTeamName.put(p.getPlayer().getName().toString(), p.getPlayer().getScoreboard().getPlayerTeam(p.getPlayer()).toString());
			    	sender.sendMessage(ChatColor.GREEN + "Added Player to alive list");
			    }
			    catch(Exception e) {		//in case player doesnt exist 		MAKE SURE THIS ACTUALLY WORKS
					sender.sendMessage(ChatColor.RED + "Could not find player");
				}

			}

			if(args.length > 0 && args[0].equalsIgnoreCase("deathremove")) {	//in case the command is /bodyspecadmin deathremove to change the state of deathremove

				//if deathremove is on then when a player dies it automatically adds the player to the dead list
				if(args.length == 2) {

				 	if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("enable")) {
						isDeathRemoveOn = true;
				 		sender.sendMessage(ChatColor.GREEN + "Death Remove enabled");
				 	}

				 	else if(args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("disable")) {
						isDeathRemoveOn = false;
				 		sender.sendMessage(ChatColor.GREEN + "Death Remove disabled");
				 	}
				 	else {
						 sender.sendMessage(ChatColor.RED + "Error Usage: /bodyspecadmin deathremove <true|false>");
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Error Usage: /bodyspec admindeathremove <true|false>");
				}
			}
		}
				    

		if (cmd.getName().equalsIgnoreCase("bodyspecleave")) {		//for command /bodyspecleave  FIXME give this messages and logic
				//handle player leaving bodyspec
			if(sender instanceof Player) {
				Player p = (Player) sender; //requires cast to player
			if(inBodySpecToTarget.containsKey(p.getName().toString())){
				removeFromBodyspec(p.getName().toString());
//				p.openInventory(p.getInventory());			//expirimental
			}
			else{
				p.sendMessage(ChatColor.RED + "You are not bodyspectating anyone");
			}
			}
				    
		}
			 //CHANGE TO HAVE A FILE optout.yml to store all the optout names
		if (cmd.getName().equalsIgnoreCase("bodyspecoptout")) {		//for command /bodyspecoptout to comply with advisories rules

			if(sender instanceof Player) {
				//if(optOutList.containsKey(sender.getName().toString())) {	//check if they have already opted out 		OLD
				if(optOutList.contains(sender.getName().toString())) {		//NEW
					sender.sendMessage(ChatColor.RED + "You have already opted out of bodyspectating. To opt back in do /bodyspecoptin");
				}
				else {
			//		optOutList.put(sender.getName().toString(), "optout");		//meaningless value NEEDS TO BE UPDATED		OLD
					optOutList.add(sender.getName().toString());		//NEW
					sender.sendMessage(ChatColor.RED + "You have opted out of bodyspectating. To opt back in do /bodyspecoptin");
					Player p = (Player) sender;
					if(targetToInBodySpec.containsKey(p.getName().toString())){
						Player rPlayer = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString()));
						removeFromBodyspec(rPlayer.getName().toString());
				}	
				}
			 		
				}

		}
			 //for command /bodyspecoptin
		if (cmd.getName().equalsIgnoreCase("bodyspecoptin")) {

			if(sender instanceof Player) {
				//if(optOutList.containsKey(sender.getName().toString())) {		//see if they have opted out		OLD
				if(optOutList.contains(sender.getName().toString())) {		//NEW
					optOutList.remove(sender.getName().toString());		//remove them from optout list		OLD actually not
					sender.sendMessage(ChatColor.GREEN + "You have opted in! You may now be body spectated by teammates. To opt back out do /bodyspecoptout");
				}
				else {		//they have not opted out do this
					sender.sendMessage(ChatColor.RED + "You have not opted out of bodyspectating! To opt out do /bodyspecoptout.");
				}			 		
			}
		}

		if (cmd.getName().equalsIgnoreCase("bodyspec")) {	//for the command /bodyspec

			if(isBodyspecEnabled){
				if(!sender.isOp())
					if(sender instanceof Player){
						if(!sender.hasPermission("bodyspec.use")){
							sender.sendMessage(ChatColor.RED + "Sorry! you do not have permission to use that command!");
						}
					}

			if(args.length == 1) { //is true when there is one argument eg /bodyspec theencomputers

					if(sender instanceof Player) {

						Player p = (Player) sender;

						//if(deadList.containsKey(p.getName().toString())) {		//see if deadlist contains player name		OLD
						if(deadList.contains(p.getName().toString())) { //NEW works

							String teamName;

							if(playersToTeamName.containsKey(p.getName().toString())) {		

								//sender.sendMessage("contains" );		//debug message
								teamName = playersToTeamName.get(p.getName().toString());	//set team name to the value of the hashmap
								Player target = Bukkit.getServer().getPlayer(args[0]);
				    			if(target.isOnline()) {	//see if the player is online 
									if(!deadList.contains(target.getName().toString())){	//NEW works
				    				if(playersToTeamName.containsKey(target.getName().toString())) {		//see if target is on the same team first see if it exists

				    					if(teamName.equals(playersToTeamName.get(target.getName().toString()))){	//see if they are on the same team

				    						//if(!optOutList.containsKey(target.toString())) {	//check if player has not opted out to comply with uhc rules		OLD
											if(!optOutList.contains(target.getName().toString())) {	//NEW			untested								
												putInBodySpec(p.getName().toString(), target.getName().toString());
				    						}
				    						else {	//in case target is opted out
												sender.sendMessage(ChatColor.RED + "That player is not allowing others to bodyspec them if this is a mistake ask them to do /bodyspecoptin");
											}	
				    					}

				    					else{	//in case player is not on the same team as the target
											sender.sendMessage(ChatColor.RED + "Sorry, you can only bodyspectate teammates.");
										}
				    				}
								}//new
								else{
									sender.sendMessage(ChatColor.RED + "Error: You cannot bodyspectate a dead player");
								}
				    			}
								else{
									sender.sendMessage(ChatColor.RED + "Error: player is offline");
								}
							}
						}
					 	else {		//in case it is preformed by the console
							sender.sendMessage("You cannot Preform this command whilist alive");
						}
						
					}
					else {		//in case the player is still alive and trying to bodyspec while alive
						sender.sendMessage(ChatColor.RED + "This command must be preformed in-game" );
					}
			}
			else {	//in case they inputed too many arguments
				sender.sendMessage(ChatColor.RED + "Error usage: /bodyspec <player>");
			}

	}	
		else{
			sender.sendMessage(ChatColor.RED + "Bodyspec is currently diabled");
		}			 				    
		}
 		return true;
	}
}



/*		TO-DO
	- make toggle sneak resync spec if they glitch out and always cancel
	- add configuration file
		-add configurable spawn and logout handler
	- thourogh testing
	- add permissions
	- bodyspec disable **added untested old statements commented**


			FEATURES TO ADD
	- shift spec among teammates
	- inventory viewer
	- allow admin to force bodyspec
	- seperate features for admins in bodyspec
*/
