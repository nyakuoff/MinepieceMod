package com.piecemod.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers PieceMod's config screen with the Mod Menu mod.
 * Declared as a {@code modmenu} entrypoint in {@code fabric.mod.json}.
 */
public class ModMenuImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreenBuilder(parent).build();
    }
}
