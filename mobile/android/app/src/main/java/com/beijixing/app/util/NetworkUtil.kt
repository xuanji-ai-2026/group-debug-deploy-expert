package com.beijixing.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object NetworkUtil : DefaultLifecycleObserver {

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isObserving = false
    
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = getConnectivityManager(context) ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            (networkInfo?.type == ConnectivityManager.TYPE_WIFI) && (networkInfo.isConnected == true)
        }
    }

    fun isMobileConnected(context: Context): Boolean {
        val cm = getConnectivityManager(context) ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            (networkInfo?.type == ConnectivityManager.TYPE_MOBILE) && (networkInfo.isConnected == true)
        }
    }

    fun getNetworkType(context: Context): String {
        return when {
            isWifiConnected(context) -> "Wi-Fi"
            isMobileConnected(context) -> "移动数据"
            else -> "无网络"
        }
    }
    
    fun startNetworkMonitoring(context: Context, lifecycleOwner: LifecycleOwner) {
        if (isObserving) return
        
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
        
        lifecycleOwner.lifecycle.addObserver(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    android.util.Log.d("NetworkUtil", "Network available")
                }
                
                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    android.util.Log.w("NetworkUtil", "Network lost")
                }
                
                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val hasInternet = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    android.util.Log.d("NetworkUtil", "Network capabilities changed: hasInternet=$hasInternet")
                }
            }
            
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            isObserving = true
        }
    }
    
    fun stopNetworkMonitoring() {
        if (!isObserving) return
        
        try {
            networkCallback?.let { callback ->
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtil", "Error unregistering network callback", e)
        } finally {
            networkCallback = null
            connectivityManager = null
            isObserving = false
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopNetworkMonitoring()
    }
    
    private fun getConnectivityManager(context: Context): ConnectivityManager? {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
}