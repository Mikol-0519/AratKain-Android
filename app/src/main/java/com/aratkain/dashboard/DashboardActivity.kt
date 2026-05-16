package com.aratkain.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
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

    /** Kept in memory so marker-click can look up the full place object. */
    private var currentPlaces: List<EstablishmentResponse> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@DashboardActivity, PreferenceManager.getDefaultSharedPreferences(this@DashboardActivity))
            userAgentValue = packageName
        }
        binding          = ActivityDashboardBinding.inflate(layoutInflater)
        bookmarkManager  = BookmarkManager(this)
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
        requestUserLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
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
            controller.setCenter(GeoPoint(10.3157, 123.8854))
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state      = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupRecyclerView() {
        nearbyAdapter = NearbyPlaceAdapter(
            bookmarkManager   = bookmarkManager,
            onPlaceClick      = { place ->
                // Pan map to the tapped place
                binding.mapView.controller.animateTo(GeoPoint(place.latitude, place.longitude))
                binding.mapView.controller.setZoom(17.0)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                // Open detail sheet
                openPlaceDetail(place)
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
        binding.fabMyLocation.setOnClickListener  { requestUserLocation() }

        // 🔖 Sidebar bookmark button → Favourites screen
        binding.btnNavSaved.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    // ── Location ───────────────────────────────────────────────

    private fun requestUserLocation() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED) {
            getLastKnownLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(fine, coarse), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        // TEMPORARY: force Cebu for emulator testing
        presenter.onLocationReady(10.3157, 123.8854)
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
            getLastKnownLocation()
        } else {
            showMapError("Location permission denied. Enable it in Settings to find nearby places.")
        }
    }

    // ── DashboardContract.View ─────────────────────────────────

    override fun centerMapOn(lat: Double, lng: Double) {
        val userPoint = GeoPoint(lat, lng)
        binding.mapView.controller.animateTo(userPoint)
        binding.mapView.controller.setZoom(16.0)

        // Remove old user-location marker, add fresh one
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
        currentPlaces = places   // keep reference for tap-lookup

        // Remove all existing place markers (keep user-location pin)
        binding.mapView.overlays.removeAll(
            binding.mapView.overlays.filterIsInstance<Marker>()
                .filter { it.id != "user_location" }
        )

        places.forEach { place ->
            Marker(binding.mapView).apply {
                id       = "place_${place.id}"
                position = GeoPoint(place.latitude, place.longitude)
                title    = place.name
                snippet  = "${place.type?.replaceFirstChar { it.uppercase() } ?: "Place"}" +
                        if (!place.address.isNullOrBlank()) " • ${place.address}" else ""
                icon     = emojiToDrawable(typeEmoji(place.type))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Tap a pin → open the place-detail bottom sheet
                setOnMarkerClickListener { _, _ ->
                    openPlaceDetail(place)
                    true   // consume the event (don't show default OSM info window)
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

    /** Shows the place-detail bottom sheet. Avoids stacking duplicates. */
    private fun openPlaceDetail(place: EstablishmentResponse) {
        val tag = "place_detail"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        PlaceDetailBottomSheet.newInstance(place).show(supportFragmentManager, tag)
    }

    /**
     * Renders an emoji string into a square [BitmapDrawable] sized for map pins.
     * Emoji rendering is handled by Android's system emoji font so it works on all
     * API levels that support the emoji character.
     */
    private fun emojiToDrawable(emoji: String): BitmapDrawable {
        val dp    = resources.displayMetrics.density
        val size  = (dp * 42).toInt()        // ~42 dp pin
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = size * 0.78f
            textAlign = Paint.Align.CENTER
        }
        // Vertically centre the glyph
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