package com.altice_crt_b.connect4.classes;

/**
 * Created by jaime on 3/7/2018.
 */

public class GameInstance {
    private int[][] board;
    private Player p1;
    private Player p2;
    private int turn;
    private boolean finished;

    public GameInstance(Player p1, Player p2) {
        this.board = new int[6][7];
        this.p1 = p1;
        this.p2 = p2;
        this.finished = false;
        //The 1st Player should always start the game.
        this.turn = 1;
    }

    public int[][] getBoard() {
        return this.board;
    }

    public boolean getGameStatus() {
        return this.finished;
    }

    public Player getWinner() {
        return this.turn == 1 ? p1 : p2;
    }

    public int getTurn() {
        return this.turn;
    }

    public Player toggleTurn() {
        this.turn = this.turn == 1? 2 : 1;
        return this.turn == 1? p1 : p2;
    }

    public void reset() {
        this.board = new int[6][7];
        this.finished = false;
        this.turn = 1;
    }

    public boolean placeChip(int column) {
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
        this.finished = this.determineWinner(row, column);
        return this.finished;
    }

    private boolean determineWinner(int row, int column) {
        return (
                //Check Left
                ((column > 2) && (board[row][column-3] == board[row][column-2] && board[row][column-2] == board[row][column-1] && board[row][column-1] == board[row][column])) ||
                //Check Right
                ((column < 4) && (board[row][column+3] == board[row][column+2] && board[row][column+2] == board[row][column+1] && board[row][column+1] == board[row][column])) ||
                //Check Down
                ((row < 3) && (board[row+3][column] == board[row+2][column] && board[row+2][column] == board[row+1][column] && board[row+1][column] == board[row][column])) ||
                //Check Up & Left
                ((row > 2 && column > 2) && (board[row-3][column-3] == board[row-2][column-2] && board[row-2][column-2] == board[row-1][column-1] && board[row-1][column-1] == board[row][column])) ||
                //Check Up & Right
                ((row > 2 && column < 4) && (board[row-3][column+3] == board[row-2][column+2] && board[row-2][column+2] == board[row-1][column+1] && board[row-1][column+1] == board[row][column])) ||
                //Check Down & Left
                ((row < 3 && column > 2) && (board[row+3][column-3] == board[row+2][column-2] && board[row+2][column-2] == board[row+1][column-1] && board[row+1][column-1] == board[row][column])) ||
                //Check Down & Right
                ((row < 3 && column < 4) && (board[row+3][column+3] == board[row+2][column+2] && board[row+2][column+2] == board[row+1][column+1] && board[row+1][column+1] == board[row][column]))
                );
    }
}
