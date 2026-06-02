# PhraseVoice

PhraseVoice is an open-source Android text-to-speech app for quickly typing or choosing common phrases and reading them aloud.

## Current Scope

Phase 1 MVP is in progress:

- Android + Kotlin + Jetpack Compose + Material 3
- Local Android `TextToSpeech` provider
- Phrase library with add, edit, delete, favorite, search, and quick speak
- Phrase JSON import/export with merge import and duplicate skipping
- History records with replay and save-as-phrase
- Audio export through Android system TTS as WAV
- Provider abstraction ready for cloud providers
- Edge TTS Forwarder provider for user-managed `ms-ra-forwarder` instances, with voice and style dropdowns
- Gemini TTS provider through Google `generateContent`, with preset voice dropdowns and WAV output wrapping
- Xiaomi MiMo V2.5 TTS provider with preset voices, VoiceDesign, description optimization, and optional preview text optimization
- GitHub Actions CI for build and unit test verification

Phase 2 will continue with multi-preset MiMo character voices and broader Custom HTTP presets.

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
- Gemini TTS uses the Google AI `generateContent` speech endpoint and stores the API key only through the app's provider configuration.
- MiMo TTS will follow Xiaomi MiMo's official V2.5 speech synthesis API and store API keys only through the app's provider configuration.
- MiMo VoiceDesign stores user-authored or user-approved voice descriptions locally. Description optimization uses the saved or temporarily entered MiMo API key, but does not store raw debug payloads.
- Edge TTS Forwarder is intended for user-managed `ms-ra-forwarder` deployments. If the deployment requires `TOKEN`, save it in the provider's Token field. Non-official services may have stability and compliance risks.

## License

Apache License 2.0. See [LICENSE](LICENSE).
