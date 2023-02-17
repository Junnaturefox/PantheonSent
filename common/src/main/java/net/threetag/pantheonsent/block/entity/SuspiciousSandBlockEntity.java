package net.threetag.pantheonsent.block.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.threetag.pantheonsent.block.PSBlocks;
import net.threetag.pantheonsent.block.SuspiciousSandBlock;
import net.threetag.pantheonsent.item.PSLootTables;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SuspiciousSandBlockEntity extends BlockEntity {

    private static final String LOOT_TABLE_TAG = "loot_table";
    private static final String LOOT_TABLE_SEED_TAG = "loot_table_seed";
    private static final String HIT_DIRECTION_TAG = "hit_direction";
    private static final String ITEM_TAG = "item";
    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int REQUIRED_BRUSHES_TO_BREAK = 10;
    private int brushCount;
    private long brushCountResetsAtTick;
    private long coolDownEndsAtTick;
    private ItemStack item;
    @Nullable
    private Direction hitDirection;
    @Nullable
    private ResourceLocation lootTable;
    private long lootTableSeed;

    public SuspiciousSandBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(PSBlockEntityTypes.SUSPICIOUS_SAND.get(), blockPos, blockState);
        this.item = ItemStack.EMPTY;
    }

    public boolean brush(long l, Player player, Direction direction) {
        if (this.hitDirection == null) {
            this.hitDirection = direction;
        }

        this.brushCountResetsAtTick = l + BRUSH_RESET_TICKS;
        if (l >= this.coolDownEndsAtTick && this.level instanceof ServerLevel) {
            this.coolDownEndsAtTick = l + BRUSH_COOLDOWN_TICKS;
            this.unpackLootTable(player);
            int i = this.getCompletionState();
            if (++this.brushCount >= REQUIRED_BRUSHES_TO_BREAK) {
                this.brushingCompleted(player);
                return true;
            } else {
                this.level.scheduleTick(this.getBlockPos(), PSBlocks.SUSPICIOUS_SAND.get(), BRUSH_RESET_TICKS);
                int j = this.getCompletionState();
                if (i != j) {
                    BlockState blockState = this.getBlockState();
                    BlockState blockState2 = blockState.setValue(SuspiciousSandBlock.DUSTED, j);
                    this.level.setBlock(this.getBlockPos(), blockState2, 3);
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public void unpackLootTable(Player player) {
        if(this.lootTable == null && this.item.isEmpty()) {
            this.lootTable = PSLootTables.SUSPICIOUS_SAND;
        }

        if (this.lootTable != null && this.level != null && !this.level.isClientSide() && this.level.getServer() != null) {
            LootTable lootTable = this.level.getServer().getLootTables().get(this.lootTable);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, this.lootTable);
            }

            LootContext.Builder builder = (new LootContext.Builder((ServerLevel) this.level)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition)).withOptionalRandomSeed(this.lootTableSeed).withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            ObjectArrayList<ItemStack> items = lootTable.getRandomItems(builder.create(LootContextParamSets.CHEST));
            ItemStack resultItem;
            if (items.size() == 0) {
                resultItem = ItemStack.EMPTY;
            } else {
                resultItem = items.get(0);
            }

            this.item = resultItem;
            this.lootTable = null;
            this.setChanged();
        }
    }

    private void brushingCompleted(Player player) {
        if (this.level != null && this.level.getServer() != null) {
            this.dropContent(player);
            this.level.levelEvent(3008, this.getBlockPos(), Block.getId(this.getBlockState()));
            this.level.setBlock(this.worldPosition, Blocks.SAND.defaultBlockState(), 3);
        }
    }

    private void dropContent(Player player) {
        if (this.level != null && this.level.getServer() != null) {
            this.unpackLootTable(player);
            if (!this.item.isEmpty()) {
                double d = EntityType.ITEM.getWidth();
                double e = 1.0 - d;
                double f = d / 2.0;
                Direction direction = Objects.requireNonNullElse(this.hitDirection, Direction.UP);
                BlockPos blockPos = this.worldPosition.relative(direction, 1);
                double g = Math.floor((double) blockPos.getX()) + 0.5 * e + f;
                double h = Math.floor((double) blockPos.getY() + 0.5) + (double) (EntityType.ITEM.getHeight() / 2.0F);
                double i = Math.floor((double) blockPos.getZ()) + 0.5 * e + f;
                ItemEntity itemEntity = new ItemEntity(this.level, g, h, i, this.item.split(this.level.random.nextInt(21) + 10));
                itemEntity.setDeltaMovement(Vec3.ZERO);
                this.level.addFreshEntity(itemEntity);
                this.item = ItemStack.EMPTY;
            }

        }
    }

    public void checkReset() {
        if (this.level != null) {
            if (this.brushCount != 0 && this.level.getGameTime() >= this.brushCountResetsAtTick) {
                int i = this.getCompletionState();
                this.brushCount = Math.max(0, this.brushCount - 2);
                int j = this.getCompletionState();
                if (i != j) {
                    this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(SuspiciousSandBlock.DUSTED, j), 3);
                }

                this.brushCountResetsAtTick = this.level.getGameTime() + 4L;
            }

            if (this.brushCount == 0) {
                this.hitDirection = null;
                this.brushCountResetsAtTick = 0L;
                this.coolDownEndsAtTick = 0L;
            } else {
                this.level.scheduleTick(this.getBlockPos(), PSBlocks.SUSPICIOUS_SAND.get(), (int) (this.brushCountResetsAtTick - this.level.getGameTime()));
            }

        }
    }

    private boolean tryLoadLootTable(CompoundTag compoundTag) {
        if (compoundTag.contains(LOOT_TABLE_TAG, 8)) {
            this.lootTable = new ResourceLocation(compoundTag.getString(LOOT_TABLE_TAG));
            this.lootTableSeed = compoundTag.getLong(LOOT_TABLE_SEED_TAG);
            return true;
        } else {
            return false;
        }
    }

    private boolean trySaveLootTable(CompoundTag compoundTag) {
        if (this.lootTable == null) {
            return false;
        } else {
            compoundTag.putString(LOOT_TABLE_TAG, this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                compoundTag.putLong(LOOT_TABLE_SEED_TAG, this.lootTableSeed);
            }

            return true;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag compoundTag = super.getUpdateTag();
        if (this.hitDirection != null) {
            compoundTag.putInt(HIT_DIRECTION_TAG, this.hitDirection.ordinal());
        }

        compoundTag.put(ITEM_TAG, this.item.save(new CompoundTag()));
        return compoundTag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (!this.tryLoadLootTable(compoundTag) && compoundTag.contains(ITEM_TAG)) {
            this.item = ItemStack.of(compoundTag.getCompound(ITEM_TAG));
        }

        if (compoundTag.contains(HIT_DIRECTION_TAG)) {
            this.hitDirection = Direction.values()[compoundTag.getInt(HIT_DIRECTION_TAG)];
        }

    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        if (!this.trySaveLootTable(compoundTag)) {
            compoundTag.put(ITEM_TAG, this.item.save(new CompoundTag()));
        }

    }

    public void setLootTable(ResourceLocation resourceLocation, long l) {
        this.lootTable = resourceLocation;
        this.lootTableSeed = l;
    }

    private int getCompletionState() {
        if (this.brushCount == 0) {
            return 0;
        } else if (this.brushCount < 3) {
            return 1;
        } else {
            return this.brushCount < 6 ? 2 : 3;
        }
    }

    @Nullable
    public Direction getHitDirection() {
        return this.hitDirection;
    }

    public ItemStack getItem() {
        return this.item;
    }
}
