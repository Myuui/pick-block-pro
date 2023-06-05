package io.github.sjouwer.pickblockpro.util;

import io.github.sjouwer.pickblockpro.PickBlockPro;
import io.github.sjouwer.pickblockpro.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Saddleable;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public class NbtUtil {
    private static final ModConfig config = PickBlockPro.getConfig();

    private static final String BLOCK_STATE_KEY = "BlockStateTag";
    private static final String BLOCK_ENTITY_KEY = "BlockEntityTag";
    private static final String ENTITY_KEY = "EntityTag";
    private static final String ID_KEY = "id";
    private static final String COUNT_KEY = "Count";
    private static final String LEVEL_KEY = "level";

    private NbtUtil() {
    }

    public static void addEntityNbt(ItemStack stack, Entity entity, boolean addLore) {
        NbtCompound entityCompound = getEntityNbt(entity);
        config.entityTagBlacklist().forEach(entityCompound::remove);
        if (entityCompound.isEmpty()) {
            return;
        }

        stack.setSubNbt(ENTITY_KEY, entityCompound);

        if (addLore) {
            addLore(stack, "\"(+Entity NBT)\"");
        }
    }

    public static NbtCompound getEntityNbt(Entity entity) {
        NbtCompound entityCompound = entity.writeNbt(new NbtCompound());

        if (entity instanceof HorseEntity horse && horse.hasArmorInSlot()) {
            NbtCompound armorCompound = new NbtCompound();
            armorCompound.putString(ID_KEY, horse.getArmorType().getItem().toString());
            armorCompound.putInt(COUNT_KEY, 1);
            entityCompound.put("ArmorItem", armorCompound);
        }

        if (entity instanceof Saddleable saddleable && saddleable.isSaddled()) {
            NbtCompound saddleCompound = new NbtCompound();
            saddleCompound.putString(ID_KEY, Items.SADDLE.toString());
            saddleCompound.putInt(COUNT_KEY, 1);
            entityCompound.put("SaddleItem", saddleCompound);
        }

        if (entity instanceof LlamaEntity llama && llama.getCarpetColor() != null) {
            NbtCompound decorCompound = new NbtCompound();
            decorCompound.putString(ID_KEY, llama.getCarpetColor() + "_carpet");
            decorCompound.putInt(COUNT_KEY, 1);
            entityCompound.put("DecorItem", decorCompound);
        }

        Identifier identifier = EntityType.getId(entity.getType());
        entityCompound.putString(ID_KEY, identifier.toString());

        return entityCompound;
    }

    public static void addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity, boolean addLore) {
        NbtCompound blockEntityCompound = blockEntity.createNbtWithIdentifyingData();
        config.blockEntityTagBlacklist().forEach(blockEntityCompound::remove);
        if (blockEntityCompound.isEmpty()) {
            return;
        }

        if (stack.getItem() instanceof SkullItem && blockEntityCompound.contains(SkullItem.SKULL_OWNER_KEY)) {
            NbtCompound skullCompound = blockEntityCompound.getCompound(SkullItem.SKULL_OWNER_KEY);
            stack.getOrCreateNbt().put(SkullItem.SKULL_OWNER_KEY, skullCompound);
            return;
        }

        stack.setSubNbt(BLOCK_ENTITY_KEY, blockEntityCompound);

        if (addLore) {
            addLore(stack, "\"(+BlockEntity NBT)\"");
        }
    }

    public static void addBlockStateNbt(ItemStack stack, BlockState state, boolean addLore) {
        NbtCompound blockStateCompound = getBlockStateNbt(state);
        config.blockStateTagBlacklist().forEach(blockStateCompound::remove);
        if (blockStateCompound.isEmpty()) {
            return;
        }

        stack.setSubNbt(BLOCK_STATE_KEY, blockStateCompound);

        if (addLore) {
            addLore(stack, "\"(+BlockState NBT)\"");
        }
    }

    public static NbtCompound getBlockStateNbt(BlockState state) {
        NbtCompound blockStateCompound = new NbtCompound();
        for (Property<?> property : state.getProperties()) {
            String key = property.getName();
            String value = state.get(property).toString().toLowerCase();
            blockStateCompound.putString(key, value);
        }

        return blockStateCompound;
    }

    public static void addLore(ItemStack stack, String tag) {
        NbtCompound loreCompound = stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY);
        NbtList loreList = loreCompound.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
        if (loreList == null) {
            loreList = new NbtList();
        }
        loreList.add(NbtString.of(tag));
        loreCompound.put(ItemStack.LORE_KEY, loreList);
        stack.setSubNbt(ItemStack.DISPLAY_KEY, loreCompound);
    }

    public static void setLightLevel(ItemStack light, int level) {
        NbtCompound blockStateTag = new NbtCompound();
        blockStateTag.putInt(LEVEL_KEY, level);
        light.setSubNbt(BLOCK_STATE_KEY, blockStateTag);
    }

    public static void cycleLightLevel(ItemStack light) {
        NbtCompound blockStateTag = light.getSubNbt(BLOCK_STATE_KEY);
        int newLightLvl;

        if (blockStateTag == null) {
            blockStateTag = new NbtCompound();
            newLightLvl = 0;
        }
        else {
            newLightLvl = blockStateTag.getInt(LEVEL_KEY) + 1;
        }
        if (newLightLvl == 16) {
            newLightLvl = 0;
        }

        blockStateTag.putInt(LEVEL_KEY, newLightLvl);
        light.setSubNbt(BLOCK_STATE_KEY, blockStateTag);
    }

    public static void setSkullOwner(ItemStack skull, PlayerEntity player) {
        NbtCompound skullOwner = new NbtCompound();
        NbtHelper.writeGameProfile(skullOwner, player.getGameProfile());
        skull.setSubNbt(SkullItem.SKULL_OWNER_KEY, skullOwner);
    }

    public static int getAmountStored(ItemStack storage, Item item) {
        int amount = 0;

        NbtCompound storageCompound = storage.getNbt();
        if (storageCompound == null || storageCompound.isEmpty()) {
            return amount;
        }

        //Shulkers use the BlockEntityTag while the bundle doesn't
        NbtCompound blockEntityCompound = storageCompound.getCompound(BLOCK_ENTITY_KEY);
        if (blockEntityCompound != null && !blockEntityCompound.isEmpty()) {
            storageCompound = blockEntityCompound;
        }

        NbtList itemList = storageCompound.getList("Items", NbtElement.COMPOUND_TYPE);
        if (itemList == null || itemList.isEmpty()) {
            return amount;
        }

        for (int i = 0; i < itemList.size(); i++) {
            NbtCompound itemCompound = itemList.getCompound(i);
            if (itemCompound.getString(ID_KEY).equals(Registries.ITEM.getId(item).toString())) {
                amount += itemCompound.getInt(COUNT_KEY);
            }
        }

        return amount;
    }
}
