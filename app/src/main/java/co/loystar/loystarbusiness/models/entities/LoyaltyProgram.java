package co.loystar.loystarbusiness.models.entities;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;
import java.util.List;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;

/**
 * Created by ordgen on 11/10/17.
 */

@Entity
public interface LoyaltyProgram extends Observable, Parcelable, Persistable {
    @Key
    int getId();

    Timestamp getCreatedAt();
    Timestamp getUpdatedAt();
    String getName();
    int getThreshold();
    String getProgramType();
    String getReward();
    boolean isDeleted();

    @Bindable
    @ManyToOne
    Merchant getOwner();

    @OneToMany(mappedBy = "loyaltyProgram")
    List<ProductEntity> getProducts();
}
