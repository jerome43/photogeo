package fr.insensa.photoepsi.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import fr.insensa.photoepsi.ui.PhotoViewFromMapActivity;
import fr.insensa.photoepsi.R;

/**
 * Created by Jérôme on 16/03/2015.
 */
public class MapsGestion implements GoogleMap.OnMarkerClickListener {
    // la map
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    // le markeur de la position de l'utilisateur
    private Marker markerMaPosition;
    // récupération du context;
    private Context context;
    // constructeur par défaut
    public MapsGestion(GoogleMap map, Context context) {
        mMap = map;
        this.context = context;
    }


    // mettre à jour la map
    public void setUpMap(Location location) {
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            afficherMaPosition(location);
            mMap.setMapType(2);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    public void afficherMaPosition(Location location) {
        if (markerMaPosition!=null) {markerMaPosition.remove();}
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            markerMaPosition = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Ma position").icon(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_action_place)));
        }
    }

    public void afficherMarkerPhoto(ArrayList<Double> latitudeArray, ArrayList<Double> longitudeArray, ArrayList<String> titreArray, ArrayList<Integer> keyIdArray) {
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            for (int i = 0, c = latitudeArray.size(); i < c; i++) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(latitudeArray.get(i), longitudeArray.get(i))).title(titreArray.get(i)).snippet(keyIdArray.get(i).toString()).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.ic_action_picture)));
            }
            mMap.setOnMarkerClickListener(this);
        }

    }

    public void setUpMapIfNeeded() {
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
            }
    }

// pour click personnalisés sur les markers
    @Override
    public boolean onMarkerClick(Marker marker) {
        // test la présence d'un snippet
        if (marker.getSnippet()!=null) {
            Intent intent = new Intent(context, PhotoViewFromMapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, marker.getSnippet());
            context.startActivity(intent);
            Log.i("jérôme", marker.getTitle());
        }
        return false;
    }
}
