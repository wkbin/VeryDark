# VeryDark(极暗)

[Source link / 项目地址](https://github.com/wkbin/VerfDark)

反馈问题可点击以上地址，发起issues

## 介绍
Android 12原生系统默认支持极暗模式，但是大部分厂商把该功能隐藏，因此我开发了自用APP，一并开源
支持以下3激活方式（均可使用磁贴）：

- 支持Root激活: 直接授予root权限即可，首次激活后可以关闭权限
- 支持Shizuku激活 ：直接授予Shizuku权限即可，首次激活后可以关闭权限
- 支持Adb激活 : 
```shell
adb shell pm grant top.wkbin.verydark android.permission.WRITE_SECURE_SETTINGS
```
