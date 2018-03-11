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

public class GameActivity extends AppCompatActivity {
    String TAG = GameActivity.class.getName();
    private GridView tilesView;
    private GridView chipsView;
    private AlertDialog gameEndDialog;
    Animation bounceOnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        Bundle extras = getIntent().getExtras();
        final GameInstance instance = extras != null ?
                //If extras isn't null, do this.
                new GameInstance(new Player(extras.getString("player_1_username")), new Player(extras.getString("player_2_username")), extras.getInt("starting_player")) :
                //If extras is null, do this.
                new GameInstance(new Player("Player 1"), new Player("Player 2"));
        gameEndDialog = new AlertDialog.Builder(GameActivity.this )
                .setTitle("Game Finished!")
                .setMessage("The Game has been won by " + instance.getWinner().getUsername() + ". Do you want a Rematch?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                            instance.reset();
                            chipsView.setAdapter(new BoardGridAdapter(GameActivity.this, R.layout.chip_layout));
                }
                })
                .create();
        bounceOnEnter = AnimationUtils.loadAnimation(GameActivity.this, R.anim.bounce_on_enter);



        tilesView = findViewById(R.id.board_grid);

        tilesView.setAdapter(new BoardGridAdapter(this, R.layout.tile_layout));

        chipsView = findViewById(R.id.board_fill);

        chipsView.setAdapter(new BoardGridAdapter(this, R.layout.chip_layout));

        tilesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "item clicked: " + i);

                int column = (int) i % 7;
                Log.d(TAG, "column: " + column);

                int row = instance.placeChip(column);

                Log.d(TAG, "row: " + row);

                if(row != -1){
                    int position = row * 7  + column ;
                    Log.d(TAG, "position: " + position);
                    View chipLayout = ( View )chipsView.getItemAtPosition(position);
                    ImageView chipView = chipLayout.findViewById(R.id.chip);

                    chipView.setImageResource( instance.getTurn() == 1 ? R.drawable.chip_vector_p1 :
                            R.drawable.chip_vector_p2 );

                    chipView.setVisibility(View.VISIBLE);
                    chipLayout.startAnimation(bounceOnEnter);

                    if( instance.getGameStatus() ){
                        gameEndDialog.setMessage("The Game has been won by " + instance.getWinner().getUsername() + ". Do you want a Rematch?");
                        gameEndDialog.show();
                    }

                    instance.toggleTurn();

                }



            }
        });


    }
}
