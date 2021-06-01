/*
 * Copyright 2021 devgianlu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.gianlu.librespot.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.metadata.PlayableId;

import java.io.IOException;

/**
 * @author devgianlu
 */
public final class ShellEvents implements Player.EventsListener, Session.ReconnectionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellEvents.class);
    private final Configuration conf;
    private final Runtime runtime;

    public ShellEvents(@NotNull Configuration conf) {
        this.runtime = Runtime.getRuntime();
        this.conf = conf;
    }

    private void exec(String command, String... envp) {
        if (!this.conf.enabled)
            return;

        if (command == null || command.trim().isEmpty())
            return;

        try {
            Process p;
            if (conf.executeWithBash) p = runtime.exec(new String[]{"/bin/bash", "-c", command.trim()}, envp);
            else p = runtime.exec(command.trim(), envp);
            int exitCode = p.waitFor();
            LOGGER.trace("Executed shell command: {} -> {}", command, exitCode);
        } catch (IOException | InterruptedException ex) {
            LOGGER.error("Failed executing command: {}", command, ex);
        }
    }

    @Override
    public void onContextChanged(@NotNull Player player, @NotNull String newUri) {
        exec(conf.onContextChanged, "CONTEXT_URI=" + newUri);
    }

    @Override
    public void onTrackChanged(@NotNull Player player, @NotNull PlayableId id, @Nullable MetadataWrapper metadata, boolean userInitiated) {
        exec(conf.onTrackChanged, "TRACK_URI=" + id.toSpotifyUri(),
                "NAME=" + (metadata == null ? "" : metadata.getName()),
                "ARTIST=" + (metadata == null ? "" : metadata.getArtist()),
                "ALBUM=" + (metadata == null ? "" : metadata.getAlbumName()),
                "DURATION=" + (metadata == null ? "" : metadata.duration()),
                "IS_USER=" + userInitiated);
    }

    @Override
    public void onPlaybackEnded(@NotNull Player player) {
        exec(conf.onPlaybackEnded);
    }

    @Override
    public void onPlaybackPaused(@NotNull Player player, long trackTime) {
        exec(conf.onPlaybackPaused, "POSITION=" + trackTime);
    }

    @Override
    public void onPlaybackResumed(@NotNull Player player, long trackTime) {
        exec(conf.onPlaybackResumed, "POSITION=" + trackTime);
    }

    @Override
    public void onTrackSeeked(@NotNull Player player, long trackTime) {
        exec(conf.onTrackSeeked, "POSITION=" + trackTime);
    }

    @Override
    public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadata) {
        exec(conf.onMetadataAvailable, "TRACK_URI=" + metadata.id.toSpotifyUri(),
                "NAME=" + metadata.getName(), "ARTIST=" + metadata.getArtist(),
                "ALBUM=" + metadata.getAlbumName(), "DURATION=" + metadata.duration());
    }

    @Override
    public void onPlaybackHaltStateChanged(@NotNull Player player, boolean halted, long trackTime) {
    }

    @Override
    public void onInactiveSession(@NotNull Player player, boolean timeout) {
        exec(conf.onInactiveSession);
    }

    @Override
    public void onVolumeChanged(@NotNull Player player, @Range(from = 0, to = 1) float volume) {
        exec(conf.onVolumeChanged, "VOLUME=" + Math.round(volume * 100f));
    }

    @Override
    public void onPanicState(@NotNull Player player) {
        exec(conf.onPanicState);
    }

    @Override
    public void onConnectionDropped() {
        exec(conf.onConnectionDropped);
    }

    @Override
    public void onConnectionEstablished() {
        exec(conf.onConnectionEstablished);
    }

    public static class Configuration {
        public final boolean enabled;
        public final boolean executeWithBash;
        public final String onContextChanged;
        public final String onTrackChanged;
        public final String onPlaybackEnded;
        public final String onPlaybackPaused;
        public final String onPlaybackResumed;
        public final String onTrackSeeked;
        public final String onMetadataAvailable;
        public final String onVolumeChanged;
        public final String onInactiveSession;
        public final String onPanicState;
        public final String onConnectionDropped;
        public final String onConnectionEstablished;

        public Configuration(boolean enabled, boolean executeWithBash, String onContextChanged, String onTrackChanged, String onPlaybackEnded, String onPlaybackPaused,
                             String onPlaybackResumed, String onTrackSeeked, String onMetadataAvailable, String onVolumeChanged,
                             String onInactiveSession, String onPanicState, String onConnectionDropped, String onConnectionEstablished) {
            this.enabled = enabled;
            this.executeWithBash = executeWithBash;
            this.onContextChanged = onContextChanged;
            this.onTrackChanged = onTrackChanged;
            this.onPlaybackEnded = onPlaybackEnded;
            this.onPlaybackPaused = onPlaybackPaused;
            this.onPlaybackResumed = onPlaybackResumed;
            this.onTrackSeeked = onTrackSeeked;
            this.onMetadataAvailable = onMetadataAvailable;
            this.onVolumeChanged = onVolumeChanged;
            this.onInactiveSession = onInactiveSession;
            this.onPanicState = onPanicState;
            this.onConnectionDropped = onConnectionDropped;
            this.onConnectionEstablished = onConnectionEstablished;
        }

        public static class Builder {
            private boolean enabled = false;
            private boolean executeWithBash = false;
            private String onContextChanged = "";
            private String onTrackChanged = "";
            private String onPlaybackEnded = "";
            private String onPlaybackPaused = "";
            private String onPlaybackResumed = "";
            private String onTrackSeeked = "";
            private String onMetadataAvailable = "";
            private String onVolumeChanged = "";
            private String onInactiveSession = "";
            private String onPanicState = "";
            private String onConnectionDropped = "";
            private String onConnectionEstablished = "";

            public Builder() {
            }

            public Builder setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder setExecuteWithBash(boolean executeWithBash) {
                this.executeWithBash = executeWithBash;
                return this;
            }

            public Builder setOnContextChanged(String command) {
                this.onContextChanged = command;
                return this;
            }

            public Builder setOnTrackChanged(String command) {
                this.onTrackChanged = command;
                return this;
            }

            public Builder setOnPlaybackEnded(String command) {
                this.onPlaybackEnded = command;
                return this;
            }

            public Builder setOnPlaybackPaused(String command) {
                this.onPlaybackPaused = command;
                return this;
            }

            public Builder setOnPlaybackResumed(String command) {
                this.onPlaybackResumed = command;
                return this;
            }

            public Builder setOnTrackSeeked(String command) {
                this.onTrackSeeked = command;
                return this;
            }

            public Builder setOnMetadataAvailable(String command) {
                this.onMetadataAvailable = command;
                return this;
            }

            public Builder setOnVolumeChanged(String command) {
                this.onVolumeChanged = command;
                return this;
            }

            public Builder setOnInactiveSession(String command) {
                this.onInactiveSession = command;
                return this;
            }

            public Builder setOnPanicState(String command) {
                this.onPanicState = command;
                return this;
            }

            public Builder setOnConnectionDropped(String command) {
                this.onConnectionDropped = command;
                return this;
            }

            public Builder setOnConnectionEstablished(String command) {
                this.onConnectionEstablished = command;
                return this;
            }

            @NotNull
            public Configuration build() {
                return new Configuration(enabled, executeWithBash, onContextChanged, onTrackChanged, onPlaybackEnded, onPlaybackPaused, onPlaybackResumed,
                        onTrackSeeked, onMetadataAvailable, onVolumeChanged, onInactiveSession, onPanicState, onConnectionDropped, onConnectionEstablished);
            }
        }
    }
}