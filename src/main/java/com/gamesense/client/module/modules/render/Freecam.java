package com.gamesense.client.module.modules.render;

import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.PlayerMoveEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.DoubleSetting;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent;

@Module.Declaration(name = "Freecam", category = Category.Render)
public class Freecam extends Module {

    BooleanSetting cancelPackets = registerBoolean("Cancel Packets", true);
    DoubleSetting speed = registerDouble("Speed", 10, 0, 20);

    private double posX, posY, posZ;
    private float pitch, yaw;

    private EntityOtherPlayerMP clonedPlayer;

    private boolean isRidingEntity;
    private Entity ridingEntity;

    public void onEnable() {
        if (mc.player != null) {
            isRidingEntity = mc.player.getRidingEntity() != null;

            if (mc.player.getRidingEntity() == null) {
                posX = mc.player.posX;
                posY = mc.player.posY;
                posZ = mc.player.posZ;
            } else {
                ridingEntity = mc.player.getRidingEntity();
                mc.player.dismountRidingEntity();
            }

            pitch = mc.player.rotationPitch;
            yaw = mc.player.rotationYaw;

            clonedPlayer = new EntityOtherPlayerMP(mc.world, mc.getSession().getProfile());
            clonedPlayer.copyLocationAndAnglesFrom(mc.player);
            clonedPlayer.rotationYawHead = mc.player.rotationYawHead;
            mc.world.addEntityToWorld(-100, clonedPlayer);
            mc.player.capabilities.isFlying = true;
            mc.player.capabilities.setFlySpeed((float) (speed.getValue() / 100f));
            mc.player.noClip = true;
        }
    }

    public void onDisable() {
        EntityPlayer localPlayer = mc.player;
        if (localPlayer != null) {
            mc.player.setPositionAndRotation(posX, posY, posZ, yaw, pitch);
            mc.world.removeEntityFromWorld(-100);
            clonedPlayer = null;
            posX = posY = posZ = 0.D;
            pitch = yaw = 0.f;
            mc.player.capabilities.isFlying = false;
            mc.player.capabilities.setFlySpeed(0.05f);
            mc.player.noClip = false;
            mc.player.motionX = mc.player.motionY = mc.player.motionZ = 0.f;

            if (isRidingEntity) {
                mc.player.startRiding(ridingEntity, true);
            }
        }
    }

    public void onUpdate() {
        mc.player.capabilities.isFlying = true;
        mc.player.capabilities.setFlySpeed((float) (speed.getValue() / 100f));
        mc.player.noClip = true;
        mc.player.onGround = false;
        mc.player.fallDistance = 0;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PlayerMoveEvent> moveListener = new Listener<>(event -> mc.player.noClip = true);

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PlayerSPPushOutOfBlocksEvent> pushListener = new Listener<>(event -> event.setCanceled(true));

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PacketEvent.Send> sendListener = new Listener<>(event -> {
        if ((PacketEvent.getPacket() instanceof CPacketPlayer || PacketEvent.getPacket() instanceof CPacketInput) && cancelPackets.getValue()) {
            event.cancel();
        }
    });
    
    public static double[] getRotationFromVec(Vec3d vec) {
        double xz = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        double yaw = normalizeAngle(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
        return new double[]{yaw, pitch};
    }
    
    public static double normalizeAngle(double angle) {
        angle %= 360.0;
        
        if (angle >= 180.0) {
            angle -= 360.0;
        }
        
        if (angle < -180.0) {
            angle += 360.0;
        }
        
        return angle;
    }
}
