package com.aratkain.updateprofile

import android.content.Context
import android.net.Uri
import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.isValidUsername
import com.aratkain.core.utils.parseErrorMessage
import com.aratkain.core.utils.toNetworkMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateProfilePresenter(
    private var view:    UpdateProfileContract.View?,
    private val session: SessionManager,
    private val context: Context
) : UpdateProfileContract.Presenter {

    companion object {
        // Replace with your actual Supabase project URL (must end with "/")
        // e.g. "https://abcdefghijkl.supabase.co/"
        private const val SUPABASE_URL  = "https://YOUR_PROJECT_ID.supabase.co/"
        private const val AVATAR_BUCKET = "avatars"
    }

    // ── Existing ──────────────────────────────────────────────────────────────

    override fun onViewCreated() {
        val username = session.getUsername() ?: ""
        val fullname = session.getFullname() ?: ""
        val email    = session.getEmail()    ?: ""
        val photoUrl = session.getPhotoUrl()           // may be null — that's fine
        view?.prefillData(username, fullname, email, photoUrl)
    }

    override fun onSaveClicked(username: String, fullname: String) {
        view?.clearErrors()

        var valid = true
        if (username.isEmpty()) {
            view?.showFieldError("username", "Username is required"); valid = false
        } else if (!username.isValidUsername()) {
            view?.showFieldError("username", "3-50 chars: letters, numbers, underscores only"); valid = false
        }
        if (fullname.isEmpty()) {
            view?.showFieldError("fullname", "Full name is required"); valid = false
        }
        if (!valid) return

        val userId = session.getUserId() ?: run {
            view?.showError("Session expired. Please log in again.")
            return
        }

        view?.showLoading()

        val updates = mapOf(
            "username" to username,
            "fullname" to fullname
        )

        SupabaseClient.db.updateUser(
            token   = session.getBearerToken(),
            userId  = "eq.$userId",
            updates = updates
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                view?.hideLoading()
                if (response.isSuccessful || response.code() == 204) {
                    session.updateProfile(username, fullname)
                    view?.showSuccess("Profile updated successfully!")
                } else {
                    view?.showError(response.parseErrorMessage())
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    override fun onDestroy() { view = null }

    // ── Photo ─────────────────────────────────────────────────────────────────

    override fun onChangePhotoClicked() {
        view?.openImagePicker()
    }

    override fun onPhotoSelected(uri: Uri) {
        val userId = session.getUserId() ?: run {
            view?.showError("Session expired. Please log in again.")
            return
        }

        val stream = context.contentResolver.openInputStream(uri) ?: run {
            view?.showError("Could not open the selected image.")
            return
        }

        view?.showPhotoUploading()

        val bytes       = stream.readBytes()
        val requestBody = bytes.toRequestBody("image/*".toMediaType())
        val filePart    = MultipartBody.Part.createFormData(
            name     = "file",
            filename = "$userId.jpg",
            body     = requestBody
        )

        SupabaseClient.storage.uploadFile(
            token  = session.getBearerToken(),
            bucket = AVATAR_BUCKET,
            path   = "$userId.jpg",
            file   = filePart
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                view?.hidePhotoUploading()
                if (response.isSuccessful) {
                    val publicUrl = "${SUPABASE_URL}storage/v1/object/public/$AVATAR_BUCKET/$userId.jpg"
                    session.savePhotoUrl(publicUrl)
                    view?.showUpdatedPhoto(publicUrl)
                    view?.showSuccess("Photo updated successfully!")
                } else {
                    view?.showError("Photo upload failed: ${response.parseErrorMessage()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                view?.hidePhotoUploading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }
}