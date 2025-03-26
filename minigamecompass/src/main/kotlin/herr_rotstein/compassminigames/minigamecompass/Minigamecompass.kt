package herr_rotstein.minigamecompass

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.block.Action

class CompassMinigamesMain : JavaPlugin(), Listener {

    private lateinit var configFile: FileConfiguration

    override fun onEnable() {
        saveDefaultConfig()
        configFile = config
        Bukkit.getPluginManager().registerEvents(this, this)

        if (configFile.getBoolean("compass-give-on-join")) {
            Bukkit.getOnlinePlayers().forEach { giveCompass(it) }
        }
    }

    override fun onDisable() {}

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (configFile.getBoolean("compass-give-on-join")) {
            giveCompass(event.player)
        }
    }

    @EventHandler
    fun onPlayerUseCompass(event: PlayerInteractEvent) {
        val item = event.item ?: return

        // Check if the item is our special Minigame Navigator
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (isMinigameCompass(item)) {
                openGameModeUI(event.player)
                event.isCancelled = true
            }
        }
    }

    private fun openGameModeUI(player: Player) {
        val inventory: Inventory = Bukkit.createInventory(null, 9, "Select a Gamemode")
        val minigames = configFile.getConfigurationSection("minigames")?.getKeys(false) ?: return

        var slot = 3
        for (minigame in minigames) {
            val material = Material.matchMaterial(configFile.getString("minigames.$minigame.material") ?: "BARRIER")
            if (material != null) {
                inventory.setItem(slot, createMenuItem(material, minigame))
                slot++
            }
        }
        player.openInventory(inventory)
    }

    private fun createMenuItem(material: Material, name: String): ItemStack {
        return ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(name)
                isUnbreakable = true
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title != "Select a Gamemode") return

        event.isCancelled = true
        val clickedItem = event.currentItem ?: return
        val itemName = clickedItem.itemMeta?.displayName ?: return

        val command = configFile.getString("minigames.$itemName.command") ?: return
        player.performCommand(command)
    }

    private fun isMinigameCompass(item: ItemStack): Boolean {
        if (item.type != Material.COMPASS) return false
        val meta = item.itemMeta ?: return false
        return meta.displayName == configFile.getString("compass-name")
    }

    private fun hasMinigameCompass(player: Player): Boolean {
        return player.inventory.contents.any { it?.let { isMinigameCompass(it) } == true }
    }

    private fun createCompass(): ItemStack {
        return ItemStack(Material.COMPASS).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(configFile.getString("compass-name") ?: "§6Minigame Navigator")
                isUnbreakable = true
            }
        }
    }

    private fun giveCompass(player: Player) {
        if (!hasMinigameCompass(player)) {
            player.inventory.addItem(createCompass())
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("reloadcompass", ignoreCase = true)) {
            if (sender.hasPermission("minigamecompass.reload")) {
                reloadConfig()
                configFile = config
                sender.sendMessage("§aMinigameCompass Config reloaded!")
            } else {
                sender.sendMessage("§cYou do not have permission for that!")
            }
            return true
        }
        return false
    }
}
