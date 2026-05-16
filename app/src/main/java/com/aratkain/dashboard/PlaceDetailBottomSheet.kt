package com.aratkain.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.databinding.FragmentPlaceDetailBinding
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

    // Reconstructed from bundle args
    private lateinit var place: EstablishmentResponse

    companion object {
        private const val ARG_ID        = "id"
        private const val ARG_NAME      = "name"
        private const val ARG_TYPE      = "type"
        private const val ARG_ADDRESS   = "address"
        private const val ARG_LAT       = "lat"
        private const val ARG_LNG       = "lng"
        private const val ARG_RATING    = "rating"
        private const val NO_RATING     = -1f

        fun newInstance(p: EstablishmentResponse) = PlaceDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(  ARG_ID,      p.id)                          // FIX 1: store as Long, not String
                putString(ARG_NAME,    p.name)
                putString(ARG_TYPE,    p.type)
                putString(ARG_ADDRESS, p.address)
                putDouble(ARG_LAT,     p.latitude)
                putDouble(ARG_LNG,     p.longitude)
                putFloat( ARG_RATING,  p.rating?.toFloat() ?: NO_RATING)
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

        // Rebuild the place object from args
        val args = requireArguments()
        val ratingRaw = args.getFloat(ARG_RATING, NO_RATING)
        place = EstablishmentResponse(
            id          = args.getLong(ARG_ID),                       // FIX 1: read back as Long
            name        = args.getString(ARG_NAME) ?: "",
            type        = args.getString(ARG_TYPE),
            address     = args.getString(ARG_ADDRESS),
            latitude    = args.getDouble(ARG_LAT),
            longitude   = args.getDouble(ARG_LNG),
            rating      = if (ratingRaw == NO_RATING) null else ratingRaw.toDouble(), // FIX 2: Float → Double
            city        = null,                                       // FIX 3: supply missing fields
            reviewCount = null,
            isOpen      = null,
            distanceKm  = null
        )

        bindViews()

        binding.btnBookmark.setOnClickListener {
            val isNowSaved = bookmarkManager.toggle(place)
            updateBookmarkState(isNowSaved)
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    // ── Private helpers ───────────────────────────────────────

    private fun bindViews() {
        with(binding) {
            tvDetailTypeIcon.text = typeEmoji(place.type)
            tvDetailName.text     = place.name
            tvDetailType.text     = typeLabel(place.type)
            tvDetailAddress.text  = place.address ?: "No address available"
            tvDetailRating.text   = place.rating
                ?.let { "${"%.1f".format(it)} / 5.0" }
                ?: "No rating yet"
        }
        updateBookmarkState(bookmarkManager.isBookmarked(place.id.toString()))
    }

    private fun updateBookmarkState(isSaved: Boolean) {
        binding.tvBookmarkLabel.text = if (isSaved) "Saved!" else "Save"
        // Highlight the entire button when saved
        binding.btnBookmark.alpha = if (isSaved) 1f else 0.65f
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