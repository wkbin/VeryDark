package top.wkbin.verydark

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import rikka.shizuku.Shizuku
import top.wkbin.verydark.ui.theme.MyApplicationTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity(), Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener, ServiceConnection,
    Shizuku.OnRequestPermissionResultListener, LifecycleEventObserver {

    companion object {
        private const val APPLICATION_ID = "top.wkbin.verydark"
        private const val PERMISSION_CODE = 10001
    }

    private val mainViewModel by viewModels<MainViewModel>()

    private var userService: IUserService? = null

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
        val context = LocalContext.current
        var isDark by remember { mutableStateOf(false) }
        var currentLight by remember { mutableIntStateOf(0) }
        val isWork by mainViewModel.isWork.collectAsStateWithLifecycle()
        // 使用derivedStateOf使状态能够响应变化

        LaunchedEffect(isWork) {
            if (isWork) {
                isDark = Settings.Secure.getInt(
                    context.contentResolver,
                    "reduce_bright_colors_activated",
                    0
                ) == 1
                currentLight = Settings.Secure.getInt(
                    context.contentResolver,
                    "reduce_bright_colors_level",
                    100
                )
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
                StatusCard()
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
                    Settings.Secure.putInt(
                        context.contentResolver,
                        "reduce_bright_colors_activated",
                        if (isDark) 1 else 0
                    )
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
                    Settings.Secure.putInt(context.contentResolver,"reduce_bright_colors_level",100 - currentLight)
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
        if (userService != null) {
            return
        }

        Shizuku.bindUserService(userServiceArgs, this)
    }

    override fun onBinderDead() {
        userService = null
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Toast.makeText(this@MainActivity, "Shizuku服务连接成功", Toast.LENGTH_SHORT).show()
        if (binder != null && binder.pingBinder()) {
            userService = IUserService.Stub.asInterface(binder)
            if (AuthHelper.hasWriteSecureSettingsPermission(this)) {
                return
            }

            val permission = "android.permission.WRITE_SECURE_SETTINGS"
            val command = "pm grant $packageName $permission"
            userService?.execLine(command)
            mainViewModel.checkWork()
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
                if (userService != null && Shizuku.pingBinder()) {
                    Shizuku.unbindUserService(userServiceArgs, this, false)
                }
            }

            else -> {}
        }
    }


    @Composable
    private fun StatusCard() {
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
                Text(text = "权限已开启", fontSize = 16.sp)
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