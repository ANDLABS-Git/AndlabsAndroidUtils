package com.andlabs.androidutils.ad;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.adsdk.sdk.Ad;
import com.adsdk.sdk.AdListener;
import com.adsdk.sdk.AdManager;
import com.andlabs.androidutils.logging.L;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.MMInterstitial;
import com.millennialmedia.android.RequestListener;
import com.revmob.RevMob;
import com.revmob.RevMobAdsListener;
import com.revmob.ads.fullscreen.RevMobFullscreen;


public class AdUtils {

    private static final String AD_TYPE = "adType";

    // Ad provider
    public static final int ALL = 0;

    public static final int MOBFOX = 1;

    public static final int REVMOB = 2;

    public static final int INMOBI = 3;

    public static final int CHARTBOOST = 4;

    public static final int MMEDIA = 5;

    public static final int INNERACTIVE = 6;


    private static final String MOBFOX_REQUEST_URL = "http://my.mobfox.com/vrequest.php";

    // Meta data keys
    private static final String MOBFOX_PUBLISHER_ID = "com.mobfox.publisher.id";

    private static final String MMEDIA_APP_ID = "com.mmedia.app.id";

    private static final String CHARTBOOST_APP_ID = "com.chartboost.app.id";

    private static final String CHARTBOOST_SIGNATURE = "com.chartboost.signature";


    private static final String FULLSCREEN_DISTANCE = "com.andlabs.util.ads.distance";


    // Ad states
    private static final int AD_NOT_INITIALIZED = 0;

    private static final int AD_INTERSTITIAL = 1;

    private static final int AD_BANNER = 2;


    private Activity mActivity;


    private int mFullscreenCounter = -1;


    // Ad manager
    private AdManager mMobFoxManager;

    private RevMob mRevMobManager;

    private RevMobFullscreen mRevMobFullscreen;

    private MMInterstitial mMMediaManager;

    private Chartboost mChartboostManager;


    // Publisher IDs
    private String mMobfoxPublisherId;

    private String mChartboostAppid;

    private String mChartboostAppSignature;

    private String mMediaAppId;

    private int mFullscreenDistance;


    // Loaded ads
    @SuppressLint("UseSparseArrays")
    private Map<Integer, Boolean> mLoadedAds = new HashMap<Integer, Boolean>();


    // Singleton code
    private static AdUtils sInstance;


    public static AdUtils getInstance(Activity activity) {
        if (sInstance == null) {
            sInstance = new AdUtils(activity);
        }

        return sInstance;
    }


    private AdUtils(Activity activity) {

        mActivity = activity;

        // Init the manifest's meta data
        initConfig();

        // Init RevMob
        mRevMobManager = RevMob.start(activity);

        // Init MobFox
        if (mMobfoxPublisherId != null) {
            mMobFoxManager = new AdManager(activity, MOBFOX_REQUEST_URL, mMobfoxPublisherId, false);
        }

        // Init Millenial Media
        if (mMediaAppId != null) {
            mMMediaManager = new MMInterstitial(activity);
            mMMediaManager.setApid(mMediaAppId);
        }

        // Init Chartboost
        if (mChartboostAppid != null && mChartboostAppSignature != null) {
            mChartboostManager = Chartboost.sharedChartboost();
            mChartboostManager.onCreate(activity, mChartboostAppid, mChartboostAppSignature, mChartboostListener);
            mChartboostManager.onStart(activity);
        }

        // Init all ads as not loaded
        mLoadedAds.put(MOBFOX, false);
        mLoadedAds.put(REVMOB, false);
        mLoadedAds.put(INMOBI, false);
        mLoadedAds.put(CHARTBOOST, false);
        mLoadedAds.put(MMEDIA, false);
        mLoadedAds.put(INNERACTIVE, false);
    }


    private void initConfig() {
        final Bundle metaData = readMetaData();
        setMetaData(metaData);
    }


    private Bundle readMetaData() {
        try {
            ApplicationInfo ai = mActivity.getPackageManager().getApplicationInfo(mActivity.getPackageName(),
                    PackageManager.GET_META_DATA);

            if (ai != null) {
                final Bundle bundle = ai.metaData;
                return bundle;
            }

        } catch (NameNotFoundException e) {
            L.e(e);
        }

        return null;
    }


    private void setMetaData(Bundle metaData) {
        // Ad provider meta data
        // mobfox publisher id
        mMobfoxPublisherId = metaData.getString(MOBFOX_PUBLISHER_ID);

        // mmedia app id
        mMediaAppId = metaData.getString(MMEDIA_APP_ID);

        // chartboost app id
        mChartboostAppid = metaData.getString(CHARTBOOST_APP_ID);

        // chartboost signature
        mChartboostAppSignature = metaData.getString(CHARTBOOST_SIGNATURE);

        // Config meta data
        // full screen distance
        mFullscreenDistance = metaData.getInt(FULLSCREEN_DISTANCE);
    }


    public boolean usesInterstiatial() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final int adType = prefs.getInt(AD_TYPE, AD_NOT_INITIALIZED);

        switch (adType) {
            case AD_NOT_INITIALIZED:
                return initializeAdType(prefs);
            case AD_INTERSTITIAL:
                return true;
            default:
                return false;
        }
    }


    private boolean initializeAdType(SharedPreferences prefs) {
        if (Math.random() < 1) {
            L.d("initialized interstitial");
            prefs.edit().putInt(AD_TYPE, AD_INTERSTITIAL).commit();
            return true;
        } else {
            L.d("initialized banner");
            prefs.edit().putInt(AD_TYPE, AD_BANNER).commit();
            return false;
        }
    }


    /**
     * Request an interstitial ad from a random provider. Calling this method for the first time initiates ad caching.
     */
    public void requestInterstitial() {
        requestInterstitial(ALL);
    }


    /**
     * Request an interstitial ad. Calling this method for the first time initiates ad caching.
     * 
     * @param provider
     */
    public void requestInterstitial(int provider) {
        showFullscreen(provider);
    }


    private boolean isFullscreenAllowed(int provider) {
        if (isFullScreenLoaded()) {
            mFullscreenCounter++;
            if (mFullscreenCounter % mFullscreenDistance == 0) {
                return true;
            } else {
                return false;
            }
        } else {
            loadFullscreen(provider);
            return false;
        }
    }


    private boolean isFullScreenLoaded() {
        final Collection<Boolean> values = mLoadedAds.values();
        for (Boolean loaded : values) {
            if (loaded) {
                return true;
            }
        }
        return false;
    }


    private void loadFullscreen(int provider) {

        switch (provider) {
            case MOBFOX:
                if (mMobFoxManager != null) {
                    mMobFoxManager.setListener(mMobFoxListener);
                    mMobFoxManager.requestAd();
                }

                break;

            case REVMOB:
                if (mRevMobFullscreen != null) {
                    mRevMobFullscreen = mRevMobManager.createFullscreen(mActivity, mRevMobListener);
                }

                break;

            case INMOBI:

                break;

            case CHARTBOOST:
                if (mChartboostManager != null) {
                    mChartboostManager.cacheInterstitial();
                }

                break;

            case MMEDIA:
                if (mMMediaManager != null) {
                    mMMediaManager.setListener(mMMediaListener);
                    mMMediaManager.fetch();
                }

                break;

            case INNERACTIVE:

                break;

            default: // ALL
                loadFullscreen(MOBFOX);
                loadFullscreen(REVMOB);
                loadFullscreen(INMOBI);
                loadFullscreen(CHARTBOOST);
                loadFullscreen(MMEDIA);
                loadFullscreen(INNERACTIVE);

                break;
        }
    }


    private void showFullscreen(int requestedProvider) {
        if (isFullscreenAllowed(requestedProvider)) {

            final int loadedProvider = getFirstLoadedProvider(requestedProvider);

            switch (loadedProvider) {
                case MOBFOX:
                    mMobFoxManager.showAd();
                    break;

                case REVMOB:
                    mRevMobFullscreen.show();
                    break;

                case INMOBI:

                    break;

                case CHARTBOOST:
                    mChartboostManager.showInterstitial();

                    break;

                case MMEDIA:
                    mMMediaManager.display();
                    break;

                case INNERACTIVE:

                    break;

                default: // Nothing loaded
            }
        }
    }


    private int getFirstLoadedProvider(int provider) {
        if (provider == ALL) { // all providers, go over the list of loaded ads
            final Collection<Boolean> values = mLoadedAds.values();
            int i = 0;
            for (Boolean loaded : values) {
                i++;
                if (loaded) {
                    return i;
                }
            }
        } else { // one selected provider, check if already loaded
            if (mLoadedAds.get(provider)) {
                return provider;
            }
        }
        return -1;
    }


    private RevMobAdsListener mRevMobListener = new RevMobAdsListener() {

        @Override
        public void onRevMobAdReceived() {
            L.d("revMob received");
            mLoadedAds.put(REVMOB, true);
        }


        @Override
        public void onRevMobAdNotReceived(String pArg0) {
            L.d("revMob not received");
            mLoadedAds.put(REVMOB, false);
        }


        @Override
        public void onRevMobAdDisplayed() {
            L.d("revMob displayed");
        }


        @Override
        public void onRevMobAdDismiss() {
            L.d("revMob dismissed");
            mLoadedAds.put(REVMOB, false);

            // User didn't like the ad, load new one
            loadFullscreen(REVMOB);
        }


        @Override
        public void onRevMobAdClicked() {
            L.d("revMob clicked");
            mLoadedAds.put(REVMOB, false);

            // User liked ad, load new one
            loadFullscreen(REVMOB);
        }
    };

    private AdListener mMobFoxListener = new AdListener() {

        @Override
        public void noAdFound() {
            L.d("mobFox not received");
            mLoadedAds.put(MOBFOX, false);
        }


        @Override
        public void adShown(Ad pArg0, boolean pArg1) {
            L.d("mobFox displayed");

        }


        @Override
        public void adLoadSucceeded(Ad pArg0) {
            L.d("mobFox received");
            mLoadedAds.put(MOBFOX, true);
        }


        @Override
        public void adClosed(Ad pArg0, boolean pArg1) {
            L.d("mobFox closed");
            mLoadedAds.put(MOBFOX, false);

            // User didn't like the ad, load new one
            loadFullscreen(MOBFOX);

        }


        @Override
        public void adClicked() {
            L.d("mobFox clicked");
            mLoadedAds.put(MOBFOX, false);

            // User liked ad, load new one
            loadFullscreen(MOBFOX);

        }
    };

    private RequestListener mMMediaListener = new RequestListener() {

        @Override
        public void MMAdOverlayClosed(MMAd pArg0) {
            L.d("mMedia closed");
            mLoadedAds.put(MMEDIA, false);

            // User didn't like the ad, load new one
            loadFullscreen(MMEDIA);
        }


        @Override
        public void MMAdOverlayLaunched(MMAd pArg0) {
            L.d("mMedia displayed");

        }


        @Override
        public void MMAdRequestIsCaching(MMAd pArg0) {

        }


        @Override
        public void onSingleTap(MMAd pArg0) {
            L.d("mMedia clicked");
            mLoadedAds.put(MMEDIA, false);

            // User liked ad, load new one
            loadFullscreen(MMEDIA);
        }


        @Override
        public void requestCompleted(MMAd pArg0) {
            L.d("mMedia received");
            mLoadedAds.put(MMEDIA, true);
        }


        @Override
        public void requestFailed(MMAd pArg0, MMException pArg1) {
            L.d("mMedia not received");
            mLoadedAds.put(MMEDIA, false);
        }
    };

    private ChartboostDelegate mChartboostListener = new ChartboostDelegate() {

        @Override
        public void didClickInterstitial(String pArg0) {
            L.d("chartboost clicked");
            mLoadedAds.put(CHARTBOOST, false);

            // User liked ad, load new one
            loadFullscreen(CHARTBOOST);
        }


        @Override
        public void didCacheInterstitial(String pArg0) {
            L.d("chartboost received");
            mLoadedAds.put(CHARTBOOST, true);
        }


        @Override
        public void didCloseInterstitial(String pArg0) {
            L.d("chartboost closed");
            mLoadedAds.put(CHARTBOOST, false);

            // User didn't like the ad, load new one
            loadFullscreen(CHARTBOOST);
        }


        @Override
        public void didShowInterstitial(String pArg0) {
            L.d("chartboost displayed");

        }


        @Override
        public void didFailToLoadInterstitial(String pArg0) {
            L.d("chartboost not received");
            mLoadedAds.put(CHARTBOOST, false);
        }


        @Override
        public void didDismissInterstitial(String pArg0) {
            L.d("chartboost closed");
            mLoadedAds.put(CHARTBOOST, false);

            // User didn't like the ad, load new one
            loadFullscreen(CHARTBOOST);
        }


        @Override
        public boolean shouldRequestMoreApps() {
            return false;
        }


        @Override
        public boolean shouldRequestInterstitialsInFirstSession() {
            return true;
        }


        @Override
        public boolean shouldRequestInterstitial(String pArg0) {
            return true;
        }


        @Override
        public boolean shouldDisplayMoreApps() {
            return false;
        }


        @Override
        public boolean shouldDisplayLoadingViewForMoreApps() {
            return false;
        }


        @Override
        public boolean shouldDisplayInterstitial(String pArg0) {
            return true;
        }


        @Override
        public void didShowMoreApps() {
        }


        @Override
        public void didFailToLoadMoreApps() {

        }


        @Override
        public void didDismissMoreApps() {
        }


        @Override
        public void didCloseMoreApps() {
        }


        @Override
        public void didClickMoreApps() {
        }


        @Override
        public void didCacheMoreApps() {
        }

    };
}
