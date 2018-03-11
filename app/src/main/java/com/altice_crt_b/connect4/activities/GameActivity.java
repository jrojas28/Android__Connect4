package com.altice_crt_b.connect4.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.GridLayoutAnimationController;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.altice_crt_b.connect4.R;
import com.altice_crt_b.connect4.adapters.BoardGridAdapter;
import com.altice_crt_b.connect4.classes.GameInstance;
import com.altice_crt_b.connect4.classes.Player;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {
    String TAG = GameActivity.class.getName();

    private GridView chipsView;
    private AlertDialog gameEndDialog;
    private GameInstance gameInstance;
    private ArrayList<Integer> usedPositions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        Bundle extras = getIntent().getExtras();
        usedPositions = new ArrayList<>();
        gameInstance = extras != null ?
                //If extras isn't null, do this.
                new GameInstance(new Player(extras.getString("player_1_username")), new Player(extras.getString("player_2_username")), extras.getInt("starting_player")) :
                //If extras is null, do this.
                new GameInstance(new Player(getString(R.string.default_p_1_name)), new Player(getString(R.string.default_p_2_name)));

        gameEndDialog = new AlertDialog.Builder(GameActivity.this )
                .setTitle(R.string.game_finished_title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    gameInstance.reset();
                    cleanBoard();
//                            chipsView.setAdapter(new BoardGridAdapter(GameActivity.this, R.layout.chip_layout));

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

            gameInstance.toggleTurn();

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
        Animation bounceOnExit = AnimationUtils.loadAnimation(GameActivity.this, R.anim.bounce_on_exit);
        bounceOnExit.setAnimationListener(new Animation.AnimationListener() {
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
        chipLayout.startAnimation(bounceOnExit);
    }
}
