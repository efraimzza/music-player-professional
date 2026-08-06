package com.example.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import android.app.Activity;
import android.content.Intent;

public class QueueManager {
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private static QueueManager instance;
    private List<Track> queue = new ArrayList<>();
    private int currentIndex = -1;
    private int repeatMode = REPEAT_OFF;
    private boolean shuffle = false;
    private Random random = new Random();
    
    public static void destroy(){
        instance = null;
    }

    public static QueueManager getInstance() {
        if (instance == null) instance = new QueueManager();
        return instance;
    }

    public void setQueue(List<Track> tracks, int startIndex,Activity activity) {
        queue.clear();
        queue.addAll(tracks);
        currentIndex = Math.max(0, Math.min(startIndex, queue.size() - 1));
        activity.sendBroadcast(new Intent("UPDATE_QUEUE_UI"));
        
    }

    public String getNext() {
        if (queue.isEmpty()) return null;
        if (repeatMode == REPEAT_ONE) {
            return queue.get(currentIndex).getPath();
        }
        if (shuffle) {
            if (queue.size() == 1) return queue.get(0).getPath();
            int newIdx;
            do {
                newIdx = random.nextInt(queue.size());
            } while (newIdx == currentIndex);
            currentIndex = newIdx;
        } else {
            int nextIdx = currentIndex + 1;
            if (nextIdx >= queue.size()) {
                if (repeatMode == REPEAT_ALL) nextIdx = 0;
                else return null;
            }
            currentIndex = nextIdx;
        }
        return queue.get(currentIndex).getPath();
    }

    public String getPrevious() {
        if (queue.isEmpty()) return null;
        if (shuffle) {
            int newIdx;
            do {
                newIdx = random.nextInt(queue.size());
            } while (newIdx == currentIndex && queue.size() > 1);
            currentIndex = newIdx;
            return queue.get(currentIndex).getPath();
        } else {
            int prevIdx = currentIndex - 1;
            if (prevIdx < 0) {
                if (repeatMode == REPEAT_ALL) prevIdx = queue.size() - 1;
                else return null;
            }
            currentIndex = prevIdx;
            return queue.get(currentIndex).getPath();
        }
    }

    public Track getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < queue.size())
            return queue.get(currentIndex);
        return null;
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
    }

    public boolean isShuffle() { return shuffle; }

    public void setRepeatMode(int mode) { this.repeatMode = mode; }
    public int getRepeatMode() { return repeatMode; }

    public List<Track> getQueue() { return queue; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int index) { this.currentIndex = index; }
}
