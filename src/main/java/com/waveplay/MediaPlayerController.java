package com.waveplay;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import java.io.File;

public class MediaPlayerController {

    private final PlaylistManager playlistManager;
    private final MediaView mediaView;
    private MediaPlayer mediaPlayer;
    
    // Properties to track time for the progress bar UI
    private final DoubleProperty currentTime = new SimpleDoubleProperty(0.0);
    private final DoubleProperty totalDuration = new SimpleDoubleProperty(0.0);
    
    // Volume state management
    private double previousVolume = 0.5;
    private boolean isMuted = false;

    public MediaPlayerController(PlaylistManager playlistManager) {
        this.playlistManager = playlistManager;
        this.mediaView = new MediaView();
        
        // Let the parent layout control the size for true responsiveness
        mediaView.setPreserveRatio(true);
    }

    /**
     * Loads the media at the current playlist index and starts playback.
     */
    public void loadAndPlayCurrentMedia() {
        String mediaPath = playlistManager.getCurrentMediaPath();
        
        // Safety check: if there's no media path, just stop the player
        if (mediaPath == null || mediaPath.isEmpty()) {
            stop();
            return;
        }

        // 1. Clean up the old player before loading a new one. 
        // This prevents memory leaks and stops overlapping audio.
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            // 2. Convert the file path to a proper URI so JavaFX can read it correctly on Windows.
            File file = new File(mediaPath);
            Media media = new Media(file.toURI().toString());
            
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // 3. Auto-advance to the next track when the current one finishes
            mediaPlayer.setOnEndOfMedia(() -> {
                int nextIndex = playlistManager.getNextIndex();
                // Check if there is a next track and it's not the same as the current one
                if (nextIndex != -1 && nextIndex != playlistManager.getCurrentIndex()) {
                    playlistManager.setCurrentIndex(nextIndex);
                    loadAndPlayCurrentMedia();
                } else {
                    stop(); // End of playlist reached
                }
            });

            // 4. Update our time properties so the UI progress bar can react to changes
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                currentTime.set(newTime.toSeconds());
            });
            
            mediaPlayer.totalDurationProperty().addListener((obs, oldDur, newDur) -> {
                if (newDur != null && !newDur.isUnknown()) {
                    totalDuration.set(newDur.toSeconds());
                }
            });

            // 5. Restore the volume state and start playing
            mediaPlayer.setVolume(isMuted ? 0.0 : previousVolume);
            mediaPlayer.play();

        } catch (MediaException e) {
            System.err.println("Error loading media: " + e.getMessage());
            
            // Show a friendly error message if the file format isn't supported by JavaFX
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Playback Error");
            alert.setHeaderText("Could not play media");
            alert.setContentText("Error: " + e.getMessage() + 
                                 "\n\nPlease ensure the file is a valid MP3, WAV, or MP4 (H.264).");
            alert.showAndWait();
            
            stop();
        }
    }

    // --- Playback Controls ---

    public void play() {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            mediaPlayer.play();
        } else if (mediaPlayer == null && !playlistManager.isEmpty()) {
            // If nothing is loaded but the playlist has items, load and play the current index
            loadAndPlayCurrentMedia();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) {
            play();
        } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pause();
        } else {
            play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            currentTime.set(0.0);
        }
    }

    public void playNext() {
        int nextIndex = playlistManager.getNextIndex();
        if (nextIndex != -1) {
            playlistManager.setCurrentIndex(nextIndex);
            loadAndPlayCurrentMedia();
        }
    }

    public void playPrevious() {
        // Standard media player behavior: if more than 3 seconds in, restart the current track.
        // Otherwise, go to the actual previous track.
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3.0) {
            mediaPlayer.seek(Duration.ZERO);
        } else {
            int prevIndex = playlistManager.getPreviousIndex();
            if (prevIndex != -1) {
                playlistManager.setCurrentIndex(prevIndex);
                loadAndPlayCurrentMedia();
            }
        }
    }

    // --- Volume Controls ---

    public void setVolume(double volume) {
        // Clamp volume between 0.0 and 1.0 to prevent errors
        volume = Math.max(0.0, Math.min(1.0, volume));
        previousVolume = volume;
        isMuted = (volume == 0.0);
        
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (mediaPlayer != null) {
            // If muting, set to 0. If unmuting, restore the previous volume level
            mediaPlayer.setVolume(isMuted ? 0.0 : previousVolume);
        }
    }

    public boolean isMuted() { 
        return isMuted; 
    }
    
    public double getVolume() { 
        return isMuted ? 0.0 : previousVolume; 
    }

    public void seekTo(double seconds) {
        if (mediaPlayer != null && totalDuration.get() > 0) {
            // Ensure the seek time is within valid bounds
            seconds = Math.max(0.0, Math.min(totalDuration.get(), seconds));
            mediaPlayer.seek(Duration.seconds(seconds));
            currentTime.set(seconds);
        }
    }

    // Getters for UI Bindin

    public MediaView getMediaView() { 
        return mediaView; 
    }
    
    public DoubleProperty currentTimeProperty() { 
        return currentTime; 
    }
    
    public DoubleProperty totalDurationProperty() { 
        return totalDuration; 
    }
    
    public MediaPlayer.Status getStatus() {
        if (mediaPlayer == null) return MediaPlayer.Status.UNKNOWN;
        return mediaPlayer.getStatus();
    }
}