package net.nicotfpn.alientech.compat;

import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Isolated class for Patchouli integration.
 * This class is only loaded if Patchouli is on the classpath,
 * avoiding ClassNotFoundErrors in the main item class.
 */
public class PatchouliCompat {

    public static void openBook(ResourceLocation bookId) {
        PatchouliAPI.get().openBookGUI(bookId);
    }
}
