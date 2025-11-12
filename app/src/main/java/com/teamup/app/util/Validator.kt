package com.teamup.app.util

import com.google.firebase.auth.*

fun isValidPassword(password: String): Pair<Boolean, String> {
    if (password.length < 6) return false to "Le mot de passe doit contenir au moins 6 caractères"
    if (!password.any { it.isUpperCase() }) return false to "Le mot de passe doit contenir au moins une majuscule"
    if (!password.any { it.isLowerCase() }) return false to "Le mot de passe doit contenir au moins une minuscule"
    if (!password.any { it.isDigit() }) return false to "Le mot de passe doit contenir au moins un chiffre"
    if (!password.any { "!@#\$%^&*()-_=+[]{};:'\",.<>?/|\\~`".contains(it) })
        return false to "Le mot de passe doit contenir au moins un caractère spécial"
    return true to ""
}

fun getFriendlyFirebaseAuthError(e: Throwable): String {
    return when (e) {
        is FirebaseAuthInvalidUserException -> "Aucun compte trouvé avec cet e-mail."
        is FirebaseAuthInvalidCredentialsException -> "Mot de passe incorrect."
        is FirebaseAuthUserCollisionException -> "Cette adresse mail est déjà associée à un compte."
        is FirebaseAuthException -> "Erreur d’authentification, veuillez réessayer."
        else -> "Une erreur est survenue : ${e.localizedMessage}"
    }
}
