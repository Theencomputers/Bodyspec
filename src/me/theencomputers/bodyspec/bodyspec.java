/* 
	Title: Bodyspec
	Author: Theencomputers
	Last Updated: 09/16/2023
	Version: 0.7.0
*/


package me.theencomputers.bodyspec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;


public class bodyspec implements Listener{
	private final Launcher plugin;
        
    public bodyspec(Launcher plugin) {
        this.plugin = plugin;
    }
	public static Map<String, String> playersToTeamName = new HashMap<String, String>(10);			//Hashmap stores all the players as keys and all the team names as values
	public static Map<String, String> inBodySpecToTarget = new HashMap<String, String>(10);		//Hashmap stores everyone in bodyspec mode as key and the person they are bodyspectating as value
	public static Map<String, String> targetToInBodySpec = new HashMap<String, String>(10);		//Hashmap sames as inBodySpecToTarget but reversed: target as key and spectator as value
	static ArrayList<String> debounceList = new ArrayList<String>();											  //store those who have pressed shift so the shift feature isnt double executed or spammed
	static ArrayList<String> optOutList= new ArrayList<String>();												  //List that stores those who opped oput
	static ArrayList<Player> notifiedList= new ArrayList<Player>();												  //List that stores those who have been notified so that you dont get spammed notifications
	static ArrayList<String> deadList= new ArrayList<String>();													  //Lis that stores those UHC players that are no longer with us
	static ArrayList<String> packetBlocker= new ArrayList<String>();

	static boolean isDeathRemoveOn = true;	//bool that is true if players are auto removed after death
	static boolean isBodyspecEnabled = true;
	static boolean isBlockingPackets = false;
	static PacketListener armSwingPacketListener;
	static PacketListener playerInfoPacketListener;


public void removeFromBodyspec(String player){		//function that removes a player from bodyspectating
	OfflinePlayer oPlayer = Bukkit.getServer().getOfflinePlayer(player.toString());
	if(oPlayer.isOnline()){
	try {
		Player p = Bukkit.getServer().getPlayer(player);
		if(inBodySpecToTarget.containsKey(p.getName().toString())){
		try {
			targetToInBodySpec.remove(inBodySpecToTarget.get(p.getName().toString()));
		} catch (Exception e) {
		}
			p.sendMessage(ChatColor.RED + "You have been removed from body spectating.");
			packetHandler();	//updated packetHandler
			inBodySpecToTarget.remove(p.getName().toString());
			p.setSpectatorTarget(null);
			p.performCommand("spawn");
			p.setGameMode(GameMode.SURVIVAL);

			p.setFlySpeed(0.2F);
			p.setFlying(false);
		}
	} catch (Exception e) {}
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
public void packetHandler(){
	if(!inBodySpecToTarget.isEmpty() && !isBlockingPackets){
		Launcher.getProtocolManager().addPacketListener(new PacketAdapter(
				Launcher.getMainInstance(),
				ListenerPriority.NORMAL,
				PacketType.Play.Client.ARM_ANIMATION
		) {

			@Override
			public void onPacketReceiving(PacketEvent event) {
				armSwingPacketListener = this;
				Player p = event.getPlayer();
				if(p.getGameMode().equals(GameMode.SPECTATOR)){
					if(inBodySpecToTarget.containsKey(p.getName())){
						interactSpectator(p);
					}
				}
			}
		});
		isBlockingPackets = true;
	} else if (isBlockingPackets && inBodySpecToTarget.isEmpty() && armSwingPacketListener != null) {
		//kill the packet listeners
		isBlockingPackets = false;
		Launcher.protocolManager.removePacketListener(armSwingPacketListener);
	}
}
public void putInBodySpec(String spectator, String target){
	try {
		Player p = Bukkit.getServer().getPlayer(spectator);
		Player tPlayer = Bukkit.getServer().getPlayer(target);

		if(!notifiedList.contains(tPlayer)){
			tPlayer.sendMessage(ChatColor.RED + "You are being bodyspectated if you do not wish to be bodyspectated please do /bodyspecoptout");
			tPlayer.playSound(tPlayer.getLocation(), Sound.LEVEL_UP, 2F, 1F);		//play sound and send a message to alert the player that they are being bodyspecced
			notifiedList.add(tPlayer);
		}

		inBodySpecToTarget.put(p.getName().toString(), tPlayer.getName().toString());		//add player to hashmap with value target
		targetToInBodySpec.put(tPlayer.getName().toString(), p.getName().toString());		//add target to hashmapa with value player

		//p.setGameMode(GameMode.ADVENTURE);
		//put them in bodyspec
		new BukkitRunnable() {
			int iteration = 1;
			public void run() {
				if(iteration == 1){
					if(!p.getGameMode().equals(GameMode.SPECTATOR)){
						blockPackets(p);			//blocks player info packet disabling highlight players and such		
						p.setGameMode(GameMode.SPECTATOR);
					}
					p.setFlySpeed(0);
					p.setSpectatorTarget(null);		//double bodyspectating causes issues sometimes needs time gap
					p.teleport(tPlayer.getLocation());
					tPlayer.getLocation().setPitch(tPlayer.getLocation().getPitch());		//helps stop pitch de sync which causes head to be unaligned behavior
					tPlayer.getLocation().setYaw(tPlayer.getLocation().getYaw());
					p.getLocation().setPitch(tPlayer.getLocation().getPitch());
					p.getLocation().setYaw(tPlayer.getLocation().getYaw());

				}
				if(iteration == 2){
					p.setGameMode(GameMode.SPECTATOR);
					p.setSpectatorTarget(tPlayer);	 
					cancel();
				}
				iteration++;
			}

		}.runTaskTimer(plugin, 1, 10);
		new BukkitRunnable() {

			public void run() {
				if(inBodySpecToTarget.containsKey(p.getName())){
					p.teleport(Bukkit.getPlayer(inBodySpecToTarget.get(p.getName())));
				}
				else{
					cancel();
				}
			}

		}.runTaskTimer(plugin, 1, 20);
	} catch (Exception e) {}
}
public void nextBodyspec(Player p, boolean returnToSpawn){			//progesses them to the next person they can bodyspec
	Boolean foundAnotherTeammate = false;
	Boolean passCurrent = false;
	for(Player iPlayer : Bukkit.getOnlinePlayers()){				//iterate through all players
		if(iPlayer.getName().equals(inBodySpecToTarget.get(p.getName()))){
																	//stop at current target
			passCurrent = true;
			continue;
		}
		if(passCurrent && iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) iPlayer).equals(iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) p))
		 && !deadList.contains(iPlayer.getName()) && !iPlayer.getName().equals(p.getName()) && !optOutList.contains(iPlayer.getName())){
			//search for another target
			putInBodySpec(p.getName(), iPlayer.getName());
			foundAnotherTeammate = true;
			p.sendMessage(ChatColor.GREEN + "You are now spectating " + iPlayer.getName() + ". In order to leave Body Spectating Do /bodyspecleave");
			break;
		}
	}
	if(!foundAnotherTeammate){ 		//if no target found start over

		for(Player iPlayer : Bukkit.getOnlinePlayers()){
			if(iPlayer.getName().equals(inBodySpecToTarget.get(p.getName()))){		//cancel when current target is found so that a full loop is made

				break;
			}
			if(iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) iPlayer).equals(iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) p))
			 && !deadList.contains(iPlayer.getName()) && !iPlayer.getName().equals(p.getName()) && !optOutList.contains(iPlayer.getName())){		//found next target

				putInBodySpec(p.getName(), iPlayer.getName());
				foundAnotherTeammate = true;
				p.sendMessage(ChatColor.GREEN + "You are now spectating " + iPlayer.getName() + ". In order to leave Body Spectating Do /bodyspecleave");

				break;
			}
		}	
	}
	if(!foundAnotherTeammate){
		if(!returnToSpawn)				//if no other team mate keep them specating the same person
			p.sendMessage(ChatColor.RED + "You have no other teammates in order to leave Body Spectating Do /bodyspecleave");
		else{
			removeFromBodyspec(p.getName());		//or if no other teammate remove them from bodyspec
		}
	}
}

public void removeFromDeathList(Player p){		//removes from death list 
	deadList.add(p.getName().toString());
}

public void handleBodyspec(Player p){		//is used to initalize bodyspectating
	if(!isBodyspecEnabled){					//if disbaled remove them
		if(p.getGameMode().equals(GameMode.SPECTATOR)){
			removeFromBodyspec(p.getName());
			p.sendMessage(ChatColor.RED + "Body Spectating has been disabled");
			return;
		}
		removeFromBodyspec(p.getName());
		return;
	}
	packetHandler();

	Boolean foundTeammate = false;
	for(Player iPlayer : Bukkit.getOnlinePlayers()){		//go through all players set them to the first legal player
		if(iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) iPlayer).equals(iPlayer.getScoreboard().getPlayerTeam((OfflinePlayer) p))
		 && !deadList.contains(iPlayer.getName()) && !iPlayer.getName().equals(p.getName())){
			if(!optOutList.contains(iPlayer.getName())){
				putInBodySpec(p.getName(), iPlayer.getName());
				foundTeammate = true;
				break;
			}
		}
	}
	if(!foundTeammate){
		if(p.getGameMode().equals(GameMode.SPECTATOR)) removeFromBodyspec(p.getName());
		else removeFromBodyspec(p.getName());;
	}
}
public void addToDeathList(Player p){
	if(targetToInBodySpec.containsKey(p.getName())){
		handleBodyspec(Bukkit.getPlayer(inBodySpecToTarget.get(p.getName())));
	}
	deadList.add(p.getName().toString());
}
public void setBodyspecEnabled(Boolean enabled){
	isBodyspecEnabled = enabled;
	if(!enabled){

		for (String key : inBodySpecToTarget.keySet()) {
				removeFromBodyspec(key);
		}
	}
}
public void optOutListAdd(Player p){
	if(!optOutList.contains(p.getName()))
		optOutList.add(p.getName());
	if(targetToInBodySpec.containsKey(p.getName())){
		for (String key : inBodySpecToTarget.keySet()) {
			nextBodyspec(Bukkit.getPlayer(key), true);
		}
		//handleBodyspec(Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName())));
	}
}

    private void blockPackets(Player player) {
		packetHandler();
		packetBlocker.add(player.getName());

		Launcher.getProtocolManager().addPacketListener(new PacketAdapter(
				Launcher.getMainInstance(),
				ListenerPriority.NORMAL,
				PacketType.Play.Server.PLAYER_INFO
		) {
			@Override
			public void onPacketSending(PacketEvent event) {
				Player p = event.getPlayer();
				if(packetBlocker.contains(p.getName())){
					packetBlocker.remove(p.getName());
					event.setCancelled(true);
					Launcher.protocolManager.removePacketListener(this);
				}
			}
		});
    }

	@EventHandler
	public void playerDeath(PlayerDeathEvent e) {	//fired when player dies and adds them to the death list if 
		Player p = e.getEntity().getPlayer();
		if(targetToInBodySpec.containsKey(p.getName().toString())){
			Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString())).sendMessage(ChatColor.RED + "The person you were body spectating died.");
			removeFromBodyspec(targetToInBodySpec.get(p.getName().toString()));

	}

}



	@EventHandler
	public void playerJoin(PlayerJoinEvent e) {	//fired on joining to handle a spectator that relogged
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
		OfflinePlayer p = e.getPlayer();

		if(targetToInBodySpec.containsKey(p.getName().toString())) {	//see if the player that logged out was being specced
			for(Player spectator : Bukkit.getOnlinePlayers()){				//iterate through all players


				if(inBodySpecToTarget.containsKey(spectator.getName())){
	    		spectator.sendMessage(ChatColor.RED + "The player you where spectating logged out");	//tell the player what happened
				nextBodyspec(spectator, true);
			}
		}
		}
		else if(inBodySpecToTarget.containsKey(p.getName().toString())){
			removeFromBodyspec(p.getName());

		}

	}


	@EventHandler
	public void teleportEvent(PlayerTeleportEvent e) {		//fixes bodyspec desync when target teleports
		Player p = e.getPlayer();


		//if(e.getCause().equals(TeleportCause.SPECTATE) || e.getCause().equals(TeleportCause.NETHER_PORTAL)){			//sketchy fix for nether woosh glitch
			if(targetToInBodySpec.containsKey(p.getName().toString())){
				Player spec = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString()));
				spec.setSpectatorTarget(null);

				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
						public void run() {
							spec.teleport(p.getLocation());
						}
					}, 40L);

					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

						@Override
							public void run() {
							spec.setSpectatorTarget(p);

							}
						}, 45L);
			}

		else if (targetToInBodySpec.containsKey(p.getName().toString()) && e.getCause().equals(TeleportCause.SPECTATE)) {		//this accounts for nether and other teleport delay glitch

			Player spec = Bukkit.getServer().getPlayer(targetToInBodySpec.get(p.getName().toString()));
			spec.setSpectatorTarget(null);		//set null to reset their spectator
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

				@Override
					public void run() {
						spec.teleport(p.getLocation());		//tp them to their target
					}
				}, 5L);		//wait 5 ticks to tp reset them as spectator

			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			    public void run() {
					spec.setSpectatorTarget(null);		//set null to reset their spectator
		    		spec.setSpectatorTarget(p);
				}
			}, 10L);		//wait 10 ticks to tp reset them as spectator
			
		}
		
	}

	@EventHandler
	public void sneakEvent(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();

		if((inBodySpecToTarget.containsKey(p.getName().toString()))){

			if(debounceList.contains(p.getName())){
				e.setCancelled(true);
				return;
			}
			else{
				e.setCancelled(true);
				debounceList.add(p.getName());
				new BukkitRunnable() {
					public void run() {
						debounceList.remove(p.getName());
						cancel();
					}
				}.runTaskTimer(plugin, 40, 5);
				nextBodyspec(p, false);
		}
	}
  
	  }
	  	public void interactSpectator(Player p) {
  
		  if(inBodySpecToTarget.containsKey(p.getName())){
  
			  //putInBodySpec(e.getPlayer().getName(), inBodySpecToTarget.get(e.getPlayer().getName().toString()));
			  Player target = Bukkit.getPlayer(inBodySpecToTarget.get(p.getName().toString()));
			  new BukkitRunnable() {
				  int iteration = 1;
				  public void run() {
					  if(iteration == 1 && p.getSpectatorTarget() != null){
						  p.setSpectatorTarget(null);
						  p.teleport(target.getLocation());
					  }
					  if(iteration == 2){
						  p.setSpectatorTarget(target);
						  //cancel();
					  }
					  iteration++;
				  }
			  }.runTaskTimer(plugin, 0, 1);
		  }
  
	  }

}
