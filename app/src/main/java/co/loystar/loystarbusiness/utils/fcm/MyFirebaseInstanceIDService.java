package co.loystar.loystarbusiness.utils.fcm;

import android.content.SharedPreferences;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.Constants;

/**
 * Created by ordgen on 12/18/17.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        storeRegIdInPref(refreshedToken);
    }

    /**
     * Persist token to shared preferences.
     *
     * @param token The new token.
     */
    private void storeRegIdInPref(String token) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(Constants.FIREBASE_REGISTRATION_TOKEN, token);
        editor.apply();
    }
}
