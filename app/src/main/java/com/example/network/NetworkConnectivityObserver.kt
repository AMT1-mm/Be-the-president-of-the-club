package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  if (cm == null) {
    trySend(true)
    close()
    return@callbackFlow
  }

  fun checkOnline(): Boolean {
    val active = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  trySend(checkOnline())

  val callback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(checkOnline())
      }

      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
      }
    }

  cm.registerDefaultNetworkCallback(callback)
  awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
}.distinctUntilChanged()
