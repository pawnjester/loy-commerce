package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.databinding.LoyaltyProgramItemBinding;
import co.loystar.loystarbusiness.fragments.LoyaltyProgramDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgram;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class LoyaltyProgramListActivity extends BaseActivity {
    private final String KEY_RECYCLER_STATE = "recycler_state";
    public static final int REQ_CREATE_PROGRAM = 110;
    private Bundle mBundleRecyclerViewState;

    private boolean mTwoPane;

    private View mLayout;
    private Context mContext;
    private ExecutorService executor;
    private SessionManager mSessionManager;
    private LoyaltyProgramListAdapter mAdapter;
    private EmptyRecyclerView mRecyclerView;
    private ReactiveEntityStore<Persistable> mDataStore;
    private MerchantEntity merchantEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyaltyprogram_list);

        if (getIntent().hasExtra(Constants.CREATE_LOYALTY_PROGRAM)) {
            Intent intent = new Intent(LoyaltyProgramListActivity.this, NewLoyaltyProgramListActivity.class);
            startActivityForResult(intent, REQ_CREATE_PROGRAM);
        }

        FloatingActionButton fab = findViewById(R.id.activity_loyalty_program_list_fab);
        fab.setOnClickListener(view -> {
            if (!AccountGeneral.isAccountActive(this)) {
                Snackbar.make(mLayout,
                        "Your subscription has expired, update subscription to add a Loyalty Program",
                        Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                    Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                    startActivity(intent);
                }).show();
            } else {
                Intent intent = new Intent(LoyaltyProgramListActivity.this, NewLoyaltyProgramListActivity.class);
                startActivityForResult(intent, REQ_CREATE_PROGRAM);
            }
        });

        mContext = this;
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();
        mLayout = findViewById(R.id.loyalty_program_list_wrapper);

        mAdapter = new LoyaltyProgramListAdapter();
        executor = Executors.newSingleThreadExecutor();
        mAdapter.setExecutor(executor);

        if (findViewById(R.id.loyalty_program_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (mTwoPane) {
            Result<LoyaltyProgramEntity> result = mAdapter.performQuery();
            if (result.iterator().hasNext() && result.first() != null) {
                Bundle arguments = new Bundle();
                arguments.putInt(LoyaltyProgramDetailFragment.ARG_ITEM_ID, result.first().getId());
                LoyaltyProgramDetailFragment fragment = new LoyaltyProgramDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.loyalty_program_detail_container, fragment, LoyaltyProgramDetailFragment.TAG)
                        .commit();
            }
        }

        EmptyRecyclerView recyclerView = findViewById(R.id.loyalty_programs_rv);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        boolean programUpdated = getIntent().getBooleanExtra(Constants.LOYALTY_PROGRAM_UPDATED, false);
        if (programUpdated) {
            showSnackbar(R.string.program_update_success);
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        View emptyView = findViewById(R.id.loyalty_programs_list_empty_container);
        ImageView stateImageView = emptyView.findViewById(R.id.stateImage);
        TextView stateIntroTextView = emptyView.findViewById(R.id.stateIntroText);
        TextView stateDescriptionTextView = emptyView.findViewById(R.id.stateDescriptionText);
        BrandButtonNormal stateActionBtn = emptyView.findViewById(R.id.stateActionBtn);

        stateImageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_campaigns));
        stateIntroTextView.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        stateDescriptionTextView.setText(getString(R.string.no_programs_found));

        stateActionBtn.setText(getString(R.string.start_loyalty_program_btn_label));
        stateActionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(LoyaltyProgramListActivity.this, NewLoyaltyProgramListActivity.class);
            startActivityForResult(intent, REQ_CREATE_PROGRAM);
        });

        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(emptyView);
    }

    private class LoyaltyProgramListAdapter extends QueryRecyclerAdapter<LoyaltyProgramEntity, BindingHolder<LoyaltyProgramItemBinding>> {

        LoyaltyProgramListAdapter() {
            super(LoyaltyProgramEntity.$TYPE);
        }

        @Override
        public Result<LoyaltyProgramEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            }

            Selection<ReactiveResult<LoyaltyProgramEntity>> programsSelection = mDataStore.select(LoyaltyProgramEntity.class);
            programsSelection.where(LoyaltyProgramEntity.OWNER.eq(merchantEntity));
            programsSelection.where(LoyaltyProgramEntity.DELETED.notEqual(true));
            return programsSelection.orderBy(LoyaltyProgramEntity.UPDATED_AT.desc()).get();
        }

        @Override
        public void onBindViewHolder(LoyaltyProgramEntity item, BindingHolder<LoyaltyProgramItemBinding> holder, int position) {
            holder.binding.setLoyaltyProgram(item);
            if (item.getProgramType().equals(getString(R.string.simple_points))) {
                holder.binding.programType.setText(getString(R.string.simple_points_program_label));
                holder.binding.programTarget.setText(
                        getString(
                                R.string.program_target_value,
                                String.valueOf(item.getThreshold()),
                                "points")
                );
            } else if (item.getProgramType().equals(getString(R.string.stamps_program))) {
                holder.binding.programType.setText(getString(R.string.stamps_program_label));
                holder.binding.programTarget.setText(
                        getString(
                                R.string.program_target_value,
                                String.valueOf(item.getThreshold()),
                                "stamps")
                );
            }
            holder.binding.getRoot().setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            );
        }

        @Override
        public BindingHolder<LoyaltyProgramItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            LoyaltyProgramItemBinding binding = LoyaltyProgramItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.deleteProgramBtn.setTag(binding);
            binding.editProgramBtn.setTag(binding);

            binding.deleteProgramBtn.setOnClickListener(view -> {
                if (!AccountGeneral.isAccountActive(mContext)) {
                    Snackbar.make(mLayout,
                            "Your subscription has expired, update subscription to delete a program",
                            Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                        Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                        startActivity(intent);
                    }).show();
                } else  {
                    LoyaltyProgramItemBinding loyaltyProgramItemBinding = (LoyaltyProgramItemBinding) view.getTag();
                    if (loyaltyProgramItemBinding != null) {
                        final LoyaltyProgram loyaltyProgram = loyaltyProgramItemBinding.getLoyaltyProgram();
                        new AlertDialog.Builder(mContext)
                                .setTitle("Are you sure?")
                                .setMessage("You won't be able to recover this program.")
                                .setPositiveButton(mContext.getString(R.string.confirm_delete_positive), (dialog, which) -> {
                                    dialog.dismiss();
                                    LoyaltyProgramEntity loyaltyProgramEntity = mDataStore.findByKey(LoyaltyProgramEntity.class, loyaltyProgram.getId()).blockingGet();
                                    if (loyaltyProgramEntity != null) {
                                        loyaltyProgramEntity.setDeleted(true);
                                        mDataStore.update(loyaltyProgramEntity).subscribe();
                                        mAdapter.queryAsync();
                                        SyncAdapter.performSync(mContext, mSessionManager.getEmail());

                                        showSnackbar(R.string.loyalty_program_deleted);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            });

            binding.editProgramBtn.setOnClickListener(view -> {
                if (!AccountGeneral.isAccountActive(mContext)) {
                    Snackbar.make(mLayout,
                            "Your subscription has expired, update subscription to edit a program",
                            Snackbar.LENGTH_LONG).setAction("Subscribe", view1 -> {
                        Intent intent = new Intent(mContext, PaySubscriptionActivity.class);
                        startActivity(intent);
                    }).show();
                } else {
                    LoyaltyProgramItemBinding loyaltyProgramItemBinding = (LoyaltyProgramItemBinding) view.getTag();
                    if (loyaltyProgramItemBinding != null) {
                        LoyaltyProgram loyaltyProgram = loyaltyProgramItemBinding.getLoyaltyProgram();
                        if (mTwoPane) {
                            Bundle arguments = new Bundle();
                            arguments.putInt(LoyaltyProgramDetailFragment.ARG_ITEM_ID, loyaltyProgram.getId());
                            LoyaltyProgramDetailFragment fragment = new LoyaltyProgramDetailFragment();
                            fragment.setArguments(arguments);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.loyalty_program_detail_container, fragment)
                                    .commit();
                        } else {
                            Intent intent = new Intent(mContext, LoyaltyProgramDetailActivity.class);
                            intent.putExtra(LoyaltyProgramDetailFragment.ARG_ITEM_ID, loyaltyProgram.getId());
                            startActivity(intent);
                        }
                    }
                }
            });
            return new BindingHolder<>(binding);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*save RecyclerView state*/
        mBundleRecyclerViewState = new Bundle();
        Parcelable listState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mBundleRecyclerViewState.putParcelable(KEY_RECYCLER_STATE, listState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*restore RecyclerView state*/
        if (mBundleRecyclerViewState != null) {
            Parcelable listState = mBundleRecyclerViewState.getParcelable(KEY_RECYCLER_STATE);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }
        mAdapter.queryAsync();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        mAdapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CREATE_PROGRAM && resultCode == RESULT_OK) {
            if (data.hasExtra(Constants.LOYALTY_PROGRAM_CREATED) && data.getBooleanExtra(Constants.LOYALTY_PROGRAM_CREATED, false)) {
                showSnackbar(R.string.create_program_success);
                mAdapter.queryAsync();
            }
        }
    }
}
