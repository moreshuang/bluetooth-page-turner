# Bluetooth Page Turner

Turn an Android phone into a Bluetooth HID page-turning remote using its physical volume buttons.

**当前阶段：v0.1.0 开发骨架，实体音量键尚未实现，蓝牙真机链路尚未验证。**
先通过屏幕上的 Previous / Next 按钮验证标准 HID 输入，再开发音量键。

## Introduction

这是一个独立实现、MIT 开源的 Android 工具，面向电子书阅读场景。
手机作为标准 Bluetooth Classic HID Keyboard，目标设备负责接收键盘事件并翻页。
Android 端没有 Kindle 私有协议，也没有复制 Komobo 或其他项目的应用源码。

## Features

- 使用 Android 官方 `BluetoothHidDevice`，注册键盘 SDP 与 HID Descriptor。
- 查看系统已配对设备，选择目标并连接/断开。
- Previous → Left Arrow；Next → Right Arrow。
- 8 字节键盘输入报告，40 ms 后自动发送全零释放报告。
- 支持 HID report / boot protocol，以及主机 GET_REPORT、LED output report 请求。
- 显示注册、连接、断开、权限与错误状态；注册和连接有超时处理。
- 前台退出时释放按键、断开、注销 HID、关闭 Profile Proxy。
- 独立的报告/时序单元测试；不使用复杂架构。

## Requirements

- Android 9（API 28）或更高版本。
- 手机系统必须开放 Bluetooth HID Device Profile；API 版本满足不代表厂商一定支持。
- 目标设备支持 Bluetooth Classic HID Keyboard，并且阅读软件接受方向键。
- 当前版本仅在 Activity 前台运行；切换应用、打开系统设置或息屏可能关闭 HID 会话。
- 编译：JDK 17 或 21（推荐），Android SDK Platform 36，Build Tools 36.0.0。
- Gradle 9.1.0 Wrapper；Android Gradle Plugin 9.0.1，内置 Kotlin 2.2.10。
- 本机首次验证使用 Android Studio 自带 JBR 25，Gradle 9.1 支持该版本。

## How it works

```text
Android phone (BluetoothHidDevice)
    → Standard Bluetooth Classic HID Keyboard
    → Kindle / Kobo / tablet / computer
    → Reader handles Left / Right Arrow
```

`bluetooth/HidManager.kt` 管理平台 API 与生命周期；`HidDescriptor.kt` 定义报告格式；
`HidKeyboard.kt` 管理 key down / key up 的时序；`HidKey.kt` 使用标准键盘 Usage ID。
原生 Android Views 实现 UI，无 Compose、AndroidX、Hilt 或额外运行时框架依赖。

`sendReport()` 返回 true 仅表示命令被接受，不证明主机已收到事件或成功翻页。
连接与注册成功必须以 Android 回调为准，最终行为以主机实测为准。
按键脉冲进行时按钮暂时禁用，不积压连续点击；释放失败会重试，持续失败则注销会话。
重新进入页面只重新注册 HID，用户手动选择/连接，不自动连接任意已配对设备。

## Build

在 Android Studio 中打开项目根目录，选择 JDK 17/21，安装 SDK Platform 36 和 Build Tools 36.0.0。
Android Studio 会创建未提交的 `local.properties`。命令行也可自行设置：

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows 使用 `gradlew.bat`。首次构建需要联网下载编译依赖；已安装 App 的使用不需要互联网。
APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

本次工作区构建使用独立、未提交的 `.android-sdk` 和 `.build-cache`，未修改全局 SDK：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
GRADLE_USER_HOME="$PWD/.build-cache" \
ANDROID_USER_HOME="$PWD/.local-tools/android-user" \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

## Usage

1. 安装 debug APK；在 Android 系统设置中开启蓝牙，并与目标设备完成配对。
2. 打开 App，Android 12+ 点击 Refresh / Register HID，允许 Nearby devices 权限。
3. 等待 HID ready；从已配对列表选择目标，点击 Connect，等待 **Connected**。
4. 在目标设备打开阅读软件，点击 Previous / Next 检查是否只翻一页。
5. 点击 Disconnect 后再 Connect，验证重新连接。

App 不扫描、不发起可发现请求、不实现自己的配对流程。打开系统蓝牙设置会让 App
离开前台，因此测试翻页时必须回到 App 并重新连接。有些主机在首次配对时就需要
看到已注册的 HID SDP；这种主机的配对流程是待真机确认的限制，不能假定已有配对一定可用。
不要把进入系统设置后 HID 被注销误认为“蓝牙硬件不支持”。

详细步骤、故障定位及验收记录见 [真机测试指南](docs/DEVICE_TESTING.md)。

## Android versions and permissions

`minSdk = 28`，`compileSdk = targetSdk = 36`。Android 16+ 的实际兼容性仍需逐版本验证。

| Android | API | 本 MVP 的权限 |
| --- | --- | --- |
| 9 | 28 | `BLUETOOTH` + `BLUETOOTH_ADMIN`，安装时授予 |
| 10 | 29 | `BLUETOOTH` + `BLUETOOTH_ADMIN`，安装时授予 |
| 11 | 30 | `BLUETOOTH` + `BLUETOOTH_ADMIN`，安装时授予 |
| 12 / 12L | 31 / 32 | `BLUETOOTH_CONNECT`，运行时 Nearby devices |
| 13 | 33 | `BLUETOOTH_CONNECT`，运行时 Nearby devices |
| 14 | 34 | `BLUETOOTH_CONNECT`，运行时 Nearby devices |
| 15 | 35 | `BLUETOOTH_CONNECT`，运行时 Nearby devices |
| 16 | 36 | `BLUETOOTH_CONNECT`，运行时 Nearby devices |
| 17+ | 37+ | 按已核查官方 CONNECT API 契约设计；未进行真机验证 |

- `BLUETOOTH` / `BLUETOOTH_ADMIN` 均带 `maxSdkVersion="30"`。
- 旧版 Android HID 服务要求 `BLUETOOTH_ADMIN` 才能注册、连接和发送报告，即使不扫描。
- 不申请 `BLUETOOTH_SCAN`：仅读取已配对设备。
- 不申请 `BLUETOOTH_ADVERTISE`：不让手机进入可发现模式。
- 不申请 `ACCESS_FINE_LOCATION` / 后台定位：没有扫描或位置功能。
- 不申请 `INTERNET`、通知、无障碍或后台服务权限。

依据见 [技术说明与官方来源](docs/TECHNICAL_NOTES.md)。

## Compatibility

目前所有设备都标为 **未验证**；理论目标不是兼容承诺。

| Target | 前提 | 实测状态 |
| --- | --- | --- |
| Kindle Paperwhite 6 + KOReader | Kindle 侧提供 HID 接收/事件转发 | 未验证，第一优先级 |
| Other Kindle / Kobo + KOReader | 具体型号、固件、蓝牙与输入栈支持 | 未验证 |
| Android tablet / iPad | 标准蓝牙键盘与阅读应用键位支持 | 未验证 |
| Windows / macOS / Linux | Bluetooth Classic HID Host + 应用方向键支持 | 未验证 |
| PowerPoint / PDF reader | 所在主机支持 HID 且应用响应方向键 | 未验证 |

越狱 Kindle 可能需要 `kindle-hid-passthrough` 或类似组件将标准 HID 输入交给 KOReader。
这里没有引入其代码/依赖，也未确认你的固件适用的安装方式。Android 端只发送标准键盘报告。
有蓝牙或支持音频设备，不等于支持键盘；KOReader 支持输入，也不等于设备蓝牙栈已转发输入。

## Privacy

No telemetry. No analytics. No account. No cloud.

无广告、无网络权限、无 SDK 数据采集。本地读取已配对设备名称/地址用于选择与连接，
不保存到文件、不上传；Logcat 不主动打印设备名称、地址或书籍信息。
异常保留平台堆栈用于排错，分享日志前应检查系统自身附带的设备信息。
禁止 Android 云备份。不持有 wake lock、不扫描、不轮询；低功耗效果尚未测量。

## Roadmap

- **v0.1.0**：HID 注册、连接、Previous / Next；完成手机 → Kindle PW6 → KOReader 真机验收。
- **v0.2.0**：在上述链路确认后实现前台音量键拦截与自定义键位。
- **v0.3.0**：研究 foreground service、息屏限制、用户选择的自动重连。
- **v0.4.0**：长按、双击和配置。
- 后续评估 Wear OS、Quick Settings、通知按钮与 BLE；本版不是 BLE HOGP。

## Contributing

见 [CONTRIBUTING.md](CONTRIBUTING.md)。保持单 app module、少依赖、小函数。
新功能先验证真实场景；新增依赖或第三方代码前检查并记录许可证。

## License

独立项目代码采用 [MIT License](LICENSE)，版权名称初始取自本地 Git 配置 `Xiong Chao`。
第三方组件保留自身许可证，不因本项目采用 MIT 而被重新授权为 MIT。
见 [依赖与来源清单](DEPENDENCIES.md) 及 [许可证原文](third_party/licenses)。
