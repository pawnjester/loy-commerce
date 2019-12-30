package co.loystar.loystarbusiness;

import android.graphics.Typeface;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

import co.loystar.loystarbusiness.utils.Foreground;
import co.loystar.loystarbusiness.utils.TypefaceUtil;
import io.smooch.core.Settings;
import io.smooch.core.Smooch;
import io.smooch.core.SmoochCallback;
import timber.log.Timber;

/**
 * Created by ordgen on 11/1/17.
 */

public class App extends MultiDexApplication{
    private static App singleton;
    private Typeface latoFont;

    public static App getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        Foreground.init(this);
        extractLato();
        setGlobalFontType();
        singleton.initializeInstance();

        Settings settings = new Settings(BuildConfig.SMOOCH_TOKEN);
        settings.setFileProviderAuthorities(getPackageName() + ".co.loystar.loystarbusiness.provider");
        Smooch.init(this, settings, response -> {});

        FirebaseApp.initializeApp(this);
        Timber.plant(new Timber.DebugTree());
    }

    protected void initializeInstance() {}

    private void extractLato() {
        latoFont = Typeface.createFromAsset(getAssets(), "fonts/Lato.ttf");
    }

    public Typeface getTypeface() {
        if (latoFont == null) {
            extractLato();
        }
        return latoFont;
    }

    /**
     * Using reflection to override default typeface
     */
    private void setGlobalFontType() {
        TypefaceUtil.overrideFont(getApplicationContext());
    }
}
