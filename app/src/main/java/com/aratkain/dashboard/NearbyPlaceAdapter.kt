package com.aratkain.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.databinding.ItemNearbyPlaceBinding

class NearbyPlaceAdapter(
    private val bookmarkManager: BookmarkManager,
    /** Called when a row is tapped — used to pan the map or open the detail sheet. */
    private val onPlaceClick: (EstablishmentResponse) -> Unit,
    /** Called when the Directions button is tapped — opens navigation to the place. */
    private val onDirectionsClick: (EstablishmentResponse) -> Unit,
    /** Optional: called after a bookmark is toggled (e.g. to refresh the Favourites badge). */
    private val onBookmarkChanged: ((place: EstablishmentResponse, isSaved: Boolean) -> Unit)? = null
) : RecyclerView.Adapter<NearbyPlaceAdapter.ViewHolder>() {

    private val places = mutableListOf<EstablishmentResponse>()

    fun submitList(newPlaces: List<EstablishmentResponse>) {
        places.clear()
        places.addAll(newPlaces)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemNearbyPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: EstablishmentResponse) {

            // ── Type emoji ───────────────────────────────────────
            binding.tvTypeIcon.text = when (place.type?.lowercase()) {
                "cafe"       -> "☕"
                "bar"        -> "🍺"
                "restaurant" -> "🍽"
                else         -> "📍"
            }

            // ── Text fields ──────────────────────────────────────
            binding.tvPlaceName.text    = place.name
            binding.tvPlaceType.text    = place.type
                ?.replaceFirstChar { it.uppercase() } ?: "Place"
            binding.tvPlaceAddress.text = place.address ?: "No address available"
            binding.tvPlaceRating.text  = place.rating
                ?.let { "★ ${"%.1f".format(it)}" } ?: ""

            // ── Distance ─────────────────────────────────────────
            // Show "350 m" for under 1 km, "1.2 km" for anything further.
            // Hidden when backend sends no distance (non-nearby endpoints).
            val dist = place.distanceKm
            if (dist != null) {
                binding.tvDistance.visibility = View.VISIBLE
                binding.tvDistance.text = if (dist < 1.0) {
                    "${"%.0f".format(dist * 1000)} m away"
                } else {
                    "${"%.1f".format(dist)} km away"
                }
            } else {
                binding.tvDistance.visibility = View.GONE
            }

            // ── Open / Closed badge ──────────────────────────────
            // Green "Open" or red "Closed". Hidden when isOpen is null
            // (establishment hasn't configured hours yet).
            when (place.isOpen) {
                true  -> {
                    binding.tvOpenStatus.visibility = View.VISIBLE
                    binding.tvOpenStatus.text       = "● Open"
                    binding.tvOpenStatus.setTextColor(Color.parseColor("#4CAF50"))
                }
                false -> {
                    binding.tvOpenStatus.visibility = View.VISIBLE
                    binding.tvOpenStatus.text       = "● Closed"
                    binding.tvOpenStatus.setTextColor(Color.parseColor("#F44336"))
                }
                null  -> binding.tvOpenStatus.visibility = View.GONE
            }

            // ── Bookmark icon state ──────────────────────────────
            refreshBookmarkIcon(bookmarkManager.isBookmarked(place.id.toString()))

            binding.btnItemBookmark.setOnClickListener {
                val isNowSaved = bookmarkManager.toggle(place)
                refreshBookmarkIcon(isNowSaved)
                onBookmarkChanged?.invoke(place, isNowSaved)
            }

            // ── Row click → map pan / detail sheet ──────────────
            binding.root.setOnClickListener { onPlaceClick(place) }

            // ── Directions button → open navigation app ──────────
            binding.btnDirections.setOnClickListener { onDirectionsClick(place) }
        }

        private fun refreshBookmarkIcon(isSaved: Boolean) {
            binding.btnItemBookmark.alpha = if (isSaved) 1f else 0.4f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(places[position])

    override fun getItemCount(): Int = places.size
}