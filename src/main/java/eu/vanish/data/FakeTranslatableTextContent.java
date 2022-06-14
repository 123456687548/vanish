package eu.vanish.data;

import net.minecraft.text.TranslatableTextContent;

public class FakeTranslatableTextContent extends TranslatableTextContent {
    public FakeTranslatableTextContent(String key) {
        super(key);
    }

    public FakeTranslatableTextContent(String key, Object... args) {
        super(key, args);
    }
}
