package net.nicotfpn.alientech.client;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Interface for BlockEntities that want to display custom
 * information on the player's HUD when looked at.
 *
 * Each implementing BlockEntity controls its own HUD rendering,
 * keeping the overlay code clean and modular.
 */
public interface IHudProvider {

    /**
     * Collects HUD text lines to be rendered on screen.
     * Each entry is a Component (supports color/style) that will be
     * drawn centered on screen above the hotbar.
     *
     * @param lines Mutable list to add lines to. Lines are rendered top-to-bottom.
     */
    void addHudLines(List<Component> lines);
}
