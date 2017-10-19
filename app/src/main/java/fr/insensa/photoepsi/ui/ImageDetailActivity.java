/**
 * Activité qui gère l'affichage d'une simple photo
 * avec ses attributs (titre, comentaire..)
 *
 */

package fr.insensa.photoepsi.ui;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import java.util.ArrayList;

import fr.insensa.photoepsi.BuildConfig;
import fr.insensa.photoepsi.R;
import fr.insensa.photoepsi.utils.AccessDatabase;
import fr.insensa.photoepsi.utils.ImageCache;
import fr.insensa.photoepsi.utils.ImageFetcher;
import fr.insensa.photoepsi.utils.Utils;

/**
 * Created by Jérôme on 24/04/2015.
 * Permet l'affichage de la vue détaillée de chaque image
 *
 */

public class ImageDetailActivity extends ActionBarActivity implements EmailDialog.EmailDialogListener {
    private static final String IMAGE_CACHE_DIR = "images";
    public static final String EXTRA_IMAGE = "extra_image";
    static final int EMAIL_REQUEST = 1;  // The request code for startActivityForResult(sendIntent, EMAIL_REQUEST)

    // tableau des URI et key-id des images
    private ArrayList<Integer> key_idArray = new ArrayList<>();
    private ArrayList<String> uri_pictureArray = new ArrayList<>();

    private ImageFetcher mImageFetcher;

    @TargetApi(VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Utils.enableStrictMode();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_pager);

        getPicturesUri();

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        final int longest = (height > width ? height : width) / 2;

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);

        // Set up ViewPager and backing adapter
       // mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), Images.imageUrls.length);
        ImagePagerAdapter mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), uri_pictureArray.size());
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
        mPager.setOffscreenPageLimit(2);

        // Set up activity to go full screen
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (Utils.hasHoneycomb()) {
            final ActionBar actionBar = getSupportActionBar();

            // Hide title text and set home as up
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Hide and show the ActionBar as the visibility changes
            mPager.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                actionBar.hide();
                            } else {
                                actionBar.show();
                            }
                        }
                    });

            // Start low profile mode and hide ActionBar
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();
        }

        // Set the current item based on the extra passed in to this activity
        final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
        if (extraCurrentItem != -1) {
            mPager.setCurrentItem(extraCurrentItem);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_grid_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_cache:
                mImageFetcher.clearCache();
                Toast.makeText(
                        this, R.string.clear_cache_complete_toast,Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called by the ViewPager child fragments to load images via the one ImageFetcher
     */
    public ImageFetcher getImageFetcher() {
        return mImageFetcher;
    }

    /**
     * The main adapter that backs the ViewPager. A subclass of FragmentStatePagerAdapter as there
     * could be a large number of items in the ViewPager and we don't want to retain them all in
     * memory at once but create/destroy them on the fly.
     */
    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private final int mSize;

        public ImagePagerAdapter(FragmentManager fm, int size) {
            super(fm);
            mSize = size;
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Fragment getItem(int position) {
         //   return ImageDetailFragment.newInstance(Images.imageUrls[position]);
            return ImageDetailFragment.newInstance(uri_pictureArray.get(position), key_idArray.get(position).toString());
        }
    }

    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)


    // pour récupérer uri et key_id des images à afficher dans le pager
    private void getPicturesUri() {
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getCursorValues();
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            for (int i = 0; i < countCursorLine; i++) {
                key_idArray.add(accessDatabase.getKeyId(i));
                uri_pictureArray.add(accessDatabase.getUri_picture(i));
            }
            accessDatabase.closeDatabase();
        }
        else {accessDatabase.closeDatabase();}
    }

    // implémentation des méthodes de EmailDialog.EmailDialogListener
    // appelé au click sur boite de dialogue d'envoi de l'Email

    @Override
    public void doPositiveClick(DialogFragment dialogFragment, String path) {
        // the result of the calling activity
        // Create the text message with a string
        Uri attachment = Uri.parse("file://"+path);
        Log.i("jerome", "positive Click, attachement : " + attachment);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "photo");
        sendIntent.putExtra(Intent.EXTRA_TEXT, "sympa la photo");
        sendIntent.putExtra(Intent.EXTRA_STREAM, attachment);


// Verify that the intent will resolve to an activity
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(sendIntent, EMAIL_REQUEST);
        }
    }

    @Override
    public void doNegativeClick(DialogFragment dialogFragment, String path) {
        Log.i("jerome", "abandon email");

    }

    /** appelé lorsque l'activité d'envoi d'Email a été effectuée
     * les resultCode renvoyé par les applications d'envoi d'Email ne sont pas homogènes
     * et ne semblent pas respecter les standards (EMAIL_REQUEST).
     * donc les résultats renvoyés sont faux tant que ces applis n'intègrent pas ces éléments
     * pour s'assurer d'avoir un resultCode adéquat, il faudrait coder
     * sa propre api d'envoi d'email.
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == EMAIL_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Email envoyé", Toast.LENGTH_LONG).show();
            }
            // si abandonné
            else if (resultCode==RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Email abandoné", Toast.LENGTH_LONG).show();
            }
            else Toast.makeText(getApplicationContext(), "erreur Email", Toast.LENGTH_LONG).show();
        }
    }
}
