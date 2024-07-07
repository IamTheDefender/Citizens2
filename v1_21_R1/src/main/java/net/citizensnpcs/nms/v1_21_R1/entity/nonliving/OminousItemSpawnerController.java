package net.citizensnpcs.nms.v1_21_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftOminousItemSpawner;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_21_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_21_R1.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_21_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OminousItemSpawnerController extends MobEntityController {
    public OminousItemSpawnerController() {
        super(EntityOminousItemSpawnerNPC.class);
    }

    @Override
    protected org.bukkit.entity.Entity createEntity(Location at, NPC npc) {
        final EntityOminousItemSpawnerNPC handle = new EntityOminousItemSpawnerNPC(EntityType.OMINOUS_ITEM_SPAWNER,
                ((CraftWorld) at.getWorld()).getHandle(), npc);
        if (npc != null) {
            handle.setItem(CraftItemStack.asNMSCopy(npc.getItemProvider().get()));
        }
        return handle.getBukkitEntity();
    }

    @Override
    public org.bukkit.entity.OminousItemSpawner getBukkitEntity() {
        return (org.bukkit.entity.OminousItemSpawner) super.getBukkitEntity();
    }

    public static class EntityOminousItemSpawnerNPC extends OminousItemSpawner implements NPCHolder {
        private final CitizensNPC npc;

        public EntityOminousItemSpawnerNPC(EntityType<? extends OminousItemSpawner> types, Level level) {
            this(types, level, null);
        }

        public EntityOminousItemSpawnerNPC(EntityType<? extends OminousItemSpawner> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public Entity changeDimension(DimensionTransition transition) {
            if (npc == null)
                return super.changeDimension(transition);
            return NMSImpl.teleportAcrossWorld(this, transition);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new OminousItemSpawnerNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public PushReaction getPistonPushReaction() {
            return Util.callPistonPushEvent(npc) ? PushReaction.IGNORE : super.getPistonPushReaction();
        }

        @Override
        public boolean isPushable() {
            return npc == null ? super.isPushable()
                    : npc.data().<Boolean> get(NPC.Metadata.COLLIDABLE, !npc.isProtected());
        }

        @Override
        protected AABB makeBoundingBox() {
            return NMSBoundingBox.makeBB(npc, super.makeBoundingBox());
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean save(CompoundTag save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public void tick() {
            if (npc != null) {
                npc.update();
            } else {
                super.tick();
            }
        }

        @Override
        public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tagkey, double d0) {
            if (npc == null)
                return super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            Vec3 old = getDeltaMovement().add(0, 0, 0);
            boolean res = super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            if (!npc.isPushableByFluids()) {
                setDeltaMovement(old);
            }
            return res;
        }
    }

    public static class OminousItemSpawnerNPC extends CraftOminousItemSpawner implements NPCHolder {
        private final CitizensNPC npc;

        public OminousItemSpawnerNPC(EntityOminousItemSpawnerNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
            setItem(npc.getItemProvider().get());
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}
