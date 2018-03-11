package com.altice_crt_b.connect4.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableRow;

import com.altice_crt_b.connect4.R;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        final Button localMultButton = (Button) findViewById(R.id.local_multiplayer_btn);
        Button onlineMultBUtton = (Button) findViewById(R.id.online_multiplayer_btn);

        localMultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Generate a popup so players can be nicknamed.
                LayoutInflater inflater = MenuActivity.this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.naming_dialog, null);
                AlertDialog.Builder localMultForm = new AlertDialog.Builder(MenuActivity.this);
                localMultForm.setView(dialogView)
                        .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.wtf("Dialog Interface", "Challange accepted..");
                                //The user has accepted, so now lets get some data.
                                EditText p1NameField = (EditText) dialogView.findViewById(R.id.player_1_username);
                                EditText p2NameField = (EditText) dialogView.findViewById(R.id.player_2_username);
                                RadioButton p1Starts = (RadioButton) dialogView.findViewById(R.id.player_1_starts);
                                Intent intent = new Intent(MenuActivity.this, GameActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("player_1_username", p1NameField.getText().length() != 0 ? p1NameField.getText().toString() : "Player 1");
                                bundle.putString("player_2_username", p2NameField.getText().length() != 0 ? p2NameField.getText().toString() : "Player 2");
                                bundle.putInt("starting_player", p1Starts.isChecked() ? 1 : 2);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("CANCEL", null);
                localMultForm.show();
            }
        });

    }

    public void namingDialog__radioButtonClicked(View view) {
        //Since we can't group Radio Buttons when using Table Layout, we'll handle selection here.
        RadioButton currentButton = (RadioButton) view;
        //In order to find the alternate button, we'll need the parent. In this case, we know it's a Table Row.
        TableRow tr = (TableRow) view.getParent();
        //Find the alternate button, meaning, P2 button for P1 and P1 button for P2.
        RadioButton alternateButton = currentButton.getId() == R.id.player_1_starts ? (RadioButton) tr.findViewById(R.id.player_2_starts) : (RadioButton) tr.findViewById(R.id.player_1_starts);
        //Deselect the alternate button and select this one.
        alternateButton.setChecked(false);
        currentButton.setChecked(true);
    }

}
