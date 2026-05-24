package com.piecemod.config;

import com.piecemod.data.BossTimerManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Builds the Cloth Config settings screen for PieceMod.
 *
 * <p>Categories:
 * <ul>
 *   <li><b>Display</b> – HUD X/Y position sliders
 *   <li><b>Bosses</b>  – per-boss visibility toggles
 * </ul>
 */
public class ConfigScreenBuilder {

    private final Screen parent;

    public ConfigScreenBuilder(Screen parent) {
        this.parent = parent;
    }

    public Screen build() {
        ModConfig config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("PieceMod Settings"))
                .setSavingRunnable(() -> ConfigManager.save(config));

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ---- Display category ----
        ConfigCategory display = builder.getOrCreateCategory(Text.literal("Display"));

        display.addEntry(
                eb.startIntSlider(Text.literal("HUD X Position"), config.getHudX(), 0, 1920)
                  .setDefaultValue(5)
                  .setSaveConsumer(config::setHudX)
                  .build());

        display.addEntry(
                eb.startIntSlider(Text.literal("HUD Y Position"), config.getHudY(), 0, 1080)
                  .setDefaultValue(5)
                  .setSaveConsumer(config::setHudY)
                  .build());

        // ---- Bosses category ----
        ConfigCategory bosses = builder.getOrCreateCategory(Text.literal("Bosses"));

        for (String boss : BossTimerManager.getInstance().getAll().keySet()) {
            final String bossName = boss;
            bosses.addEntry(
                    eb.startBooleanToggle(Text.literal(bossName), config.isBossEnabled(bossName))
                      .setDefaultValue(true)
                      .setSaveConsumer(v -> config.setBossEnabled(bossName, v))
                      .build());
        }

        return builder.build();
    }
}
