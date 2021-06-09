package eu.vanish.data;

import net.minecraft.text.TranslatableText;

public class FakeTranslatableText extends TranslatableText {
    public FakeTranslatableText(String key) {
        super(key);
    }

    public FakeTranslatableText(String key, Object... args) {
        super(key, args);
    }
}
