package dev.aurelium.auraskills.bukkit.skills.smithing;

import dev.aurelium.auraskills.api.mana.ManaAbilities;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.mana.ReadiedManaAbility;
import dev.aurelium.auraskills.common.message.type.ManaAbilityMessage;
import dev.aurelium.auraskills.common.scheduler.Task;
import dev.aurelium.auraskills.common.scheduler.TaskRunnable;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ForgeOverdrive extends ReadiedManaAbility {

    private final Map<UUID, Task> tasks = new ConcurrentHashMap<>();

    public ForgeOverdrive(AuraSkills plugin) {
        super(plugin, ManaAbilities.FORGE_OVERDRIVE, ManaAbilityMessage.FORGE_OVERDRIVE_START, ManaAbilityMessage.FORGE_OVERDRIVE_END,
                new String[]{"COAL", "CHARCOAL"}, new Action[]{Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR});
    }

    @Override
    public void onActivate(Player player, User user) {
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
        int radius = manaAbility.optionInt("radius", 10);
        Task task = plugin.getScheduler().timerSync(new TaskRunnable() {
            @Override
            public void run() {
                accelerateFurnaces(player, radius);
            }
        }, 100, 100, TimeUnit.MILLISECONDS); // Every 2 ticks
        tasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void activationListener(PlayerInteractEvent event) {
        if (isDisabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) return;

        Player player = event.getPlayer();
        if (failsChecks(player)) return;
        checkActivation(player);
    }

    @Override
    public void onStop(Player player, User user) {
        Task task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void accelerateFurnaces(Player player, int radius) {
        if (!isActivated(plugin.getUser(player))) {
            onStop(player, plugin.getUser(player));
            return;
        }
        int px = player.getLocation().getBlockX();
        int py = player.getLocation().getBlockY();
        int pz = player.getLocation().getBlockZ();
        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = Math.max(player.getWorld().getMinHeight(), py - radius); y <= Math.min(player.getWorld().getMaxHeight() - 1, py + radius); y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    if (!(block.getState() instanceof Furnace furnace)) continue;
                    if (furnace.getBurnTime() <= 0) continue;
                    if (furnace.getCookTimeTotal() <= 0) continue;
                    // +1 cook progress per 2 ticks on top of vanilla = ~1.5-2x effective speed
                    furnace.setCookTime((short) Math.min(furnace.getCookTimeTotal() - 1, furnace.getCookTime() + 1));
                    furnace.update();
                }
            }
        }
    }
}
