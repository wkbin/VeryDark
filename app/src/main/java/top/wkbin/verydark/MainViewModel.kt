package top.wkbin.verydark

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isWork = MutableStateFlow(AuthHelper.hasWriteSecureSettingsPermission(application))
    val isWork: StateFlow<Boolean> = _isWork.asStateFlow()


    init {
        if (!_isWork.value){
            if (RootChecker.isDeviceRooted()){
                val permission = "android.permission.WRITE_SECURE_SETTINGS"
                val command = "pm grant ${application.packageName} $permission"
                val status = Runtime.getRuntime().exec(command).waitFor()
                if (status != -1){
                    _isWork.value = true
                }
            }
        }
    }

    fun checkWork(){
        viewModelScope.launch {
            delay(500)
            if (AuthHelper.hasWriteSecureSettingsPermission(getApplication())){
                _isWork.value = true
            }
        }
    }

}