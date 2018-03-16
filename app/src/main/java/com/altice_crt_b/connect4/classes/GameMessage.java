package com.altice_crt_b.connect4.classes;

import java.io.Serializable;

/**
 * Created by moise on 3/14/2018.
 */

public class GameMessage implements Serializable{

    private boolean isInitial = false;
    private int player = 0;
    private int positionPlayed = -1;
    private String displayName = "";
    public GameMessage() {
    }


    public GameMessage(boolean isInitial, int player) {
        this.isInitial = isInitial;
        this.player = player;
    }

    public GameMessage(boolean isInitial, int player, String displayName) {
        this.isInitial = isInitial;
        this.player = player;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public GameMessage(int player, int positionPlayed) {
        this.player = player;
        this.positionPlayed = positionPlayed;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public void setInitial(boolean initial) {
        isInitial = initial;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public int getPositionPlayed() {
        return positionPlayed;
    }

    public void setPositionPlayed(int positionPlayed) {
        this.positionPlayed = positionPlayed;
    }
}
