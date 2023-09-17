package me.theencomputers.bodyspec;


import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Launcher extends JavaPlugin{
    static Launcher plugin;
    static ProtocolManager protocolManager;
	public void onEnable(){
		plugin = this;
        Bukkit.getServer().getPluginManager().registerEvents((Listener) new bodyspec(this), this);
            getCommand("bodyspec").setExecutor(new bodyspeCMD());
            getCommand("bodyspecleave").setExecutor(new bodyspeCMD());
            getCommand("bodyspecadmin").setExecutor(new bodyspeCMD());
            getCommand("bodyspecoptout").setExecutor(new bodyspeCMD());
            getCommand("bodyspecoptin").setExecutor(new bodyspeCMD());
         protocolManager = ProtocolLibrary.getProtocolManager();

	}
    public static Launcher getMainInstance(){
        return plugin;
    }
    public static ProtocolManager getProtocolManager(){
        return protocolManager;
    }
}
