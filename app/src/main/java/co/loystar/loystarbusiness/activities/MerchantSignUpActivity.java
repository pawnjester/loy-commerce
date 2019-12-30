package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.api.ApiUtils;
import co.loystar.loystarbusiness.fragments.MerchantSignUpStepOneFragment;
import co.loystar.loystarbusiness.fragments.MerchantSignUpStepTwoFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Merchant;
import co.loystar.loystarbusiness.models.databinders.MerchantWrapper;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MerchantSignUpActivity extends BaseActivity implements
        MerchantSignUpStepOneFragment.OnMerchantSignUpStepOneFragmentInteractionListener,
        MerchantSignUpStepTwoFragment.OnMerchantSignUpStepTwoFragmentInteractionListener{

    private SharedPreferences sharedPref;
    private ProgressDialog progressDialog;
    private View mLayout;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_sign_up);

        mContext = this;
        mLayout = findViewById(R.id.activity_merchant_sign_up_wrapper);
        sharedPref = getSharedPreferences(getString(R.string.merchant_sign_up_pref), Context.MODE_PRIVATE);
        String merchantPhoneNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);
        Bundle data = new Bundle();

        data.putString(Constants.PHONE_NUMBER, merchantPhoneNumber);
        MerchantSignUpStepOneFragment stepOneFragment = new MerchantSignUpStepOneFragment();
        stepOneFragment.setArguments(data);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_merchant_sign_up_container, stepOneFragment).commit();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onMerchantSignUpStepOneFragmentInteraction() {
        MerchantSignUpStepTwoFragment signUpStepTwoFragment = new MerchantSignUpStepTwoFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_merchant_sign_up_container, signUpStepTwoFragment).addToBackStack(null).commit();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }
                else {
                    onBackPressed();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMerchantSignUpStepTwoFragmentInteraction() {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.signing_up_msg));
        progressDialog.show();

        final String mPassword = sharedPref.getString(Constants.PASSWORD, "");
        ApiClient apiClient = new ApiClient(mContext);
        apiClient.getLoystarApi(false)
                .signUpMerchant(
                        sharedPref.getString(Constants.FIRST_NAME, ""),
                        sharedPref.getString(Constants.BUSINESS_EMAIL, ""),
                        sharedPref.getString(Constants.BUSINESS_NAME, ""),
                        sharedPref.getString(Constants.PHONE_NUMBER, ""),
                        sharedPref.getString(Constants.BUSINESS_CATEGORY, ""),
                        sharedPref.getString(Constants.CURRENCY, ""),
                        mPassword).enqueue(new Callback<MerchantWrapper>() {
            @Override
            public void onResponse(@NonNull Call<MerchantWrapper> call, @NonNull Response<MerchantWrapper> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (response.isSuccessful()) {
                    String authToken = response.headers().get("Access-Token");
                    String client = response.headers().get("Client");
                    MerchantWrapper merchantWrapper = response.body();
                    if (merchantWrapper == null) {
                        showSnackbar(R.string.unknown_error);
                    } else {
                        Merchant merchant = merchantWrapper.getMerchant();
                        final MerchantEntity merchantEntity = new MerchantEntity();
                        merchantEntity.setId(merchant.getId());
                        merchantEntity.setFirstName(merchant.getFirst_name());
                        merchantEntity.setLastName(merchant.getLast_name());
                        merchantEntity.setBusinessName(merchant.getBusiness_name());
                        merchantEntity.setEmail(merchant.getEmail());
                        merchantEntity.setBusinessType(merchant.getBusiness_type());
                        merchantEntity.setContactNumber(merchant.getContact_number());
                        merchantEntity.setCurrency(merchant.getCurrency());
                        if (merchant.getSubscription_expires_on() != null) {
                            merchantEntity.setSubscriptionExpiresOn(new Timestamp(merchant.getSubscription_expires_on().getMillis()));
                        }

                        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
                        databaseManager.insertNewMerchant(merchantEntity);
                        SessionManager sessionManager = new SessionManager(mContext);
                        sessionManager.setMerchantSessionData(
                                merchant.getId(),
                                merchant.getEmail(),
                                merchant.getFirst_name(),
                                merchant.getLast_name(),
                                merchant.getContact_number(),
                                merchant.getBusiness_name(),
                                merchant.getBusiness_type(),
                                merchant.getCurrency(),
                                authToken,
                                client,
                                merchant.getAddress_line1(), merchant.getAddress_line2());

                        Bundle params = new Bundle();
                        params.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, "email");
                        FirebaseAnalytics.getInstance(mContext).logEvent(FirebaseAnalytics.Event.SIGN_UP, params);

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.pref_turn_on_pos_key), merchant.isTurn_on_point_of_sale() != null && merchant.isTurn_on_point_of_sale());
                        editor.apply();

                        SharedPreferences.Editor signUpEditor = sharedPref.edit();
                        signUpEditor.clear();
                        signUpEditor.apply();

                        Bundle bundle = new Bundle();
                        Intent intent = new Intent();

                        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, merchant.getEmail());
                        bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                        bundle.putString(AccountManager.KEY_PASSWORD, mPassword);
                        intent.putExtras(bundle);

                        setResult(RESULT_OK, intent);
                        finish();
                    }
                } else if (response.code() == 422){
                    ObjectMapper mapper = ApiUtils.getObjectMapper(false);
                    try {
                        ResponseBody responseBody = response.errorBody();
                        if (responseBody == null) {
                            showSnackbar(R.string.unknown_error);
                        } else {
                            JsonNode responseObject = mapper.readTree(responseBody.charStream());
                            JSONObject errorObject = new JSONObject(responseObject.toString());
                            JSONObject errors = errorObject.getJSONObject("errors");
                            JSONArray fullMessagesArray = errors.getJSONArray("full_messages");
                            Snackbar.make(mLayout, fullMessagesArray.join(", "), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (IOException | JSONException e) {
                        showSnackbar(R.string.unknown_error);
                        e.printStackTrace();
                    }
                } else {
                    showSnackbar(R.string.unknown_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MerchantWrapper> call, @NonNull Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                showSnackbar(R.string.error_internet_connection_timed_out);
            }
        });
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}