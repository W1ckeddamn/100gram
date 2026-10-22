package com.exteragram.messenger.api.dto;

import java.util.List;
import java.util.Objects;

public final class NowPlayingDTO {
    private final String trackName;
    private final List<String> artists;
    private final String albumName;
    private final String coverUrl;
    private final String previewUrl;
    private final String songUrl;
    private final boolean isPlaying;
    private final String deviceName;
    private final String platform;
    private final Long duration;

    public NowPlayingDTO(String trackName, List<String> artists, String albumName, String coverUrl, String previewUrl, String songUrl, boolean isPlaying, String deviceName, String platform, Long duration) {
        this.trackName = trackName;
        this.artists = artists;
        this.albumName = albumName;
        this.coverUrl = coverUrl;
        this.previewUrl = previewUrl;
        this.songUrl = songUrl;
        this.isPlaying = isPlaying;
        this.deviceName = deviceName;
        this.platform = platform;
        this.duration = duration;
    }

    public String getTrackName() {
        return trackName;
    }

    public List<String> getArtists() {
        return artists;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean getIsPlaying() {
        return isPlaying;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public Long getDuration() {
        return duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NowPlayingDTO that = (NowPlayingDTO) o;
        return isPlaying == that.isPlaying &&
                Objects.equals(trackName, that.trackName) &&
                Objects.equals(artists, that.artists) &&
                Objects.equals(albumName, that.albumName) &&
                Objects.equals(coverUrl, that.coverUrl) &&
                Objects.equals(previewUrl, that.previewUrl) &&
                Objects.equals(songUrl, that.songUrl) &&
                Objects.equals(deviceName, that.deviceName) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackName, artists, albumName, coverUrl, previewUrl, songUrl, isPlaying, deviceName, platform, duration);
    }

    @Override
    public String toString() {
        return "NowPlayingDTO{" +
                "trackName='" + trackName + '\'' +
                ", artists=" + artists +
                ", albumName='" + albumName + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", previewUrl='" + previewUrl + '\'' +
                ", songUrl='" + songUrl + '\'' +
                ", isPlaying=" + isPlaying +
                ", deviceName='" + deviceName + '\'' +
                ", platform='" + platform + '\'' +
                ", duration=" + duration +
                '}';
    }
}