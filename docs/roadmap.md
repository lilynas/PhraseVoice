# PhraseVoice Roadmap

## Phase 1 MVP

- Create the Android project and app module.
- Build the Compose + Material 3 shell.
- Implement the `TtsProvider` abstraction.
- Implement `AndroidSystemTtsProvider` with `speak`, `stop`, voice listing, and WAV export.
- Implement phrase CRUD, favorites, search, quick speak, and history.
- Store ordinary app data in DataStore as JSON.
- Verify builds through GitHub Actions CI.

## Phase 2 Cloud Providers

- Implement `OpenAiTtsProvider` with `gpt-4o-mini-tts` as the default model.
- Implement `GeminiTtsProvider` with isolated response parsing.
- Implement `MiMoTtsProvider` for Xiaomi MiMo V2.5 TTS using the official chat completions speech synthesis API.
- Implement `CustomHttpTtsProvider` for OpenAI-compatible services, user-managed Edge TTS servers, and other TTS endpoints.
- Add secure provider settings for API keys, base URLs, headers, models, voices, and request templates.

### MiMo Provider Plan

- Follow the Xiaomi MiMo V2.5 TTS documentation: https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/speech-synthesis-v2.5
- Default endpoint: `https://api.xiaomimimo.com/v1/chat/completions`.
- Default model: `mimo-v2.5-tts`.
- Default voice: `mimo_default`, with built-in choices such as `冰糖`, `茉莉`, `苏打`, `白桦`, `Mia`, `Chloe`, `Milo`, and `Dean`.
- Request shape: place style/control instructions in the `user` message when present, place the target text in the `assistant` message, and set `audio.format` / `audio.voice`.
- Response parsing: decode the base64 audio payload from the returned message audio data into a local audio file.

## Phase 3 Polish

- Add audio cache controls and sharing.
- Add import/export for phrases as JSON.
- Expand unit and UI state tests.
- Add screenshots and release docs.
