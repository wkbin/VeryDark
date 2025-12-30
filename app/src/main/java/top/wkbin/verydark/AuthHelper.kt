package top.wkbin.verydark

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuRemoteProcess
import java.io.DataOutputStream

object AuthHelper {
    private const val TAG = "AuthHelper"
    
    /**
     * 检查是否有 WRITE_SECURE_SETTINGS 权限
     */
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查 Shizuku 是否可用
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.v(TAG, "Shizuku not available: ${e.message}")
            false
        }
    }
    
    /**
     * 检查 Shizuku 是否已授权
     */
    fun isShizukuAuthorized(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.v(TAG, "Shizuku permission check failed: ${e.message}")
            false
        }
    }
    
    /**
     * 请求 Shizuku 权限
     */
    fun requestShizukuPermission(context: Context, requestCode: Int) {
        try {
            if (!isShizukuAvailable()) {
                // 引导用户安装 Shizuku
                openShizukuInstallPage(context)
                return
            }
            
            if (isShizukuAuthorized()) {
                // 已经授权，直接尝试授予 WRITE_SECURE_SETTINGS
                grantWriteSecureSettingsViaShizuku(context)
            } else {
                // 请求 Shizuku 权限
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求 Shizuku 权限失败", e)
        }
    }
    
    /**
     * 通过 Shizuku 授予 WRITE_SECURE_SETTINGS 权限
     * 
     * 注意：WRITE_SECURE_SETTINGS 是一个特殊权限，需要通过 shell 命令 "pm grant" 来授予
     */
    fun grantWriteSecureSettingsViaShizuku(context: Context): Boolean {
        return try {
            if (!isShizukuAuthorized()) {
                Log.e(TAG, "Shizuku 未授权，无法执行命令")
                return false
            }
            
            val packageName = context.packageName
            val permission = "android.permission.WRITE_SECURE_SETTINGS"
            val command = "pm grant $packageName $permission"
            
            // 使用 Shizuku 执行命令。
            // 既然 newProcess 是私有的，我们可以使用 Shizuku.newRemoteProcess 或者通过反射
            // 但在最新 SDK 中，推荐使用 Shizuku.newProcess(String[] cmd, String[] env, String dir)
            // 如果报错是私有，可能是因为导入的库版本或混淆问题。
            // 尝试使用其提供的公共入口
            val process = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess

            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // 命令执行成功，验证权限
                Thread.sleep(500)
                if (hasWriteSecureSettingsPermission(context)) {
                    Log.d(TAG, "通过 Shizuku 成功授予 WRITE_SECURE_SETTINGS 权限")
                    return true
                }
            }
            
            Log.w(TAG, "Shizuku 命令执行结束，但权限验证失败或退出码不为0: $exitCode")
            false
        } catch (e: Exception) {
            Log.e(TAG, "通过 Shizuku 授予权限失败", e)
            false
        }
    }
    
    /**
     * 检查是否有 Root 权限
     */
    suspend fun checkRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            Log.v(TAG, "Root not available: ${e.message}")
            false
        }
    }
    
    /**
     * 通过 Root 授予 WRITE_SECURE_SETTINGS 权限
     */
    suspend fun grantWriteSecureSettingsViaRoot(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageName = context.packageName
            val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
            
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            Log.e(TAG, "通过 Root 授予权限失败", e)
            false
        }
    }
    
    /**
     * 获取 ADB 授权命令
     */
    fun getAdbGrantCommand(context: Context): String {
        val packageName = context.packageName
        return "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
    }
    
    /**
     * 复制文本到剪贴板
     */
    fun copyToClipboard(context: Context, text: String, label: String = "命令") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
    
    /**
     * 打开 Shizuku 安装页面
     */
    private fun openShizukuInstallPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://shizuku.rikka.app/")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开 Shizuku 安装页面失败", e)
        }
    }
    
    /**
     * 打开 Shizuku 应用
     */
    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                openShizukuInstallPage(context)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开 Shizuku 应用失败", e)
            false
        }
    }
}
