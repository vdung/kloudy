package vdung.android.kloudy.data.retrofit

import okhttp3.*
import vdung.android.kloudy.data.user.UserRepository

class UserAuthenticator(
        private val userRepository: UserRepository
) : Authenticator, Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = userRepository.getUser()

        if (user != null) {
            return chain.proceed(chain.request().withAuthorization(Credentials.basic(user.username, user.password)))
        }

        return chain.proceed(chain.request())
    }

    override fun authenticate(route: Route, response: Response): Request? {
        val user = userRepository.getUser() ?: return null
        val request = response.request()
        if (request.header("Authorization") == Credentials.basic(user.username, user.password)) {
            return null
        }

        return response
                .request()
                .withAuthorization(Credentials.basic(user.username, user.password))
    }

    private fun Request.withAuthorization(authorization: String) = this.newBuilder().header("Authorization", authorization).build()
}