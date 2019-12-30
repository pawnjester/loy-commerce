package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.trello.rxlifecycle2.components.support.RxFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.LoyaltyProgramDetailActivity;
import co.loystar.loystarbusiness.activities.LoyaltyProgramListActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.LoyaltyProgram;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A fragment representing a single LoyaltyProgram detail screen.
 * This fragment is either contained in a {@link LoyaltyProgramListActivity}
 * in two-pane mode (on tablets) or a {@link LoyaltyProgramDetailActivity}
 * on handsets.
 */
public class LoyaltyProgramDetailFragment extends RxFragment {
    public static final String ARG_ITEM_ID = "item_id";
    public static final String TAG = LoyaltyProgramDetailFragment.class.getSimpleName();

    private LoyaltyProgramEntity mItem;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private boolean mTwoPane;

    /*views*/
    private View rootView = null;
    private EditText programNameView;
    private CurrencyEditText spendingTargetView;
    private EditText rewardView;
    private EditText stampsTarget;
    private  ProgressDialog progressDialog;

    public LoyaltyProgramDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            return;
        }

        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        mSessionManager = new SessionManager(getActivity());

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = mDatabaseManager.getLoyaltyProgramById(getArguments().getInt(ARG_ITEM_ID, 0));

            Activity activity = this.getActivity();
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                if (mItem != null) {
                    actionBar.setTitle(mItem.getName());
                }
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            View multiPaneView = activity.findViewById(R.id.loyalty_program_detail_container);
            // The multiPaneView container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            mTwoPane = multiPaneView != null && multiPaneView.getTag() != null && multiPaneView.getTag().toString().equals("multiPaneLoyaltyDetail");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String merchantCurrencySymbol = CurrenciesFetcher.getCurrencies(getContext()).getCurrency(
                mSessionManager.getCurrency()
        ).getSymbol();

        if (mItem != null) {
            if (mItem.getProgramType().equals(getString(R.string.simple_points))) {
                rootView = inflater.inflate(R.layout.simple_points_program_layout, container, false);
                String spendTarget = merchantCurrencySymbol  + " " + mItem.getThreshold();
                programNameView = rootView.findViewById(R.id.program_name);
                spendingTargetView = rootView.findViewById(R.id.spending_target);
                rewardView = rootView.findViewById(R.id.customer_reward);
                ((TextView) rootView.findViewById(R.id.spending_target_explanation)).setText(String.format(Locale.UK, getString(R.string.spending_target_explanation), merchantCurrencySymbol));

                spendingTargetView.setText(spendTarget);
                programNameView.setText(mItem.getName());
                rewardView.setText(mItem.getReward());

                String defaultProgramName = mSessionManager.getBusinessName() + " " + "Points Rewards";
                programNameView.setText(defaultProgramName);

                TextView rewardExplanation = rootView.findViewById(R.id.reward_text_explanation);
                String merchantBusinessType = mSessionManager.getBusinessType();

                if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts))) {
                    rewardExplanation.setText(getString(R.string.beverages_and_deserts_reward));
                }
                else if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
                    rewardExplanation.setText(getString(R.string.salon_and_beauty_reward_eg));
                }
                else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
                    rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
                }
                else if (merchantBusinessType.equals(getString(R.string.gym_and_fitness))) {
                    rewardExplanation.setText(getString(R.string.gym_and_fitness_reward_points));
                }
                else if (merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
                    rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
                }
                else if (merchantBusinessType.equals(getString(R.string.travel_and_hotel))) {
                    rewardExplanation.setText(getString(R.string.travel_and_hotel_reward));
                }
                else {
                    rewardExplanation.setText(getString(R.string.discount_on_next_purchase));
                }

                BrandButtonNormal updateBtn = rootView.findViewById(R.id.points_program_update_btn);
                updateBtn.setVisibility(View.VISIBLE);
                RxView.clicks(updateBtn).subscribe(o -> {
                    if (formIsDirty()) {
                        closeKeyBoard();
                        submitForm();
                    }
                });
            } else if (mItem.getProgramType().equals(getString(R.string.stamps_program))) {
                rootView = inflater.inflate(R.layout.stamps_program_layout, container, false);
                programNameView = rootView.findViewById(R.id.program_name);
                stampsTarget = rootView.findViewById(R.id.stamps_target);
                rewardView = rootView.findViewById(R.id.customer_reward);
                TextView stampsTargetExplanation = rootView.findViewById(R.id.stamps_target_explanation);
                String sTemp = "%s eg. 5";

                programNameView.setText(mItem.getName());
                rewardView.setText(mItem.getReward());
                stampsTargetExplanation.setText(String.format(sTemp, getString(R.string.stamps_target_explanation)));
                stampsTarget.setText(String.valueOf(mItem.getThreshold()));

                TextView rewardExplanation = rootView.findViewById(R.id.reward_text_explanation);
                String merchantBusinessType = mSessionManager.getBusinessType();

                String defaultProgramName = mSessionManager.getBusinessName() + " " + "Stamps Rewards";
                programNameView.setText(defaultProgramName);

                if (merchantBusinessType.equals(getString(R.string.beverages_and_deserts))) {
                    rewardExplanation.setText(getString(R.string.beverages_and_deserts_reward));
                }
                else if (merchantBusinessType.equals(getString(R.string.hair_and_beauty))) {
                    rewardExplanation.setText(getString(R.string.salon_and_beauty_reward_eg));
                }
                else if (merchantBusinessType.equals(getString(R.string.fashion_and_accessories))) {
                    rewardExplanation.setText(getString(R.string.fashion_and_accessories_reward_stamps));
                }
                else if (merchantBusinessType.equals(getString(R.string.gym_and_fitness))) {
                    rewardExplanation.setText(getString(R.string.gym_and_fitness_reward_stamps));
                }
                else if (merchantBusinessType.equals(getString(R.string.bakery_and_pastry))) {
                    rewardExplanation.setText(getString(R.string.next_purchase_free));
                }
                else if (merchantBusinessType.equals(getString(R.string.travel_and_hotel))) {
                    rewardExplanation.setText(getString(R.string.travel_and_hotel_reward));
                }
                else {
                    rewardExplanation.setText(getString(R.string.next_purchase_free));
                }

                BrandButtonNormal updateBtn = rootView.findViewById(R.id.stamps_program_update_btn);
                updateBtn.setVisibility(View.VISIBLE);
                RxView.clicks(updateBtn).subscribe(o -> {
                    if (formIsDirty()) {
                        closeKeyBoard();
                        submitForm();
                    }
                });
            }
        }

        return rootView;
    }

    private boolean formIsDirty() {
        return !programNameView.getText().toString().equals(mItem.getName()) || !rewardView.getText().toString().equals(mItem.getReward()) || spendingTargetView != null && !spendingTargetView.getFormattedValue(spendingTargetView.getRawValue()).equals(String.valueOf(mItem.getThreshold())) || stampsTarget != null && !stampsTarget.getText().toString().equals(String.valueOf(mItem.getThreshold()));

    }

    private void submitForm() {
        if (programNameView.getText().toString().trim().isEmpty()) {
            programNameView.setError(getString(R.string.error_program_name_required));
            programNameView.requestFocus();
            return;
        }
        if (spendingTargetView != null && spendingTargetView.getRawValue() == 0) {
            spendingTargetView.setError(getString(R.string.error_spend_target_cant_be_zero));
            spendingTargetView.requestFocus();
            return;
        }
        if (stampsTarget != null && stampsTarget.getText().toString().trim().isEmpty()) {
            stampsTarget.setError(getString(R.string.error_stamps_threshold));
            stampsTarget.requestFocus();
            return;
        }
        if (rewardView.getText().toString().trim().isEmpty()) {
            rewardView.setError(getString(R.string.error_reward_required));
            rewardView.requestFocus();
            return;
        }

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.updating_loyalty_wait));
        progressDialog.show();


        try {
            JSONObject jsonObjectRequestData = new JSONObject();
            jsonObjectRequestData.put("name", programNameView.getText().toString());
            jsonObjectRequestData.put("reward", rewardView.getText().toString());

            if (mItem.getProgramType().equals(getString(R.string.simple_points))) {
                jsonObjectRequestData.put("threshold", spendingTargetView.getFormattedValue(spendingTargetView.getRawValue()));
            }
            else if (mItem.getProgramType().equals(getString(R.string.stamps_program))) {
                jsonObjectRequestData.put("threshold", stampsTarget.getText().toString());
            }

            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObjectRequestData);

            ApiClient mApiClient = new ApiClient(getActivity());
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            mApiClient.getLoystarApi(false).updateMerchantLoyaltyProgram(String.valueOf(mItem.getId()), requestBody).enqueue(new Callback<LoyaltyProgram>() {
                @Override
                public void onResponse(@NonNull Call<LoyaltyProgram> call, @NonNull Response<LoyaltyProgram> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (response.isSuccessful()) {
                        LoyaltyProgram loyaltyProgram = response.body();

                        if (loyaltyProgram == null) {
                            Toast.makeText(getContext(), getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                        } else {
                            mItem.setName(loyaltyProgram.getName());
                            mItem.setProgramType(loyaltyProgram.getProgram_type());
                            mItem.setReward(loyaltyProgram.getReward());
                            mItem.setThreshold(loyaltyProgram.getThreshold());
                            mItem.setUpdatedAt(new Timestamp(loyaltyProgram.getUpdated_at().getMillis()));

                            mDatabaseManager.updateLoyaltyProgram(mItem);

                            if (mTwoPane) {
                                Snackbar.make(rootView, getString(R.string.program_update_success), Snackbar.LENGTH_LONG).show();
                            } else {
                                Intent intent = new Intent(getActivity(), LoyaltyProgramListActivity.class);
                                intent.putExtra(Constants.LOYALTY_PROGRAM_UPDATED, true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        }
                    }
                    else {
                        Toast.makeText(getContext(), getString(R.string.error_program_update), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoyaltyProgram> call, @NonNull Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), getString(R.string.error_program_update_connection), Toast.LENGTH_LONG).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void closeKeyBoard() {
        if (getActivity() == null) {
            return;
        }
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
