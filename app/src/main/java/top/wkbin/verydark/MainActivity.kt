package top.wkbin.verydark

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import top.wkbin.verydark.shizuku.ShizukuUtils
import top.wkbin.verydark.ui.theme.MyApplicationTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity(), Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener, ServiceConnection,
    Shizuku.OnRequestPermissionResultListener, LifecycleEventObserver {

    companion object {
        private const val APPLICATION_ID = "top.wkbin.verydark"
        private const val PERMISSION_CODE = 10001
    }

    private val _userService = MutableStateFlow<IUserService?>(null)
    val userService: StateFlow<IUserService?> = _userService.asStateFlow()
    @Composable
    fun AppBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "极暗",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            MenuButton()
        }
    }

    @Composable
    fun MenuButton() {
        var expanded by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Row {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "菜单"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("关于") },
                    onClick = {
                        expanded = false
                        showAboutDialog = true
                    }
                )
            }
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismissRequest = { showAboutDialog = false },
                onOpenGitHub = {
                    showAboutDialog = false
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "https://github.com/wkbin/VeryDark".toUri()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
        }
    }

    @Composable
    fun AboutDialog(
        onDismissRequest: () -> Unit,
        onOpenGitHub: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "关于") },
            text = {
                Column {
                    Text("极暗 - 调暗屏幕，保护视力")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "https://github.com/wkbin/VeryDark",
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onOpenGitHub() }
                    )
                }
            },
            confirmButton = {
                Button(onClick = onOpenGitHub) {
                    Text("打开GitHub")
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text("关闭")
                }
            }
        )
    }

    private val userServiceArgs = Shizuku
        .UserServiceArgs(ComponentName(APPLICATION_ID, UserService::class.java.name))
        .daemon(false)
        .processNameSuffix("adb_shell")
        .debuggable(false)
        .version(1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { AppBar() }
                ) { innerPadding ->
                    MainPage(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        lifecycle.addObserver(this)
    }

    @Composable
    fun MainPage(modifier: Modifier = Modifier) {
        val userService by userService.collectAsStateWithLifecycle()
        var isDark by remember { mutableStateOf(false) }
        var currentLight by remember { mutableIntStateOf(0) }
        // 使用derivedStateOf使状态能够响应变化
        val isDeviceRooted by remember {
            derivedStateOf { RootChecker.isDeviceRooted() }
        }

        val isWork by remember {
            derivedStateOf { isDeviceRooted || userService != null }
        }

        LaunchedEffect(userService) {
            isDark = if (userService != null) {
                val cmd = "settings get secure reduce_bright_colors_activated"
                (userService?.execArr(cmd.split(" ").toTypedArray())?.toIntOrNull() ?: 0) == 1
            } else {
                SettingsUtils.getReduceBrightColorsActivated()
            }
            currentLight = if (userService != null) {
                val cmd = "settings get secure  reduce_bright_colors_level"
                userService?.execArr(cmd.split(" ").toTypedArray())?.toIntOrNull() ?: 100
            } else {
                SettingsUtils.getReduceBrightColorsLevel()
            }
        }
        Column(modifier.padding(20.dp)) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "调暗屏幕，看手机时会更舒适",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(25.dp))
            if (isWork) {
                if (ShizukuUtils.checkPermission()) {
                    if (userService != null) {
                        StatusCard("Shizuku")
                    } else {
                        WarningCard("Shizuku服务未启动")
                    }
                } else {
                    StatusCard("Root")
                }
            } else {
                WarningCard("权限不足")
            }
            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "将屏幕调成极暗")
                Switch(checked = isDark, enabled = isWork, onCheckedChange = {
                    isDark = it
                    if (userService != null) {
                        val cmd =
                            "settings put secure reduce_bright_colors_activated ${if (isDark) 1 else 0}"
                        userService?.execArr(cmd.split(" ").toTypedArray())
                    } else {
                        SettingsUtils.setReduceBrightColorsActivated(isDark)
                    }
                })
            }
            Spacer(modifier = Modifier.height(25.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                text = "选项",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(text = "亮度")
            Slider(
                value = currentLight.toFloat(),
                enabled = isDark && isWork,
                valueRange = 0f..100f,
                onValueChange = {
                    currentLight = it.toInt()
                    if (userService != null) {
                        val cmd =
                            "settings put secure reduce_bright_colors_level ${100 - currentLight}"
                        userService?.execArr(cmd.split(" ").toTypedArray())
                    } else {
                        SettingsUtils.setReduceBrightColorsLevel(100 - currentLight)
                    }
                })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "更暗", fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray
                )
                Text(
                    text = "更亮", fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray
                )
            }
        }
    }

    override fun onBinderReceived() {
        if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
            connectShizuku()
        } else {
            AuthHelper.requestShizukuPermission(this@MainActivity, PERMISSION_CODE)
        }
    }

    private fun connectShizuku() {
        if (_userService.value != null) {
            return
        }

        Shizuku.bindUserService(userServiceArgs, this)
    }

    override fun onBinderDead() {
        _userService.value = null
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Toast.makeText(this@MainActivity, "Shizuku服务连接成功", Toast.LENGTH_SHORT).show()
        if (binder != null && binder.pingBinder()) {
            _userService.value = IUserService.Stub.asInterface(binder)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PERMISSION_GRANTED) {
            connectShizuku()
        }
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event
    ) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                Shizuku.addRequestPermissionResultListener(this)
                Shizuku.addBinderReceivedListenerSticky(this)
                Shizuku.addBinderDeadListener(this)
            }

            Lifecycle.Event.ON_STOP -> {
                Shizuku.removeRequestPermissionResultListener(this)
                Shizuku.removeBinderReceivedListener(this)
                Shizuku.removeBinderDeadListener(this)
            }

            Lifecycle.Event.ON_DESTROY -> {
                if (_userService.value != null && Shizuku.pingBinder()) {
                    Shizuku.unbindUserService(userServiceArgs, this, false)
                }
            }

            else -> {}
        }
    }


    @Composable
    private fun StatusCard(workModel: String) {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.secondaryContainer)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CheckCircle, "工作中")
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "工作模式：$workModel", fontSize = 16.sp)
            }
        }
    }

    @Composable
    fun WarningCard(
        message: String,
        color: Color = MaterialTheme.colorScheme.error,
        onClick: (() -> Unit)? = null
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = color
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Warning, null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}