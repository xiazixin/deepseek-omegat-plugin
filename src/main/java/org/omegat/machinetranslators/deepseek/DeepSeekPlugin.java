package org.omegat.machinetranslators.deepseek;

import org.omegat.core.Core;

public final class DeepSeekPlugin {

    private DeepSeekPlugin() {
    }

    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepSeekTranslate.class);
    }

    public static void unloadPlugins() {
    }
}
