package vdung.android.kloudy.ui.settings

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Base64
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import vdung.android.kloudy.R
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.unmarshall
import vdung.android.kloudy.data.user.KeyStoreWrapper
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.databinding.UserPreferenceBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.login.LoginActivity
import javax.inject.Inject

class UserPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    @Inject
    internal lateinit var keyStoreWrapper: KeyStoreWrapper

    init {
        layoutResource = R.layout.user_preference
    }

    override fun onAttached() {
        super.onAttached()
        keyStoreWrapper = KeyStoreWrapper(context)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        unmarshall<User>(keyStoreWrapper.decrypt(Base64.decode(getPersistedString(null), 0)))?.let { user ->
            val config = NextcloudConfig(user)

            UserPreferenceBinding.bind(holder.itemView).apply {
                this.user = user

                GlideApp.with(holder.itemView)
                        .load(config.avatarUri())
                        .circleCrop()
                        .into(userAvatar)
            }
        }
    }
}