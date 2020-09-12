package com.edvard.poseestimation.data

import com.edvard.poseestimation.data.model.LoggedInUser
import com.edvard.poseestimation.login.MongoDBAccess
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
            val mongoDB = MongoDBAccess()
            mongoDB.validateUserPassword(username, password)
            val fakeUser = LoggedInUser(java.util.UUID.randomUUID().toString(), "Jane Doe")
            return Result.Success(fakeUser)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

