package yyz.chl.phantomcontrol.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager {
    
    public static final int GUI_SIZE = 27;
    public static final int SLOT_ENABLE = 11;
    public static final int SLOT_STATUS = 13;
    public static final int SLOT_INFO = 4;
    public static final int SLOT_DISABLE = 15;
    
    private static final int[] BORDER_TOP = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int[] BORDER_BOTTOM = {18, 19, 20, 21, 22, 23, 24, 25, 26};
    private static final int BORDER_LEFT = 9;
    private static final int BORDER_RIGHT = 17;
    
    private final PhantomManager phantomManager;
    private final ConfigManager configManager;
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    
    private Material borderMaterial;
    private Material statusEnabledMaterial;
    private Material statusDisabledMaterial;
    private Material enableButtonMaterial;
    private Material disableButtonMaterial;
    private Material infoButtonMaterial;
    
    private ItemStack borderItemStack;

    public GUIManager(PhantomManager phantomManager, ConfigManager configManager) {
        this.phantomManager = phantomManager;
        this.configManager = configManager;
        refreshGUIConfig();
    }

    public void refreshGUIConfig() {
        this.borderMaterial = parseMaterial(
            configManager.getString("settings.gui.border-material", "GRAY_STAINED_GLASS_PANE"),
            Material.GRAY_STAINED_GLASS_PANE);
        this.statusEnabledMaterial = parseMaterial(
            configManager.getString("settings.gui.status-enabled-material", "GREEN_WOOL"),
            Material.GREEN_WOOL);
        this.statusDisabledMaterial = parseMaterial(
            configManager.getString("settings.gui.status-disabled-material", "RED_WOOL"),
            Material.RED_WOOL);
        this.enableButtonMaterial = parseMaterial(
            configManager.getString("settings.gui.enable-button-material", "LIME_DYE"),
            Material.LIME_DYE);
        this.disableButtonMaterial = parseMaterial(
            configManager.getString("settings.gui.disable-button-material", "RED_DYE"),
            Material.RED_DYE);
        this.infoButtonMaterial = parseMaterial(
            configManager.getString("settings.gui.info-button-material", "BOOK"),
            Material.BOOK);
        this.borderItemStack = createItem(borderMaterial, " ");
    }
    
    private Material parseMaterial(String materialName, Material defaultMaterial) {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return defaultMaterial;
        }
    }
    
    public void openPhantomControlGUI(Player player) {
        String guiTitle = configManager.getMessage(player, "gui.title", "幻翼控制");
        Inventory inventory = Bukkit.createInventory(new GUIHolder(), GUI_SIZE, guiTitle);
        
        fillBorder(inventory);
        setupControlButtons(inventory, player);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), inventory);
    }
    
    public boolean isPhantomControlInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof GUIHolder;
    }
    
    public void updateGUI(Player player) {
        Inventory inventory = openInventories.get(player.getUniqueId());
        if (inventory != null) {
            setupControlButtons(inventory, player);
        }
    }
    
    public void handleInventoryClose(Player player) {
        openInventories.remove(player.getUniqueId());
    }
    
    private void fillBorder(Inventory inventory) {
        for (int slot : BORDER_TOP) {
            inventory.setItem(slot, borderItemStack);
        }
        for (int slot : BORDER_BOTTOM) {
            inventory.setItem(slot, borderItemStack);
        }

        inventory.setItem(BORDER_LEFT, borderItemStack);
        inventory.setItem(BORDER_RIGHT, borderItemStack);
    }
    
    private void setupControlButtons(Inventory inventory, Player player) {
        boolean isEnabled = phantomManager.hasPhantomsEnabled(player);
        
        ItemStack statusItem;
        if (isEnabled) {
            statusItem = createItem(
                statusEnabledMaterial, 
                configManager.getMessage(player, "gui.status-enabled", "&a当前状态: 已启用"),
                configManager.getMessage(player, "gui.status-enabled-lore1", "&7幻翼会正常生成"),
                configManager.getMessage(player, "gui.status-enabled-lore2", "&7点击禁用幻翼生成")
            );
        } else {
            statusItem = createItem(
                statusDisabledMaterial, 
                configManager.getMessage(player, "gui.status-disabled", "&c当前状态: 已禁用"),
                configManager.getMessage(player, "gui.status-disabled-lore1", "&7幻翼不会生成"),
                configManager.getMessage(player, "gui.status-disabled-lore2", "&7点击启用幻翼生成")
            );
        }
        inventory.setItem(SLOT_STATUS, statusItem);
        
        ItemStack enableItem = createItem(
            enableButtonMaterial, 
            configManager.getMessage(player, "gui.enable-button", "&a启用幻翼"),
            configManager.getMessage(player, "gui.enable-button-lore", "&7允许幻翼生成")
        );
        inventory.setItem(SLOT_ENABLE, enableItem);
        
        ItemStack disableItem = createItem(
            disableButtonMaterial, 
            configManager.getMessage(player, "gui.disable-button", "&c禁用幻翼"),
            configManager.getMessage(player, "gui.disable-button-lore", "&7阻止幻翼生成")
        );
        inventory.setItem(SLOT_DISABLE, disableItem);
        
        ItemStack infoItem = createItem(
            infoButtonMaterial, 
            configManager.getMessage(player, "gui.info-button", "&e关于幻翼"),
            configManager.getMessage(player, "gui.info-button-lore1", "&7幻翼会在玩家长时间"),
            configManager.getMessage(player, "gui.info-button-lore2", "&7未睡觉时生成"),
            configManager.getMessage(player, "gui.info-button-lore3", "&7使用此界面可以控制"),
            configManager.getMessage(player, "gui.info-button-lore4", "&7是否允许幻翼生成")
        );
        inventory.setItem(SLOT_INFO, infoItem);
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            meta.setLore(java.util.Arrays.asList(lore));
        }
        
        item.setItemMeta(meta);
        
        return item;
    }
    
    public static class GUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
