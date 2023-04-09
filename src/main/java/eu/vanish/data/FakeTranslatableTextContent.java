package eu.vanish.data;

import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.Nullable;

public class FakeTranslatableTextContent extends TranslatableTextContent {

    public FakeTranslatableTextContent(String key, @Nullable String fallback, Object[] args) {
        super(key, fallback, args);
    }
}
