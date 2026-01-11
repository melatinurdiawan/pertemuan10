package com.learn.adxvm.data.data

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)
data class UserData(
    val userId: String,
    val username: String?,
    val profilPictureUrl: String?
)