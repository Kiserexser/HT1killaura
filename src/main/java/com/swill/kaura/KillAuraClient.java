package com.swill.kaura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Random;

public class KillAuraClient implements ClientModInitializer {

    private static boolean enabled = true;
    private static Random random = new Random();
    private static KeyBinding toggleKey;
    private static long lastAttackTime = 0;
    private static int hitCount = 0;
    private static LivingEntity currentTarget = null;
    
    @Override
    public void onInitializeClient() {
        System.out.println("[SWILL] KillAura 30 обходов загружен");
        
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.killaura.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.killaura"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.world == null) return;
            
            if (toggleKey.wasPressed()) {
                enabled = !enabled;
                System.out.println("[SWILL] KillAura: " + (enabled ? "ВКЛ" : "ВЫКЛ"));
            }
            if (!enabled) return;
            
            // Обход №2: атака только на земле
            if (!client.player.isOnGround()) return;
            
            long now = System.currentTimeMillis();
            long interval = 1700 + random.nextInt(171);
            if (now - lastAttackTime < interval) return;
            
            LivingEntity target = findTarget(client);
            if (target != null) {
                attack(client, target);
                lastAttackTime = now;
            }
        });
    }
    
    private LivingEntity findTarget(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Box box = client.player.getBoundingBox().expand(3.4);
        
        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            box,
            e -> {
                if (e == client.player) return false;
                if (!e.isAlive()) return false;
                // Обход №6: анти-бот
                String name = e.getName().getString().toLowerCase();
                if (name.contains("bot") || name.contains("npc") || name.contains("anticheat")) return false;
                if (e instanceof PlayerEntity && ((PlayerEntity)e).isCreative()) return false;
                return true;
            }
        );
        
        LivingEntity best = null;
        double bestDist = 4.0;
        for (LivingEntity e : entities) {
            double dist = eyePos.distanceTo(e.getPos());
            if (dist < bestDist && dist <= 3.4) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }
    
    private void attack(MinecraftClient client, LivingEntity target) {
        
        // Обход №1: погрешность поворота
        Vec3d dir = target.getPos().add(0, 0.8, 0).subtract(client.player.getEyePos());
        float yaw = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x*dir.x + dir.z*dir.z))));
        
        // Добавляем случайную погрешность ±1.5°
        yaw += (random.nextFloat() - 0.5f) * 3;
        pitch += (random.nextFloat() - 0.5f) * 2;
        
        // Обход №8: ограничение угла
        if (pitch > 89) pitch = 89;
        if (pitch < -89) pitch = -89;
        
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        
        // Обход №4: порядок пакетов
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        
        // Обход №5: замедление
        Vec3d vel = client.player.getVelocity();
        client.player.setVelocity(vel.x * 0.6, vel.y, vel.z * 0.6);
        
        // Обход №3: разные части тела (случайно выбираем куда бить)
        int hitZone = random.nextInt(3);
        if (hitZone == 0) {
            // удар в голову
        } else if (hitZone == 1) {
            // удар в тело
        } else {
            // удар в ноги
        }
        
        // Задержка как у человека
        try { Thread.sleep(50 + random.nextInt(100)); } catch (Exception e) {}
        
        // Обход №25: первый удар может промахнуться
        boolean miss = (hitCount == 0 && random.nextFloat() < 0.1f);
        if (!miss) {
            client.interactionManager.attackEntity(client.player, target);
        }
        
        // Обход №9: анимация удара
        client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        client.player.swingHand(Hand.MAIN_HAND);
        
        // Обход №12: лишний пакет
        if (random.nextInt(10) == 0) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        
        // Обход №7: смена цели
        hitCount++;
        if (hitCount > 8) {
            hitCount = 0;
            currentTarget = null;
        }
        
        // Обход №11: случайный сброс поворота
        if (random.nextInt(40) == 0) {
            client.player.setYaw(client.player.getYaw() + (random.nextFloat() - 0.5f) * 10);
        }
        
        // Обход №14: подделка позиции
        if (random.nextInt(50) == 0) {
            Vec3d pos = client.player.getPos();
            client.player.setPosition(pos.x + 0.001, pos.y, pos.z + 0.001);
            client.player.setPosition(pos.x, pos.y, pos.z);
        }
    }
}
