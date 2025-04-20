package com.example.autorecorder.adb

import com.example.autorecorder.AutoRecorderApp
import io.github.muntashirakon.adb.AdbStream
import io.github.muntashirakon.adb.LocalServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AdbRepository{
    private val manager
        get() = AdbConnectionManager.getInstance(AutoRecorderApp.appContext)
    private var adbShellStream: AdbStream? = null
    private var clearEnabled = false
    private val outputGenerator = Runnable {
        try {
            BufferedReader(InputStreamReader(adbShellStream!!.openInputStream())).use { reader ->
                val sb = StringBuilder()
                var s: String?
                while ((reader.readLine().also { s = it }) != null) {
                    if (clearEnabled) {
                        sb.delete(0, sb.length)
                        clearEnabled = false
                    }
                    sb.append(s).append("\n")
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        try {
            AdbConnectionManager.resetInstance()
        } catch (th: Throwable) {
            println(th.message)
        }
    }

    suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            manager.connect(ip, port)
        } catch (th: Throwable) {
            println(th.message)
            false
        }
    }

    suspend fun pair(port: Int, code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext manager.pair(port, code)
        } catch (th: Throwable) {
            println(th.message)
            return@withContext false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            manager.disconnect()
        } catch (th: Throwable) {
            println(th.message)
        }
    }

    suspend fun isConnected() = withContext(Dispatchers.IO) {
        try {
            manager.isConnected
        } catch (th: Throwable) {
            println(th.message)
            false
        }
    }

    suspend fun execute(command: String) = withContext(Dispatchers.IO) {
        try {
            if (adbShellStream == null || adbShellStream?.isClosed == true) {
                adbShellStream = manager.openStream(LocalServices.SHELL)
                Thread(outputGenerator).start()
            }
            if (adbShellStream == null) {
                return@withContext
            }
            if (command == "clear") {
                clearEnabled = true
            }
            adbShellStream!!.openOutputStream().use { os ->
                os.write(
                    java.lang.String.format("%1\$s\n", command).toByteArray(StandardCharsets.UTF_8)
                )
                os.flush()
                os.write("\n".toByteArray(StandardCharsets.UTF_8))
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}