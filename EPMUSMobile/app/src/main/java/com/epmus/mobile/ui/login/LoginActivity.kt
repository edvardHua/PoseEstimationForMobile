package com.epmus.mobile.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.epmus.mobile.ForgotPasswordActivity
import com.epmus.mobile.MainMenuActivity
import com.epmus.mobile.R
import io.realm.mongodb.Credentials

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var loading: ProgressBar
    private lateinit var forgetPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        login = findViewById<Button>(R.id.login)
        loading = findViewById<ProgressBar>(R.id.loading)
        forgetPassword = findViewById<TextView>(R.id.forgotPassword)

        forgetPassword.setOnClickListener {
            val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                validateCredentials()
                /*if(validateCredentials()) {
                    loading.visibility = View.VISIBLE
                    loginViewModel.login(username.text.toString(), password.text.toString())
                    val intent = Intent(this@LoginActivity, MainMenuActivity::class.java)
                    startActivity(intent)
                }*/
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun validateCredentials() {

        val username = username.text.toString()
        val password = password.text.toString()

        val creds = Credentials.emailPassword(username, password)
        realmApp.loginAsync(creds) {
            if (!it.isSuccess) {
                Toast.makeText(baseContext, it.error.message ?: "Mot de passe et/ou nom d'utilisateur invalide", Toast.LENGTH_LONG).show()
            } else {
                // TODO finish might not work, watch for the already done redirection
                loading.visibility = View.VISIBLE
                loginViewModel.login(username, password)
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
            }
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}