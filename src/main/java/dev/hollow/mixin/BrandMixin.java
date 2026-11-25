package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.MiscModules;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientBrandRetriever.class)
abstract class BrandMixin {
    /** Asked for before the mod has started, too. */
    @ModifyReturnValue(method = "getClientModName", at = @At("RETURN"))
    private static String hollow$brand(String b) { return Hollow.started() ? Hollow.module(MiscModules.BrandSpoof.class).brand(b) : b; }
}
