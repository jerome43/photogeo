package fr.insensa.photoepsi.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import fr.insensa.photoepsi.R;
import fr.insensa.photoepsi.utils.AccessDatabase;
import fr.insensa.photoepsi.utils.CameraPreview;
import fr.insensa.photoepsi.utils.MapsGestion;


public class MainActivity extends ActionBarActivity implements PhotoUIInputValuesFragment.OnPhotoUIInputValuesFragmentInteractionListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {


    // variables de LOG
    private static final String TAG = "Log jérôme";

    // variables pour utiliser la camera
    private static final String STATE_DISPLAY_CAMERA = "state displaying camera";
    private Uri outputFileUri; // chemin d'acceès à la dernière photo prise
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private Location locationPicture; // la localisation de la dernière photo prise
    private Float orientationPicture; // l'orietation de la dernière photo prise
    private Date datePicture; // la date de la dernière photo prise


    // les options par défaut de création de la map
    private GoogleMapOptions options = new GoogleMapOptions();

    // pour indiquer les différentes vues ou fragments affichés
    private boolean mapFragmentIsDisplayed = false;
    private boolean photoUIInputFragmentIsDisplayed = false;
    private boolean fragmentMainIsDisplayed = false;
    private boolean cameraPreviewIsDiplayed = false;
    private Menu myMenu;// pour récupérer le menu


    // VARIABLES DE GEOLOCALISATION
    private Location mCurrentLocation;// position de l'utilisateur
    private GoogleApiClient mGoogleApiClient; // l'api de géolocalisation
    private String mLastUpdateTime;// pour récupérer l'heure de la dernière MAJ de la géolocalisation
    private Location mLastLocation;// pour récupérer la dernière position
    private boolean mRequestingLocationUpdates = false; // pour savoir si le service de géolocalisation est allumé
    // les attributs globaux du service de géolocalisation (intervalle de rafraichissement, précision)
    private LocationRequest mLocationRequest;
    // pour récupérer infos dans Bundle
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY = "LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "LAST_UPDATED_TIME_STRING_KEY";
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";


    // variables pour calculer l'ORIENTATION
    /**
     * Tableau qui récupère les enregistrements de l'accéléromètre et du magnétomètre
     */
    private float[] accelerometerValues;
    private float[] magneticFieldValues;
    // sensor manager qui gère l'accéléromètre et le magnétomètre, utile pour récupérer l'orientation.
    private SensorManager sensorManager;
    // création de l'écouteur d'accéléromètre
    final SensorEventListener myAccelerometerListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    // création de l'écouteur du champ magnétique terresre
    final SensorEventListener myMagneticFieldListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            fragmentMainIsDisplayed = true;
        } else {
            updateValuesFromBundle(savedInstanceState);
            mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        }


        // accès au gestionnaire de localisation
        // on indique qu'on veut activer l'update de la géolocalisatin
        mRequestingLocationUpdates = true;
        buildGoogleApiClient(); // To connect to the API, you need to create an instance of the Google Play services API client.
        createLocationRequest();

        // lancement du sensorManager, utile pour le calcul de l'orientation
        String service_name = Context.SENSOR_SERVICE;
        sensorManager = (SensorManager) getSystemService(service_name);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "api connect");
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        // enregistrement des écouteurs d'orientation
        registerAccelerometerAndMagnetometer(sensorManager);

        // permet de relancer la camera si elle était affichée
        // et détruite lors d'une rotation de l'appareil
        if (cameraPreviewIsDiplayed) {
            setCameraInstance();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        stopLocationUpdates(); // arret de la MAJ de la géolocalisation
        // désenrregistrement des écouteurs utilisés pour l'orientation
        sensorManager.unregisterListener(this);
        sensorManager.unregisterListener(myAccelerometerListener);
        sensorManager.unregisterListener(myMagneticFieldListener);
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        savedInstanceState.putBoolean(STATE_DISPLAY_CAMERA, cameraPreviewIsDiplayed);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState.keySet().contains(STATE_DISPLAY_CAMERA)) {
                cameraPreviewIsDiplayed = savedInstanceState.getBoolean(
                        STATE_DISPLAY_CAMERA);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        myMenu = menu;
        if (cameraPreviewIsDiplayed) {
            myMenu.findItem(R.id.photo).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.photo) {
            releaseCamera();
            myMenu.findItem(R.id.photo).setVisible(false);
            displayFragmentCameraPreview();
            return true;
        } else if (id == R.id.listphoto) {
            releaseCamera();
            displayImageGridActivities();
            return true;
        } else if (id == R.id.map) {
            releaseCamera();
            // verification préalable de la disponiblité du réseau
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                // on rend visible le bouton d'affichage de la camera
                myMenu.findItem(R.id.photo).setVisible(true);
                // et on rend invisible le bouton d'accès à la map
                myMenu.findItem(R.id.map).setVisible(false);
                displayMapFragment();
                return true;
            } else
                Toast.makeText(getApplicationContext(), "Aucun réseau disponible, veuillez activez le wifi ou la connexion de données", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     * utilisé pour la vue par défaut au démarrage de l'application
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }


// CAMERA ET PRISE DES PHOTOS

    /** pour afficher le fragment de prévisualisation de la camera
     */
    private void displayFragmentCameraPreview() {
        // affichage du menu map si nécessaire
        myMenu.findItem(R.id.map).setVisible(true);
        if (mapFragmentIsDisplayed || photoUIInputFragmentIsDisplayed || fragmentMainIsDisplayed) {
            PlaceholderFragmentCamera mPlaceholderFragmentCamera = new PlaceholderFragmentCamera();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.container, mPlaceholderFragmentCamera);
            fragmentTransaction.commit();
            FragmentManager fragmentManager = getFragmentManager();
            // permet d'attendre que le commit soit terminé avant de continuer à exécuter le code
            fragmentManager.executePendingTransactions();

        }
        setCameraInstance();
        fragmentMainIsDisplayed = false;
        photoUIInputFragmentIsDisplayed = false;
        mapFragmentIsDisplayed = false;
        cameraPreviewIsDiplayed = true;
    }

    /**
     * A placeholder fragment containing a simple view.
     * utilisé pour la caméra
     */
    public static class PlaceholderFragmentCamera extends Fragment {

        public PlaceholderFragmentCamera() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_camera_preview, container, false);
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "PhotoEpsi");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("PhotoEpsi", "failed to create directory");
                return null;
            }
        }
        // Create a media file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_PHOTO_EPSI" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    public void setCameraInstance() {
    // Create an instance of Camera
    mCamera=getCameraInstance();
    // Create our Preview view and set it as the content of our activity.
    CameraPreview mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);


    // In order to retrieve a picture, use the Camera.displayFragmentCameraPreview() method.
    // This method takes three parameters which receive data from the camera.
    // In order to receive data in a JPEG format, you must implement an Camera.PictureCallback
    // interface to receive the image data and write it to a file.


    // Add a listener to the Capture button
    Button captureButton = (Button) findViewById(R.id.button_capture);
    captureButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
            // get an image from the camera
            mCamera.takePicture(null, null, mPicture);
            // enregistrement des variables de géolocalisation, date et orientation au moment de la prise de photo
            locationPicture = mCurrentLocation;
            float[] orientationPictureTab = calculateOrientation();
            orientationPicture = orientationPictureTab[0];
            datePicture = new Date();
            }
        });
    }

    // appelé quand la photo a été prise
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        outputFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }

        displayPhotoUIInputValuesFragment();

        BitmapFactory.Options factoryOptions = new
                BitmapFactory.Options();

        factoryOptions.inSampleSize = 2;

        Bitmap bitmap =
                BitmapFactory.decodeFile(outputFileUri.getPath(),
                        factoryOptions);

        setPhotoUIInputValuesFragment(bitmap);
    }
};

    // pour mettre à jour la photo prise dans l'interface de validation de la photo
    private void setPhotoUIInputValuesFragment(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onPhotoUIInputValuesFragmentInteraction(Uri uri) {
        // au besoin pour communiquer avec le fragment PhotoUIInputValuesFragment
    }

    // chargement et affichage du fragment de saisie des infos relatives à la photo
    protected void displayPhotoUIInputValuesFragment() {
        // on quitte d'abord la camera
        releaseCamera();
        PhotoUIInputValuesFragment photoUIInputValuesFragment = PhotoUIInputValuesFragment.newInstance("param1", "param2");
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, photoUIInputValuesFragment);
        fragmentTransaction.commit();
        FragmentManager fragmentManager = getFragmentManager();
        // permet d'attendre que le commit soit terminé avant de continuer à exécuter le code
        fragmentManager.executePendingTransactions();
        photoUIInputFragmentIsDisplayed = true;
        fragmentMainIsDisplayed = false;
        mapFragmentIsDisplayed = false;
        cameraPreviewIsDiplayed = false;
        // on réaffiche l'item camera sur le menu
        myMenu.findItem(R.id.photo).setVisible(true);
    }


// onClick button save PhotoUIInputValuesFragment
    public void enregistrerPhoto(View button) {
        // Do click handling here
        final EditText editComment = (EditText) findViewById(R.id.editComment);
        final EditText editTitre = (EditText) findViewById(R.id.editTitre);
        final EditText editAuthor = (EditText) findViewById(R.id.editAuthor);
        String comment = editComment.getText().toString();
        String titre = editTitre.getText().toString();
        String author = editAuthor.getText().toString();
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.addNewPicture(titre, locationPicture.toString(), comment, outputFileUri.getEncodedPath() , datePicture.toString(), author, orientationPicture.toString(), locationPicture.getLatitude(), locationPicture.getLongitude());
        Log.i("infos : ", outputFileUri.getEncodedPath() + " - " + outputFileUri.getLastPathSegment());
        // récupération du KeyId de la photo qu'on vient d'enregistrer (la dernière en BD)
        accessDatabase.getLastCursorValue();
        String key_id = accessDatabase.getOneKeyId().toString();
        accessDatabase.closeDatabase();
        Toast.makeText(getApplicationContext(), "photo enregistrée", Toast.LENGTH_LONG).show();

        // lancement de la vue détaillée

        Intent intent = new Intent(getApplicationContext(), PhotoViewFromMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_TEXT, key_id);
        startActivity(intent);

    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    // pour quiter proprement la camera
    private void releaseMediaRecorder(){

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }
    // pour quiter proprement la camera
    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    // FIN CAMERA ET PRISE DE PHOTOS



    // ---------------------   AFFICHAGE MAP --------------------------

    // fonction pour affichage de map-fragment
    protected void displayMapFragment() {
        MapFragment mMapFragment = MapFragment.newInstance(options);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, mMapFragment);
        fragmentTransaction.commit();
        FragmentManager fragmentManager = getFragmentManager();
        // permet d'attendre que le commit soit terminé avant de continuer à exécuter le code
        fragmentManager.executePendingTransactions();
        // on récupère la carte pour pouvoir travailler dessus
        //GoogleMap mMap = mMapFragment.getMap();
        mMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                MapsGestion mapsGestion = new MapsGestion(googleMap, getApplicationContext());
                // Actualisation de la position
                if (mLastLocation!=null) {
                    mapsGestion.setUpMap(mLastLocation);
                    mapsGestion.afficherMaPosition(mLastLocation);
                    mapsGestion.afficherMarkerPhoto(getLatitude(), getLongitude(), getTitre(), getKeyId());
                }
                else {
                    mapsGestion.setUpMapIfNeeded();
                }

                // on indique que la MapFragment est affiché
                fragmentMainIsDisplayed = false;
                photoUIInputFragmentIsDisplayed = false;
                mapFragmentIsDisplayed = true;
                cameraPreviewIsDiplayed = false;
                // affichage du menu camera
                myMenu.findItem(R.id.photo).setVisible(true);

            }
        });

    }



    // -------- GEOLOCALISATION ET API GOOGLE CLIENT--------------

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.i(TAG, "googleApiClient created");
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Log.i(TAG, "mLocationRequest created");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "google location api connected");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            mCurrentLocation=mLastLocation;        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            Toast.makeText(getApplicationContext(), "impossible d'utiliser les services Google, veuillez réessayer plus tard", Toast.LENGTH_LONG).show();

        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    protected void startLocationUpdates() {
        Log.i(TAG, "start locatiojn updtate");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "location updated");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    }


    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mRequestingLocationUpdates=false;
        }
    }



    // The rest of this code is all about building the error dialog
    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity)getActivity()).onDialogDismissed();
        }
    }


    // NOUVELLE ACTIVITE POUR AFFICHAGE DE LA LISTE DES PHOTOS
    protected void displayImageGridActivities() {
        Intent intent = new Intent(getApplicationContext(), ImageGridActivity.class);
        startActivity(intent);
    }


    /*------- FONCTIONS POUR L'ORIENTATION -----------------*/

    // fonction pour enregistrer les écouteurs accéléromètre et champs magnétique terrestre
    private void registerAccelerometerAndMagnetometer(SensorManager sm) {
        Sensor aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mfSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(myAccelerometerListener,
                aSensor,
                SensorManager.SENSOR_DELAY_UI);

        sm.registerListener(myMagneticFieldListener,
                mfSensor,
                SensorManager.SENSOR_DELAY_UI);
    }

    // fonctions qui retourne un tableau avec trois valeurs standards de l'orientation
    // aymuth, itch et Roll
    private float[] calculateOrientation() {
        /**
         * Listing 12-6: Finding the current orientation using the accelerometer and magnetometer
         */
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null,
                accelerometerValues,
                magneticFieldValues);
        SensorManager.getOrientation(R, values);

        // Convert from radians to degrees if preferred.
        values[0] = (float) Math.toDegrees(values[0]); // Azimuth
        values[1] = (float) Math.toDegrees(values[1]); // Pitch
        values[2] = (float) Math.toDegrees(values[2]); // Roll
        return values;
    }

    private void calculateRemappedOrientation() {
        float[] inR = new float[9];
        float[] outR = new float[9];
        float[] values = new float[3];

        /**
         * Listing 12-7: Remapping the orientation reference frame based on the natural orientation of the device
         */
        // Determine the current orientation relative to the natural orientation
        String windoSrvc = Context.WINDOW_SERVICE;
        WindowManager wm = ((WindowManager) getSystemService(windoSrvc));
        Display display = wm.getDefaultDisplay();
        int rotation = display.getRotation();

        int x_axis = SensorManager.AXIS_X;
        int y_axis = SensorManager.AXIS_Y;

        switch (rotation) {
            case (Surface.ROTATION_0):
                break;
            case (Surface.ROTATION_90):
                x_axis = SensorManager.AXIS_Y;
                y_axis = SensorManager.AXIS_MINUS_X;
                break;
            case (Surface.ROTATION_180):
                y_axis = SensorManager.AXIS_MINUS_Y;
                break;
            case (Surface.ROTATION_270):
                x_axis = SensorManager.AXIS_MINUS_Y;
                y_axis = SensorManager.AXIS_X;
                break;
            default:
                break;
        }

        SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);

        // Obtain the new, remapped, orientation values.
        SensorManager.getOrientation(outR, values);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // ----------------- RECUPERATION DE DONNEES EN BD ---------------------------------

    protected ArrayList<Double> getLatitude() {
        ArrayList<Double> lattitudeArray = new ArrayList<>();
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getCursorValues();
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            for (int i = 0; i < countCursorLine; i++) {
                System.out.println("boucle read data base");
                lattitudeArray.add(accessDatabase.getLattitude(i));
            }
            accessDatabase.closeDatabase();
        }
        else {accessDatabase.closeDatabase();}
        return lattitudeArray;
    }

    protected ArrayList<Double> getLongitude() {
        ArrayList<Double> longitudeArray = new ArrayList<>();
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getCursorValues();
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            for (int i = 0; i < countCursorLine; i++) {
                longitudeArray.add(accessDatabase.getLongitude(i));
            }
            accessDatabase.closeDatabase();
        }
        else {accessDatabase.closeDatabase();}
        return longitudeArray;
    }

    protected ArrayList<Integer> getKeyId() {
        ArrayList<Integer> keyIdArray = new ArrayList<>();
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getCursorValues();
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            for (int i = 0; i < countCursorLine; i++) {
                keyIdArray.add(accessDatabase.getKeyId(i));
            }
            accessDatabase.closeDatabase();
        }
        else {accessDatabase.closeDatabase();}
        return keyIdArray;
    }

    protected ArrayList<String> getTitre() {
        ArrayList<String> titreArray = new ArrayList<>();
        AccessDatabase accessDatabase =  new AccessDatabase(getApplicationContext());
        accessDatabase.getCursorValues();
        int countCursorLine= accessDatabase.getCountCursorLine();
        if (countCursorLine != 0) {
            for (int i = 0; i < countCursorLine; i++) {
                titreArray.add(accessDatabase.getTitre(i));
            }
            accessDatabase.closeDatabase();
        }
        return titreArray;
    }
}