package com.altice_crt_b.connect4.classes;

import android.graphics.Point;
import android.util.Log;

/**
 * Created by jaime on 3/7/2018.
 */

public class GameInstance {
    private int[][] board;
    private Point[] lastWinningPattern;
    private Player p1;
    private Player p2;
    private int turn;
    private boolean finished;

    public GameInstance(Player p1, Player p2) {
        this.board = new int[6][7];
        this.lastWinningPattern = new Point[4];
        this.p1 = p1;
        this.p2 = p2;
        this.finished = false;
        //The 1st Player should always start the game unless stated otherwise..
        this.turn = 1;
    }

    public GameInstance(Player p1, Player p2, int turn) {
        this.board = new int[6][7];
        this.lastWinningPattern = new Point[4];
        this.p1 = p1;
        this.p2 = p2;
        this.finished = false;
        this.turn = turn;
    }

    /**
     * Function intended to obtain the board.
     * @return int[][] board -- The board array
     */
    public int[][] getBoard() {
        return this.board;
    }

    /**
     * Function intended to obtain the game's status.
     * @return boolean finished -- Wether the game is finished or not.
     */
    public boolean getGameStatus() {
        return this.finished;
    }

    /**
     * Function intended to return the winner of the game.
     * @return Player winner -- The winner of the game based on whose turn it was.
     */
    public Player getWinner() {
        return this.turn == 1 ? p1 : p2;
    }

    /**
     * Function intended to return the last winning pattern.
     * @return Point[] lastWinningPattern -- The last winning pattern made by the players.
     */
    public Point[] getLastWinningPattern() {
        return this.lastWinningPattern;
    }

    /**
     * Function intended to determine wether or not a certain row / column combination is part of the winning pattern.
     * @param row -- the row that is to be checked.
     * @param column -- the column that is to be checked.
     * @return result -- Wether this row / column combination is part of the winning pattern.
     */
    public boolean isInLastWinningPattern(int row, int column) {
        Point p = new Point(row, column);
        for(int i = 0; i < lastWinningPattern.length; i++) {
            if(p.x == lastWinningPattern[i].x && p.y == lastWinningPattern[i].y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Function intended to obtain the turn of the next player
     * @return int turn -- 1 for player 1, 2 for player 2.
     */
    public int getTurn() {
        return this.turn;
    }

    /**
     * Function intended to toggle the actual turn and return the player whose turn it is.
     * @return Player player -- the player whose turn it is next.
     */
    public Player toggleTurn() {
        this.turn = this.turn == 1? 2 : 1;
        return this.turn == 1? p1 : p2;
    }

    /**
     * Function intended to reset the game and start a new one.
     */
    public void reset() {
        this.board = new int[6][7];
        this.finished = false;
        this.turn = 1;
    }

    /**
     * Function intended to reset the game and start a new one.
     * @param int turn -- The turn of the player who will start the game.
     */
    public void reset(int turn) {
        this.board = new int[6][7];
        this.finished = false;
        this.turn = turn;
    }

    /**
     * Function intended to help determine chip placement on the board.
     * @param column -- the column on which the player decided to place his chip.
     * @return int row -- the row on which the player's chip was placed.
     */
    public int placeChip(int column) {
        int row = -1;
        for(int i = 5; i >= 0; i--) {
            //Determine if the column is empty.
            if(board[i][column] == 0) {
                //If it is, then set the row played as this one, place a chip, and stop the loop.
                row = i;
                board[row][column] = this.getTurn();
                break;
            }
        }
        //If no empty row was found, return -1.
        if(row == -1) {
            return -1;
        }
        this.finished = this.determineWinner(row, column);
        return row;
    }

    /**
     * Function intended to help determine if the game was won by the last chip.
     * @param row -- the row on which the player's chip has been placed.
     * @param column -- the column on which the player's chip has been placed.
     * @return boolean gameWon -- wether the game has been won or not.
     */
    private boolean determineWinner(int row, int column) {
        Log.wtf("Match Log", "Row is " + Integer.toString(row) + " Column is " + Integer.toString(column));
        //Check Horizontally
        for(int i = column - 3; i <= column; i++) {
            if(i < 0 || i + 3 > 6) {
                continue;
            }
            if(board[row][i] == board[row][i+1] && board[row][i+1] == board[row][i+2] && board[row][i+2] == board[row][i+3]) {
                Log.wtf("Match Log", "Horizontal Match - " + Integer.toString(row) + "," + Integer.toString(i));
                lastWinningPattern[0] = new Point(row, i);
                lastWinningPattern[1] = new Point(row, i + 1);
                lastWinningPattern[2] = new Point(row, i + 2);
                lastWinningPattern[3] = new Point(row, i + 3);
                return true;
            }
        }
        //Check Vertically
        for(int i = row + 3; i >= row; i--) {
            if(i - 3 < 0 || i > 5) {
                continue;
            }
            if(board[i][column] == board[i-1][column] && board[i-1][column] == board[i-2][column] && board[i-2][column] == board[i-3][column]) {
                Log.wtf("Match Log", "Vertical Match - " + Integer.toString(i) + "," + Integer.toString(column));
                lastWinningPattern[0] = new Point(i, column);
                lastWinningPattern[1] = new Point(i - 1, column);
                lastWinningPattern[2] = new Point(i - 2, column);
                lastWinningPattern[3] = new Point(i - 3, column);
                return true;
            }
        }
        //Check from Bottom Left to Top Right
        for(int i = row + 3; i >= row; i--) {
            if(i - 3 < 0 || i > 5) {
                continue;
            }
            //J must be determined based on I, so it will only check the diagonal that represents the actual chip.
            int j = column - (i - row);

            if(j < 0 || j + 3 > 6) {
                continue;
            }
            if(board[i][j] == board[i-1][j+1] && board[i-1][j+1] == board[i-2][j+2] && board[i-2][j+2] == board[i-3][j+3]) {
                Log.wtf("Match Log", "Bottom Left - Top Right Match - " + Integer.toString(i) + "," + Integer.toString(j));
                lastWinningPattern[0] = new Point(i, j);
                lastWinningPattern[1] = new Point(i - 1, j + 1);
                lastWinningPattern[2] = new Point(i - 2, j + 2);
                lastWinningPattern[3] = new Point(i - 3, j + 3);
                return true;
            }
        }
        //Check from Top Left to Bottom Right
        for(int i = row + 3; i >= row; i--) {
            if(i - 3 < 0 || i > 5) {
                continue;
            }
            //J must be determined based on I, so it will only check the diagonal that represents the actual chip.
            int j = column + (i - row);

            if(j - 3 < 0 || j > 6) {
                continue;
            }
            if(board[i][j] == board[i-1][j-1] && board[i-1][j-1] == board[i-2][j-2] && board[i-2][j-2] == board[i-3][j-3]) {
                Log.wtf("Match Log", "Top Left - Bottom Right Match - " + Integer.toString(i) + "," + Integer.toString(j));
                lastWinningPattern[0] = new Point(i, j);
                lastWinningPattern[1] = new Point(i - 1, j - 1);
                lastWinningPattern[2] = new Point(i - 2, j - 2);
                lastWinningPattern[3] = new Point(i - 3, j - 3);
                return true;
            }
        }
        return false;
    }
}
