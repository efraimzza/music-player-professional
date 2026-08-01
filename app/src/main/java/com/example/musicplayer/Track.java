
package com.example.musicplayer;

public class Track {
    private long id;
    private String path;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private long duration;
    private String albumArtPath;
    private int cdTrackMumber;

    public Track() {}

    public Track(String path, String title, String artist, String album, String genre, int cdTrackMumber) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.cdTrackMumber=cdTrackMumber;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getAlbumArtPath() { return albumArtPath; }
    public void setAlbumArtPath(String albumArtPath) { this.albumArtPath = albumArtPath; }
    public int getCdTrackMumber() { return cdTrackMumber; }
    public void setCdTrackMumber(int cdTrackMumber) { this.cdTrackMumber = cdTrackMumber; }
}
