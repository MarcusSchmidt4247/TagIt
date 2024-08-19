/* TagIt
 * MediaControlView.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class MediaControlView extends StackPane
{
    private final MediaView mediaView;
    private MediaPlayer mediaPlayer = null;

    private final Button playButton;
    private final Button volumeButton;
    private final Slider timeSlider;
    private final Slider volumeSlider;
    private final Label currentTime;
    private final Label totalTime;

    private boolean mute = false;
    private double volume = 1.0;
    private double mediaRatio = -1;

    public MediaControlView()
    {
        super();

        /* The MediaView cannot be managed by its parent StackPane *and* bound to its parent's dimensions or else
         * it won't allow its parent to shrink. Instead, add listeners that update the MediaView's dimensions and
         * then manually center it in the StackPane. */
        mediaView = new MediaView();
        mediaView.setManaged(false);
        widthProperty().addListener((observableValue, number, t1) ->
        {
            mediaView.setFitWidth(getWidth());
            centerMedia();
        });
        heightProperty().addListener((observableValue, number, t1) ->
        {
            mediaView.setFitHeight(getHeight());
            centerMedia();
        });
        getChildren().add(mediaView);

        // Create a horizontal layout where the media player controls will be placed
        HBox controlBox = new HBox();
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setFillHeight(true);
        controlBox.setPadding(new Insets(10, 10, 10, 10));

        // Add a button that plays/pauses the media and changes its icon when clicked
        playButton = new Button();
        playButton.setGraphic(new FontIcon("bx-pause"));
        playButton.setVisible(isVisible());
        playButton.setOnAction(actionEvent ->
        {
            if (mediaPlayer != null)
            {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING)
                {
                    mediaPlayer.pause();
                    playButton.setGraphic(new FontIcon("bx-play"));
                }
                else if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN && mediaPlayer.getStatus() != MediaPlayer.Status.HALTED)
                {
                    mediaPlayer.play();
                    playButton.setGraphic(new FontIcon("bx-pause"));
                }
            }
        });
        controlBox.getChildren().add(playButton);

        HBox buttonSpacer = new HBox();
        buttonSpacer.setMinWidth(7.0);
        controlBox.getChildren().add(buttonSpacer);

        // Create a label that will show the current time in the media
        currentTime = new Label("0:00");
        currentTime.setVisible(isVisible());
        controlBox.getChildren().add(currentTime);

        // Create a label that will show the total duration of the media
        totalTime = new Label(" / 0:00");
        totalTime.setVisible(isVisible());
        controlBox.getChildren().add(totalTime);

        HBox timeSpacer = new HBox();
        timeSpacer.setMinWidth(7.0);
        controlBox.getChildren().add(timeSpacer);

        // Create a slider to control the current time in the media with a listener
        timeSlider = new Slider();
        timeSlider.setVisible(isVisible());
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        timeSlider.valueProperty().addListener(new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                if (timeSlider.isValueChanging() && mediaPlayer != null)
                {
                    mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(timeSlider.getValue() / timeSlider.getMax()));
                    currentTime.setText(formatTime(mediaPlayer.getCurrentTime()));
                }
            }
        });
        controlBox.getChildren().add(timeSlider);

        HBox timelineSpacer = new HBox();
        timelineSpacer.setMinWidth(20.0);
        controlBox.getChildren().add(timelineSpacer);

        // Create a button that toggles the media's audio when clicked and changes its icon based on the volume level
        volumeButton = new Button();
        volumeButton.setVisible(isVisible());
        volumeButton.setGraphic(getVolumeIcon());
        volumeButton.setOnAction(actionEvent ->
        {
            mute = !mute;
            volumeButton.setGraphic(getVolumeIcon());
            if (mediaPlayer != null)
                mediaPlayer.setMute(mute);
        });
        controlBox.getChildren().add(volumeButton);

        // Create a slider to control the media's volume and update the mute button's icon with a listener
        volumeSlider = new Slider();
        volumeSlider.setVisible(isVisible());
        volumeSlider.setPrefWidth(60.0);
        volumeSlider.setValue(volume * volumeSlider.getMax());
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1)
            {
                volume = volumeSlider.getValue() / volumeSlider.getMax();
                volumeButton.setGraphic(getVolumeIcon());
                if (mediaPlayer != null)
                    mediaPlayer.setVolume(volume);
            }
        });
        controlBox.getChildren().add(volumeSlider);

        // Create a vertical layout that will place the horizontal controls layout at the bottom of the parent StackPane
        VBox bottomPos = new VBox();
        bottomPos.setAlignment(Pos.BOTTOM_LEFT);
        bottomPos.getChildren().add(controlBox);

        // Add these controls to the StackPane and create a listener that toggles their visibility based on mouse hover
        getChildren().add(bottomPos);
        hoverProperty().addListener((observableValue, aBoolean, t1) ->
        {
            if (isVisible())
                setControlsVisible(observableValue.getValue());
        });
    }

    /**
     * Loads and begins playing a media file. The volume setting is preserved from the previous media played.
     * @param file the media file to play
     */
    public void load(File file)
    {
        // Create a new MediaPlayer that will play the provided File and assign the previous audio settings to it
        Media media = new Media(file.toPath().toUri().toString());
        if (mediaPlayer != null)
            mediaPlayer.dispose();
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setMute(mute);
        mediaPlayer.setVolume(volume);
        // Create a listener that updates the timeline slider and time label to accurately represent the current time in this media
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>()
        {
            @Override
            public void changed(ObservableValue<? extends Duration> observableValue, Duration duration, Duration t1)
            {
                timeSlider.setValue(timeSlider.getMax() * (mediaPlayer.getCurrentTime().toSeconds() / media.getDuration().toSeconds()));
                currentTime.setText(formatTime(mediaPlayer.getCurrentTime()));
            }
        });
        mediaPlayer.setOnReady(() ->
        {
            totalTime.setText(String.format(" / %s", formatTime(mediaPlayer.getTotalDuration())));
            mediaRatio = (double) media.getWidth() / media.getHeight();
            centerMedia();
        });
        // Set the MediaPlayer to loop
        mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.seek(mediaPlayer.getStartTime()));
        mediaView.setMediaPlayer(mediaPlayer);
    }

    /**
     * Stops playing and frees the current media's memory.
     */
    public void unload()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.dispose();
            mediaPlayer = null;
            mediaRatio = -1;
        }
    }

    public void setVisibility(boolean visible)
    {
        setVisible(visible);
        // If the MediaControlView is being shown and the mouse is already hovering over the pane, also show the controls immediately
        if (visible && getParent().isHover())
            setControlsVisible(true);
        // If the MediaControlView is being hidden, hide the controls regardless of mouse hover and dispose any media currently being played
        else if (!visible)
        {
            setControlsVisible(false);
            if (mediaPlayer != null)
                mediaPlayer.dispose();
        }
    }

    /**
     * Centers the <code>MediaView</code>'s content in its parent <code>StackPane</code>. The <code>MediaPlayer</code> preserves
     * its content's aspect ratio but needs a translation along the axis the media does not fill.
     */
    private void centerMedia()
    {
        if (mediaPlayer != null)
        {
            Media media = mediaPlayer.getMedia();
            if (media != null)
            {
                // If the StackPane currently has a wider aspect ratio than the media, the media will fill the vertical space and leave empty
                // horizontal space, so center the media on the X-axis
                if ((getWidth() / getHeight()) > mediaRatio)
                {
                    double mediaWidth = getHeight() * mediaRatio;
                    mediaView.setTranslateX((getWidth() / 2.0) - (mediaWidth / 2.0));
                    mediaView.setTranslateY(0.0);
                }
                // The opposite is true when the StackPane currently has a narrower aspect ratio, so center the media on the Y-axis
                else
                {
                    mediaView.setTranslateX(0.0);
                    double mediaHeight = getWidth() * (1.0 / mediaRatio);
                    mediaView.setTranslateY((getHeight() / 2.0) - (mediaHeight / 2.0));
                }
            }
        }
    }

    private void setControlsVisible(boolean visible)
    {
        playButton.setVisible(visible);
        timeSlider.setVisible(visible);
        volumeButton.setVisible(visible);
        volumeSlider.setVisible(visible);
        currentTime.setVisible(visible);
        totalTime.setVisible(visible);
    }

    private FontIcon getVolumeIcon()
    {
        if (mute)
            return new FontIcon("bx-volume-mute");
        else if (volume > 0.6)
            return new FontIcon("bx-volume-full");
        else if (volume > 0.1)
            return new FontIcon("bx-volume-low");
        else
            return new FontIcon("bx-volume");
    }

    private String formatTime(Duration duration)
    {
        StringBuilder time = new StringBuilder();

        // Get the total number of seconds in the provided duration
        int seconds = (int) Math.floor(duration.toSeconds());
        if (seconds >= 60)
        {
            // If there are enough seconds to constitute at least one minute, break it down into minutes and seconds
            int minutes = (int) Math.floor((double) seconds / 60.0);
            seconds -= minutes * 60;

            if (minutes < 60)
                time.append(minutes);
            else
            {
                // If there are enough minutes to constitute at least one hour, break it down into hours and minutes
                int hours = (int) Math.floor((double) minutes / 60.0);
                minutes -= hours * 60;
                time.append(String.format("%d:%02d", hours, minutes));
            }
        }
        else
            time.append("0");
        time.append(String.format(":%02d", seconds));
        return time.toString();
    }
}
