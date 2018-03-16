package com.altice_crt_b.connect4.classes;

/**
 * Created by jaime on 3/7/2018.
 */

public class Player {
    String username;
    int gamesWon;

    public Player(String username) {
        this.username = username;
        this.gamesWon = 0;
    }

    public String getUsername() {
        return this.username;
    }

    public int getGamesWon() {
        return this.gamesWon;
    }

    public void incrementGamesWon(int increment) {
        this.gamesWon += increment;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
