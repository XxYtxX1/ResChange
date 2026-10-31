package com.reschange.app

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Singleton bridge to the Shizuku-hosted UserService.
 * Call bind() from MainActivity after permission is granted.
 */
object ShizukuBridge {

    @Volatile
    var service: IRemoteService? = null
        private set

    var onBindStateChange: ((Boolean) -> Unit)? = null

    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IRemoteService.Stub.asInterface(ShizukuBinderWrapper(binder))
            onBindStateChange?.invoke(true)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            onBindStateChange?.invoke(false)
        }
    }

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    fun bind(packageName: String, versionCode: Int) {
        if (!isShizukuAvailable() || !hasPermission()) return
        if (userServiceArgs == null) {
            userServiceArgs = Shizuku.UserServiceArgs(
                ComponentName(packageName, RemoteSystemService::class.java.name)
            ).daemon(false)
                .processNameSuffix("service")
                .version(versionCode)
        }
        try {
            Shizuku.bindUserService(userServiceArgs!!, serviceConnection)
        } catch (e: Exception) {
            // Shizuku unavailable or not started
        }
    }

    fun unbind() {
        try {
            userServiceArgs?.let { Shizuku.unbindUserService(it, serviceConnection, true) }
        } catch (_: Exception) {}
        service = null
    }

    val isBound: Boolean get() = service != null
}
