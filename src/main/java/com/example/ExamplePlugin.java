package com.example;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Invalid Movement",
        description = "Display invalid movement blocked tiles",
        tags = {"invalid", "blocked", "movement", "tile", "flags"}
)
public class ExamplePlugin extends Plugin
{
    @Inject
    private InvalidMovementMapOverlay mapOverlay;

    @Inject
    private InvalidMovementSceneOverlay sceneOverlay;

    @Inject
    private InvalidMovementMinimapOverlay minimapOverlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    ExampleConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(mapOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(minimapOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(mapOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(minimapOverlay);
    }
}