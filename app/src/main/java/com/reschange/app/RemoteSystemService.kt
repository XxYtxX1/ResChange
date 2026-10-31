package com.reschange.app

import android.content.Intent
import android.os.IBinder

/**
 * UserService that runs under Shizuku's shell UID.
 * Has permission to execute wm size / wm density.
 */
class RemoteSystemService : IRemoteService.Stub() {

    override fun execute(command: String): Int = try {
        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        proc.waitFor()
    } catch (e: Exception) {
        -1
    }

    override fun read(command: String): String = try {
        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        out
    } catch (e: Exception) {
        ""
    }

    override fun destroy() {
        // no-op; Shizuku handles lifecycle
    }

    companion object {
        // Required by Shizuku: entry point for UserService
        @JvmStatic
        fun main(args: Array<String>) {
            // Shizuku calls this for IPC setup; nothing needed here
        }
    }
}
