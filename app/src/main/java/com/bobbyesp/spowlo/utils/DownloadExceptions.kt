package com.bobbyesp.spowlo.utils

class UnsupportedLinkException : Exception()

class TrackLinkRequiredException : Exception()

class SpotifyApiException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
