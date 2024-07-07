package net.citizensnpcs.nms.v1_21_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_21_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_21_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_21_R1.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_21_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TippedArrowController extends MobEntityController {
    public TippedArrowController() {
        super(EntityTippedArrowNPC.class);
    }

    @Override
    public org.bukkit.entity.Arrow getBukkitEntity() {
        return (org.bukkit.entity.Arrow) super.getBukkitEntity();
    }

    public static class EntityTippedArrowNPC extends Arrow implements NPCHolder {
        private final CitizensNPC npc;

        public EntityTippedArrowNPC(EntityType<? extends Arrow> types, Level level) {
            this(types, level, null);
        }

        public EntityTippedArrowNPC(EntityType<? extends Arrow> types, Level level, NPC npc) {
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
                NMSImpl.setBukkitEntity(this, new TippedArrowNPC(this));
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

    public static class TippedArrowNPC extends CraftArrow implements ForwardingNPCHolder {
        public TippedArrowNPC(EntityTippedArrowNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
