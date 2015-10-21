package nl.dykam.dev.gradualdespawn;

import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import sun.misc.GC;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

class GradualDespawnTick implements Runnable {
    GradualDespawnPlugin plugin;
    World world;
    int maxAge;
    boolean immitateNaturalDespawn;
    Queue<WeakReference<LivingEntity>> spawnerMobs = new LinkedList<>(); // spawn tick -> mob uuid

    public GradualDespawnTick(GradualDespawnPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    @Override
    public void run() {
        int removed = 0;
        int alive = 0;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
            if (entity instanceof Player)
                continue;
            if (entity.getTicksLived() <= maxAge || immitateNaturalDespawn && !entity.getRemoveWhenFarAway()) {
                alive++;
                continue;
            }
            removed++;
            entity.remove();
        }

//        int isNull = 0, isDead = 0, ticksLived100 = 0;
//        System.gc();System.runFinalization(); // Just for testing, never production
        for (Iterator<WeakReference<LivingEntity>> iterator = spawnerMobs.iterator(); iterator.hasNext(); ) {
            WeakReference<LivingEntity> reference = iterator.next();
            LivingEntity entity = reference.get();
//            if(entity == null) {
//                isNull++;
//            } else {
//                if (entity.isDead())
//                    isDead++;
//                if (entity.getTicksLived() == 100)
//                    ticksLived100++;
//            }
            if (entity != null && entity.getTicksLived() <= maxAge && !entity.isDead()) {
                continue;
            }
            alive--;
            removed++;
            if (entity != null) {
                entity.remove();
            }
            iterator.remove();
        }

        if(plugin.showMessages) {
            plugin.getLogger().info("Removed " + Integer.toString(removed) + " and kept " + Integer.toString(alive) + " mobs alive in world " + world.getName() + ".");
//            plugin.getLogger().info(isNull + ", " + isDead + ", " + ticksLived100);
        }
    }

    public int getMaxAge() {
        return maxAge;
    }

    public GradualDespawnTick setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public boolean isImmitateNaturalDespawn() {
        return immitateNaturalDespawn;
    }

    public GradualDespawnTick setImmitateNaturalDespawn(boolean immitateNaturalDespawn) {
        this.immitateNaturalDespawn = immitateNaturalDespawn;
        return this;
    }

    public void creatureSpawnerSpawned(LivingEntity entity) {
        if(entity instanceof Player)
            return;
        spawnerMobs.add(new WeakReference<>(entity));
    }
}
