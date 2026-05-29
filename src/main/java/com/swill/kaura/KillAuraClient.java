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
    private static int rotationCount = 0;
    private static long lastRotationReset = 0;
    private static LivingEntity currentTarget = null;
    private static long lastPacketTime = 0;
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    
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
            
            // ===== ОБХОД №2: атака только на земле (AAC, Spartan) =====
            if (!client.player.isOnGround()) return;
            
            // ===== ОБХОД №23: плавное изменение CPS (Verus, Themis) =====
            long now = System.currentTimeMillis();
            double cpsVariation = 0.5 + Math.sin(now / 10000.0) * 0.3;
            long interval = (long)(1700 * cpsVariation) + random.nextInt(200);
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
        
        // ===== ОБХОД №27: подмена высоты глаз (Grim, Spartan) =====
        if (random.nextInt(20) == 0) {
            eyePos = eyePos.add(0, 0.2, 0);
        }
        
        Box box = client.player.getBoundingBox().expand(3.4);
        
        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            box,
            e -> {
                if (e == client.player) return false;
                if (!e.isAlive()) return false;
                
                // ===== ОБХОД №6: анти-бот (Grim, Vulcan, Spartan) =====
                String name = e.getName().getString().toLowerCase();
                if (name.contains("bot") || name.contains("npc") || name.contains("anticheat")) return false;
                if (name.contains("grim") || name.contains("vulcan") || name.contains("polar")) return false;
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
        
        // ===== ОБХОД №7: смена цели после 8 ударов (Grim, Vulcan) =====
        if (target != currentTarget) {
            hitCount = 0;
            currentTarget = target;
        }
        hitCount++;
        if (hitCount > 8) {
            hitCount = 0;
            currentTarget = null;
        }
        
        // ===== ОБХОД №3: разные части тела (Spartan, Themis) =====
        double yOffset = 0.8;
        int hitZone = random.nextInt(3);
        if (hitZone == 0) yOffset = 1.5;      // голова
        else if (hitZone == 1) yOffset = 0.8; // тело
        else yOffset = 0.2;                   // ноги
        
        // ===== ОБХОД №14: смещение хитбокса (Spartan, Negativity) =====
        double xOff = (random.nextDouble() - 0.5) * 0.3;
        double zOff = (random.nextDouble() - 0.5) * 0.3;
        Vec3d hitPos = target.getPos().add(xOff, yOffset, zOff);
        
        Vec3d dir = hitPos.subtract(client.player.getEyePos());
        float yaw = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x*dir.x + dir.z*dir.z))));
        
        // ===== ОБХОД №1: погрешность поворота (Polar, Intave) =====
        yaw += (random.nextFloat() - 0.5f) * 3;
        pitch += (random.nextFloat() - 0.5f) * 2;
        
        // ===== ОБХОД №8: ограничение угла наклона (Polar, Themis) =====
        if (pitch > 89) pitch = 89;
        if (pitch < -89) pitch = -89;
        
        // ===== ОБХОД №28: ограничение поворотов в секунду (Vulcan, Matrix) =====
        long now = System.currentTimeMillis();
        if (now - lastRotationReset > 1000) {
            rotationCount = 0;
            lastRotationReset = now;
        }
        if (rotationCount < 6) {
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
            rotationCount++;
        }
        
        // ===== ОБХОД №4: порядок пакетов (Grim, Vulcan) =====
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        
        // ===== ОБХОД №5: замедление при атаке (Verus) =====
        Vec3d vel = client.player.getVelocity();
        client.player.setVelocity(vel.x * 0.6, vel.y, vel.z * 0.6);
        
        // ===== ОБХОД №20: человеческая задержка (Grim, Polar) =====
        try { Thread.sleep(30 + random.nextInt(70)); } catch (Exception e) {}
        
        // ===== ОБХОД №25: первый удар с промахом (Polar, Spartan) =====
        boolean miss = (hitCount <= 1 && random.nextFloat() < 0.1f);
        if (!miss) {
            client.interactionManager.attackEntity(client.player, target);
        }
        
        // ===== ОБХОД №18: принудительная анимация (Grim, Polar) =====
        client.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        client.player.swingHand(Hand.MAIN_HAND);
        
        // ===== ОБХОД №11: лишний пустой пакет (Grim) =====
        if (random.nextInt(10) == 0) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        
        // ===== ОБХОД №12: случайный сброс поворота (Grim, Vulcan) =====
        if (random.nextInt(40) == 0) {
            client.player.setYaw(client.player.getYaw() + (random.nextFloat() - 0.5f) * 10);
        }
        
        // ===== ОБХОД №13: рассинхрон позиции (Polar, Intave) =====
        if (random.nextInt(50) == 0) {
            Vec3d pos = client.player.getPos();
            client.player.setPosition(pos.x + 0.001, pos.y, pos.z + 0.001);
            client.player.setPosition(pos.x, pos.y, pos.z);
        }
        
        // ===== ОБХОД №10: подделка "на земле" (AAC, Spartan) =====
        if (random.nextInt(30) == 0) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        
        // ===== ОБХОД №24: задержка при смене цели (Grim, Vulcan) =====
        if (hitCount == 1 && hitCount > 0) {
            try { Thread.sleep(100); } catch (Exception e) {}
        }
        
        // ===== ОБХОД №21: обход времени атаки (Grim, Vulcan, Polar) =====
        long attackTime = System.currentTimeMillis();
        if (attackTime - lastPacketTime < 100) {
            try { Thread.sleep(10); } catch (Exception e) {}
        }
        lastPacketTime = attackTime;
        
        // ===== ОБХОД №26: обход дистанции (Grim, Spartan) =====
        if (random.nextInt(25) == 0) {
            Vec3d pos = client.player.getPos();
            client.player.setPosition(pos.x + 0.005, pos.y, pos.z + 0.005);
            client.player.setPosition(pos.x, pos.y, pos.z);
        }
        
        // ===== ОБХОД №29: обход ротации (Matrix, Vulcan) =====
        if (random.nextInt(35) == 0) {
            client.player.setYaw(client.player.getYaw() + (random.nextFloat() - 0.5f) * 5);
        }
        
        // ===== ОБХОД №30: обход движения (Verus, Intave) =====
        if (random.nextInt(45) == 0) {
            client.player.setVelocity(0, client.player.getVelocity().y, 0);
        }
        
        // ===== ОБХОД №15: обход шаблонов атак (Themis, Intave) =====
        if (hitCount == 3 || hitCount == 6) {
            try { Thread.sleep(80); } catch (Exception e) {}
        }
        
        // ===== ОБХОД №9: сохранение последнего поворота =====
        lastYaw = client.player.getYaw();
        lastPitch = client.player.getPitch();
        
        // ===== ОБХОД №17: обход урона за тик (Polar, Karhu) =====
        if (random.nextInt(20) == 0) {
            try { Thread.sleep(20); } catch (Exception e) {}
        }
        
        // ===== ОБХОД №19: обход стабильного CPS (Verus, Themis) =====
        if (random.nextInt(15) == 0) {
            try { Thread.sleep(50); } catch (Exception e) {}
        }
        
        // ===== ОБХОД №22: обход повторов (Spartan, Negativity) =====
        if (hitCount == 4 || hitCount == 7) {
            try { Thread.sleep(60); } catch (Exception e) {}
        }
    }
}
