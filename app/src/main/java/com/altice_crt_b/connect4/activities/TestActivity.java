package com.altice_crt_b.connect4.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.altice_crt_b.connect4.R;
import com.altice_crt_b.connect4.classes.*;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        GameInstance instance = new GameInstance(new Player("jrojas"), new Player("lrojas"));
        renderBoard(instance);

    }

    public void renderBoard(final GameInstance instance) {
        GridLayout view = (GridLayout) findViewById(R.id.board);
        view.removeAllViews();
        int[][] board = instance.getBoard();

        for(int i = 0; i < 6; i++) {
            for(int j = 0; j < 7; j++) {
                GridLayout.LayoutParams tvParams = new GridLayout.LayoutParams();
                tvParams.width = 125;

                TextView tv = new TextView(this);
                tv.setText(Integer.toString(board[i][j]));
                tv.setTextSize(18);
                tv.setGravity(Gravity.CENTER);
                tv.setLayoutParams(tvParams);
                view.addView(tv);
                Log.wtf("Board Log", tv.toString());
            }
        }
        Log.wtf("Board Log", "Initializing Buttons...");
        for(int i = 0; i < 7; i++) {
            final int buttonValue = i;
            Button btn = new Button(this);
            GridLayout.LayoutParams btnParams = new GridLayout.LayoutParams();
            btnParams.width = 125;
            btnParams.height = 125;
            btn.setLayoutParams(btnParams);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TestActivity.this);
                    builder.setTitle("Game Finished!");
                    builder.setMessage("The Game has been won by " + instance.getWinner().getUsername() + ". Do you want a Rematch?");
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            instance.reset();
                            renderBoard(instance);
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Do nothing.
                        }
                    });
                    if(!instance.getGameStatus()) {
                        boolean gameWon = instance.placeChip(buttonValue);
                        if(gameWon) {
                            renderBoard(instance);
                            builder.show();
                        }
                        else {
                            renderBoard(instance);
                            Toast.makeText(TestActivity.this, "It's " + instance.toggleTurn().getUsername() + " Turn.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        builder.show();
                    }

                }
            });
            view.addView(btn);
        }
    }

}
