/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.insensa.photoepsi.ui;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import fr.insensa.photoepsi.R;
import fr.insensa.photoepsi.utils.AccessDatabase;
import fr.insensa.photoepsi.utils.ImageFetcher;
import fr.insensa.photoepsi.utils.ImageWorker;
import fr.insensa.photoepsi.utils.Utils;


/**
 * fragment utilisé par ImageDetailActivity pour peupler chaque enfant du ViewPager
 */

public class ImageDetailFragment extends Fragment {
    private static final String IMAGE_DATA_URI = "extra_image_uri";
    private static final String IMAGE_DATA_KEY_ID = "extra_image_key_id";

    // key_id et path de l'image à afficher
    private String mImageUrl;
    private String mImageKeyId;

    // les Views qui contiendront les éléments à afficher
    private ImageView mImageView;
    private TextView datePhoto;
    private TextView titrePhoto;
    private TextView commentPhoto ;
    private TextView authorPhoto;
    private TextView latitudePhoto;
    private TextView longitudePhoto;
    private TextView orientationPhoto;


    // les attributs de chaque image
    private String titre;
    private String commentaire;
    private String date;
    private String author;
    private String orientation;
    private Double lattitude;
    private Double longitude;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl, String key_id) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_URI, imageUrl);
        args.putString(IMAGE_DATA_KEY_ID, key_id);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(String, String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_URI) : null;
        mImageKeyId = getArguments() != null ? getArguments().getString(IMAGE_DATA_KEY_ID) : null;
        getPicturesInfos();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the differents Views
        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        datePhoto = (TextView) v.findViewById(R.id.datePhoto);
        titrePhoto = (TextView) v.findViewById(R.id.titrePhoto);
        commentPhoto = (TextView) v.findViewById(R.id.commentPhoto);
        authorPhoto = (TextView) v.findViewById(R.id.authorPhoto);
        latitudePhoto = (TextView) v.findViewById(R.id.latitudePhoto);
        longitudePhoto  = (TextView) v.findViewById(R.id.longitudePhoto);
        orientationPhoto = (TextView) v.findViewById(R.id.orientationPhoto);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (ImageDetailActivity.class.isInstance(getActivity())) {
            // Use the parent activity to load the image asynchronously into the ImageView (so a single
            // cache can be used over all pages in the ViewPager
            ImageFetcher mImageFetcher = ((ImageDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(mImageUrl, mImageView);

            // intégration des attributs de l'image
            datePhoto.setText(date);
            titrePhoto.setText(titre);
            commentPhoto.setText(commentaire);
            latitudePhoto.setText(lattitude.toString());
            longitudePhoto.setText(longitude.toString());
            orientationPhoto.setText(orientation);
            authorPhoto.setText(author);
        }

        // Pass clicks on the ImageView to the parent activity to handle

        if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
        // pour affichage boite de dialogue d'envoi d'email au clik sur l'image
        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {showDialogEmail();}
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }

    // pour récupérer attributs de l'image à afficher
    private void getPicturesInfos() {
        AccessDatabase accessDatabase =  new AccessDatabase(getActivity().getApplicationContext());
        accessDatabase.getOneCursorValues(mImageKeyId);
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            titre =accessDatabase.getOneTitre();
            commentaire = accessDatabase.getOneCommentaire();
            date =accessDatabase.getOneDate();
            author =accessDatabase.getOneAuthor();
            orientation =accessDatabase.getOneOrientation();
            lattitude =accessDatabase.getOneLattitude();
            longitude =accessDatabase.getOneLongitude();

            // fermeture de la connexion
            accessDatabase.closeDatabase();
        }
        else {accessDatabase.closeDatabase();}
    }

    // fonction d'affichage de la boite de dialog d'envoi d'email
    void showDialogEmail() {
        DialogFragment newFragment = EmailDialog.newInstance(mImageUrl);
        newFragment.show(getActivity().getFragmentManager(), "dialog");
    }

}
