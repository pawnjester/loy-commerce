package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.view.MenuItem;
import android.view.View;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.fragments.CustomerDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.EventBus.CustomerDetailFragmentEventBus;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

public class CustomerDetailActivity extends BaseActivity {
    private static final int REQUEST_CHOOSE_PROGRAM = 145;
    private int customerId;
    private ReactiveEntityStore<Persistable> mDataStore;
    private CustomerEntity mItem;
    private Context mContext;
    private MerchantEntity merchantEntity;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        SessionManager sessionManager = new SessionManager(this);
        DatabaseManager mDatabaseManager = DatabaseManager.getInstance(this);
        mLayout = findViewById(R.id.detail_wrapper);
        merchantEntity = mDatabaseManager.getMerchant(sessionManager.getMerchantId());
        customerId = getIntent().getIntExtra(CustomerDetailFragment.ARG_ITEM_ID, 0);
        mItem = mDatabaseManager.getCustomerById(customerId);

        FloatingActionButton fab = findViewById(R.id.customer_detail_fab);
        if (fab != null) {
            fab.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_create_white_48px));
            fab.setOnClickListener(view -> {
                if (mItem != null) {
                    if (!AccountGeneral.isAccountActive(this)) {
                        Snackbar.make(mLayout,
                                "Your subscription has expired, update subscription to edit a customer",
                                Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                            Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                            startActivity(intent);
                        }).show();
                    } else {
                        Intent intent = new Intent(CustomerDetailActivity.this, EditCustomerDetailsActivity.class);
                        intent.putExtra(Constants.CUSTOMER_ID, customerId);
                        startActivity(intent);
                    }
                }
            });
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(CustomerDetailFragment.ARG_ITEM_ID, customerId);
            CustomerDetailFragment fragment = new CustomerDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.customer_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, CustomerListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CustomerDetailFragmentEventBus
                .getInstance()
                .getFragmentEventObservable()
                .compose(bindToLifecycle())
                .subscribe(bundle -> {
                    if (bundle.getInt(Constants.FRAGMENT_EVENT_ID, 0) == CustomerDetailFragmentEventBus.ACTION_START_SALE) {
                        customerId = bundle.getInt(Constants.CUSTOMER_ID, 0);
                        startSale();
                    }
                });
    }

    private void startSale() {
        mDataStore.count(LoyaltyProgramEntity.class)
                .get()
                .single()
                .toObservable()
                .compose(bindToLifecycle())
                .subscribe(integer -> {
                    if (integer == 0) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("No Loyalty Program Found!")
                                .setMessage("To record a sale, you would have to start a loyalty program.")
                                .setPositiveButton(mContext.getString(R.string.start_loyalty_program_btn_label), (dialog, which) -> {
                                    dialog.dismiss();
                                    startLoyaltyProgram();
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
                    } else if (integer == 1) {
                        LoyaltyProgramEntity loyaltyProgramEntity = merchantEntity.getLoyaltyPrograms().get(0);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            startSaleWithoutPos(loyaltyProgramEntity.getId());
                        }
                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean isPosTurnedOn = sharedPreferences.getBoolean(getString(R.string.pref_turn_on_pos_key), false);
                        if (isPosTurnedOn) {
                            startSaleWithPos();
                        } else {
                            chooseProgram();
                        }
                    }
                });
    }

    private void chooseProgram() {
        Intent intent = new Intent(this, ChooseProgramActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_PROGRAM);
    }

    private void startSaleWithPos() {
        Intent intent = new Intent(this, SaleWithPosActivity.class);
        intent.putExtra(Constants.CUSTOMER_ID, customerId);
        startActivity(intent);
    }

    private void startSaleWithoutPos(int programId) {
        Intent intent = new Intent(this, SaleWithoutPosActivity.class);
        intent.putExtra(Constants.CUSTOMER_ID, customerId);
        intent.putExtra(Constants.LOYALTY_PROGRAM_ID, programId);
        startActivity(intent);
    }

    private void startLoyaltyProgram() {
        Intent intent = new Intent(mContext, LoyaltyProgramListActivity.class);
        intent.putExtra(Constants.CREATE_LOYALTY_PROGRAM, true);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHOOSE_PROGRAM) {
                int programId = data.getIntExtra(Constants.LOYALTY_PROGRAM_ID, 0);
                startSaleWithoutPos(programId);
            }
        }
    }
}
