package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.LoyaltyProgramDetailFragment;

public class LoyaltyProgramDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_program_detail);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(LoyaltyProgramDetailFragment.ARG_ITEM_ID,
                    getIntent().getIntExtra(LoyaltyProgramDetailFragment.ARG_ITEM_ID, 0));
            LoyaltyProgramDetailFragment fragment = new LoyaltyProgramDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.loyalty_program_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, LoyaltyProgramListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}