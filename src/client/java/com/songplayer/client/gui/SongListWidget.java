package com.songplayer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SongListWidget extends ObjectSelectionList<SongListWidget.SongEntry> {

    private File nowPlaying;

    public SongListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    public void setNowPlaying(File file) {
        this.nowPlaying = file;
    }

    public void addSong(File file) {
        SongEntry entry = new SongEntry(file);
        this.addEntry(entry);
        this.scrollToEntry(entry);
    }

    public void addSongQuietly(File file) {
        this.addEntry(new SongEntry(file));
    }

    public void setSongs(List<File> files) {
        this.clearEntries();
        this.setSelected(null);
        for (File file : files) {
            this.addSongQuietly(file);
        }
    }

    public List<File> getSongFiles() {
        List<File> files = new ArrayList<>();
        for (SongEntry entry : this.children()) {
            files.add(entry.file);
        }
        return files;
    }

    public File getSelectedFile() {
        SongEntry selected = this.getSelected();
        return selected == null ? null : selected.file;
    }

    public void removeSelected() {
        SongEntry selected = this.getSelected();
        if (selected != null) {
            this.removeEntry(selected);
            this.setSelected(null);
        }
    }

    public class SongEntry extends ObjectSelectionList.Entry<SongEntry> {
        private final File file;
        private final Component label;

        public SongEntry(File file) {
            this.file = file;
            this.label = Component.literal(file.getName());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
            boolean isPlaying = this.file.equals(SongListWidget.this.nowPlaying);
            guiGraphics.text(
                    SongListWidget.this.minecraft.font,
                    this.label,
                    this.getContentX() + 4,
                    this.getContentY() + (this.getContentHeight() - 9) / 2,
                    isPlaying ? 0xFF55FF55 : 0xFFFFFFFF
            );
        }

        @Override
        public Component getNarration() {
            return this.label;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            SongListWidget.this.setSelected(this);
            return true;
        }
    }
}
