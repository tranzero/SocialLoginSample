package tranzero.com.sociallogin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.util.concurrent.Callable;

import io.fabric.sdk.android.Fabric;

/**
 * A login screen that offers login via multiple providers.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private TwitterLoginButton mTwitterSignInButton;
    private GoogleApiClient mGoogleApiClient;
    private CallbackManager mFacebookCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerFacebook(this);
        registerTwitter(this);

        setContentView(R.layout.activity_login);

        Intent intent = getIntent();
        if (intent != null) {
            checkForInstagramData(this, intent);
        }

        View rootView = getWindow().getDecorView();
        initGoogleSignInButton(this, rootView);
        initFaceBookSignInButton(this, rootView);
        initTwitterSignInButton(this, rootView);
        initInstagramSignInButton(this, rootView);
    }

    private void initInstagramSignInButton(final Context context, View rootView) {
        Button instagramSignInButton = (Button) rootView.findViewById(R.id.instagram_sign_in_button);
        instagramSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithInstagram(context);
            }
        });
    }

    private void initTwitterSignInButton(final Context context, View rootView) {
        mTwitterSignInButton = (TwitterLoginButton) rootView.findViewById(R.id.twitter_sign_in_button);
        mTwitterSignInButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(final Result<TwitterSession> result) {
                handleSignInResult(context, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        TwitterCore.getInstance().logOut();
                        return null;
                    }
                });
            }

            @Override
            public void failure(TwitterException e) {
                Log.d(LoginActivity.class.getCanonicalName(), e.getMessage());
                handleSignInResult(context, null);
            }
        });
    }

    private void initFaceBookSignInButton(final Context context, View rootView) {
        LoginButton facebookSignInButton = (LoginButton) rootView.findViewById(R.id.facebook_sign_in_button);
        facebookSignInButton.registerCallback(mFacebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(final LoginResult loginResult) {
                        handleSignInResult(context, new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                LoginManager.getInstance().logOut();
                                return null;
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        handleSignInResult(context,null);
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(LoginActivity.class.getCanonicalName(), error.getMessage());
                        handleSignInResult(context,null);
                    }
                }
        );
    }

    private void initGoogleSignInButton(final Context context, View rootView) {
        SignInButton googleSignInButton = (SignInButton) rootView.findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle(context);
            }
        });
    }

    private void registerTwitter(Context context) {
        // The private key that follows should never be public
        // (consider this when deploying the application)
        TwitterAuthConfig authConfig = new TwitterAuthConfig("lPcEPVTOHSdQgfy22rxYlvz04",
                "Rd9yQ4B8dSffj025UJP8y3QQIbJvRO6eUv68jmgIhe1dUSdjNq");
        Fabric.with(context, new TwitterCore(authConfig));
    }

    private void registerFacebook(Context context) {
        FacebookSdk.sdkInitialize(context.getApplicationContext());
        mFacebookCallbackManager = CallbackManager.Factory.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                final GoogleApiClient client = mGoogleApiClient;

                handleSignInResult(this, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if (client != null) {

                            Auth.GoogleSignInApi.signOut(client).setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(Status status) {
                                            Log.d(LoginActivity.class.getCanonicalName(),
                                                    status.getStatusMessage());

                                        /* TODO: handle logout failures */
                                        }
                                    }
                            );

                        }

                        return null;
                    }
                });

            } else {
                handleSignInResult(this, null);
            }
        } else if (TwitterAuthConfig.DEFAULT_AUTH_REQUEST_CODE == requestCode) {
            mTwitterSignInButton.onActivityResult(requestCode, resultCode, data);
        } else {
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void signInWithGoogle(Context context) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signInWithInstagram(Context context) {
        final Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https")
                .authority("api.instagram.com")
                .appendPath("oauth")
                .appendPath("authorize")
                .appendQueryParameter("client_id", "18a8b74da9644bd7a9294caef1c5e76c")
                .appendQueryParameter("redirect_uri", "sociallogin://redirect")
                .appendQueryParameter("response_type", "token");
        final Intent browser = new Intent(Intent.ACTION_VIEW, uriBuilder.build());
        context.startActivity(browser);
    }

    private void checkForInstagramData(Context context, @NonNull Intent intent) {
        final Uri data = intent.getData();
        if (data != null && data.getScheme().equals("sociallogin") && data.getFragment() != null) {
            final String accessToken = data.getFragment().replaceFirst("access_token=", "");
            if (accessToken != null) {
                handleSignInResult(context, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // Do nothing, just throw the access token away.
                        return null;
                    }
                });
            } else {
                handleSignInResult(context,null);
            }
        }
    }

    private void handleSignInResult(Context context, Callable<Void> logout) {
        if (logout == null) {
            /* Login error */
            Toast.makeText(getApplicationContext(), R.string.login_error, Toast.LENGTH_SHORT).show();
        } else {
            /* Login success */
            startActivity(new Intent(context, LoggedInActivity.class));
        }
    }
}

