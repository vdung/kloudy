package vdung.android.kloudy.data.retrofit

import okhttp3.*
import vdung.android.kloudy.data.model.UserDao

class UserAuthenticator(
        private val userDao: UserDao
) : Authenticator, Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = userDao.getActiveUser()

        if (user != null && user.isValid) {
            return chain.proceed(chain.request().withAuthorization(Credentials.basic(user.username, user.password)))
        }

        return chain.proceed(chain.request())
    }

    override fun authenticate(route: Route, response: Response): Request? {
        val user = userDao.getActiveUser() ?: return null
        val request = response.request()
        if (request.header("Authorization") == Credentials.basic(user.username, user.password)) {
            if (user.isValid) {
                userDao.update(user.copy(isValid = false))
            }

            return null
        }

        if (user.isValid) {
            return response
                    .request()
                    .withAuthorization(Credentials.basic(user.username, user.password))
        }

        return null
    }

    private fun Request.withAuthorization(authorization: String) = this.newBuilder().header("Authorization", authorization).build()
}