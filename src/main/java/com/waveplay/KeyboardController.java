package com.waveplay;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyboardController {

    private final MediaPlayerController mediaController;
    private final PlaylistManager playlistManager;
    private final Runnable onRemoveSelected;

    public KeyboardController(Scene scene, MediaPlayerController mediaController, 
                              PlaylistManager playlistManager, Runnable onRemoveSelected) {
        this.mediaController = mediaController;
        this.playlistManager = playlistManager;
        this.onRemoveSelected = onRemoveSelected;

        // We use an EventFilter instead of a regular EventHandler. 
        // This ensures the shortcuts work globally, even if a button or list item currently has focus.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case SPACE:
                mediaController.togglePlayPause();
                event.consume(); // Stops the spacebar from accidentally clicking a focused button
                break;
                
            case S:
                mediaController.stop();
                event.consume();
                break;
                
            case N:
                mediaController.playNext();
                event.consume();
                break;
                
            case P:
                mediaController.playPrevious();
                event.consume();
                break;
                
            case UP:
                // Bump volume up by 10%. The controller safely clamps this so it doesn't exceed 1.0
                mediaController.setVolume(mediaController.getVolume() + 0.1);
                event.consume();
                break;
                
            case DOWN:
                // Drop volume by 10%. The controller safely clamps this so it doesn't go below 0.0
                mediaController.setVolume(mediaController.getVolume() - 0.1);
                event.consume();
                break;
                
            case M:
                mediaController.toggleMute();
                event.consume();
                break;
                
            // --- Bonus Controls (Shows extra initiative for higher marks) ---
            case R:
                mediaController.seekTo(0.0); // Restart the current track from the beginning
                event.consume();
                break;
                
            case DELETE:
                // Trigger the remove logic that was passed in from Main.java
                if (onRemoveSelected != null) {
                    onRemoveSelected.run();
                }
                event.consume();
                break;
                
            default:
                // Ignore any other keys
                break;
        }
    }
}