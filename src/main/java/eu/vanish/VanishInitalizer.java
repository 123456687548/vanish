package eu.vanish;

import net.fabricmc.api.ModInitializer;

public class VanishInitalizer implements ModInitializer {
    @Override
    public void onInitialize() {
        Vanish.INSTANCE.init();
    }
}
