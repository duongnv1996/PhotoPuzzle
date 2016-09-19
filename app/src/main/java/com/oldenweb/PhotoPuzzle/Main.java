package com.oldenweb.PhotoPuzzle;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main extends Activity {
    final Handler h = new Handler();
    List<ImageView> items;
    List<Integer> items_rotations;
    SharedPreferences sp;
    Editor ed;
    boolean isForeground = true;
    MediaPlayer mp;
    SoundPool sndpool;
    int snd_move;
    int snd_result;
    int snd_info;
    float start_x;
    float start_y;
    int item_size;
    boolean items_enabled;
    AnimatorSet anim;
    int screen_width;
    int screen_height;
    int t;
    int current_section = R.id.main;
    ViewPager pager;
    int num_cols;
    int num_rows;
    int spacing;
    final int num_photos = 5; // number of photos

    // AdMob
    AdView adMobBanner;
    InterstitialAd adMobInterstitial;
    AdRequest adRequest;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        AppLovinSdk.initializeSdk(this);
        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ed = sp.edit();

        // AdMob
        adMob();

        // bg sound
        mp = new MediaPlayer();
        try {
            AssetFileDescriptor descriptor = getAssets().openFd("snd_bg.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setLooping(true);
            mp.setVolume(0, 0);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }

        // if mute
        if (sp.getBoolean("mute", false))
            ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
        else
            mp.setVolume(0.5f, 0.5f);

        // game mode
        if (sp.getInt("mode", 0) == 1) {
            ((Button) findViewById(R.id.btn_mode)).setText(getString(R.string.btn_easy));
            num_cols = 8;
            num_rows = 10;
        } else {
            ((Button) findViewById(R.id.btn_mode)).setText(getString(R.string.btn_hard));
            num_cols = 4;
            num_rows = 5;
        }

        // spacing between blocks
        spacing = (int) DpToPx(1);
        spacing = (int) (Math.ceil(spacing * 0.5f) * 2);

        // SoundPool
        sndpool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_move = sndpool.load(getAssets().openFd("snd_move.mp3"), 1);
            snd_result = sndpool.load(getAssets().openFd("snd_result.mp3"), 1);
            snd_info = sndpool.load(getAssets().openFd("snd_info.mp3"), 1);
        } catch (IOException e) {
        }

        // hide navigation bar listener
        findViewById(R.id.all).setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hide_navigation_bar();
            }
        });

        // custom font
        Typeface font = Typeface.createFromAsset(getAssets(), "CooperBlack.otf");
        ((TextView) findViewById(R.id.txt_result)).setTypeface(font);
        ((TextView) findViewById(R.id.txt_high_result)).setTypeface(font);
        ((TextView) findViewById(R.id.mess)).setTypeface(font);
        font = Typeface.createFromAsset(getAssets(), "BankGothic.ttf");
        ((TextView) findViewById(R.id.txt_description)).setTypeface(font);

        // photos list
        pager = new ViewPager(this);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(new SwipeAdapter());
        ((ViewGroup) findViewById(R.id.photos)).addView(pager);

        SCALE();
    }

    // SCALE
    void SCALE() {
        // text mess
        ((TextView) findViewById(R.id.mess)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));

        // txt_description
        ((TextView) findViewById(R.id.txt_description)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(18));

        // buttons text
        ((TextView) findViewById(R.id.btn_sound)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(14));
        ((TextView) findViewById(R.id.btn_start)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(18));
        ((TextView) findViewById(R.id.btn_exit)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(14));
        ((TextView) findViewById(R.id.btn_mode)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(14));
        ((TextView) findViewById(R.id.btn_home)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_start2)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));

        // text result
        ((TextView) findViewById(R.id.txt_result)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(40));
        ((TextView) findViewById(R.id.txt_high_result)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(16));
    }

    // SwipeAdapter
    public class SwipeAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(View collection, int position) {
            ImageView view = new ImageView(getApplicationContext());
            try {
                view.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("image" + position + ".jpg")));
            } catch (IOException e1) {
            }
            ((ViewPager) collection).addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return num_photos;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }
    }

    // START
    void START() {
        items_enabled = true;
        t = 0;
        items = new ArrayList<ImageView>();
        items_rotations = new ArrayList<Integer>();
        ((ViewGroup) findViewById(R.id.game)).removeAllViews();

        // screen size
        screen_width = Math.min(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());
        screen_height = Math.max(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());

        // item size
        item_size = (int) Math.floor(Math.min((screen_width - (num_cols - 1) * spacing) / num_cols,
                (screen_height - (num_rows - 1) * spacing) / num_rows));

        // start position
        start_x = (screen_width - item_size * num_cols - (num_cols - 1) * spacing) / 2;
        start_y = (screen_height - item_size * num_rows - (num_rows - 1) * spacing) / 2;

        // bitmap
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open("image" + pager.getCurrentItem() + ".jpg"));
        } catch (IOException e) {
        }

        // add items
        float x_pos = 0;
        float y_pos = 0;
        for (int i = 0; i < num_rows * num_cols; i++) {
            ImageView item = new ImageView(this);
            item.setClickable(true);
            item.setLayoutParams(new LayoutParams(item_size, item_size));

            // image piece
            item.setImageBitmap(Bitmap.createBitmap(bitmap, (int) x_pos * bitmap.getWidth() / num_cols,
                    (int) y_pos * bitmap.getHeight() / num_rows, bitmap.getWidth() / num_cols, bitmap.getHeight() / num_rows));

            // position
            item.setX(start_x + x_pos * item_size + x_pos * spacing);
            item.setY(start_y + y_pos * item_size + y_pos * spacing);

            // random rotation
            items_rotations.add((int) (Math.round(Math.random() * 3) * 90));
            item.setRotation(items_rotations.get(i));

            ((ViewGroup) findViewById(R.id.game)).addView(item);
            items.add(item);

            x_pos++;
            if (x_pos == num_cols) {
                x_pos = 0;
                y_pos++;
            }

            // click listener
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (items_enabled) {
                        // animation list
                        final List<Animator> anim_list = new ArrayList<Animator>();

                        // scale down
                        anim = new AnimatorSet();
                        anim.playTogether(ObjectAnimator.ofFloat(v, "scaleX", 0.5f), ObjectAnimator.ofFloat(v, "scaleY", 0.5f));
                        anim_list.add(anim);

                        // rotate
                        final int current_item = items.indexOf(v);
                        items_rotations.set(current_item, items_rotations.get(current_item) + 90);
                        anim_list.add(ObjectAnimator.ofFloat(v, "rotation", items_rotations.get(current_item)));

                        // scale up
                        anim = new AnimatorSet();
                        anim.playTogether(ObjectAnimator.ofFloat(v, "scaleX", 1), ObjectAnimator.ofFloat(v, "scaleY", 1));
                        anim_list.add(anim);

                        // animation
                        anim = new AnimatorSet();
                        anim.playSequentially(anim_list);
                        anim.setDuration(50);
                        anim.addListener(new AnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                check_items();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                // sound
                                if (!sp.getBoolean("mute", false) && isForeground)
                                    sndpool.play(snd_move, 0.8f, 0.8f, 0, 0, 1);
                            }
                        });
                        anim.start();
                    }
                }
            });
        }

        show_section(R.id.game);
    }

    // check_items
    void check_items() {
        for (int i = 0; i < items.size(); i++)
            if ((float) items_rotations.get(i) / 360 != (float) Math.round(items_rotations.get(i) / 360))
                return;

        // all done
        items_enabled = false;
        h.removeCallbacks(TIMER);

        // save time
        if (!sp.contains(sp.getInt("mode", 0) + "time" + pager.getCurrentItem())
                || t < sp.getInt(sp.getInt("mode", 0) + "time" + pager.getCurrentItem(), 0)) {
            ed.putInt(sp.getInt("mode", 0) + "time" + pager.getCurrentItem(), t);
            ed.commit();
        }

        // show message
        ((TextView) findViewById(R.id.mess)).setText(R.string.completed);
        findViewById(R.id.mess).setVisibility(View.VISIBLE);

        // sound
        if (!sp.getBoolean("mute", false) && isForeground)
            sndpool.play(snd_info, 1f, 1f, 0, 0, 1);

        h.postDelayed(STOP, 3000);
    }

    // TIMER
    Runnable TIMER = new Runnable() {
        @Override
        public void run() {
            t++;
            h.postDelayed(TIMER, 1000);
        }
    };

    // STOP
    Runnable STOP = new Runnable() {
        @Override
        public void run() {
            // show result
            show_section(R.id.result);

            // show result
            ((TextView) findViewById(R.id.txt_result)).setText(getString(R.string.time) + " " + timeConvert(t));
            ((TextView) findViewById(R.id.txt_high_result)).setText(getString(R.string.best_time) + " "
                    + timeConvert(sp.getInt(sp.getInt("mode", 0) + "time" + pager.getCurrentItem(), 0)));

            // sound
            if (!sp.getBoolean("mute", false) && isForeground)
                sndpool.play(snd_result, 1f, 1f, 0, 0, 1);

            // AdMob Interstitial
            if (adMobInterstitial != null)
                if (adMobInterstitial.isLoaded())
                    adMobInterstitial.show(); // show
                else if (!adMobInterstitial.isLoading() && ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null)
                    adMobInterstitial.loadAd(adRequest); // load
        }
    };

    // onClick
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
            case R.id.btn_start2:

                if(AppLovinInterstitialAd.isAdReadyToDisplay(this)){
                    // An ad is available to display.  It's safe to call show.
                    AppLovinInterstitialAd.show(this);
                    Log.d("ads"," load");
                }
                else{
                    // No ad is available to display.  Perform failover logic...
                    Log.d("ads","cannot load");
                }
                START();
                break;
            case R.id.btn_exit:
                finish();
                break;
            case R.id.btn_sound:
                if (sp.getBoolean("mute", false)) {
                    ed.putBoolean("mute", false);
                    mp.setVolume(0.5f, 0.5f);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_mute));
                } else {
                    ed.putBoolean("mute", true);
                    mp.setVolume(0, 0);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
                }
                ed.commit();
                break;
            case R.id.btn_mode:
                if (sp.getInt("mode", 0) == 0) {
                    ed.putInt("mode", 1);
                    num_cols = 8;
                    num_rows = 10;
                    ((Button) findViewById(R.id.btn_mode)).setText(getString(R.string.btn_easy));
                } else {
                    ed.putInt("mode", 0);
                    num_cols = 4;
                    num_rows = 5;
                    ((Button) findViewById(R.id.btn_mode)).setText(getString(R.string.btn_hard));
                }
                ed.commit();
                break;
            case R.id.btn_home:
                show_section(R.id.main);
                break;
            case R.id.mess:
                if (items_enabled) {
                    h.postDelayed(TIMER, 1000);
                    findViewById(R.id.mess).setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        switch (current_section) {
            case R.id.main:
                super.onBackPressed();
                break;
            case R.id.result:
                show_section(R.id.main);
                break;
            case R.id.game:
                show_section(R.id.main);
                h.removeCallbacks(TIMER);
                h.removeCallbacks(STOP);

                if (anim != null)
                    anim.cancel();
                break;
        }
    }

    // show_section
    void show_section(int section) {
        current_section = section;
        findViewById(R.id.main).setVisibility(View.GONE);
        findViewById(R.id.game).setVisibility(View.GONE);
        findViewById(R.id.result).setVisibility(View.GONE);
        findViewById(current_section).setVisibility(View.VISIBLE);

        if (current_section == R.id.game) {
            ((TextView) findViewById(R.id.mess)).setText(R.string.faq);
            findViewById(R.id.mess).setVisibility(View.VISIBLE);
        } else
            findViewById(R.id.mess).setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(TIMER);
        h.removeCallbacks(STOP);
        mp.release();
        sndpool.release();

        // destroy AdMob
        if (adMobBanner != null) {
            adMobBanner.setAdListener(null);
            adMobBanner.destroyDrawingCache();
            adMobBanner.destroy();
            adMobBanner = null;
        }
        if (adMobInterstitial != null) {
            adMobInterstitial.setAdListener(null);
            adMobInterstitial = null;
        }
		adRequest = null;

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

        if (!sp.getBoolean("mute", false) && isForeground)
            mp.setVolume(0.5f, 0.5f);

        // timer
        if (current_section == R.id.game && items_enabled) {
            h.removeCallbacks(TIMER);
            h.postDelayed(TIMER, 1000);
        }
    }

    @Override
    protected void onPause() {
        isForeground = false;
        mp.setVolume(0, 0);
        h.removeCallbacks(TIMER);
        super.onPause();
    }

    // timeConvert
    String timeConvert(int t) {
        String str = "";
        int d, h, m, s;

        if (t / 86400 >= 1) {// if day exist
            d = t / 86400;
            str += d + ":";
        } else {
            d = 0;
        }

        t = t - (86400 * d);

        if (t / 3600 >= 1) {// if hour exist
            h = t / 3600;
            if (h < 10 && d > 0) {
                str += "0";
            }
            str += h + ":";
        } else {
            h = 0;
        }

        if ((t - h * 3600) / 60 >= 1) {// if minute exist
            m = (t - h * 3600) / 60;
            s = (t - h * 3600) - m * 60;
            if (m < 10 && h > 0) {
                str += "0";
            }
            str += m + ":";
        } else {
            m = 0;
            s = t - h * 3600;
        }

        if (s < 10 && m > 0) {
            str += "0";
        }
        str += s;

        return str;
    }

    // DpToPx
    float DpToPx(float dp) {
        return (dp * Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) / 500f);
    }

    // hide_navigation_bar
    @TargetApi(Build.VERSION_CODES.KITKAT)
    void hide_navigation_bar() {
        // fullscreen mode
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hide_navigation_bar();
    }

    // adMob
    void adMob() {
        if (getResources().getBoolean(R.bool.show_admob)) {
            // make AdMob request
            Builder builder = new AdRequest.Builder();
            if (getResources().getBoolean(R.bool.admob_test))
                builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice(
                        MD5(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)));
            adRequest = builder.build();

            // AdMob Interstitial
            adMobInterstitial = new InterstitialAd(Main.this);
            adMobInterstitial.setAdUnitId(getString(R.string.adMob_interstitial));
            adMobInterstitial.setAdListener(new AdListener() {
                public void onAdClosed() {
                    if (((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null)
                        adMobInterstitial.loadAd(adRequest);
                }
            });

            if (((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null) {
                // AdMob Banner
				adMobBanner = new AdView(Main.this);
                adMobBanner.setAdUnitId(getString(R.string.adMob_banner));
                adMobBanner.setAdSize(AdSize.SMART_BANNER);
                ((ViewGroup) findViewById(R.id.admob)).addView(adMobBanner);
                
				// load
				adMobBanner.loadAd(adRequest);
				adMobInterstitial.loadAd(adRequest);
            }
        }
    }

    // MD5
    String MD5(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i)
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            return sb.toString().toUpperCase(Locale.ENGLISH);
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}