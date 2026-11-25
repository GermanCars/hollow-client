package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.ui.MixedGlyphs;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
abstract class FontProviderMixin {
    @Shadow protected abstract GlyphSource getGlyphSource(FontDescription description);

    @ModifyReturnValue(method = "getGlyphSource", at = @At("RETURN"))
    private GlyphSource hollow$mix(GlyphSource source, FontDescription description) {
        if (description.equals(FontDescription.DEFAULT)) return MixedGlyphs.game(s -> getGlyphSource(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hollow", "label" + s))), source);
        if (description instanceof FontDescription.Resource r && r.id().getNamespace().equals("hollow")) return MixedGlyphs.own(source, getGlyphSource(FontDescription.DEFAULT));
        return source;
    }
}
