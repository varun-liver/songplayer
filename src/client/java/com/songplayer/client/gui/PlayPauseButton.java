package com.songplayer.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;

public class PlayPauseButton extends ImageButton {

    // play_button.png visually contains the pause (||) icon and pause_button.png
    // visually contains the play (▶) icon - the two files were named opposite
    // their artwork, so these point at what each file actually looks like.
    private static final Identifier PLAY_ICON = Identifier.fromNamespaceAndPath("songplayer", "pause_button");
    private static final Identifier PAUSE_ICON = Identifier.fromNamespaceAndPath("songplayer", "play_button");

    private final BooleanSupplier isPlaying;

    public PlayPauseButton(int x, int y, int width, int height, BooleanSupplier isPlaying, Button.OnPress onPress) {
        super(x, y, width, height, new WidgetSprites(PLAY_ICON), onPress);
        this.isPlaying = isPlaying;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        Identifier icon = this.isPlaying.getAsBoolean() ? PAUSE_ICON : PLAY_ICON;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, this.getX(), this.getY(), this.width, this.height);
    }
}
