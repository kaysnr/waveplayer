package com.waveplay;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

public class Main extends Application {

    // Core controllers to handle logic separately from the UI
    private PlaylistManager playlistManager;
    private MediaPlayerController mediaController;
    private KeyboardController keyboardController;

    // UI components that we need to update dynamically
    private ListView<String> playlistListView;
    private MediaView mediaView;
    private VBox audioPlaceholder;
    private Label nowPlayingLabel;
    private Slider progressSlider;
    private Label currentTimeLabel;
    private Label totalDurationLabel;
    private Slider volumeSlider;
    private Label volumeIconLabel;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize our logic controllers
        playlistManager = new PlaylistManager();
        mediaController = new MediaPlayerController(playlistManager);
        mediaView = mediaController.getMediaView();

        // 2. Set up the main layout using BorderPane for good resizing behavior
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-background");

        root.setTop(createHeader());
        root.setLeft(createSidebar());
        root.setCenter(createCenter());
        root.setBottom(createBottom());

        // 3. Create the scene and apply our custom CSS
        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // 4. Configure the main window
        primaryStage.setTitle("WAVEPLAY - Your media. Your sound.");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900); // Prevents the window from getting too small
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // 5. Set up keyboard shortcuts and initial UI states
        setupKeyboardControls(scene);
        updateNowPlayingUI();
        updateAudioVideoView(null);
    }

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.getStyleClass().add("header");
        
        // Using anonymous inner classes for quick styling, keeps code concise
        header.getChildren().addAll(
            new Label("🎵 WAVEPLAY") {{ getStyleClass().add("brand-title"); }},
            new Label("Your media. Your sound.") {{ getStyleClass().add("brand-tagline"); }}
        );
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("playlist-sidebar");
        sidebar.setPadding(new Insets(15));
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(250); // Keeps the sidebar from squishing too much on resize

        Label playlistTitle = new Label("PLAYLIST");
        playlistTitle.getStyleClass().add("playlist-title");

        // Set up the playlist list view
        playlistListView = new ListView<>();
        playlistListView.getStyleClass().add("playlist-list");
        // Directly bind the UI list to our manager's data list
        playlistListView.setItems(playlistManager.getPlaylist());
        
        // When a user clicks a song, update the player and UI
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int selectedIndex = playlistListView.getSelectionModel().getSelectedIndex();
                playlistManager.setCurrentIndex(selectedIndex);
                mediaController.loadAndPlayCurrentMedia();
                updateNowPlayingUI();
                updateAudioVideoView(newVal);
            }
        });

        // Add and Remove buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button addButton = new Button("＋ Add Media");
        addButton.getStyleClass().add("playlist-button");
        addButton.setOnAction(e -> handleAddMedia());

        Button removeButton = new Button("− Remove");
        removeButton.getStyleClass().add("playlist-button");
        removeButton.setOnAction(e -> handleRemoveMedia());

        buttonBox.getChildren().addAll(addButton, removeButton);
        sidebar.getChildren().addAll(playlistTitle, playlistListView, buttonBox);
        
        // Tells the list view to take up all extra vertical space in the sidebar
        VBox.setVgrow(playlistListView, Priority.ALWAYS); 
        return sidebar;
    }

    private VBox createCenter() {
        VBox center = new VBox(20);
        center.getStyleClass().add("main-content");
        center.setAlignment(Pos.CENTER);
        
        // Allows the center section to grow and fill the screen when maximized
        VBox.setVgrow(center, Priority.ALWAYS); 

        // Container for both video and audio placeholder
        StackPane mediaContainer = new StackPane();
        mediaContainer.getStyleClass().add("media-container");
        
        // Let the media container grow with the window
        VBox.setVgrow(mediaContainer, Priority.ALWAYS);
        StackPane.setAlignment(mediaView, Pos.CENTER);
        
        // Binds the video size to the container so it scales smoothly when resized
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());

        // Fallback UI for when an audio file (MP3/WAV) is playing
        audioPlaceholder = new VBox(20);
        audioPlaceholder.setAlignment(Pos.CENTER);
        audioPlaceholder.getStyleClass().add("audio-placeholder");
        Label audioIcon = new Label("🎵");
        audioIcon.getStyleClass().add("audio-icon");
        Label audioText = new Label("Now Playing Audio");
        audioText.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        audioPlaceholder.getChildren().addAll(audioIcon, audioText);

        mediaContainer.getChildren().addAll(mediaView, audioPlaceholder);

        // "Now Playing" info panel
        VBox nowPlayingBox = new VBox(5);
        nowPlayingBox.setAlignment(Pos.CENTER);
        nowPlayingBox.getStyleClass().add("now-playing");
        nowPlayingBox.getChildren().addAll(
            new Label("NOW PLAYING") {{ getStyleClass().add("now-playing-title"); }},
            nowPlayingLabel = new Label("No media selected") {{ getStyleClass().add("now-playing-text"); }}
        );

        center.getChildren().addAll(mediaContainer, nowPlayingBox);
        return center;
    }

    private VBox createBottom() {
        VBox bottom = new VBox(15);
        bottom.setPadding(new Insets(15, 20, 15, 20));
        bottom.getStyleClass().add("app-background");

        // Progress bar section
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.getStyleClass().add("progress-container");

        currentTimeLabel = new Label("00:00");
        currentTimeLabel.getStyleClass().add("time-label");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.getStyleClass().add("progress-bar");
        // Lets the progress bar stretch to fill the available width
        HBox.setHgrow(progressSlider, Priority.ALWAYS); 
        
        // Only seek when the user releases the mouse to prevent jitter
        progressSlider.setOnMouseReleased(e -> {
            double totalSeconds = mediaController.totalDurationProperty().get();
            double seekSeconds = (progressSlider.getValue() / 100.0) * totalSeconds;
            mediaController.seekTo(seekSeconds);
        });

        totalDurationLabel = new Label("00:00");
        totalDurationLabel.getStyleClass().add("time-label");
        progressBox.getChildren().addAll(currentTimeLabel, progressSlider, totalDurationLabel);

        // Playback and volume controls
        HBox controlsBox = new HBox(20);
        controlsBox.setAlignment(Pos.CENTER);

        Button prevBtn = createControlButton("⏮");
        prevBtn.setOnAction(e -> mediaController.playPrevious());

        Button playPauseBtn = createControlButton("▶");
        playPauseBtn.getStyleClass().add("play-button");
        playPauseBtn.setOnAction(e -> {
            mediaController.togglePlayPause();
            updatePlayPauseIcon(playPauseBtn);
        });

        Button stopBtn = createControlButton("■");
        stopBtn.setOnAction(e -> {
            mediaController.stop();
            updatePlayPauseIcon(playPauseBtn);
        });

        Button nextBtn = createControlButton("⏭");
        nextBtn.setOnAction(e -> mediaController.playNext());

        volumeIconLabel = new Label("🔊");
        volumeIconLabel.setStyle("-fx-font-size: 20px; -fx-cursor: hand;");
        volumeIconLabel.setOnMouseClicked(e -> {
            mediaController.toggleMute();
            updateVolumeUI();
        });

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mediaController.setVolume(newVal.doubleValue());
            updateVolumeUI();
        });

        controlsBox.getChildren().addAll(prevBtn, playPauseBtn, stopBtn, nextBtn, volumeIconLabel, volumeSlider);

        // Keyboard shortcut guide at the very bottom
        HBox footer = new HBox(20);
        footer.getStyleClass().add("keyboard-footer");
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().addAll(
            createShortcut("SPACE", "Play/Pause"),
            createShortcut("S", "Stop"),
            createShortcut("N", "Next"),
            createShortcut("P", "Previous"),
            createShortcut("↑", "Vol Up"),
            createShortcut("↓", "Vol Down"),
            createShortcut("M", "Mute")
        );

        bottom.getChildren().addAll(progressBox, controlsBox, footer);
        return bottom;
    }

    // Helper to create uniformly styled control buttons
    private Button createControlButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("control-button");
        return btn;
    }

    // Helper to build the keyboard shortcut labels
    private HBox createShortcut(String key, String action) {
        HBox shortcut = new HBox(8);
        shortcut.getStyleClass().add("shortcut-item");
        shortcut.setAlignment(Pos.CENTER);
        shortcut.getChildren().addAll(
            new Label(key) {{ getStyleClass().add("shortcut-key"); }},
            new Label(action)
        );
        return shortcut;
    }

    private void setupKeyboardControls(Scene scene) {
        // Pass a reference to the remove method so the keyboard controller can trigger it
        keyboardController = new KeyboardController(scene, mediaController, playlistManager, this::handleRemoveMedia);
        
        // Listen for time changes to update the progress bar and time labels
        mediaController.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            double current = newVal.doubleValue();
            double total = mediaController.totalDurationProperty().get();
            
            currentTimeLabel.setText(formatTime(current));
            totalDurationLabel.setText(formatTime(total));
            
            // Only update the slider position if the user isn't currently dragging it
            if (total > 0 && !progressSlider.isPressed()) {
                progressSlider.setValue((current / total) * 100);
            }
        });
    }

    private void handleAddMedia() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media Files");
        // Restrict to formats JavaFX actually supports
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Media Files", "*.mp3", "*.wav", "*.mp4"),
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"),
            new FileChooser.ExtensionFilter("Video Files", "*.mp4")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            playlistManager.addMediaFiles(files);
            
            // If this is the first file added, select and play it automatically
            if (playlistManager.getCurrentIndex() == -1) {
                playlistManager.setCurrentIndex(0);
                playlistListView.getSelectionModel().select(0);
                mediaController.loadAndPlayCurrentMedia();
                updateNowPlayingUI();
                updateAudioVideoView(playlistManager.getCurrentMediaPath());
            }
        }
    }

    private void handleRemoveMedia() {
        int selectedIndex = playlistListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            // Stop playback if we are deleting the currently playing track
            if (selectedIndex == playlistManager.getCurrentIndex()) {
                mediaController.stop();
            }
            
            playlistManager.removeMedia(selectedIndex);
            
            // Safely update the UI selection after removal
            if (!playlistManager.isEmpty()) {
                int newIndex = Math.min(selectedIndex, playlistManager.getPlaylist().size() - 1);
                playlistListView.getSelectionModel().select(newIndex);
            } else {
                updateNowPlayingUI();
                updateAudioVideoView(null);
            }
        }
    }

    private void updateNowPlayingUI() {
        if (playlistManager.isEmpty() || playlistManager.getCurrentIndex() == -1) {
            nowPlayingLabel.setText("No media selected");
        } else {
            nowPlayingLabel.setText(playlistManager.getDisplayName(playlistManager.getCurrentIndex()));
        }
    }

    private void updateAudioVideoView(String filePath) {
        if (filePath == null) {
            mediaView.setVisible(false);
            audioPlaceholder.setVisible(true);
        } else {
            String lowerPath = filePath.toLowerCase();
            // Show audio placeholder for audio files, otherwise show the video player
            if (lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav")) {
                mediaView.setVisible(false);
                audioPlaceholder.setVisible(true);
            } else {
                mediaView.setVisible(true);
                audioPlaceholder.setVisible(false);
            }
        }
    }

    private void updatePlayPauseIcon(Button btn) {
        if (mediaController.getStatus() == javafx.scene.media.MediaPlayer.Status.PLAYING) {
            btn.setText("⏸");
        } else {
            btn.setText("▶");
        }
    }

    private void updateVolumeUI() {
        double vol = mediaController.getVolume();
        volumeSlider.setValue(vol);
        volumeIconLabel.setText((mediaController.isMuted() || vol == 0.0) ? "🔇" : "🔊");
    }

    // Converts seconds into a clean MM:SS format
    private String formatTime(double seconds) {
        if (seconds <= 0 || Double.isNaN(seconds)) return "00:00";
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", mins, secs);
    }

    public static void main(String[] args) {
        launch(args);
    }
}