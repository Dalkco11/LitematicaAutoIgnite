package com.dalkco.litematicaautoignite;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Loaded only when Mod Menu is installed (it is an optional dependency). */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LitematicaAutoIgniteConfigScreen::new;
    }
}
