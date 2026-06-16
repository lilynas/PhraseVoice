# PhraseVoice

[![Android CI](https://github.com/lilynas/PhraseVoice/actions/workflows/android.yml/badge.svg)](https://github.com/lilynas/PhraseVoice/actions/workflows/android.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)

**语言**：简体中文 | [English](README.en.md)

PhraseVoice 是一个简洁的 Android 文本转语音工具，适合快速输入文本、管理常用语，并通过不同语音服务生成朗读音频。

## 截图

<p>
  <img src="docs/screenshots/home.png" width="220" alt="工作台页面" />
  <img src="docs/screenshots/provider.png" width="220" alt="Provider 配置" />
  <img src="docs/screenshots/settings.png" width="220" alt="设置页" />
</p>

## 功能

- 文本朗读、停止、保存与分享音频
- 常用语管理：新增、编辑、收藏、搜索、导入和导出 JSON
- 历史记录：重播、保存为常用语
- 多 Provider：Android System TTS、Offline sherpa-onnx、OpenAI TTS、Edge TTS Forwarder、Gemini TTS、MiMo TTS、Custom HTTP
- 离线语音包管理：下载并导入 sherpa-onnx TTS 模型后，可在工作台离线朗读
- MiMo VoiceDesign 角色声音、提示词优化与流式合成
- 首次配置引导与 Provider 可用状态提示
- 朗读场景预设、文本优化工具与当前声音试听
- 深色/浅色主题、应用内语言切换、调试日志开关

## 离线语音包

入口在「设置 → 发音与引擎 → 离线语音包管理」，也可以在 Provider 页选择 `Offline sherpa-onnx` 后点击「管理离线语音包」直达。

1. 在离线语音包管理页下载推荐的 sherpa-onnx 模型包。
2. 下载完成后点击「导入」，选择 `.tar.bz2`、`.zip` 或 `.tar` 模型文件。
3. 回到工作台，将 Provider 选为 `Offline sherpa-onnx`，再在 Voice 下拉中选择已导入模型即可离线朗读。

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Android DataStore
- Kotlin Coroutines
- OkHttp
- Media3 ExoPlayer
- GitHub Actions CI

## 下载

请在 [Releases](https://github.com/lilynas/PhraseVoice/releases) 下载最新 APK。

APK 按 CPU 架构分包发布：多数手机选择 `arm64-v8a`，MuMu/部分模拟器选择 `x86_64`。

## 构建

项目通过 GitHub Actions 编译。CI 使用 JDK 17、Android SDK 35 和 Gradle 8.7 执行：

```bash
gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --stacktrace
```

## License

Apache License 2.0. See [LICENSE](LICENSE).
