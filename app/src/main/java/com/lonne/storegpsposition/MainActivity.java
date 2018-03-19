package com.lonne.storegpsposition;

/************************************
 *    imports
 ************************************/

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.Locale;

/************************************
 *    Class
 ************************************/

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private static final LatLng TIMMERMANSGATAN = new LatLng(59.31316, 18.06295);
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    FirebaseDatabase database;
    DatabaseReference myRef_lat;
    DatabaseReference myRef_long;
    boolean google_maps_access_granted = false;
    int pos_count = 0;
    String pos_long = "";
    String pos_lat = "";
    Double lat_value = 0.0;
    Double long_value = 0.0;
    Marker myMarker = null;

    //************************************
    //    onCreate
    //************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Check app permissions
        checkPermissions();

        // Fetch application data from cloud
        setupFirebaseConnection();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //************************************
    //    modulo
    //************************************
    public int modulo(int m, int n) {
        int mod = m % n;
        Log.d("DEBUG", "MODULO m:" + m + " n:" + n + " mod:" + mod);
        return (mod < 0) ? mod + n : mod;
    }

    //************************************
    //    setupFirebaseConnection
    //************************************
    private void setupFirebaseConnection() {

        // If google map permission is not granted
        //if (google_maps_access_granted == false) {
        //    Log.d("DEBUG", "Permission to DATABASE not granted");
        //    lat_value = 59.31316;
        //    long_value = 18.06292;
        //    return;
        //}

        // TODO: Add check that we can access firebase !!

        database = FirebaseDatabase.getInstance();
        myRef_lat = database.getReference("pos_lat");

        myRef_lat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                pos_lat = dataSnapshot.getValue(String.class);
                lat_value = Double.parseDouble(pos_lat);
                Log.d("DEBUG", "LAT Value is: " + pos_lat + " index :" + pos_count);
                pos_count++;

                if (modulo(pos_count, 2) == 0) {
                    Log.d("DEBUG", "Set new marker when firebase updated or fetched first time");
                    setNewMarker(mMap, new LatLng(lat_value, long_value));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("DEBUG", "Failed to read value");
            }
        });
        myRef_long = database.getReference("pos_long");
        myRef_long.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                pos_long = dataSnapshot.getValue(String.class);
                long_value = Double.parseDouble(pos_long);
                Log.d("DEBUG", "LONG Value is: " + pos_long + " index :" + pos_count);
                pos_count++;

                if (modulo(pos_count, 2) == 0) {
                    Log.d("DEBUG", "Set new marker when firebase updated or fetched first time");
                    setNewMarker(mMap, new LatLng(lat_value, long_value));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //************************************
    //    checkPermissions
    //************************************
    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        google_maps_access_granted = true;
        return true;
    }

    //************************************
    //    updateFirebaseValue
    //************************************
    private boolean updateFirebaseValue(String pos_lat, String pos_long) {
        // If permission is not granted return
        //if (!fireBase_access_granted) {
        //    Log.d("DEBUG", "Permission to DATABASE not granted");
        //    return false;
        //}

        // Obtain the FirebaseAnalytics instance.
        database = FirebaseDatabase.getInstance();

        // TODO: Add check that access is ok
        myRef_lat = database.getReference("pos_lat");
        myRef_lat.setValue(pos_lat);
        myRef_long = database.getReference("pos_long");
        myRef_long.setValue(pos_long);
        return true;
    }

    //************************************
    //    enableMyLocation
    //************************************
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    //************************************
    //    setMapLongClick
    //************************************
    private void setMapLongClick(final GoogleMap map) {
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                DecimalFormat myFormatter = new DecimalFormat("00.00000");

                // Update cloud values
                if (updateFirebaseValue(myFormatter.format(latLng.latitude), myFormatter.format(latLng.longitude))) {
                    setNewMarker(map, latLng);
                }
            }
        });
    }

    //************************************
    //    setNewMarker
    //************************************
    private void setNewMarker(final GoogleMap map, LatLng latlng) {

        // Remove old if any exist
        if (myMarker != null) {
            Log.d("DEBUG", "Remove old position");
            myMarker.remove();
        }
        String snippet = String.format(Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latlng.latitude,
                latlng.longitude);

        myMarker = map.addMarker(new MarkerOptions().position(latlng).title(getString(R.string.dropped_pin)).snippet(snippet));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15));

        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());

        // Construct a CameraPosition focusing on a pos and animate the camera to that position.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latlng)         // Sets the center of the map
                .zoom(15)               // Sets the zoom
                .bearing(0)             // Sets the orientation of the camera to east
                .build();               // Creates a CameraPosition
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    //************************************
    //    gottoMarker
    //************************************
    private void gotoMarker() {

        LatLng pos = new LatLng(lat_value, long_value);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));

        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());

        // Construct a CameraPosition focusing on a position and animate the camera to that position.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(pos)   // Sets the center of the map
                .zoom(15)      // Sets the zoom
                .bearing(0)    // Sets the orientation of the camera to east
                .build();      // Creates a CameraPosition
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    //************************************
    //  onMapReady
    //   The callback is triggered when the map is ready.
    //************************************
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Hide the zoom controls as the button panel will cover it.
        mMap.getUiSettings().setZoomControlsEnabled(false);

        // Sets the map type to be "hybrid"
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TIMMERMANSGATAN, 15));
        Log.d("DEBUG", "Set initial map position");

        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        boolean TRUE = true;

        //if (checkPermissions() == false) {

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        //    return;
        //}
        //mMap.setMyLocationEnabled(true); // setMyLocationEnabled(true);
        //mMap.setOnMyLocationButtonClickListener(this);
        //mMap.setOnMyLocationClickListener(this);
        setMapLongClick(mMap);

        if (google_maps_access_granted)
            enableMyLocation();
    }

    //************************************
    // onBackPressed
    //************************************
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //************************************
    //  onCreateOptionsMenu
    //************************************
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //************************************
    //  onOptionsItemSelected
    //************************************
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //************************************
    //  onNavigationItemSelected
    //
    // Handles events generated from navigation view
    // (popup menu)
    //
    //************************************
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.find_pos:
                gotoMarker();
                break;
            case R.id.normal_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.hybrid_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.satellite_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.terrain_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case R.id.nav_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
