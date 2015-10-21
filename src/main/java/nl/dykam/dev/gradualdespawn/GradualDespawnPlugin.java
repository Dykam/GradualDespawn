package nl.dykam.dev.gradualdespawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class GradualDespawnPlugin extends JavaPlugin implements Listener {
    Map<World, BukkitTask> despawnTasks;
    Map<World, GradualDespawnTick> despawnTicks;
    boolean showMessages;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        despawnTasks = new HashMap<>();
        despawnTicks = new HashMap<>();
        loadConfig(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("gradualdespawn"))
            return false;

        if (args.length != 1)
            return false;

        if (args[0].equals("reload")) {
            loadConfig(true);
            sender.sendMessage(ChatColor.DARK_PURPLE + "[GD]" + ChatColor.DARK_GREEN + " Reloaded configuration for GradualDespawnPlugin.");
            return true;
        }
        return false;
    }

    void loadConfig() {
        loadConfig(false);
    }

    void loadConfig(boolean reload) {
        saveDefaultConfig();
        if (reload)
            reloadConfig();

        for (Map.Entry<World, BukkitTask> task : despawnTasks.entrySet()) {
            getLogger().info("Deactivated for world " + task.getKey().getName());
            task.getValue().cancel();
        }
        despawnTasks.clear();

       showMessages = getConfig().getBoolean("show-messages", false);

        ConfigurationSection global = getConfig().getConfigurationSection("global");
        int interval = global.getInt("interval", 6000);


        for (World world : Bukkit.getWorlds()) {
            String prefix = "world." + world.getName();
            // Activate gd for this world if either
            // - interval is set and worlds.worldname is not false
            // - world.worldname exists and worlds.worldname is not false
            if (!getConfig().getBoolean(prefix, true) || (!global.isSet("interval") && !getConfig().isSet(prefix)))
                continue;

            if (!despawnTicks.containsKey(world))
                despawnTicks.put(world, new GradualDespawnTick(this, world));
            GradualDespawnTick tick = despawnTicks.get(world);

            ConfigurationSection section = getConfig().isSet(prefix) ? getConfig().getConfigurationSection(prefix) : global;
            tick.setMaxAge(section.getInt("max-age", global.getInt("max-age", 24000)));
            tick.setImmitateNaturalDespawn(section.getBoolean("immitate-natural-despawn", global.getBoolean("immitate-natural-despawn", false)));
            int worldInterval = section.getInt("interval", interval);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, tick, worldInterval, worldInterval);
            despawnTasks.put(world, task);
            getLogger().info("Activated for world " + world.getName());
        }
    }

    // Utilities to easily set metadata on objects
    void setData(Metadatable metadatable, String key, Object data) {
        metadatable.setMetadata(key, new FixedMetadataValue(this, data));
    }

    MetadataValue getData(Metadatable metadatable, String key) {
        for (MetadataValue value : metadatable.getMetadata(key)) {
            if (value.getOwningPlugin().equals(this))
                return value;
        }
        return null;
    }

    @EventHandler
    private void mobSpawnEvent(CreatureSpawnEvent event) {
        if(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER)
            return;
        GradualDespawnTick despawnTick = despawnTicks.get(event.getLocation().getWorld());
        despawnTick.creatureSpawnerSpawned(event.getEntity());
    }
}

