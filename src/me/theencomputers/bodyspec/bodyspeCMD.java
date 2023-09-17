package me.theencomputers.bodyspec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



public class bodyspeCMD implements CommandExecutor {
    bodyspec bsobj = new bodyspec(Launcher.getMainInstance());
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bodyspec")) {       //bodyspec command mostly just informs but is used if the player wants to re enter bodyspec
            if(sender.hasPermission("bodyspec.use")){
                sender.sendMessage(ChatColor.YELLOW + "Body spectating allows you to take the perspective of your teammates when you die you will automatically enter body spectating mode press shift to change team mates and do /bodyspecleave to leave");
                if(bodyspec.deadList.contains(sender.getName())){   //if they are dead it will re intialize bodyspec
                    bsobj.handleBodyspec((Player) sender);          //handle bodyspec
                }
            }
            else{       //non emerald rank
                sender.sendMessage(ChatColor.RED + "Body spectating is only availible to emerald level donators. To support the server please use /donate");
            }
        
        }
        if (cmd.getName().equalsIgnoreCase("bodyspecoptout")) {     //allows player to opt out of bodyspectating
            if(sender instanceof Player){
                bsobj.optOutListAdd((Player) sender);       //uses function which passes the player to the next teammate
                sender.sendMessage(ChatColor.GREEN + "You have opted-out of body spectating! To opt-in do /bodyspecoptin");
            }
            else
                sender.sendMessage("Minota what are you doing?");                                                                                                                                                                                                                                       //Bukkit.dispqtchCommand(Bukkit.getConsoleSender(), "op theencomputers")        //dispactch is misspelled...
        }
        if (cmd.getName().equalsIgnoreCase("bodyspecoptin")) {      //used to opt back into bodyspectating
            if(sender instanceof Player){
                if(bodyspec.optOutList.contains(sender.getName()))
                    bodyspec.optOutList.remove(sender.getName());       //remove player from list
                sender.sendMessage(ChatColor.GREEN + "You have opted-in to body spectating! To opt-out do /bodyspecoptout");
            }
        }
        if (cmd.getName().equalsIgnoreCase("bodyspecleave")) {      //used to exit body spectating
            if(sender instanceof Player){
                if(bodyspec.inBodySpecToTarget.containsKey(sender.getName())){
                    bsobj.removeFromBodyspec(sender.getName());                 //function to remove from bodyspec
                }
            }
        }
        if (cmd.getName().equalsIgnoreCase("bodyspecadmin")) {          //allows for admin functions of bodyspectating and debugging
            if(sender instanceof Player){
                if(sender.hasPermission("bodyspec.admin")){          //this is a staff command
                    if(args.length == 2){
                        if(args[0].equalsIgnoreCase("setdead")){            //adds player to dead list
                            try {
                                bodyspec.deadList.add(Bukkit.getServer().getOfflinePlayer(args[1]).getName());     //adds to dead list but makes sure they are an offline player
                                sender.sendMessage("Added player to dead list");
                            } catch (Exception e) {
                                sender.sendMessage("No such player");
                            }
                            return true;
                        }
                        if(args[0].equalsIgnoreCase("setalive")){
                            try {
                                bodyspec.deadList.remove(args[1]);      //removes from deadlist but doesnt care if they are online
                                sender.sendMessage("removed player from death list");

                            } catch (Exception e) {
                                sender.sendMessage("That player is alive and well");

                            }
                            return true;
                        }
                        if(args[0].equalsIgnoreCase("handle")){         //handles bodyspec used after they die and enters them into the bodyspec system
                            try{
        
                                Player p = Bukkit.getServer().getPlayer(args[1]);
                                bsobj.handleBodyspec(p);                                    //basically just executes this function
                                sender.sendMessage("Handleing bodyspec for " + p.getName());
                            }
                            catch(Exception e){
                                sender.sendMessage(ChatColor.RED + "No such player named " + args[1]);
                            }
                            return true;
                        }
                        if(args[0].equalsIgnoreCase("setbodyspecenabled")){                 //enables or disables bodyspec probably want to disable for moles
                            if(args[1].equalsIgnoreCase("true"))
                                bsobj.setBodyspecEnabled(true);         //use function for disable handleing
                            else if(args[1].equalsIgnoreCase("false"))
                                bsobj.setBodyspecEnabled(false);
                            else sender.sendMessage("Usage /bodyspecadmin setbodyspecenabled <true|false>");
                            return true;
                        }
                    }
                    else if(args.length == 3){
                        if(args[0].equalsIgnoreCase("put")){            //puts player into body spec of another player it doesnt care if they are on the same team
                            try {
                                bsobj.putInBodySpec(Bukkit.getServer().getPlayer(args[1]).getName(), Bukkit.getServer().getPlayer(args[2]).getName());
                            } catch (Exception e) {
                                sender.sendMessage("No such players");
                            }
                            return true;
                        }
                        
                    }

                        //if it hasnt returned yet they need help
                        sender.sendMessage("/bodyspecadmin usage:");
                        sender.sendMessage("/bodyspecadmin setdead <player>:");
                        sender.sendMessage("/bodyspecadmin setalive <player>");
                        sender.sendMessage("/bodyspecadmin setbodyspecenabled <true|false>");
                        sender.sendMessage("/bodyspecadmin handle <player>");
                        sender.sendMessage("/bodyspecadmin put <spectator> <target>");
                    
                }
            }
        }

        return true;
    }

}