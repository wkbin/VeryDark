package top.wkbin.verydark

import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickSettingService : TileService() {


    private var isDark = false

    override fun onClick() {
        super.onClick()
        isDark = !isDark
        Settings.Secure.putInt(
            contentResolver,
            "reduce_bright_colors_activated",
            if (isDark) 1 else 0
        )
        qsTile.state = if (isDark) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        isDark =
            Settings.Secure.getInt(contentResolver, "reduce_bright_colors_activated", 0) == 1
        qsTile.state = if (isDark) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}