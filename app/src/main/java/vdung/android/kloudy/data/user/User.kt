package vdung.android.kloudy.data.user

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
        val username: String,
        val password: String,
        val server: String
) : Parcelable {
    companion object {
        @JvmStatic val NONE = User("", "", "")
    }
}
