package com.songplayer.client;


import com.mojang.blaze3d.platform.InputConstants;
import com.songplayer.client.gui.SongPlayerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SongplayerClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("songplayer", "main"));

    private static KeyMapping openMenuKeyMapping;
    private static KeyMapping playKeyMapping;
    private static KeyMapping pauseKeyMapping;
    private static KeyMapping backKeyMapping;
    private static KeyMapping forwardKeyMapping;

    @Override
    public void onInitializeClient() {
        openMenuKeyMapping = registerUnboundKey("key.songplayer.open_menu", GLFW.GLFW_KEY_K);
        playKeyMapping = registerUnboundKey("key.songplayer.play", GLFW.GLFW_KEY_UNKNOWN);
        pauseKeyMapping = registerUnboundKey("key.songplayer.pause", GLFW.GLFW_KEY_UNKNOWN);
        backKeyMapping = registerUnboundKey("key.songplayer.back", GLFW.GLFW_KEY_UNKNOWN);
        forwardKeyMapping = registerUnboundKey("key.songplayer.forward", GLFW.GLFW_KEY_UNKNOWN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKeyMapping.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new SongPlayerScreen());
                }
            }

            if (client.screen instanceof SongPlayerScreen songPlayerScreen) {
                while (playKeyMapping.consumeClick()) {
                    songPlayerScreen.playAllSongs();
                }
                while (pauseKeyMapping.consumeClick()) {
                    songPlayerScreen.stopPlayback();
                }
                while (backKeyMapping.consumeClick()) {
                    songPlayerScreen.skipSong(-1);
                }
                while (forwardKeyMapping.consumeClick()) {
                    songPlayerScreen.skipSong(1);
                }
            }
        });
    }

    private static KeyMapping registerUnboundKey(String name, int defaultKeyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name,
                InputConstants.Type.KEYSYM,
                defaultKeyCode,
                CATEGORY
        ));
    }
}