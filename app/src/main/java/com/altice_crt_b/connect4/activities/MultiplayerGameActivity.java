package com.altice_crt_b.connect4.activities;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.altice_crt_b.connect4.MainActivty;
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
import com.google.android.gms.tasks.Task;

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
    private View waitingForPlayerLy;
    private TextView playerCenteredName;
    private Button cancelButton;
    private CountDownTimer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        Bundle extras = getIntent().getExtras();
        usedPositions = new ArrayList<>();
        cancelButton = findViewById(R.id.cancel_btn);
        playerCenteredName = findViewById(R.id.player_center_text);
        //Initialize the players & the instance.
        if(extras != null) {
            player1 = new Player(extras.getString("player_1_username"));
            player2 = new Player(extras.getString("player_2_username"));
            gameInstance = new GameInstance(player1, player2, extras.getInt("starting_player"));

            if(gameInstance.getTurn() == 1) {
                playerCenteredName.setText(player1.getUsername());
            }
            else {
                playerCenteredName.setText(player2.getUsername());
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
                        //We reload the activity instead of going back, can be accomplished in a more polished way
                        cleanClients();
                        Intent refresh = new Intent(MultiplayerGameActivity.this, MultiplayerGameActivity.class);
                        startActivity(refresh);
                        finish(); //
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
        loopWaitingAnimation();
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelMatch(mMatch.getMatchId());

                }
            });

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
                    displayWinningChips();
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
        final ImageView p1CenteredPic = (ImageView) findViewById(R.id.p1_center_icon);
        final ImageView p2CenteredPic = (ImageView) findViewById(R.id.p2_center_icon);
        final Animation overshootOnEnter = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.overshoot_on_enter);
        Animation anticipateOnExit = AnimationUtils.loadAnimation(MultiplayerGameActivity.this, R.anim.anticipate_on_exit);

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
                        cancelButton.setVisibility(View.VISIBLE);

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
            // This game was already created by another player so that means i should be assigned to be player 2
            // and get the init message sent by player 1
            myTurn = 2;
            handleOtherPlayer(match);
        }else{
            myTurn = 1;
        }

        setPlayerInfo(myTurn, mDisplayName);
        sendInitialPlayerInfo(myTurn, match);
        refreshPlayerTextView();
    }

    /**
     * Method used to change the active player textview once the init messages are received.
     */
    public void refreshPlayerTextView() {
        playerCenteredName.setText( gameInstance.getTurn() == 1 ? player1.getUsername() : player2.getUsername() );
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
        Log.d("QQUCMATCH", match.getMatchId());
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
                Toast.makeText(MultiplayerGameActivity.this, R.string.match_cancelled_by_player, Toast.LENGTH_LONG).show();
                //cancelMatch(match.getMatchId());
                finish();
                //showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                Log.d("QUICKMATCH", "updating status: expired");
                //showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                Log.d("QUICKMATCH", "updating status: auto-matching");
                //showWarning("Waiting for auto-match...",
                //        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                Log.d("QUICKMATCH", "updating status: complete");
//                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
//                    showWarning("Complete!",
//                            "This game is over; someone finished it, and so did you!  " +
//                                    "There is nothing to be done.");
//                    break;
//                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
//                showWarning("Complete!",
//                        "This game is over; someone finished it!  You can only finish it now.");
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
                //showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                ///showWarning("Good inititative!",
                 //       "Still waiting for invitations.\n\nBe patient!");
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
        final String nextParticipantId = getNextParticipantId(mPlayerId, mMatch);
        GameMessage turnMessage = new GameMessage(0, clickedPosition);
        //if the game is over we send a finished game message instead of taking a turn
        if(gameInstance.getGameStatus()){
            mTurnBasedMultiplayerClient.finishMatch(mMatch.getMatchId(), SerializationUtils.serialize(turnMessage));
            return;
        }

        // Create the next turn
        Log.d("QUICKMATCH", "nextParticipantId: " + nextParticipantId);
        mTurnBasedMultiplayerClient.takeTurn(mMatch.getMatchId(),
               SerializationUtils.serialize(turnMessage), nextParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        Log.d("QUICKMATCH", "clicked item match");
                        Log.d("TMER", nextParticipantId);
                        timer = new CountDownTimer(120000,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                Log.d(TAG, "seconds remaining: " + millisUntilFinished / 1000);
                            }

                            @Override
                            public void onFinish() {
                                Log.d(TAG, "End");
                                cancelMatch(mMatch.getMatchId());
                            }
                        }.start();
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
            if(timer != null){
                Log.d(TAG, "Cancel");
                timer.cancel();
                timer = null;
            }

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

    public void cancelMatch (String matchID){
        Log.d(TAG, matchID);
        Task<String> result = mTurnBasedMultiplayerClient.cancelMatch(matchID);
        result.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                MultiplayerGameActivity.this.finish();
                Log.d(TAG, "Good");
            }
        });
        result.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                MultiplayerGameActivity.this.finish();
                Log.d(TAG, "Bad");
            }
        });
    }

    /**
     * A function intended to loop a waiting animation with 4 chips going and leaving the screen.
     */
    public void loopWaitingAnimation() {
        //First, let's create an array with the loading icons.
        final ImageView[] loadingChips = {
                (ImageView) waitingForPlayerLy.findViewById(R.id.loading_1),
                (ImageView) waitingForPlayerLy.findViewById(R.id.loading_2),
                (ImageView) waitingForPlayerLy.findViewById(R.id.loading_3),
                (ImageView) waitingForPlayerLy.findViewById(R.id.loading_4)
        };
        //Now, create the animations for each.
        final TranslateAnimation[] enterAnims = {
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -1.0f,
                        Animation.RELATIVE_TO_PARENT,
                        0.075f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -1.0f,
                        Animation.RELATIVE_TO_PARENT,
                        0.025f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -1.0f,
                        Animation.RELATIVE_TO_PARENT,
                        -0.025f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -1.0f,
                        Animation.RELATIVE_TO_PARENT,
                        -0.075f,
                        0,
                        0,
                        0,
                        0
                ),
        };

        final TranslateAnimation[] exitAnims = {
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        0.075f,
                        Animation.RELATIVE_TO_PARENT,
                        1.0f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        0.025f,
                        Animation.RELATIVE_TO_PARENT,
                        1.0f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -0.025f,
                        Animation.RELATIVE_TO_PARENT,
                        1.0f,
                        0,
                        0,
                        0,
                        0
                ),
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT,
                        -0.075f,
                        Animation.RELATIVE_TO_PARENT,
                        1.0f,
                        0,
                        0,
                        0,
                        0
                ),
        };
        final int loadingChipCount = loadingChips.length;
        for(int i = 0; i < loadingChipCount; i++) {
            final int currentI = i;
            //Set the interpolators for the Enter / Exit animations.
            enterAnims[i].setInterpolator(new OvershootInterpolator());
            enterAnims[i].setDuration(700);
            enterAnims[i].setFillEnabled(true);
            enterAnims[i].setFillAfter(true);
            exitAnims[i].setInterpolator(new AnticipateInterpolator());
            exitAnims[i].setDuration(700);
            exitAnims[i].setFillEnabled(true);
            exitAnims[i].setFillAfter(true);
            //If we're talking about the first 3 chips, we'll handle the animations in a specific way.
            if(i < loadingChipCount - 1) {
                //Develop the Enter Animation.
                enterAnims[i].setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        loadingChips[currentI].setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //On end, the next animation should start playing.
                        loadingChips[currentI + 1].startAnimation(enterAnims[currentI + 1]);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                //Procedure of the Exit Animation
                exitAnims[i].setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //On end, this animations must shoot for the next one.
                        //It must, also, disappear the current chip.
                        loadingChips[currentI + 1].startAnimation(exitAnims[currentI + 1]);
                        loadingChips[currentI].setVisibility(View.INVISIBLE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
            //The last chip, on the other hand must:
            //Start the EXIT ANIMATIONS if the animation comes from start OR
            //Start the ENTER ANIMATIONS if the animations come from ending.
            else {
                enterAnims[i].setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        loadingChips[currentI].setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //Since this is the last chip, on end, the 1st chip should start the leaving animation
                        loadingChips[0].startAnimation(exitAnims[0]);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });

                exitAnims[i].setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //On end, this animations must shoot for the next one.
                        //It must, also, disappear the current chip.
                        loadingChips[0].startAnimation(enterAnims[0]);
                        loadingChips[currentI].setVisibility(View.INVISIBLE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
        }
        //Add a small offset to the 1st animation.
        //this helps keep it smoother on lower speed phones.
        enterAnims[0].setStartOffset(200);
        //now that we have successfully set up the animations for each item, let's start them.
        loadingChips[0].startAnimation(enterAnims[0]);
    }




@Override
    public void onBackPressed() {
       // super.onBackPressed();
        Log.d(TAG, "Good");
        final AlertDialog.Builder cancel = new AlertDialog.Builder(MultiplayerGameActivity.this);
        cancel.setMessage("Are you sure you want to leave Match?");
        cancel.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancelMatch(mMatch.getMatchId());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        cancel.show();


    }

    public void cleanClients(){
        mTurnBasedMultiplayerClient.unregisterTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
        mTurnBasedMultiplayerClient = null;
    }

    /**
     * Listener that listens for match changes made by other players.
     */
    private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
        @Override
        public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {

            showWaitingLayout(false);
            handleOtherPlayer(turnBasedMatch);
            Log.d(TAG,"A match was updated ");
            updateMatch(turnBasedMatch);
        }


        @Override
        public void onTurnBasedMatchRemoved(@NonNull String matchId) {
            Toast.makeText(MultiplayerGameActivity.this, "A match was removed.", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
       if(mTurnBasedMultiplayerClient != null){
           cleanClients();

       }
       if(timer != null) {
           timer.cancel();
            timer = null;
       }
       super.onDestroy();

    }
}
