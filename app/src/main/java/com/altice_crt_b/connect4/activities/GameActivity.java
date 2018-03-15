package com.altice_crt_b.connect4.activities;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.altice_crt_b.connect4.R;
import com.altice_crt_b.connect4.adapters.BoardGridAdapter;
import com.altice_crt_b.connect4.classes.GameInstance;
import com.altice_crt_b.connect4.classes.Player;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {
    String TAG = GameActivity.class.getName();

    private GridView chipsView;
    private AlertDialog gameEndDialog;
    private Player player1;
    private Player player2;
    private GameInstance gameInstance;
    private ArrayList<Integer> usedPositions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        Bundle extras = getIntent().getExtras();
        usedPositions = new ArrayList<>();
        //Initialize the players & the instance.
        if(extras != null) {
            player1 = new Player(extras.getString("player_1_username"));
            player2 = new Player(extras.getString("player_2_username"));
            gameInstance = new GameInstance(player1, player2, extras.getInt("starting_player"));
            TextView playerName = (TextView) findViewById(R.id.player_center_text);
            if(gameInstance.getTurn() == 1) {
                playerName.setText(player1.getUsername());
            }
            else {
                playerName.setText(player2.getUsername());
                ((ImageView) findViewById(R.id.p1_center_icon)).setVisibility(View.INVISIBLE);
                ((ImageView) findViewById(R.id.p2_center_icon)).setVisibility(View.VISIBLE);
            }

        }
        else {
            player1 = new Player(getString(R.string.default_p_1_name));
            player2 = new Player(getString(R.string.default_p_2_name));
            gameInstance = new GameInstance(player1, player2);
        }
        //Set the names on the players to display on their respective textViews.
        gameEndDialog = new AlertDialog.Builder(GameActivity.this )
                .setTitle(R.string.game_finished_title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    ImageView p1Icon = (ImageView) GameActivity.this.findViewById(R.id.p1_center_icon);
                    ImageView p2Icon = (ImageView) GameActivity.this.findViewById(R.id.p2_center_icon);
                    TextView tv = (TextView) GameActivity.this.findViewById(R.id.player_center_text);
                    //The loser of the match is the person who will start the next match.
                    int firstPlayer = gameInstance.getTurn() == 1 ? 2 : 1;
                    //If the first player is P1, then set the name to P1 and show his icon.
                    if(firstPlayer == 1) {
                        p1Icon.setVisibility(View.VISIBLE);
                        p2Icon.setVisibility(View.INVISIBLE);
                        tv.setText(player1.getUsername());
                    }
                    //Otherwise, set the name to P2 and show his icon.
                    else {
                        p1Icon.setVisibility(View.INVISIBLE);
                        p2Icon.setVisibility(View.VISIBLE);
                        tv.setText(player2.getUsername());
                    }
                    gameInstance.reset(firstPlayer);
                    cleanBoard();
                }
                })
                .setNegativeButton("Back to Main Menu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .create();

        GridView tilesView = findViewById(R.id.board_grid);

        tilesView.setAdapter(new BoardGridAdapter(this, R.layout.tile_layout));

        chipsView = findViewById(R.id.board_fill);

        chipsView.setAdapter(new BoardGridAdapter(this, R.layout.chip_layout));

        tilesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               handlePlay(i);
            }
        });
    }

    public void handlePlay(int clickedItem){
        //If the game isn't finished, handle the last play.
        if( !gameInstance.getGameStatus() ) {
            int column = clickedItem % 7;
            int row = gameInstance.placeChip(column);

            if(row != -1){
                int position = row * 7  + column ;
                placeChipAnimated(position);
                usedPositions.add(position);

                if( gameInstance.getGameStatus() ){
                    displayWinningChips();
                    gameEndDialog.setMessage( String.format( getString(R.string.game_end_content), gameInstance.getWinner().getUsername() ));
                    gameEndDialog.show();
                }
                else {
                    toggleTurn();
                }
            }
        }
        else {
            gameEndDialog.show();
        }
    }

    public void displayWinningChips(){

        boolean firstWon = gameInstance.getWinner().equals(player1);
        for(Point p : gameInstance.getLastWinningPattern() ){
            int position = p.x * 7  + p.y;
            View chipLayout = ( View )chipsView.getItemAtPosition(position);
            ImageView chipView = chipLayout.findViewById(R.id.chip);

            chipView.setImageResource( firstWon ? R.drawable.chip_vector_p1_w :
                    R.drawable.chip_vector_p2_w );

        }
    }

    public void placeChipAnimated(int position){

        View chipLayout = ( View )chipsView.getItemAtPosition(position);
        ImageView chipView = chipLayout.findViewById(R.id.chip);

        chipView.setImageResource( gameInstance.getTurn() == 1 ? R.drawable.chip_vector_p1 :
                R.drawable.chip_vector_p2 );

        chipView.setVisibility(View.VISIBLE);

        Animation bounceOnEnter = AnimationUtils.loadAnimation(GameActivity.this, R.anim.bounce_on_enter);
        chipLayout.startAnimation(bounceOnEnter);
    }

    public void cleanBoard(){
        for ( int position:
              usedPositions) {
            removeChipAnimated(position);

        }
        usedPositions.clear();
    }

    public void removeChipAnimated(int position){
        View chipLayout = ( View )chipsView.getItemAtPosition(position);
        final ImageView chipView = chipLayout.findViewById(R.id.chip);
        Animation dropOnExit = AnimationUtils.loadAnimation(GameActivity.this, R.anim.drop_on_exit);
        dropOnExit.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                chipView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        chipLayout.startAnimation(dropOnExit);
    }

    public void toggleTurn() {
        //Initialize Variables
        final Player activePlayer = gameInstance.toggleTurn();
        int turn = gameInstance.getTurn();

        //Find the views related to each player.
        final ImageView p1CenteredPic = (ImageView) findViewById(R.id.p1_center_icon);
        final ImageView p2CenteredPic = (ImageView) findViewById(R.id.p2_center_icon);
        final TextView playerCenteredName = (TextView) findViewById(R.id.player_center_text);
        final Animation overshootOnEnter = AnimationUtils.loadAnimation(GameActivity.this, R.anim.overshoot_on_enter);
        Animation anticipateOnExit = AnimationUtils.loadAnimation(GameActivity.this, R.anim.anticipate_on_exit);

        //It's the 1st player's Turn.
        if(turn == 1) {
            Log.wtf(TAG, "It's the 1st Player Turn");
            anticipateOnExit.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    ObjectAnimator fadeOut = ObjectAnimator.ofObject(playerCenteredName, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(0,255,255,255));
                    fadeOut.setDuration(550);
                    fadeOut.start();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //On End, we'll enter the other animation.
                    p2CenteredPic.setVisibility(View.INVISIBLE);
                    p1CenteredPic.setVisibility(View.VISIBLE);
                    p1CenteredPic.startAnimation(overshootOnEnter);
                    playerCenteredName.setText(activePlayer.getUsername());
                    ObjectAnimator fadeIn = ObjectAnimator.ofObject(playerCenteredName, "textColor", new android.animation.ArgbEvaluator(), Color.argb(0,255,255,255), Color.argb(255,255,255,255));
                    fadeIn.setDuration(550);
                    fadeIn.start();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    //Nothing on Repeat
                }
            });
            p2CenteredPic.startAnimation(anticipateOnExit);
        }
        //It's the 2nd player's Turn.
        else {
            Log.wtf(TAG, "It's the 2nd Player Turn");
            anticipateOnExit.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    ObjectAnimator fadeOut = ObjectAnimator.ofObject(playerCenteredName, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(0,255,255,255));
                    fadeOut.setDuration(550);
                    fadeOut.start();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //On End, we'll enter the other animation.
                    p1CenteredPic.setVisibility(View.INVISIBLE);
                    p2CenteredPic.setVisibility(View.VISIBLE);
                    p2CenteredPic.startAnimation(overshootOnEnter);
                    playerCenteredName.setText(activePlayer.getUsername());
                    ObjectAnimator fadeIn = ObjectAnimator.ofObject(playerCenteredName, "textColor", new android.animation.ArgbEvaluator(), Color.argb(0,255,255,255), Color.argb(255,255,255,255));
                    fadeIn.setDuration(550);
                    fadeIn.start();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    //Nothing on Repeat
                }
            });
            p1CenteredPic.startAnimation(anticipateOnExit);
        }
    }
}
