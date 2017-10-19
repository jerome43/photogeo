package fr.insensa.photoepsi.ui;

//import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import fr.insensa.photoepsi.R;
import fr.insensa.photoepsi.utils.AccessDatabase;

/**
 * Created by jerome on 08/04/15.
 */
public class PhotoViewFromMapActivity extends ActionBarActivity implements EmailDialog.EmailDialogListener {

    static final int EMAIL_REQUEST = 1;  // The request code

    // tableau des attributs des images
    private String key_id;
    private String titre;
    private String commentaire;
  //  private String localisation;
    private String uri_picture;
    private String date;
    private String author;
    private String orientation;
    private Double lattitude;
    private Double longitude;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_fragment);
        // affichage du bouton de retour en arrière
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // pour récupérer intent
        Intent intent = getIntent();

        // si le contenu des extras de l'intent n'est pas vide
        if (intent.getExtras()!=null) {
            key_id = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (key_id!=null) {
                Log.i("info", key_id);
                // récupération des attributs de l'image
                getPicturesInfos();
                // récupération et mise à jours des vues des attributs de l'image
                TextView datePhoto = (TextView) findViewById(R.id.datePhoto);
                datePhoto.setText(date);
                TextView titrePhoto = (TextView) findViewById(R.id.titrePhoto);
                titrePhoto.setText(titre);
                TextView commentPhoto = (TextView) findViewById(R.id.commentPhoto);
                commentPhoto.setText(commentaire);
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setCropToPadding(true);
                imageView.setImageURI(Uri.parse(uri_picture));
                TextView authorPhoto = (TextView) findViewById(R.id.authorPhoto);
                authorPhoto.setText(author);
                TextView latitudePhoto = (TextView) findViewById(R.id.latitudePhoto);
                latitudePhoto.setText(lattitude.toString());
                TextView longitudePhoto  = (TextView) findViewById(R.id.longitudePhoto);
                longitudePhoto.setText(longitude.toString());
                TextView orientationPhoto = (TextView) findViewById(R.id.orientationPhoto);
                orientationPhoto.setText(orientation);

                // écouteur au click sur Photo pour afficher boite de dialogue mail
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {showDialogEmail();}
                });
            }
            else Log.i("Info : ", "key_id intent null");
        }
    }


    // pour récupérer infos des images à afficher dans la grille
    private void getPicturesInfos() {
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getOneCursorValues(key_id);
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
                titre =accessDatabase.getOneTitre();
               // localisation = accessDatabase.getOneLocalisation();
                commentaire = accessDatabase.getOneCommentaire();
                uri_picture =accessDatabase.getOneUri_picture();
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

    // pour afficher la boite de dialogue Email
    void showDialogEmail() {
        DialogFragment newFragment = EmailDialog.newInstance(uri_picture);
        newFragment.show(getFragmentManager(), "dialog");
    }

    // callbacks aux cliks sur boite de dialogue mail
    @Override
    public void doPositiveClick(DialogFragment dialogFragment, String path) {
        // the result of the calling activity

        // Create the text message with a string
        Uri attachment = Uri.parse("file://"+uri_picture);
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
