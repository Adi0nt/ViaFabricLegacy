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
package com.viaversion.fabric.mc189.providers;

import com.viaversion.fabric.mc189.ViaFabric;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.HandItemProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VFHandItemProvider extends HandItemProvider {
    public final Map<UUID, Item> serverPlayers = new ConcurrentHashMap<>();
    public Item clientItem = null;

    @Override
    public Item getHandItem(UserConnection info) {
        Item serverItem;
        if (info.isClientSide()) {
            return getClientItem();
        } else if ((serverItem = serverPlayers.get(info.getProtocolInfo().getUuid())) != null) {
            return serverItem.copy();
        }
        return super.getHandItem(info);
    }

    private Item getClientItem() {
        if (clientItem == null) {
            return new DataItem(0, (byte) 0, (short) 0, null);
        }
        return clientItem.copy();
    }

    @Environment(EnvType.CLIENT)
    public void registerClientTick() {
        try {
            MinecraftClientEvents.TICK_END.register(minecraft -> tickClient());
        } catch (NoClassDefFoundError ignored1) {
            ViaFabric.JLOGGER.info("OSL Client Lifecycle isn't installed");
        }
    }

    public void registerServerTick() {
        try {
            MinecraftServerEvents.TICK_END.register(this::tickServer);
        } catch (NoClassDefFoundError ignored1) {
            ViaFabric.JLOGGER.info("OSL Server Lifecycle isn't installed");
        }
    }

    private void tickClient() {
        LocalClientPlayerEntity p = Minecraft.getInstance().player;
        if (p != null) {
            clientItem = fromNative(p.inventory.getSelectedItem());
        }
    }

    private void tickServer(MinecraftServer server) {
        serverPlayers.clear();
        server.getPlayerManager().getAll().forEach(it -> serverPlayers
                .put(it.getUuid(), fromNative(it.inventory.getSelectedItem())));
    }

    private Item fromNative(ItemStack original) {
        if (original == null) return new DataItem(0, (byte) 0, (short) 0, null);
        int id = net.minecraft.item.Item.getId(original.getItem());
        return new DataItem(id, (byte) original.size, (short) original.getDamage(), null);
    }
}
