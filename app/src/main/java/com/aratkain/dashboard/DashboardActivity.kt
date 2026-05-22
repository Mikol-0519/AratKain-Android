package com.aratkain.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.getApp
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.databinding.ActivityDashboardBinding
import com.aratkain.favorites.FavoritesActivity
import com.aratkain.login.LoginActivity
import com.aratkain.profile.ProfileActivity
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import androidx.preference.PreferenceManager

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private lateinit var binding:             ActivityDashboardBinding
    private lateinit var presenter:           DashboardPresenter
    private lateinit var nearbyAdapter:       NearbyPlaceAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var bookmarkManager:     BookmarkManager

    // ── Real location client ──────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    /**
     * Guard: true once we have dispatched a location to the presenter in this
     * session. Prevents onResume from triggering a second nearby fetch when
     * returning from Profile / Favourites, while still re-fetching when the
     * user explicitly taps the FAB.
     */
    private var locationDispatched = false

    /** Kept in memory so marker-click can look up the full place object. */
    private var currentPlaces: List<EstablishmentResponse> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    // ── Lifecycle ──────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@DashboardActivity, PreferenceManager.getDefaultSharedPreferences(this@DashboardActivity))
            userAgentValue = packageName
        }
        binding              = ActivityDashboardBinding.inflate(layoutInflater)
        bookmarkManager      = BookmarkManager(this)
        fusedLocationClient  = LocationServices.getFusedLocationProviderClient(this)
        setContentView(binding.root)
        presenter = DashboardPresenter(this, SessionManager(this), getApp())
        setupMap()
        setupBottomSheet()
        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        presenter.onViewResumed()

        // Only fetch location (and therefore nearby places) once per session.
        // The FAB forces a fresh fetch by resetting locationDispatched = false.
        if (!locationDispatched) {
            requestUserLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        stopLocationUpdates()   // stop ongoing updates to save battery
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // ── Setup ──────────────────────────────────────────────────

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(10.3157, 123.8854))  // default centre; overwritten once GPS fires
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state      = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupRecyclerView() {
        nearbyAdapter = NearbyPlaceAdapter(
            bookmarkManager  = bookmarkManager,
            onPlaceClick     = { place ->
                // FIX: place.latitude / place.longitude are the correct DTO fields
                binding.mapView.controller.animateTo(GeoPoint(place.latitude, place.longitude))
                binding.mapView.controller.setZoom(17.0)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                openPlaceDetail(place)
            },
            onDirectionsClick = { place ->
                val lat = place.latitude ?: return@NearbyPlaceAdapter
                val lng = place.longitude ?: return@NearbyPlaceAdapter
                navigateToDirections(lat, lng, place.name ?: "Destination")
            }
        )
        binding.rvNearbyPlaces.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter        = nearbyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnViewProfile.setOnClickListener { presenter.onProfileClicked() }
        binding.cardWelcome.setOnClickListener    { presenter.onProfileClicked() }
        binding.btnNavProfile.setOnClickListener  { presenter.onProfileClicked() }
        binding.btnLogout.setOnClickListener      { presenter.onLogoutClicked() }

        binding.btnNavSaved.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        // FAB: force a fresh location + nearby fetch
        binding.fabMyLocation.setOnClickListener {
            locationDispatched = false
            requestUserLocation()
        }
    }

    // ── Location ───────────────────────────────────────────────

    private fun requestUserLocation() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED) {
            fetchRealLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(fine, coarse), LOCATION_PERMISSION_REQUEST)
        }
    }

    /**
     * Gets the device's real position via FusedLocationProviderClient.
     *
     * Strategy:
     *  1. Try [getLastLocation] first — instant, no power cost.
     *  2. If the cached fix is null (fresh boot, airplane mode just disabled,
     *     emulator with no mock location), fall back to a single
     *     [requestLocationUpdates] to force a fresh fix.
     */
    @SuppressLint("MissingPermission")   // permission already checked by caller
    private fun fetchRealLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    android.util.Log.d("DASHBOARD", "lastLocation: lat=${location.latitude} lng=${location.longitude}")
                    dispatchLocation(location.latitude, location.longitude)
                } else {
                    android.util.Log.d("DASHBOARD", "lastLocation null — requesting fresh fix")
                    requestSingleLocationUpdate()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DASHBOARD", "lastLocation failed: ${e.message}", e)
                requestSingleLocationUpdate()   // try the active path anyway
            }
    }

    /**
     * Requests a single high-accuracy location update.
     * Removes itself from updates as soon as the first fix arrives.
     */
    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0L)
            .setMaxUpdates(1)           // one fix is enough
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                android.util.Log.d("DASHBOARD", "freshFix: lat=${loc.latitude} lng=${loc.longitude}")
                dispatchLocation(loc.latitude, loc.longitude)
                stopLocationUpdates()
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request, locationCallback!!, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    /**
     * Single point where a GPS fix reaches the presenter.
     * Sets [locationDispatched] so onResume doesn't re-fetch unnecessarily.
     */
    private fun dispatchLocation(lat: Double, lng: Double) {
        locationDispatched = true
        presenter.onLocationReady(lat, lng)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchRealLocation()
        } else {
            showMapError("Location permission denied. Enable it in Settings to find nearby places.")
        }
    }

    // ── DashboardContract.View ─────────────────────────────────

    override fun centerMapOn(lat: Double, lng: Double) {
        val userPoint = GeoPoint(lat, lng)
        binding.mapView.controller.animateTo(userPoint)
        binding.mapView.controller.setZoom(16.0)

        binding.mapView.overlays.removeAll(
            binding.mapView.overlays.filterIsInstance<Marker>()
                .filter { it.id == "user_location" }
        )
        Marker(binding.mapView).apply {
            id       = "user_location"
            position = userPoint
            title    = "You are here"
            icon     = emojiToDrawable("📌")
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            binding.mapView.overlays.add(this)
        }
        binding.mapView.invalidate()
    }

    override fun addMapMarkers(places: List<EstablishmentResponse>) {
        currentPlaces = places

        binding.mapView.overlays.removeAll(
            binding.mapView.overlays.filterIsInstance<Marker>()
                .filter { it.id != "user_location" }
        )
        places.forEach { place ->
            // FIX 1: skip places with no coordinates — don't pin them at (0,0)
            val lat = place.latitude ?: return@forEach
            val lng = place.longitude ?: return@forEach

            Marker(binding.mapView).apply {
                id       = "place_${place.id}"
                position = GeoPoint(lat, lng)
                title    = place.name
                snippet  = "${place.type?.replaceFirstChar { it.uppercase() } ?: "Place"}" +
                        if (!place.address.isNullOrBlank()) " • ${place.address}" else ""
                icon     = emojiToDrawable(typeEmoji(place.type))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    openPlaceDetail(place)
                    true
                }
                binding.mapView.overlays.add(this)
            }
        }
        binding.mapView.invalidate()
    }

    override fun showNearbyPlaces(places: List<EstablishmentResponse>) {
        nearbyAdapter.submitList(places)
    }

    override fun updateNearbyCount(count: Int) {
        binding.tvNearbyCount.text = count.toString()
    }

    override fun showMapError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
    }

    override fun navigateToDirections(lat: Double, lng: Double, label: String) {
        // "google.navigation" triggers turn-by-turn driving directions immediately,
        // matching the behaviour of the Google Maps "Directions" button on the web.
        // Falls back to a plain geo URI if Google Maps is not installed.
        val navUri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (navIntent.resolveActivity(packageManager) != null) {
            startActivity(navIntent)
        } else {
            // Fallback: any app that handles geo URIs (Waze, OsmAnd, etc.)
            val geoUri = android.net.Uri.parse(
                "geo:$lat,$lng?q=$lat,${lng}(${android.net.Uri.encode(label)})"
            )
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
            if (geoIntent.resolveActivity(packageManager) != null) {
                startActivity(geoIntent)
            } else {
                showMapError("No navigation app found. Please install Google Maps.")
            }
        }
    }

    override fun showLoading()  { binding.progressBar.show() }
    override fun hideLoading()  { binding.progressBar.hide() }

    override fun showUserInfo(user: UserData) {
        binding.tvWelcome.text  = "Welcome back, ${user.username}! 👋"
        binding.tvFullname.text = user.fullname.ifEmpty { user.username }
        binding.tvUsername.text = "@${user.username}"
        binding.tvEmail.text    = user.email
        val initials = if (user.fullname.isNotEmpty())
            user.fullname.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        else user.username.take(2)
        binding.tvInitials.text = initials.uppercase()
        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(binding.ivAvatar)
            binding.ivAvatar.show()
            binding.tvInitials.hide()
        } else {
            binding.ivAvatar.hide()
            binding.tvInitials.show()
        }
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
    }

    override fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ -> presenter.confirmLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    override fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    // ── Private helpers ────────────────────────────────────────

    private fun openPlaceDetail(place: EstablishmentResponse) {
        val tag = "place_detail"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        PlaceDetailBottomSheet.newInstance(place).show(supportFragmentManager, tag)
    }

    private fun emojiToDrawable(emoji: String): BitmapDrawable {
        val dp     = resources.displayMetrics.density
        val size   = (dp * 42).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = size * 0.78f
            textAlign = Paint.Align.CENTER
        }
        val yOffset = (paint.descent() + paint.ascent()) / 2
        canvas.drawText(emoji, size / 2f, size / 2f - yOffset, paint)
        return BitmapDrawable(resources, bitmap)
    }

    private fun typeEmoji(type: String?) = when (type?.lowercase()) {
        "cafe"       -> "☕"
        "bar"        -> "🍺"
        "restaurant" -> "🍽"
        else         -> "📍"
    }
}