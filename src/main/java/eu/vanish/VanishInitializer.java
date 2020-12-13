package eu.vanish;

import net.fabricmc.api.ModInitializer;

public class VanishInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        Vanish.INSTANCE.init();
    }
}
