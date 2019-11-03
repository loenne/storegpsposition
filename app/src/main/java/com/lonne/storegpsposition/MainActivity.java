package com.lonne.storegpsposition;

/************************************
 *    imports
 ************************************/

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

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
    int pos_count = 0;
    String pos_long = "";
    String startup_states = "";
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
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        startup_states = "1:";
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        startup_states = startup_states + "2:";

        // Check app permissions
//        if (checkPermission() == false) {
//            startup_states = startup_states + "3:";
//            fetchPermission();
//        }

        // Fetch application data from cloud
        setupFirebaseConnection();
        startup_states = startup_states + "4:";

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startup_states = startup_states + "5:";
    }

    private void showPopup(String text1, String text2) {
        //We need to get the instance of the LayoutInflater, use the context of this activity
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Inflate the view from a predefined XML layout (no need for root id, using entire layout)
        View layout = inflater.inflate(R.layout.popup, null);

        ((TextView) layout.findViewById(R.id.popup_info1)).setText(text1);
        ((TextView) layout.findViewById(R.id.popup_info2)).setText(text2);

        //Get the devices screen density to calculate correct pixel sizes
        float density = MainActivity.this.getResources().getDisplayMetrics().density;
        // create a focusable PopupWindow with the given layout and correct size
        final PopupWindow pw = new PopupWindow(layout, (int) density * 240, (int) density * 285, true);

        //Button to close the pop-up
        ((Button) layout.findViewById(R.id.popup_close)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pw.dismiss();
            }
        });

        //Set up touch closing outside of pop-up
        pw.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        pw.setTouchInterceptor(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    pw.dismiss();
                    return true;
                }
                return false;
            }
        });
        pw.setOutsideTouchable(true);

        // display the pop-up in the center
        pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
    }

    //************************************
    //    modulo
    //************************************
    public int modulo(int m, int n) {
        int mod = m % n;
        //Log.d("DEBUG", "MODULO m:" + m + " n:" + n + " mod:" + mod);
        return (mod < 0) ? mod + n : mod;
    }

    //************************************
    //    setupFirebaseConnection
    //************************************
    private void setupFirebaseConnection() {

        // TODO: Add check that we can access firebase !!

        database = FirebaseDatabase.getInstance();
        myRef_lat = database.getReference("pos_lat");

        myRef_lat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                pos_lat = dataSnapshot.getValue(String.class);
                lat_value = Double.parseDouble(pos_lat);
                Log.d("DEBUG", "pos_lat value is: " + pos_lat + " index :" + pos_count);
                pos_count++;

                // Only update marker when we have received both values !!
                if (modulo(pos_count, 2) == 0) {
                    //Log.d("DEBUG", "onDataChange: Set new marker when firebase updated or fetched first time");
                    //Log.d("DEBUG", "onDataChange: lat_value:" + lat_value + " long_value: " + long_value);
                    setNewMarker(mMap, new LatLng(lat_value, long_value));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Log.d("DEBUG", "Failed to read value");
                showPopup("FAIL 1", "onCancelled: Failed to read value");
            }
        });
        myRef_long = database.getReference("pos_long");
        myRef_long.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                pos_long = dataSnapshot.getValue(String.class);
                //Log.d("DEBUG", "pos_long value is: " + pos_long + " index :" + pos_count);
                long_value = Double.parseDouble(pos_long);
                pos_count++;

                // Only update marker when we have received both values !!
                if (modulo(pos_count, 2) == 0) {
                    //Log.d("DEBUG", "Set new marker when firebase updated or fetched first time");
                    //Log.d("DEBUG", "onDataChange: lat_value:" + lat_value + " long_value: " + long_value);
                    setNewMarker(mMap, new LatLng(lat_value, long_value));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showPopup("FAIL 2", "onCancelled: Failed to read value");
            }
        });
    }

    //************************************
    //    checkPermissions
    //************************************
    private boolean checkPermission() {

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) +
                ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            //Log.d("DEBUG", "checkPermission: FINE OR COARSE LOCATION access granted");
            //google_maps_access_granted = true;
            return true;
        }
        //Log.d("DEBUG", "checkPermission: FINE OR COARSE LOCATION access not granted");
        return false;
    }

    //************************************
    //    fetchPermissions
    //************************************
    private void fetchPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale
                (MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            //Log.d("DEBUG", "fetchPermission: Check if we shouldshowrequestpermission for FINE LOCATION");

            Snackbar.make(findViewById(android.R.id.content),
                    "Please Grant Permissions",
                    Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        }
                    }).show();
        } else {
            //Log.d("DEBUG", "fetchPermission: Ask for PERMISSION FINE LOCATION");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    //************************************
    //    updateFirebaseValue
    //************************************
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        //Log.d("DEBUG", "onRequestPermissionsResult: Start : request_Code: " + requestCode + ":" + REQUEST_LOCATION_PERMISSION );
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //Log.d("DEBUG", "FINE OR COARSE LOCATION access has been granted by user");
                    enableMyLocation();
                } else {
                    //Log.d("DEBUG", "FINE OR COARSE LOCATION access has not been granted by user");
                    // TODO: Should we do something ?
                }
                //
//                    Snackbar.make(findViewById(android.R.id.content), "Enable Permissions from settings",
//                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
//                            new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Intent intent = new Intent();
//                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
//                                    intent.setData(Uri.parse("package:" + getPackageName()));
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                                    startActivity(intent);
//                                }
//                            }).show();
//                }
//                return;
            }
        }
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

        if (checkPermission() == true) {
            mMap.setMyLocationEnabled(true);

            //Log.d("DEBUG", "enableMyLocation since map access granted");
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            //Log.d("DEBUG", "FINE_LOCATION asked for permission");
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
            //Log.d("DEBUG", "Remove old map position");
            myMarker.remove();
        }
        String snippet = String.format(Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latlng.latitude,
                latlng.longitude);

        //Log.d("DEBUG", "The position to mark in google maps: lat :" + latlng.latitude + " long :" + latlng.longitude);

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
        //Log.d("DEBUG", "onMapReady: Set initial map position");

        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        boolean TRUE = true;

        // Check app permissions
//        if (checkPermission() == false) {
//            startup_states = startup_states + "3:";
//            fetchPermission();
//        }

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
                //showPopup("SUCCESS 1", startup_states);
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
