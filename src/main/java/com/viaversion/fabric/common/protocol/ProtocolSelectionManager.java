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
package com.viaversion.fabric.common.protocol;

import com.viaversion.fabric.common.AddressParser;
import com.viaversion.fabric.common.config.VFConfig;
import com.viaversion.fabric.common.util.ProtocolUtils;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class ProtocolSelectionManager {
    public static final int AUTO_DETECT_PROTOCOL = -2;

    private ProtocolSelectionManager() {
    }

    public static List<Entry> getSelectableProtocols() {
        List<Entry> entries = new ArrayList<>();
        entries.add(Entry.autoDetect());

        List<ProtocolVersion> protocols = new ArrayList<>(ProtocolVersion.getProtocols());
        protocols.removeIf(protocol -> !isSelectable(protocol));
        protocols.sort(Collections.reverseOrder());

        for (ProtocolVersion protocol : protocols) {
            entries.add(Entry.protocol(protocol));
        }

        return entries;
    }

    public static boolean isSelectable(ProtocolVersion protocol) {
        if (!ProtocolUtils.isValid(protocol)) {
            return false;
        }

        VersionType type = protocol.getVersionType();
        if (type != VersionType.RELEASE && type != VersionType.RELEASE_INITIAL) {
            return false;
        }

        return ProtocolUtils.isSupportedClientSide(protocol);
    }

    public static boolean isValidSelection(int protocol) {
        if (protocol == AUTO_DETECT_PROTOCOL) {
            return true;
        }
        return ProtocolVersion.isRegistered(protocol) && isSelectable(ProtocolVersion.getProtocol(protocol));
    }

    public static void selectProtocol(VFConfig config, int protocol) {
        if (protocol == AUTO_DETECT_PROTOCOL || !isValidSelection(protocol)) {
            config.setAutoDetectProtocol(true);
            config.setSelectedProtocolVersion(AUTO_DETECT_PROTOCOL);
            config.setClientSideVersion(AUTO_DETECT_PROTOCOL);
            return;
        }

        config.setAutoDetectProtocol(false);
        config.setSelectedProtocolVersion(protocol);
        config.setClientSideVersion(protocol);
    }

    public static ProtocolVersion resolveTargetProtocol(VFConfig config, ProtocolVersion clientVersion, SocketAddress address,
                                                        AutoDetector detector, Logger logger) {
        int configuredProtocol = config.getClientSideVersion();

        if (address instanceof InetSocketAddress) {
            InetSocketAddress inetAddress = (InetSocketAddress) address;
            AddressParser parser = AddressParser.parse(inetAddress.getHostName());
            Integer addressProtocol = parser.protocol();
            if (addressProtocol != null) {
                configuredProtocol = addressProtocol;
                debug(config, logger, "Using address protocol override " + ProtocolUtils.getProtocolName(configuredProtocol)
                        + " for " + inetAddress.getHostString());
            } else if (configuredProtocol == AUTO_DETECT_PROTOCOL) {
                ProtocolVersion detected = detectProtocol(detector, inetAddress, config, logger);
                if (detected != null) {
                    configuredProtocol = detected.getVersion();
                }
            }
        }

        if (!ProtocolVersion.isRegistered(configuredProtocol)) {
            debug(config, logger, "Invalid configured protocol " + configuredProtocol + "; falling back to native client protocol");
            return clientVersion;
        }

        ProtocolVersion target = ProtocolVersion.getProtocol(configuredProtocol);
        debug(config, logger, "Resolved target protocol " + target.getName() + " (" + target.getVersion() + ")");
        return target;
    }

    private static ProtocolVersion detectProtocol(AutoDetector detector, InetSocketAddress address, VFConfig config, Logger logger) {
        try {
            CompletableFuture<ProtocolVersion> future = detector.detect(address);
            ProtocolVersion detected = future.getNow(null);
            if (detected != null) {
                debug(config, logger, "Auto Detect resolved " + address.getHostString() + " to " + detected.getName());
            } else {
                debug(config, logger, "Auto Detect has no cached result yet for " + address.getHostString());
            }
            return detected;
        } catch (Exception e) {
            logger.warning("ViaFabric protocol auto detect failed: " + e);
            return null;
        }
    }

    private static void debug(VFConfig config, Logger logger, String message) {
        if (config.isDebugLoggingEnabled()) {
            logger.info("[ProtocolSelection] " + message);
        }
    }

    public interface AutoDetector {
        CompletableFuture<ProtocolVersion> detect(InetSocketAddress address);
    }

    public static final class Entry {
        private final int protocol;
        private final String name;
        private final String detail;

        private Entry(int protocol, String name, String detail) {
            this.protocol = protocol;
            this.name = name;
            this.detail = detail;
        }

        public static Entry autoDetect() {
            return new Entry(AUTO_DETECT_PROTOCOL, "Auto Detect (1.7+ servers)", "Use ping detection when connecting");
        }

        public static Entry protocol(ProtocolVersion protocol) {
            return new Entry(protocol.getVersion(), protocol.getName(), "Protocol " + protocol.getVersion());
        }

        public int getProtocol() {
            return protocol;
        }

        public String getName() {
            return name;
        }

        public String getDetail() {
            return detail;
        }
    }
}
