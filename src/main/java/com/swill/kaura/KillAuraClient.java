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

    // ==================== НАСТРОЙКИ ====================
    private static boolean enabled = true;
    private static double reach = 3.4;
    private static long minIntervalMs = 1700;
    private static long maxIntervalMs = 1870;
    private static float maxYawError = 1.5f;
    private static float maxPitchError = 1.0f;
    private static boolean onlyOnGround = true;
    private static boolean randomHitbox = true;
    private static boolean packetOrderBypass = true;
    private static boolean slowOnAttack = true;
    private static boolean antiBot = true;
    private static int maxHitsOnSameTarget = 8;
    private static boolean forceSwingPacket = true;
    private static boolean randomRotationReset = true;
    private static boolean groundSpoof = true;
    private static boolean extraDummyPacket = true;
    private static boolean desyncPosition = true;
    private static boolean randomHitboxOffset = true;
    private static long minHumanDelayMs = 50;
    private static long maxHumanDelayMs = 150;
    private static boolean randomPatternChange = true;
    private static int patternChangeEvery = 8;
    private static boolean firstHitMiss = true;
    private static float firstMissChance = 0.1f;
    private static boolean newTargetDelay = true;
    private static int newTargetDelayTicks = 2;
    
    // ==================== ВНУТРЕННИЕ ПЕРЕМЕННЫЕ ====================
    private static Random random = new Random();
    private static KeyBinding toggleKey;
    private static long lastAttackTime = 0;
    private static int hitsOnCurrentTarget = 0;
    private static LivingEntity currentTarget = null;
    private static int attackPatternCounter = 0;
    private static LivingEntity lastTarget = null;
    
    @Override
    public void onInitializeClient() {
        System.out.println("[SWILL] KillAura ULTIMATE загружен | 28 обходов");
        
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.killaura.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.killaura"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.world == null) return;
            
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                System.out.println("[SWILL] KillAura: " + (enabled ? "ВКЛ" : "ВЫКЛ"));
            }
            if (!enabled) return;
            
            // Обход №2: только на земле
            if (onlyOnGround && !client.player.isOnGround()) return;
            
            // Обход №10: подделка "на земле"
            if (groundSpoof && random.nextInt(30) == 0) {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            }
            
            // Обход №23: плавное изменение интервала
            long now = System.currentTimeMillis();
            long interval = minIntervalMs + (long)(random.nextDouble() * (maxIntervalMs - minIntervalMs));
            
            if (now - lastAttackTime < interval) return;
            
            // Обход №22: смена паттерна атак
            if (randomPatternChange) {
                attackPatternCounter++;
                if (attackPatternCounter >= patternChangeEvery) {
                    attackPatternCounter = 0;
                }
            }
            
            LivingEntity target = findBestTarget(client);
            if (target != null) {
                // Обход №29: задержка при смене цели
                if (newTargetDelay && lastTarget != null && lastTarget != target) {
                    try { Thread.sleep(newTargetDelayTicks * 50); } catch (InterruptedException e) {}
                }
                lastTarget = target;
                attackWithAllBypasses(client, target);
                lastAttackTime = now;
            }
        });
    }
    
    private LivingEntity findBestTarget(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Box searchBox = client.player.getBoundingBox().expand(reach);
        
        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                if (entity == client.player) return false;
                if (!entity.isAlive()) return false;
                if (antiBot && isBot(entity)) return false;
                if (entity instanceof PlayerEntity && ((PlayerEntity)entity).isCreative()) return false;
                return true;
            }
        );
        
        LivingEntity bestTarget = null;
        double bestDistance = reach + 0.5;
        
        for (LivingEntity entity : entities) {
            double distance = eyePos.distanceTo(entity.getPos());
            if (distance <= reach && distance < bestDistance) {
                bestDistance = distance;
                bestTarget = entity;
            }
        }
        return bestTarget;
    }
    
    private boolean isBot(LivingEntity entity) {
        String name = entity.getName().getString().toLowerCase();
        if (name.startsWith("bot_")) return true;
        if (name.contains("anticheat")) return true;
        if (name.contains("grim")) return true;
        if (name.contains("vulcan")) return true;
        if (name.contains("npc")) return true;
        if (entity.getUuid().toString().contains("00000000")) return true;
        return false;
    }
    
    private void attackWithAllBypasses(MinecraftClient client, LivingEntity target) {
        
        // Обход №17: смена цели
        if (target == currentTarget) {
            hitsOnCurrentTarget++;
            if (hitsOnCurrentTarget >= maxHitsOnSameTarget) {
                LivingEntity newTarget = findBestTarget(client);
                if (newTarget != null && newTarget != currentTarget) {
                    currentTarget = newTarget;
                    hitsOnCurrentTarget = 0;
                } else {
                    hitsOnCurrentTarget = maxHitsOnSameTarget / 2;
                }
            }
        } else {
            hitsOnCurrentTarget = 0;
            currentTarget = target;
        }
        
        // Обход №19: смещение хитбокса
        double yOffset = 0.8;
        if (randomHitbox) {
            int hitZone = random.nextInt(3);
            if (hitZone == 0) yOffset = 1.5;
            else if (hitZone == 1) yOffset = 0.8;
            else yOffset = 0.2;
        }
        
        Vec3d hitPos;
        if (randomHitboxOffset) {
            double xOff = (random.nextDouble() - 0.5) * 0.3;
            double zOff = (random.nextDouble() - 0.5) * 0.3;
            hitPos = target.getPos().add(xOff, yOffset, zOff);
        } else {
            hitPos = target.getPos().add(0, yOffset, 0);
        }
        
        Vec3d direction = hitPos.subtract(client.player.getEyePos());
        float calculatedYaw = (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90);
        float calculatedPitch = (float)(-Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))));
        
        // Обход №1: погрешность поворота
        float yawError = (float)((random.nextDouble() - 0.5) * maxYawError * 2);
        float pitchError = (float)((random.nextDouble() - 0.5) * maxPitchError * 2);
        float finalYaw = calculatedYaw + yawError;
        float finalPitch = calculatedPitch + pitchError;
        
        // Поворот камеры
        client.player.setYaw(finalYaw);
        client.player.setPitch(finalPitch);
        
        // Обход №9: порядок пакетов
        if (packetOrderBypass) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        
        // Обход №15: замедление при атаке
        if (slowOnAttack) {
            Vec3d vel = client.player.getVelocity();
            client.player.setVelocity(vel.x * 0.5, vel.y, vel.z * 0.5);
        }
        
        // Обход №21: человеческая задержка
        long humanDelay = minHumanDelayMs + (long)(random.nextDouble() * (maxHumanDelayMs - minHumanDelayMs));
        try { Thread.sleep(humanDelay); } catch (InterruptedException e) {}
        
        // Обход №25: первый удар с промахом
        boolean shouldMiss = false;
        if (firstHitMiss && hitsOnCurrentTarget == 0 && random.nextFloat() < firstMissChance) {
            shouldMiss = true;
        }
        
        if (!shouldMiss) {
            client.interactionManager.attackEntity(client.player, target);
        }
        
        // Обход №18: принудительная анимация
        if (forceSwingPacket) {
            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        client.player.swingHand(Hand.MAIN_HAND);
        
        // Обход №18: лишний пустой пакет
        if (extraDummyPacket && random.nextInt(10) == 0) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        
        // Обход №13: рассинхрон позиции
        if (desyncPosition && random.nextInt(50) == 0) {
            Vec3d pos = client.player.getPos();
            client.player.setPosition(pos.x + 0.001, pos.y, pos.z + 0.001);
            client.player.setPosition(pos.x, pos.y, pos.z);
        }
        
        // Обход №7: случайный сброс поворота
        if (randomRotationReset && random.nextInt(40) == 0) {
            client.player.setYaw(client.player.getYaw() + (random.nextFloat() - 0.5f) * 10);
        }
    }
}
