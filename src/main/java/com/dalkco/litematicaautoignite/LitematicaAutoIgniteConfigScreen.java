package com.dalkco.litematicaautoignite;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Settings screen (opened from Mod Menu). */
public class LitematicaAutoIgniteConfigScreen extends Screen {
    private static final int W = 220;
    private static final int H = 20;
    private static final int GAP = 24;

    private final Screen parent;

    public LitematicaAutoIgniteConfigScreen(Screen parent) {
        super(Component.translatable("litematica-auto-ignite.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LitematicaAutoIgniteConfig.Data cfg = LitematicaAutoIgniteConfig.get();
        int x = this.width / 2 - W / 2;
        int y = Math.max(32, this.height / 6);

        y = toggle(x, y, "litematica-auto-ignite.config.enabled", cfg.enabled, v -> cfg.enabled = v);
        y = toggle(x, y, "litematica-auto-ignite.config.layer_range", cfg.respectLayerRange,
                v -> cfg.respectLayerRange = v);
        y = toggle(x, y, "litematica-auto-ignite.config.swing_hand", cfg.swingHand, v -> cfg.swingHand = v);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, Math.max(y + 12, this.height - 32), W, H).build());
    }

    private int toggle(int x, int y, String key, boolean value, Consumer<Boolean> setter) {
        Tooltip tooltip = Tooltip.create(Component.translatable(key + ".tooltip"));
        addRenderableWidget(CycleButton.onOffBuilder(value)
                .withTooltip(v -> tooltip)
                .create(x, y, W, H, Component.translatable(key), (b, v) -> {
                    setter.accept(v);
                    LitematicaAutoIgniteConfig.save();
                }));
        return y + GAP;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        LitematicaAutoIgniteConfig.save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}
