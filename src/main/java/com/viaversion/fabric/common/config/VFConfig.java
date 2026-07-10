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
package com.viaversion.fabric.common.config;

import com.viaversion.viaversion.util.Config;
import com.viaversion.fabric.common.protocol.ProtocolSelectionManager;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class VFConfig extends Config {
    public static final String ENABLE_CLIENT_SIDE = "enable-client-side";
    public static final String CLIENT_SIDE_VERSION = "client-side-version";
    public static final String AUTO_DETECT_PROTOCOL = "auto-detect-protocol";
    public static final String SELECTED_PROTOCOL_VERSION = "selected-protocol-version";
    public static final String SHOW_VERSION_SELECTOR_BUTTON = "show-version-selector-button";
    public static final String ENABLE_DEBUG_LOGGING = "enable-debug-logging";
    public static final String CLIENT_SIDE_FORCE_DISABLE = "client-side-force-disable";
    public static final String HIDE_BUTTON = "hide-button";
    public static final String SEND_CONNECTION_DETAILS = "send-connection-details";

    public VFConfig(File configFile, Logger logger) {
        super(configFile, logger);
        reload();
    }

    @Override
    public URL getDefaultConfigURL() {
        return getClass().getClassLoader().getResource("assets/viafabric/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
        int legacyProtocol = getIntFromMap(map, CLIENT_SIDE_VERSION, ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
        boolean autoDetect = getBooleanFromMap(map, AUTO_DETECT_PROTOCOL, legacyProtocol == ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
        int selectedProtocol = getIntFromMap(map, SELECTED_PROTOCOL_VERSION,
                autoDetect ? ProtocolSelectionManager.AUTO_DETECT_PROTOCOL : legacyProtocol);

        if (autoDetect || !ProtocolSelectionManager.isValidSelection(selectedProtocol)) {
            autoDetect = true;
            selectedProtocol = ProtocolSelectionManager.AUTO_DETECT_PROTOCOL;
        }

        map.put(AUTO_DETECT_PROTOCOL, autoDetect);
        map.put(SELECTED_PROTOCOL_VERSION, selectedProtocol);
        map.put(CLIENT_SIDE_VERSION, autoDetect ? ProtocolSelectionManager.AUTO_DETECT_PROTOCOL : selectedProtocol);

        if (!map.containsKey(SHOW_VERSION_SELECTOR_BUTTON)) {
            map.put(SHOW_VERSION_SELECTOR_BUTTON, !getBooleanFromMap(map, HIDE_BUTTON, false));
        }
        if (!map.containsKey(ENABLE_DEBUG_LOGGING)) {
            map.put(ENABLE_DEBUG_LOGGING, false);
        }
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }

    public boolean isClientSideEnabled() {
        return getBoolean(ENABLE_CLIENT_SIDE, false);
    }

    public void setClientSideEnabled(boolean val) {
        set(ENABLE_CLIENT_SIDE, val);
    }

    public int getClientSideVersion() {
        if (isAutoDetectProtocol()) {
            return ProtocolSelectionManager.AUTO_DETECT_PROTOCOL;
        }
        int protocol = getSelectedProtocolVersion();
        if (!ProtocolSelectionManager.isValidSelection(protocol)) {
            return ProtocolSelectionManager.AUTO_DETECT_PROTOCOL;
        }
        return protocol;
    }

    public void setClientSideVersion(int val) {
        if (val == ProtocolSelectionManager.AUTO_DETECT_PROTOCOL || !ProtocolSelectionManager.isValidSelection(val)) {
            setAutoDetectProtocol(true);
            setSelectedProtocolVersion(ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
            set(CLIENT_SIDE_VERSION, ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
            return;
        }

        setAutoDetectProtocol(false);
        setSelectedProtocolVersion(val);
        set(CLIENT_SIDE_VERSION, val);
    }

    public boolean isAutoDetectProtocol() {
        return getBoolean(AUTO_DETECT_PROTOCOL, true);
    }

    public void setAutoDetectProtocol(boolean val) {
        set(AUTO_DETECT_PROTOCOL, val);
        if (val) {
            set(CLIENT_SIDE_VERSION, ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
        }
    }

    public int getSelectedProtocolVersion() {
        return getInt(SELECTED_PROTOCOL_VERSION, ProtocolSelectionManager.AUTO_DETECT_PROTOCOL);
    }

    public void setSelectedProtocolVersion(int val) {
        set(SELECTED_PROTOCOL_VERSION, val);
    }

    public Collection<?> getClientSideForceDisable() {
        return get(CLIENT_SIDE_FORCE_DISABLE, Collections.emptyList());
    }

    public void setHideButton(boolean val) {
        set(HIDE_BUTTON, val);
        set(SHOW_VERSION_SELECTOR_BUTTON, !val);
    }

    public boolean isHideButton() {
        return !isShowVersionSelectorButton();
    }

    public boolean isShowVersionSelectorButton() {
        return getBoolean(SHOW_VERSION_SELECTOR_BUTTON, true);
    }

    public void setShowVersionSelectorButton(boolean val) {
        set(SHOW_VERSION_SELECTOR_BUTTON, val);
        set(HIDE_BUTTON, !val);
    }

    public boolean isForcedDisable(String line) {
        return getClientSideForceDisable().contains(line);
    }

    public boolean isSendConnectionDetails() {
        return getBoolean(SEND_CONNECTION_DETAILS, false);
    }

    public boolean isDebugLoggingEnabled() {
        return getBoolean(ENABLE_DEBUG_LOGGING, false);
    }

    public void setDebugLoggingEnabled(boolean val) {
        set(ENABLE_DEBUG_LOGGING, val);
    }

    private static int getIntFromMap(Map<String, Object> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static boolean getBooleanFromMap(Map<String, Object> map, String key, boolean def) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return def;
    }
}
