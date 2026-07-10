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
package com.viaversion.fabric.mc189.gui;

import com.viaversion.fabric.common.protocol.ProtocolSelectionManager;
import com.viaversion.fabric.mc189.ViaFabric;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.TranslatableText;
import org.lwjgl.input.Mouse;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ViaProtocolSelectionScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFD84A;
    private static final int SUBTITLE_COLOR = 0xFFFFFF;
    private static final int SELECTED_COLOR = 0x55FF55;
    private static final int UNSELECTED_COLOR = 0xFF5555;
    private static final int ENTRY_HEIGHT = 26;
    private static final int ENTRY_GAP = 4;
    private static final int MOUSE_WHEEL_STEP = 32;

    private final Screen parent;
    private List<ProtocolSelectionManager.Entry> entries;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int maxScroll;
    private float scroll;

    public ViaProtocolSelectionScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void init() {
        this.entries = ProtocolSelection.getEntries();
        this.listWidth = Math.min(260, this.width - 40);
        this.listLeft = (this.width - this.listWidth) / 2;
        this.listTop = 58;
        this.listHeight = Math.max(60, this.height - 112);
        this.maxScroll = Math.max(0, this.entries.size() * (ENTRY_HEIGHT + ENTRY_GAP) - ENTRY_GAP - this.listHeight);
        this.scroll = clamp(this.scroll, 0, this.maxScroll);

        int bottomY = this.height - 32;
        this.buttons.add(new ListeneableButton("via-options".hashCode(), this.width / 2 - 154, bottomY, 100, 20,
                "Options", button -> Minecraft.getInstance().openScreen(new ViaOptionsScreen(this))));
        this.buttons.add(new ListeneableButton("done".hashCode(), this.width / 2 - 50, bottomY, 100, 20,
                new TranslatableText("gui.done").getString(), button -> Minecraft.getInstance().openScreen(this.parent)));
        this.buttons.add(new ListeneableButton("enable-client-side".hashCode(), this.width / 2 + 54, bottomY, 100, 20,
                getClientSideText(), this::toggleClientSide));
    }

    @Override
    public void handleMouse() {
        super.handleMouse();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && isMouseOverList(Mouse.getEventX() * this.width / this.minecraft.width,
                this.height - Mouse.getEventY() * this.height / this.minecraft.height - 1)) {
            this.scroll = clamp(this.scroll - Integer.signum(wheel) * MOUSE_WHEEL_STEP, 0, this.maxScroll);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0 || !isMouseOverList(mouseX, mouseY)) {
            return;
        }

        int relativeY = mouseY - this.listTop + (int) this.scroll;
        int entryIndex = relativeY / (ENTRY_HEIGHT + ENTRY_GAP);
        int entryY = entryIndex * (ENTRY_HEIGHT + ENTRY_GAP);
        if (entryIndex < 0 || entryIndex >= this.entries.size() || relativeY < entryY || relativeY > entryY + ENTRY_HEIGHT) {
            return;
        }

        ProtocolSelectionManager.Entry entry = this.entries.get(entryIndex);
        ProtocolSelectionManager.selectProtocol(ViaFabric.config, entry.getProtocol());
        ViaFabric.config.setClientSideEnabled(true);
        ViaFabric.config.save();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderDimmedBackground();

        drawCenteredString(this.textRenderer, "ViaFabricPlus", this.width / 2, 18, TITLE_COLOR);
        drawCenteredString(this.textRenderer, "ViaFabric protocol selector", this.width / 2, 36, SUBTITLE_COLOR);

        renderList(mouseX, mouseY);
        super.render(mouseX, mouseY, delta);
    }

    private void renderDimmedBackground() {
        this.renderBackground();
        GuiElement.fill(0, 0, this.width, this.height, 0xAA000000);
    }

    private void renderList(int mouseX, int mouseY) {
        GuiElement.fill(this.listLeft - 2, this.listTop - 2, this.listLeft + this.listWidth + 2,
                this.listTop + this.listHeight + 2, 0x66000000);

        int selectedProtocol = ViaFabric.config.getClientSideVersion();
        int contentTop = this.listTop - (int) this.scroll;

        for (int i = 0; i < this.entries.size(); i++) {
            int y = contentTop + i * (ENTRY_HEIGHT + ENTRY_GAP);
            if (y + ENTRY_HEIGHT < this.listTop || y > this.listTop + this.listHeight) {
                continue;
            }

            ProtocolSelectionManager.Entry entry = this.entries.get(i);
            boolean selected = entry.getProtocol() == selectedProtocol;
            boolean hovered = mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth
                    && mouseY >= y && mouseY <= y + ENTRY_HEIGHT
                    && mouseY >= this.listTop && mouseY <= this.listTop + this.listHeight;

            int background = selected ? 0x55225522 : hovered ? 0x55333333 : 0x33111111;
            GuiElement.fill(this.listLeft, y, this.listLeft + this.listWidth, y + ENTRY_HEIGHT, background);
            drawCenteredString(this.textRenderer, entry.getName(), this.width / 2, y + 5, selected ? SELECTED_COLOR : UNSELECTED_COLOR);
            drawCenteredString(this.textRenderer, entry.getDetail(), this.width / 2, y + 16, 0xB0B0B0);
        }

        renderScrollbar();
    }

    private void renderScrollbar() {
        int scrollbarLeft = this.listLeft + this.listWidth + 6;
        GuiElement.fill(scrollbarLeft, this.listTop, scrollbarLeft + 3, this.listTop + this.listHeight, 0x66000000);
        if (this.maxScroll <= 0) {
            GuiElement.fill(scrollbarLeft, this.listTop, scrollbarLeft + 3, this.listTop + this.listHeight, 0xFF888888);
            return;
        }

        int thumbHeight = Math.max(18, this.listHeight * this.listHeight / (this.listHeight + this.maxScroll));
        int thumbTop = this.listTop + (int) (this.scroll * (this.listHeight - thumbHeight) / this.maxScroll);
        GuiElement.fill(scrollbarLeft, thumbTop, scrollbarLeft + 3, thumbTop + thumbHeight, 0xFFAAAAAA);
    }

    private boolean isMouseOverList(int mouseX, int mouseY) {
        return mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth
                && mouseY >= this.listTop && mouseY <= this.listTop + this.listHeight;
    }

    private String getClientSideText() {
        return ViaFabric.config.isClientSideEnabled() ? "Disable Via" : "Enable Via";
    }

    private void toggleClientSide(ButtonWidget button) {
        ViaFabric.config.setClientSideEnabled(!ViaFabric.config.isClientSideEnabled());
        ViaFabric.config.save();
        button.message = getClientSideText();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
