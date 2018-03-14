package com.altice_crt_b.connect4.activities;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.altice_crt_b.connect4.R;
import com.altice_crt_b.connect4.adapters.BoardGridAdapter;
import com.altice_crt_b.connect4.classes.GameInstance;
import com.altice_crt_b.connect4.classes.GameMessage;
import com.altice_crt_b.connect4.classes.Player;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.commons.lang3.SerializationUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MultiplayerGameActivity extends AppCompatActivity {
    String TAG = MultiplayerGameActivity.class.getName();

    private GridView chipsView;
    private AlertDialog gameEndDialog;
    private Player player1;
    private Player player2;
    private GameInstance gameInstance;
    private ArrayList<Integer> usedPositions;
    private int myTurn = -1;

    // Client used to interact with the TurnBasedMultiplayer system.
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient mInvitationsClient = null;
    private String mDisplayName;
    private String mPlayerId;
    private TurnBasedMatch mMatch;
    private LinearLayout waitingForPlayerLy;


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

        gameEndDialog = new AlertDialog.Builder(MultiplayerGameActivity.this )
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
            //if the row was placed then send the play
            if(gameInstance.getTurn() == myTurn){
                if(handlePlay(i)){
                    sendPlay(i);
                }
            }


            }
        });

        waitingForPlayerLy = findViewById(R.id.waiting_for_player_layout);
        showWaitingLayout(true);

        if(isSignedIn()){
            Log.d(TAG, "Connected");
            onConnected(GoogleSignIn.getLastSignedInAccount(this));
        }else{
            Log.d(TAG, "Not Connected");
        }
    }
    public void showWaitingLayout(boolean show){
        waitingForPlayerLy.setVisibility( show ? View.VISIBLE : View.GONE);
    }
    public boolean handlePlay(int clickedItem){
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
                return true;

            }
        }
        else {
            gameEndDialog.show();
        }
        return false;
    }

    public void placeChipAnimated(int position){

        View chipLayout = ( View )chipsView.getItemAtPosition(position);
        ImageView chipView = chipLayout.findViewById(R.id.chip);

        chipView.setImageResource( gameInstance.getTurn() == 1 ? R.drawable.chip_vector_p1 :
                R.drawable.chip_vector_p2 );

        chipView.setVisibility(View.VISIBLE);

        Animation bounceOnEnter = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.bounce_on_enter);
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
        Animation dropOnExit = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.drop_on_exit);
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
//        final TextView p1Name = (TextView) findViewById(R.id.p1_name);
//        TextView p2Name = (TextView) findViewById(R.id.p2_name);
//        ImageView p1Pic = (ImageView) findViewById(R.id.p1_icon);
//        ImageView p2Pic = (ImageView) findViewById(R.id.p2_icon);

        final ImageView p1CenteredPic = (ImageView) findViewById(R.id.p1_center_icon);
        final ImageView p2CenteredPic = (ImageView) findViewById(R.id.p2_center_icon);
        final TextView playerCenteredName = (TextView) findViewById(R.id.player_center_text);
        final Animation overshootOnEnter = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.overshoot_on_enter);
        Animation anticipateOnExit = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.anticipate_on_exit);

//        Animation bounce = AnimationUtils.loadAnimation(GameActivity.this, R.anim.bounce_loop);

        //It's the 1st player's Turn.
        if(turn == 1) {
            Log.wtf(TAG, "It's the 1st Player Turn");
//            ObjectAnimator fadeToWhite = ObjectAnimator.ofObject(p1Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,0,0,0), Color.argb(255,255,255,255));
//            fadeToWhite.setDuration(500);
//            fadeToWhite.start();
//
//            ObjectAnimator fadeToBlack = ObjectAnimator.ofObject(p2Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(255,0,0,0));
//            fadeToBlack.setDuration(500);
//            fadeToBlack.start();
//            p2Pic.clearAnimation();
//            p1Pic.startAnimation(bounce);
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
//            ObjectAnimator fadeToWhite = ObjectAnimator.ofObject(p2Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,0,0,0), Color.argb(255,255,255,255));
//            fadeToWhite.setDuration(500);
//            fadeToWhite.start();
//
//            ObjectAnimator fadeToBlack = ObjectAnimator.ofObject(p1Name, "textColor", new android.animation.ArgbEvaluator(), Color.argb(255,255,255,255), Color.argb(255,0,0,0));
//            fadeToBlack.setDuration(400);
//            fadeToBlack.start();
//            p1Pic.clearAnimation();
//            p2Pic.startAnimation(bounce);
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

    /**
     * Check if there's an account signed in with google services
     * @return boolean
     */
    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    // This is a helper function that will do all the setup to create a simple failure message.
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

    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof TurnBasedMultiplayerClient.MatchOutOfDateApiException) {
            TurnBasedMultiplayerClient.MatchOutOfDateApiException matchOutOfDateApiException =
                    (TurnBasedMultiplayerClient.MatchOutOfDateApiException) exception;

            new android.app.AlertDialog.Builder(this)
                    .setMessage("Match was out of date, updating with latest match data...")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();

            mMatch = matchOutOfDateApiException.getMatch();


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

        new android.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Function used to get the signed player information and start the match process
     * @param googleSignInAccount
     */
    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");

        mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(this, googleSignInAccount);
        mInvitationsClient = Games.getInvitationsClient(this, googleSignInAccount);

        Games.getPlayersClient(this, googleSignInAccount)
                .getCurrentPlayer()
                .addOnSuccessListener(
                        new OnSuccessListener<com.google.android.gms.games.Player>() {
                            @Override
                            public void onSuccess(com.google.android.gms.games.Player player) {
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
                                updateMatch(match);
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
        mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
    }

    /**
     * Starts the turnbased quick match. Once created proceeds to call the match initialization.
     */
    public void quickMatch() {
        Log.d("QUICKMATCH", "quickmatch");
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        //showSpinner();

        // Start the match
        mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        Log.d("QUICKMATCH", "match created correctly");
                        onInitiateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }


    /**
     * Initiates the match's data and assigns each player's position or order.
     * @param match the turnbased match acquired from  mTurnBasedMultiplayerClient.createMatch in 'quickmatch()' method
     */
    private void onInitiateMatch(TurnBasedMatch match) {
        Log.d("QUICKMATCH", "initiating match");

        //This means the player 1 is already set
        if (match.getData() != null) {

            // This game was already created by another player so that means i should toggle to be player 2
            //updateMatch(match);
            setPlayerInfo(2, mDisplayName);
            myTurn = 2;
            handleOtherPlayer(match);
            sendInitialPlayerInfo(2, match);
            return;
        }
        myTurn = 1;
        setPlayerInfo(1, mDisplayName);
        sendInitialPlayerInfo(1, match);
    }

    /**
     * In charge of sending the initial exchange messages between players
     * @param player where it's player 1 or 2 who is issuing the message for the opposite player.
     * @param match
     */
    public void sendInitialPlayerInfo(int player, final TurnBasedMatch match) {
        //
        Log.d("QUICKMATCH", "starting match");

        mMatch = match;

        //showSpinner();
        Log.d("QUICKMATCH", "FIRSTnextParticipantId: "   + getNextParticipantId(mPlayerId,match));
        GameMessage initialMessage = new GameMessage(true, player, mDisplayName);

        mTurnBasedMultiplayerClient.takeTurn(match.getMatchId(),
                 SerializationUtils.serialize(initialMessage), getNextParticipantId(mPlayerId,match))
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        Log.d("QUICKMATCH", "started match");
                        mMatch = turnBasedMatch;
                        //updateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
    }

    /**NOT BEING USED CURRENTLY
     * Method imported from example to see the different statuses of a match.
     * @param match
     */
    public void updateMatch(TurnBasedMatch match) {
        Log.d("QUICKMATCH", "updating match");
        mMatch = match;

        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                Log.d("QUICKMATCH", "updating status: canceled");
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                Log.d("QUICKMATCH", "updating status: expired");
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                Log.d("QUICKMATCH", "updating status: auto-matching");
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                Log.d("QUICKMATCH", "updating status: complete");
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    showWarning("Complete!",
                            "This game is over; someone finished it, and so did you!  " +
                                    "There is nothing to be done.");
                    break;
                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
                showWarning("Complete!",
                        "This game is over; someone finished it!  You can only finish it now.");
        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                Log.d("QUICKMATCH", "updating turn: my turn");
                //gameInstance = GameInstance.unpersist(mMatch.getData());
                //setGameplayUI();
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                Log.d("QUICKMATCH", "updating turn: theirs");
                // Should return results.
                showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good inititative!",
                        "Still waiting for invitations.\n\nBe patient!");
        }

    }

    /**
     * Method used to display alert dialogs.
     * @param title
     * @param message
     */
    public void showWarning(String title, String message) {
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        android.app.AlertDialog mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    //NOT USED YET
    public void onUpdateMatch(TurnBasedMatch match) {
        //dismissSpinner();

        if (match.canRematch()) {
            //askForRematch();
        }

        boolean isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

        if (isDoingTurn) {
            updateMatch(match);

        }

        //setViewVisibility();
    }

    /**
     * Method used to send the play made by a player. Sends the position clicked by the player on the board by the other player.
     * @param clickedPosition
     */
    public void sendPlay(int clickedPosition) {
        //howSpinner();

            String nextParticipantId = getNextParticipantId(mPlayerId, mMatch);
            GameMessage turnMessage = new GameMessage(0, clickedPosition);
            // Create the next turn
            Log.d("QUICKMATCH", "nextParticipantId: " + nextParticipantId);
            mTurnBasedMultiplayerClient.takeTurn(mMatch.getMatchId(),
                   SerializationUtils.serialize(turnMessage), nextParticipantId)
                    .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                        @Override
                        public void onSuccess(TurnBasedMatch turnBasedMatch) {
                            Log.d("QUICKMATCH", "clicked item match");
                            mMatch = turnBasedMatch;
                            //updateMatch(turnBasedMatch);
                            //onUpdateMatch(turnBasedMatch);
                        }
                    })
                    .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));


    }

    /**
     * Method used to get who's the one that will receive a turn
     *
     * @param myPlayerId
     * @param match
     * @return String if there's a participant otherwise returns null
     */
    public String getNextParticipantId(String myPlayerId, TurnBasedMatch match) {
        String myParticipantId = match.getParticipantId(myPlayerId);

        ArrayList<String> participantIds = match.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (match.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    /**
     * Method used to handle the messages interchanged by players.
     * I.e if it's not an init message proceeds to send the played position made by the opposite player calling 'handlePlay()'
     * @param turnBasedMatch
     */
    public void handleOtherPlayer(TurnBasedMatch turnBasedMatch){
        mMatch = turnBasedMatch;
        GameMessage m = SerializationUtils.deserialize(turnBasedMatch.getData());
        Log.d("DESERIALIZING:", m.isInitial() + " " + m.getPlayer() + " " + m.getPositionPlayed());
        if(!m.isInitial()){
            handlePlay(m.getPositionPlayed());
        }else{
            setPlayerInfo(m.getPlayer(), m.getDisplayName());
        }


    }

    /**
     * Method used to set the players basic info
     * @param player
     * @param displayName
     */
    public void setPlayerInfo(int player, String displayName){
        if(player == 1){
            player1.setUsername(displayName);
        }else if (player == 2){
            player2.setUsername(displayName);
        }
    }

    /**
     * Listener that listens for match changes made by other players.
     */
    private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
        @Override
        public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {

            showWaitingLayout(false);
            handleOtherPlayer(turnBasedMatch);
            Toast.makeText(MultiplayerGameActivity.this, "A match was updated ", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onTurnBasedMatchRemoved(@NonNull String matchId) {
            Toast.makeText(MultiplayerGameActivity.this, "A match was removed.", Toast.LENGTH_SHORT).show();
        }
    };

}
