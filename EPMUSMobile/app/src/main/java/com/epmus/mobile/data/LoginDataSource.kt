package com.epmus.mobile.data

import io.realm.mongodb.User
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(user: User?): Result<User> {
        return try {
            if(user == null){
                throw IOException()
            }
            Result.Success(user)
        } catch (e: Throwable) {
            Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}