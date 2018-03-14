package com.altice_crt_b.connect4.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableRow;

import com.altice_crt_b.connect4.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MenuActivity extends AppCompatActivity {

    private String TAG = MenuActivity.class.getName();

    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;
    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient = null;
    // Client used to interact with the TurnBasedMultiplayer system.
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient mInvitationsClient = null;
    private String mDisplayName;
    private String mPlayerId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        // Create the Google API Client with access to Games
        // Create the client used to sign in.
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

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

        onlineMultBUtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSignedIn()){
                    Intent intent = new Intent(MenuActivity.this, MultiplayerGameActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("player_1_username", "Player 1");
                    bundle.putString("player_2_username", "Player 2");
                    bundle.putInt("starting_player", 1);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }else{
                    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
                }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                Log.d("SigningIn", "" + apiException.getStatusCode());
                if (message == null || message.isEmpty()) {
                    message = "Error signing in";
                }

                 onDisconnected();

                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }else if( requestCode == RC_SELECT_PLAYERS){

        }
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    /**
     * Try to sign in without displaying dialogs to the user.
     * <p>
     * If the user has already signed in previously, it will not show dialog.
     */
    public void signInSilently() {
        Log.d(TAG, "signInSilently()");

        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Log.d(TAG, "signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");

        mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(this, googleSignInAccount);
        mInvitationsClient = Games.getInvitationsClient(this, googleSignInAccount);

        Games.getPlayersClient(this, googleSignInAccount)
                .getCurrentPlayer()
                .addOnSuccessListener(
                        new OnSuccessListener<Player>() {
                            @Override
                            public void onSuccess(Player player) {
                                mDisplayName = player.getDisplayName();
                                mPlayerId = player.getPlayerId();
                                quickMatch();
                               // selectOpponent();

                                //setViewVisibility();
                            }
                        }
                )
                .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

        Log.d(TAG, "onConnected(): Connection successful");

        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(this, googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle hint) {
                        if (hint != null) {
                            TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

                            if (match != null) {
                                //updateMatch(match);
                            }
                        }
                    }
                })
                .addOnFailureListener(createFailureListener(
                        "There was a problem getting the activation hint!"));



        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
//        // invitation events, you will get standard notifications instead.
//        // Standard notifications may be preferable behavior in many cases.
//        mInvitationsClient.registerInvitationCallback(mInvitationCallback);
//
//        // Likewise, we are registering the optional MatchUpdateListener, which
//        // will replace notifications you would get otherwise. You do *NOT* have
//        // to register a MatchUpdateListener.
//        mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
    }

    // This is a helper functio that will do all the setup to create a simple failure message.
    // Add it to any task and in the case of an failure, it will report the string in an alert
    // dialog.
    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, string);
            }
        };
    }

    private void onDisconnected() {

        Log.d(TAG, "onDisconnected()");

        mTurnBasedMultiplayerClient = null;
        mInvitationsClient = null;


    }

    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof TurnBasedMultiplayerClient.MatchOutOfDateApiException) {
            TurnBasedMultiplayerClient.MatchOutOfDateApiException matchOutOfDateApiException =
                    (TurnBasedMultiplayerClient.MatchOutOfDateApiException) exception;

            new AlertDialog.Builder(this)
                    .setMessage("Match was out of date, updating with latest match data...")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();

            TurnBasedMatch match = matchOutOfDateApiException.getMatch();
            //updateMatch(match);

            return;
        }

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

//        if (!checkStatusCode(status)) {
//            return;
//        }

        String message = getString(R.string.status_exception_error, details, status, exception);

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    // Open the create-game UI. You will get back an onActivityResult
    // and figure out what to do.
    public void selectOpponent() {
        mTurnBasedMultiplayerClient.getSelectOpponentsIntent(1, 7, true)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                })
                .addOnFailureListener(createFailureListener(
                        getString(R.string.error_get_select_opponents)));
    }
    // Create a one-on-one automatch game.
    public void quickMatch() {

        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        //showSpinner();

        // Start the match
        mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        //onInitiateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }



}
