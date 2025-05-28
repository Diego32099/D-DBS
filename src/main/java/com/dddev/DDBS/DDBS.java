package com.dddev.DDBS;

import com.dddev.DDBS.utils.BulletManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


import org.slf4j.Logger;

@Mod(DDBS.MODID)
public class DDBS {

    public static final String MODID = "ddbs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DDBS() {
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BulletManager.serverTick();
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("DDBS mod initialized on server!");
    }

    // Client-side setup
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("DDBS client setup complete!");
            LOGGER.info("Logged in as: {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // Common event handler
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            // Common server-side events
        }
    }
}
