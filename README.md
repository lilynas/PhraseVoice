# PhraseVoice

PhraseVoice is an open-source Android text-to-speech app for quickly typing or choosing common phrases and reading them aloud.

## Current Scope

Phase 1 MVP is in progress:

- Android + Kotlin + Jetpack Compose + Material 3
- Local Android `TextToSpeech` provider
- Phrase library with add, edit, delete, favorite, search, and quick speak
- History records with replay and save-as-phrase
- Audio export through Android system TTS as WAV
- Provider abstraction ready for cloud providers
- GitHub Actions CI for build and unit test verification

Phase 2 will add OpenAI TTS, Gemini TTS, and configurable Custom HTTP providers.

## Build Verification

This workspace does not assume a local Android toolchain. CI is the source of truth for compilation:

```bash
gradle :app:assembleDebug :app:testDebugUnitTest --stacktrace
```

The workflow installs JDK 17, Android SDK 35, and Gradle 8.7.

## Provider And API Key Safety

- Do not commit API keys, tokens, `.env` files, keystores, or provider secrets.
- Network request logging must stay disabled or redacted before cloud providers are enabled.
- Users are responsible for third-party API costs.
- Edge TTS network integrations should be configured through Custom HTTP providers unless an official Android TTS service is installed. Non-official services may have stability and compliance risks.

## License

Apache License 2.0. See [LICENSE](LICENSE).
