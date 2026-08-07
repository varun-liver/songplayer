package com.songplayer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SongPlayerScreen extends Screen {

    private static final int BUTTON_TOP = 24;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TOGGLE_SIZE = 20;
    private static final int NAV_BUTTON_WIDTH = 50;
    private static final int BUTTON_GAP = 8;
    private static final int STATUS_Y = BUTTON_TOP + BUTTON_HEIGHT + 6;
    private static final int SELECTED_ROW_Y = STATUS_Y + 12;
    private static final int SELECTED_BUTTON_WIDTH = 70;
    private static final int PLAYLIST_ROW_Y = SELECTED_ROW_Y + BUTTON_HEIGHT + 8;
    private static final int PLAYLIST_NAME_WIDTH = 100;
    private static final int PLAYLIST_BUTTON_WIDTH = 60;
    private static final int PLAYLIST_STATUS_Y = PLAYLIST_ROW_Y + BUTTON_HEIGHT + 4;
    private static final int LIST_TOP = PLAYLIST_STATUS_Y + 12;
    private static final int LIST_BOTTOM_MARGIN = 12;
    private static final int ENTRY_HEIGHT = 14;

    // Song list and playback state are static so they survive this screen being
    // closed and a new instance being created the next time the menu is opened.
    private static final List<File> playlist = new ArrayList<>();
    private static volatile boolean stopRequested;
    private static volatile Player currentPlayer;
    private static volatile File currentSong;

    private SongListWidget songListWidget;
    private Button playSelectedButton;
    private Button deleteSelectedButton;
    private EditBox playlistNameBox;
    private Component playlistStatus;

    public SongPlayerScreen() {
        super(Component.translatable("gui.songplayer.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.songListWidget = this.addRenderableWidget(new SongListWidget(
                this.minecraft,
                this.width,
                this.height - LIST_TOP - LIST_BOTTOM_MARGIN,
                LIST_TOP,
                ENTRY_HEIGHT
        ));
        for (File song : playlist) {
            this.songListWidget.addSongQuietly(song);
        }

        int rowWidth = BUTTON_WIDTH + BUTTON_GAP + NAV_BUTTON_WIDTH + BUTTON_GAP + TOGGLE_SIZE + BUTTON_GAP + NAV_BUTTON_WIDTH;
        int toggleY = BUTTON_TOP + (BUTTON_HEIGHT - TOGGLE_SIZE) / 2;
        int x = (this.width - rowWidth) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.songplayer.add_song"), button -> this.openFilePicker())
                .bounds(x, BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        x += BUTTON_WIDTH + BUTTON_GAP;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.songplayer.back"), button -> this.skipSong(-1))
                .bounds(x, BUTTON_TOP, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        x += NAV_BUTTON_WIDTH + BUTTON_GAP;

        this.addRenderableWidget(new PlayPauseButton(
                x,
                toggleY,
                TOGGLE_SIZE,
                TOGGLE_SIZE,
                () -> currentSong != null,
                button -> {
                    if (currentSong != null) {
                        this.stopPlayback();
                    } else {
                        this.playAllSongs();
                    }
                }
        ));
        x += TOGGLE_SIZE + BUTTON_GAP;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.songplayer.forward"), button -> this.skipSong(1))
                .bounds(x, BUTTON_TOP, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        int selectedRowWidth = SELECTED_BUTTON_WIDTH * 2 + BUTTON_GAP;
        int selectedRowLeft = (this.width - selectedRowWidth) / 2;

        this.playSelectedButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.songplayer.play_song"), button -> this.playSelectedSong())
                        .bounds(selectedRowLeft, SELECTED_ROW_Y, SELECTED_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
        this.deleteSelectedButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.songplayer.delete_song"), button -> this.deleteSelectedSong())
                        .bounds(selectedRowLeft + SELECTED_BUTTON_WIDTH + BUTTON_GAP, SELECTED_ROW_Y, SELECTED_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
        this.playSelectedButton.visible = false;
        this.deleteSelectedButton.visible = false;

        int playlistRowWidth = PLAYLIST_NAME_WIDTH + BUTTON_GAP + PLAYLIST_BUTTON_WIDTH + BUTTON_GAP + PLAYLIST_BUTTON_WIDTH;
        int playlistX = (this.width - playlistRowWidth) / 2;

        this.playlistNameBox = this.addRenderableWidget(new EditBox(
                this.font, playlistX, PLAYLIST_ROW_Y, PLAYLIST_NAME_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.songplayer.playlist_name")
        ));
        this.playlistNameBox.setMaxLength(48);
        this.playlistNameBox.setHint(Component.translatable("gui.songplayer.playlist_name_hint"));
        playlistX += PLAYLIST_NAME_WIDTH + BUTTON_GAP;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.songplayer.save_playlist"), button -> this.savePlaylist())
                .bounds(playlistX, PLAYLIST_ROW_Y, PLAYLIST_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        playlistX += PLAYLIST_BUTTON_WIDTH + BUTTON_GAP;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.songplayer.load_playlist"), button -> this.loadPlaylist())
                .bounds(playlistX, PLAYLIST_ROW_Y, PLAYLIST_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void openFilePicker() {
        Minecraft minecraft = this.minecraft;
        Thread thread = new Thread(() -> {
            String selectedPath;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterPatterns = stack.mallocPointer(1);
                filterPatterns.put(stack.UTF8("*.mp3"));
                filterPatterns.flip();

                selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select an MP3 file",
                        "",
                        filterPatterns,
                        "MP3 Audio Files",
                        false
                );
            }

            if (selectedPath == null) {
                return;
            }

            File selectedFile = new File(selectedPath);
            minecraft.execute(() -> {
                playlist.add(selectedFile);
                if (this.songListWidget != null) {
                    this.songListWidget.addSong(selectedFile);
                }
            });
        }, "songplayer-file-picker");
        thread.setDaemon(true);
        thread.start();
    }

    public void playAllSongs() {
        if (this.songListWidget == null) {
            return;
        }
        playSongs(this.songListWidget.getSongFiles());
    }

    private void playSelectedSong() {
        if (this.songListWidget == null) {
            return;
        }
        File selected = this.songListWidget.getSelectedFile();
        if (selected != null) {
            playSongs(List.of(selected));
        }
    }

    public void skipSong(int direction) {
        if (this.songListWidget == null) {
            return;
        }
        List<File> songs = this.songListWidget.getSongFiles();
        if (songs.isEmpty()) {
            return;
        }

        int currentIndex = songs.indexOf(currentSong);
        int nextIndex = currentIndex < 0
                ? (direction > 0 ? 0 : songs.size() - 1)
                : Math.floorMod(currentIndex + direction, songs.size());

        playSongs(songs.subList(nextIndex, songs.size()));
    }

    private void deleteSelectedSong() {
        if (this.songListWidget == null) {
            return;
        }
        File selected = this.songListWidget.getSelectedFile();
        if (selected != null) {
            if (selected.equals(currentSong)) {
                this.stopPlayback();
            }
            playlist.remove(selected);
            this.songListWidget.removeSelected();
        }
    }

    private void savePlaylist() {
        if (this.songListWidget == null || this.playlistNameBox == null) {
            return;
        }
        String name = sanitizeFileName(this.playlistNameBox.getValue());
        if (name.isEmpty()) {
            return;
        }

        Path playlistFile = getPlaylistsDirectory(this.minecraft).resolve(name + ".txt");
        List<File> songFiles = this.songListWidget.getSongFiles();
        Minecraft minecraft = this.minecraft;

        Thread thread = new Thread(() -> {
            boolean success;
            try {
                Files.createDirectories(playlistFile.getParent());
                List<String> lines = new ArrayList<>();
                for (File song : songFiles) {
                    lines.add(song.getAbsolutePath());
                }
                Files.write(playlistFile, lines);
                success = true;
            } catch (IOException e) {
                success = false;
            }

            boolean saved = success;
            minecraft.execute(() -> this.playlistStatus = saved
                    ? Component.translatable("gui.songplayer.playlist_saved", name)
                    : Component.translatable("gui.songplayer.playlist_save_failed", name));
        }, "songplayer-save-playlist");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadPlaylist() {
        if (this.songListWidget == null || this.playlistNameBox == null) {
            return;
        }
        String name = sanitizeFileName(this.playlistNameBox.getValue());
        if (name.isEmpty()) {
            return;
        }

        Path playlistFile = getPlaylistsDirectory(this.minecraft).resolve(name + ".txt");
        Minecraft minecraft = this.minecraft;

        Thread thread = new Thread(() -> {
            List<File> loadedSongs;
            try {
                loadedSongs = new ArrayList<>();
                for (String line : Files.readAllLines(playlistFile)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        loadedSongs.add(new File(trimmed));
                    }
                }
            } catch (IOException e) {
                minecraft.execute(() -> this.playlistStatus = Component.translatable("gui.songplayer.playlist_load_failed", name));
                return;
            }

            minecraft.execute(() -> {
                this.stopPlayback();
                playlist.clear();
                playlist.addAll(loadedSongs);
                if (this.songListWidget != null) {
                    this.songListWidget.setSongs(loadedSongs);
                }
                this.playlistStatus = Component.translatable("gui.songplayer.playlist_loaded", name, loadedSongs.size());
            });
        }, "songplayer-load-playlist");
        thread.setDaemon(true);
        thread.start();
    }

    private static Path getPlaylistsDirectory(Minecraft minecraft) {
        Path baseDir = minecraft.hasSingleplayerServer()
                ? minecraft.getSingleplayerServer().getServerDirectory()
                : minecraft.gameDirectory.toPath();
        return baseDir.resolve("songplayer").resolve("playlists");
    }

    private static String sanitizeFileName(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9 _-]", "");
    }

    private void playSongs(List<File> songs) {
        if (songs.isEmpty()) {
            return;
        }

        stopPlayback();
        stopRequested = false;

        Thread thread = new Thread(() -> {
            for (File song : songs) {
                if (stopRequested) {
                    break;
                }
                try (FileInputStream input = new FileInputStream(song)) {
                    Player player = new Player(input);
                    currentPlayer = player;
                    currentSong = song;
                    player.play();
                } catch (JavaLayerException | IOException ignored) {
                    // Skip a song that fails to open or decode and continue with the rest.
                }
            }
            currentPlayer = null;
            currentSong = null;
        }, "songplayer-playback");
        thread.setDaemon(true);
        thread.start();
    }

    public void stopPlayback() {
        stopRequested = true;
        currentSong = null;
        Player player = currentPlayer;
        if (player != null) {
            player.close();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        File playing = currentSong;
        if (this.songListWidget != null) {
            this.songListWidget.setNowPlaying(playing);

            boolean hasSelection = this.songListWidget.getSelectedFile() != null;
            this.playSelectedButton.visible = hasSelection;
            this.deleteSelectedButton.visible = hasSelection;
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        Component status = playing == null
                ? Component.translatable("gui.songplayer.not_playing")
                : Component.translatable("gui.songplayer.now_playing", playing.getName());
        guiGraphics.centeredText(this.font, status, this.width / 2, STATUS_Y, 0xFFAAAAAA);

        if (this.playlistStatus != null) {
            guiGraphics.centeredText(this.font, this.playlistStatus, this.width / 2, PLAYLIST_STATUS_Y, 0xFFFFFF55);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}