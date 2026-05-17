package com.aratkain.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.databinding.FragmentPlaceDetailBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A modal [BottomSheetDialogFragment] that displays the full details of an
 * [EstablishmentResponse] and lets the user bookmark / un-bookmark it.
 *
 * All data is passed as Bundle arguments so the fragment survives configuration changes.
 */
class PlaceDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaceDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var place: EstablishmentResponse

    companion object {
        private const val ARG_ID       = "id"
        private const val ARG_NAME     = "name"
        private const val ARG_TYPE     = "type"
        private const val ARG_ADDRESS  = "address"
        private const val ARG_LAT      = "lat"
        private const val ARG_LNG      = "lng"
        private const val ARG_RATING   = "rating"
        private const val ARG_IS_OPEN  = "is_open"   // -1 = null, 0 = false, 1 = true
        private const val ARG_DISTANCE = "distance"  // -1.0 = null
        private const val ARG_PHOTO    = "photo"
        private const val NO_RATING    = -1f
        private const val NO_DISTANCE  = -1.0

        fun newInstance(p: EstablishmentResponse) = PlaceDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(  ARG_ID,       p.id)
                putString(ARG_NAME,     p.name)
                putString(ARG_TYPE,     p.type)
                putString(ARG_ADDRESS,  p.address)
                putDouble(ARG_LAT,      p.latitude)
                putDouble(ARG_LNG,      p.longitude)
                putFloat( ARG_RATING,   p.rating?.toFloat() ?: NO_RATING)
                // Boolean → Int so it survives the Bundle round-trip cleanly
                putInt(   ARG_IS_OPEN,  when (p.isOpen) { true -> 1; false -> 0; else -> -1 })
                putDouble(ARG_DISTANCE, p.distanceKm ?: NO_DISTANCE)
                putString(ARG_PHOTO,    p.photoUrl)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookmarkManager = BookmarkManager(requireContext())

        // ── Rebuild place from bundle ─────────────────────────
        val args      = requireArguments()
        val ratingRaw = args.getFloat(ARG_RATING, NO_RATING)
        val distRaw   = args.getDouble(ARG_DISTANCE, NO_DISTANCE)
        val isOpenRaw = args.getInt(ARG_IS_OPEN, -1)

        place = EstablishmentResponse(
            id          = args.getLong(ARG_ID),
            name        = args.getString(ARG_NAME) ?: "",
            type        = args.getString(ARG_TYPE),
            address     = args.getString(ARG_ADDRESS),
            latitude    = args.getDouble(ARG_LAT),
            longitude   = args.getDouble(ARG_LNG),
            rating      = if (ratingRaw == NO_RATING) null else ratingRaw.toDouble(),
            city        = null,
            reviewCount = null,
            isOpen      = when (isOpenRaw) { 1 -> true; 0 -> false; else -> null },
            distanceKm  = if (distRaw == NO_DISTANCE) null else distRaw,
            photoUrl    = args.getString(ARG_PHOTO)
        )

        bindViews()

        binding.btnBookmark.setOnClickListener {
            val isNowSaved = bookmarkManager.toggle(place)
            updateBookmarkState(isNowSaved)
        }

        binding.btnDirections.setOnClickListener { openDirections() }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    // ── View binding ──────────────────────────────────────────

    private fun bindViews() {
        with(binding) {

            // ── Photo banner ──────────────────────────────────
            // Show cover photo when available; fall back to the large
            // emoji icon so the sheet always looks complete.
            if (!place.photoUrl.isNullOrBlank()) {
                ivDetailPhoto.visibility    = View.VISIBLE
                tvDetailTypeIcon.visibility = View.GONE
                Glide.with(this@PlaceDetailBottomSheet)
                    .load(place.photoUrl)
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(ivDetailPhoto)
            } else {
                ivDetailPhoto.visibility    = View.GONE
                tvDetailTypeIcon.visibility = View.VISIBLE
                tvDetailTypeIcon.text       = typeEmoji(place.type)
            }

            // ── Core text ─────────────────────────────────────
            tvDetailName.text    = place.name
            tvDetailType.text    = typeLabel(place.type)
            tvDetailAddress.text = place.address ?: "No address available"
            tvDetailRating.text  = place.rating
                ?.let { "★ ${"%.1f".format(it)} / 5.0" }
                ?: "No rating yet"

            // ── Open / Closed status ──────────────────────────
            when (place.isOpen) {
                true  -> {
                    tvOpenStatus.visibility = View.VISIBLE
                    tvOpenStatus.text       = "● Open now"
                    tvOpenStatus.setTextColor(Color.parseColor("#4CAF50"))
                }
                false -> {
                    tvOpenStatus.visibility = View.VISIBLE
                    tvOpenStatus.text       = "● Closed"
                    tvOpenStatus.setTextColor(Color.parseColor("#F44336"))
                }
                null  -> tvOpenStatus.visibility = View.GONE
            }

            // ── Distance ──────────────────────────────────────
            val dist = place.distanceKm
            if (dist != null) {
                tvDetailDistance.visibility = View.VISIBLE
                tvDetailDistance.text = if (dist < 1.0) {
                    "${"%.0f".format(dist * 1000)} m from you"
                } else {
                    "${"%.1f".format(dist)} km from you"
                }
            } else {
                tvDetailDistance.visibility = View.GONE
            }
        }

        updateBookmarkState(bookmarkManager.isBookmarked(place.id.toString()))
    }

    // ── Directions ────────────────────────────────────────────

    /**
     * Opens Google Maps in navigation mode pointed at the place.
     * Falls back to the generic geo: URI (any maps app), then a browser URL.
     */
    private fun openDirections() {
        val lat  = place.latitude
        val lng  = place.longitude
        val name = Uri.encode(place.name)

        val navIntent = Intent(Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$lat,$lng&mode=d"))
            .apply { setPackage("com.google.android.apps.maps") }

        try {
            startActivity(navIntent)
        } catch (e: ActivityNotFoundException) {
            val geoIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)"))
            if (geoIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(geoIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"))
                try {
                    startActivity(webIntent)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(requireContext(),
                        "No maps app found. Please install Google Maps.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun updateBookmarkState(isSaved: Boolean) {
        binding.tvBookmarkLabel.text = if (isSaved) "Saved!" else "Save"
        binding.btnBookmark.alpha    = if (isSaved) 1f else 0.65f
    }

    private fun typeEmoji(type: String?) = when (type?.lowercase()) {
        "cafe"       -> "☕"
        "bar"        -> "🍺"
        "restaurant" -> "🍽"
        else         -> "📍"
    }

    private fun typeLabel(type: String?) = when (type?.lowercase()) {
        "cafe"       -> "☕  Café"
        "bar"        -> "🍺  Bar"
        "restaurant" -> "🍽  Restaurant"
        else         -> "📍  ${type?.replaceFirstChar { it.uppercase() } ?: "Place"}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}