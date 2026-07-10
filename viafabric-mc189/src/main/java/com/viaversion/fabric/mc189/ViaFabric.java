/*
 * This file is part of ViaFabric - https://github.com/ViaVersion/ViaFabric
 * Copyright (C) 2018-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.fabric.mc189;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.viaversion.fabric.common.config.VFConfig;
import com.viaversion.fabric.common.platform.FabricInjector;
import com.viaversion.fabric.common.protocol.HostnameParserProtocol;
import com.viaversion.fabric.common.util.JLoggerToLog4j;
import com.viaversion.fabric.mc189.commands.NMSCommandImpl;
import com.viaversion.fabric.mc189.commands.VFCommandHandler;
import com.viaversion.fabric.mc189.platform.FabricPlatform;
import com.viaversion.fabric.mc189.platform.VFLoader;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoop;
import io.netty.channel.local.LocalEventLoopGroup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

public class ViaFabric implements ModInitializer {
    public static final Logger JLOGGER = new JLoggerToLog4j(LogManager.getLogger("ViaFabric"));
    public static final ExecutorService ASYNC_EXECUTOR;
    public static final EventLoop EVENT_LOOP;
    public static final CompletableFuture<Void> INIT_FUTURE = new CompletableFuture<>();
    public static VFConfig config;

    static {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ViaFabric-%d").build();
        ASYNC_EXECUTOR = Executors.newFixedThreadPool(8, factory);
        EVENT_LOOP = new LocalEventLoopGroup(1, factory).next(); // ugly code
        EVENT_LOOP.submit(INIT_FUTURE::join); // https://github.com/ViaVersion/ViaFabric/issues/53 ugly workaround code but works tm
    }

    @Override
    public void onInitialize() {
        FabricPlatform platform = new FabricPlatform();

        Via.init(ViaManagerImpl.builder()
                .injector(new FabricInjector())
                .loader(new VFLoader())
                .commandHandler(new VFCommandHandler())
                .platform(platform).build());

        platform.init();

        ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        HostnameParserProtocol.INSTANCE.initialize();
        HostnameParserProtocol.INSTANCE.register(Via.getManager().getProviders());
        ProtocolVersion.register(-2, "AUTO");

        // ViaBackwards must be initialized before ViaRewind (and any other addon that
        // calls BackwardsProtocol), because ViaRewind.init() calls
        // ViaBackwards.getPlatform().getLogger() which will NPE if ViaBackwards hasn't
        // registered its platform yet. Fabric Loader does not guarantee entrypoint order,
        // so we use getEntrypointContainers to get reliable mod IDs and explicitly run
        // ViaBackwards first, then all remaining addons.
        java.util.List<EntrypointContainer<Runnable>> viaApiContainers = FabricLoader.getInstance()
                .getEntrypointContainers("viafabric:via_api_initialized", Runnable.class);
        viaApiContainers.stream()
                .filter(c -> c.getProvider().getMetadata().getId().equals("viabackwards"))
                .forEach(c -> c.getEntrypoint().run());
        viaApiContainers.stream()
                .filter(c -> !c.getProvider().getMetadata().getId().equals("viabackwards"))
                .forEach(c -> c.getEntrypoint().run());

        registerCommandsV1();

        config = new VFConfig(FabricLoader.getInstance().getConfigDir().resolve("ViaFabric")
                .resolve("viafabric.yml").toFile(), JLOGGER);

        manager.onServerLoaded();

        INIT_FUTURE.complete(null);
    }

    private void registerCommandsV1() {
        try {
            net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents.START.register(server -> {
                ((net.minecraft.server.command.handler.CommandRegistry) server.getCommandHandler()).register(new NMSCommandImpl(Via.getManager().getCommandHandler()));
            });
        } catch (NoClassDefFoundError ignored2) {
            JLOGGER.info("Couldn't register command as OSL isn't installed");
        }
    }
}
