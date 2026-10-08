package com.waveplay;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.util.List;

public class PlaylistManager {
    
    // We use an ObservableList so the UI (ListView) updates automatically 
    // whenever we add or remove a file, without needing extra code.
    private final ObservableList<String> playlist;
    
    // Tracks which song/video is currently playing. -1 means nothing is playing.
    private int currentIndex = -1;

    public PlaylistManager() {
        this.playlist = FXCollections.observableArrayList();
    }

    /**
     * Adds media files to the playlist.
     * Checks for duplicates so the same file isn't added twice.
     */
    public void addMediaFiles(List<File> files) {
        for (File file : files) {
            String path = file.getAbsolutePath();
            if (!playlist.contains(path)) {
                playlist.add(path);
            }
        }
    }

    /**
     * Removes a file from the playlist at the given index.
     * Safely adjusts the current index if the playing track is deleted.
     */
    public void removeMedia(int index) {
        if (index >= 0 && index < playlist.size()) {
            playlist.remove(index);
            
            // If the user deleted the track that is currently playing, stop and reset
            if (index == currentIndex) {
                currentIndex = -1; 
            } 
            // If we deleted a track before the current one, shift the index back by 1
            else if (index < currentIndex) {
                currentIndex--; 
            }
        }
    }

    /**
     * Clears the whole playlist and resets the player state.
     */
    public void clearPlaylist() {
        playlist.clear();
        currentIndex = -1;
    }

    /**
     * Gets the index of the next track. 
     * Uses modulo (%) to loop back to the start if we are at the end of the playlist.
     */
    public int getNextIndex() {
        if (playlist.isEmpty()) return -1;
        return (currentIndex + 1) % playlist.size();
    }

    /**
     * Gets the index of the previous track. 
     * Loops to the end of the playlist if we are at the beginning.
     */
    public int getPreviousIndex() {
        if (playlist.isEmpty()) return -1;
        return (currentIndex - 1 + playlist.size()) % playlist.size();
    }

    /**
     * Updates the currently playing index.
     */
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < playlist.size()) {
            this.currentIndex = index;
        } else {
            this.currentIndex = -1;
        }
    }

    // --- Getters ---

    public ObservableList<String> getPlaylist() {
        return playlist;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getCurrentMediaPath() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public boolean isEmpty() {
        return playlist.isEmpty();
    }
    
    /**
     * Extracts just the file name from the full path (e.g., "C:\music\song.mp3" -> "song.mp3")
     * so it looks clean in the UI.
     */
    public String getDisplayName(int index) {
        if (index >= 0 && index < playlist.size()) {
            File file = new File(playlist.get(index));
            return file.getName();
        }
        return "Unknown";
    }
}