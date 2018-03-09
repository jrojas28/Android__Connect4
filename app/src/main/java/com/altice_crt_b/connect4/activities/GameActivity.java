package com.altice_crt_b.connect4.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.altice_crt_b.connect4.R;
import com.altice_crt_b.connect4.adapters.BoardGridAdapter;
import com.altice_crt_b.connect4.classes.GameInstance;
import com.altice_crt_b.connect4.classes.Player;

public class GameActivity extends AppCompatActivity {
    String TAG = GameActivity.class.getName();
    private GridView tilesView;
    private GridView chipsView;
    private AlertDialog gameEndDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        final GameInstance instance = new GameInstance(new Player("jrojas"), new Player("lrojas"));
        gameEndDialog = new AlertDialog.Builder(GameActivity.this )
                .setTitle("Game Finished!")
                .setMessage("The Game has been won by " + instance.getWinner().getUsername() + ". Do you want a Rematch?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                    instance.reset(); }
        })
                .create();



        tilesView = findViewById(R.id.board_grid);

        tilesView.setAdapter(new BoardGridAdapter(this, R.layout.tile_layout));

        chipsView = findViewById(R.id.board_fill);

        chipsView.setAdapter(new BoardGridAdapter(this, R.layout.chip_layout));


        tilesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                View v = ( View )chipsView.getItemAtPosition(i);
                v.findViewById(R.id.chip).setVisibility(View.VISIBLE);
                int column = (int) l % 7;

                if( !instance.getGameStatus() ){
                    gameEndDialog.setMessage("The Game has been won by " + instance.getWinner().getUsername() + ". Do you want a Rematch?");
                }


                Log.d(TAG, "position: " + i + " column: " +  column);

            }
        });


    }
}
