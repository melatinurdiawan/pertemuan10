package com.learn.adxvm.data.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.learn.adxvm.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient (
    private val context: Context
    ){
    private val auth = FirebaseAuth.getInstance()

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): SignInResult {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            return handlesSignIn(result)

        } catch (e: GetCredentialCancellationException){
            return SignInResult(data = null, errorMessage = "Csnceled")
        }catch (e: Exception) {
            e.printStackTrace()
            return SignInResult(data = null, errorMessage = e.message)
        }
    }
    private suspend fun handlesSignIn(result: GetCredentialResponse): SignInResult {
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                val googleidToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(googleidToken, null)

                val user = auth.signInWithCredential(firebaseCredential).await().user
                return SignInResult(
                    data = user?.run {
                        UserData(
                            userId = uid,
                            username = displayName,
                            profilPictureUrl = photoUrl?.toString()
                        )
                    },
                    errorMessage = null
                )
            } catch (e: Exception){
                e.printStackTrace()
                return SignInResult(data = null, errorMessage = e.message)
            }
        }
        return SignInResult(data = null, errorMessage = "No Google Crendential found")
    }
    suspend fun signOut(){
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            auth.signOut()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            username = displayName,
            profilPictureUrl = photoUrl?.toString()
        )
    }
}