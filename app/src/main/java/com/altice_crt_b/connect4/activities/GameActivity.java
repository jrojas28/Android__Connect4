package com.altice_crt_b.connect4.activities;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
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
        }
        else {
            player1 = new Player(getString(R.string.default_p_1_name));
            player2 = new Player(getString(R.string.default_p_2_name));
            gameInstance = new GameInstance(player1, player2);
        }
        //Set the names on the players to display on their respective textViews.
        TextView p1Name = (TextView) findViewById(R.id.p1_name);
        TextView p2Name = (TextView) findViewById(R.id.p2_name);

        p1Name.setText(player1.getUsername());
        p2Name.setText(player2.getUsername());
        gameEndDialog = new AlertDialog.Builder(GameActivity.this )
                .setTitle(R.string.game_finished_title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    gameInstance.reset();
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
                    gameEndDialog.setMessage( String.format( getString(R.string.game_end_content), gameInstance.getWinner().getUsername() ));
                    gameEndDialog.show();
                }

                toggleTurn();

            }
        }
        else {
            gameEndDialog.show();
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
        Player activePlayer = gameInstance.toggleTurn();
        int turn = gameInstance.getTurn();

        //Find the views related to each player.
        TextView p1Name = (TextView) findViewById(R.id.p1_name);
        TextView p2Name = (TextView) findViewById(R.id.p2_name);
        ImageView p1Pic = (ImageView) findViewById(R.id.p1_icon);
        ImageView p2Pic = (ImageView) findViewById(R.id.p2_icon);
        Animation bounce = AnimationUtils.loadAnimation(GameActivity.this, R.anim.bounce_loop);

        //It's the 1st player's Turn.
        if(turn == 1) {
            ObjectAnimator fadeToWhite = ObjectAnimator.ofObject(p1Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,0,0,0), Color.argb(255,255,255,255));
            fadeToWhite.setDuration(500);
            fadeToWhite.start();

            ObjectAnimator fadeToBlack = ObjectAnimator.ofObject(p2Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(255,0,0,0));
            fadeToBlack.setDuration(500);
            fadeToBlack.start();
            p2Pic.clearAnimation();
            p1Pic.startAnimation(bounce);
        }
        //It's the 2nd player's Turn.
        else {
            ObjectAnimator fadeToWhite = ObjectAnimator.ofObject(p2Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,0,0,0), Color.argb(255,255,255,255));
            fadeToWhite.setDuration(500);
            fadeToWhite.start();

            ObjectAnimator fadeToBlack = ObjectAnimator.ofObject(p1Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(255,0,0,0));
            fadeToBlack.setDuration(400);
            fadeToBlack.start();
            p1Pic.clearAnimation();
            p2Pic.startAnimation(bounce);
        }
    }
}
