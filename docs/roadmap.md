# PhraseVoice Roadmap

## Phase 1 MVP

- Create the Android project and app module.
- Build the Compose + Material 3 shell.
- Implement the `TtsProvider` abstraction.
- Implement `AndroidSystemTtsProvider` with `speak`, `stop`, voice listing, and WAV export.
- Implement phrase CRUD, favorites, search, quick speak, and history.
- Add phrase JSON import/export with merge import and duplicate skipping. Done.
- Store ordinary app data in DataStore as JSON.
- Verify builds through GitHub Actions CI.

## Phase 2 Cloud Providers

- Implement `OpenAiTtsProvider` with `gpt-4o-mini-tts` as the default model.
- Implement `EdgeTtsForwarderProvider` for user-managed `ms-ra-forwarder` instances.
- Implement `GeminiTtsProvider` with isolated response parsing. Done.
- Implement `MiMoTtsProvider` for Xiaomi MiMo V2.5 TTS using the official chat completions speech synthesis API. Done for non-streaming preset voices.
- Add MiMo VoiceDesign character voices so users can create, preview, and reuse custom role voices from text descriptions. Initial single-description VoiceDesign mode is implemented; multi-preset character storage remains future work.
- Implement `CustomHttpTtsProvider` for OpenAI-compatible services, user-managed Edge TTS servers, and other TTS endpoints.
- Add secure provider settings for API keys, base URLs, headers, models, voices, and request templates.

### Edge TTS Forwarder Plan

- Use the `ms-ra-forwarder` `/api/text-to-speech` endpoint as a first-class provider.
- Default endpoint: `https://tts.shirone.de/api/text-to-speech`.
- Request shape: send `voice`, `volume`, `rate`, `pitch`, `personality`, and `text` as GET query parameters.
- Authentication: when the deployment has `TOKEN` configured, send `Authorization: Bearer <token>` using the saved Token field.
- Initial voice list: include common Chinese and English Microsoft voice names; users can still paste a full Microsoft voice name as the default voice.
- Keep this provider separate from Custom HTTP so common Edge TTS usage requires less manual template editing.

### MiMo Provider Plan

- Follow the Xiaomi MiMo V2.5 TTS documentation: https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/speech-synthesis-v2.5
- Default endpoint: `https://api.xiaomimimo.com/v1/chat/completions`.
- Default model: `mimo-v2.5-tts`.
- Default voice: `mimo_default`, with built-in choices such as `冰糖`, `茉莉`, `苏打`, `白桦`, `Mia`, `Chloe`, `Milo`, and `Dean`.
- Request shape: place style/control instructions in the `user` message when present, place the target text in the `assistant` message, and set `audio.format` / `audio.voice`.
- Response parsing: decode the base64 audio payload from the returned message audio data into a local audio file.
- Current implementation supports non-streaming WAV responses; streaming and voice clone uploads remain out of scope.

### MiMo VoiceDesign Plan

- Add a separate character voice design mode for `mimo-v2.5-tts-voicedesign` rather than treating it as another preset voice ID. Done.
- Voice description input: collect role/persona, age/gender, timbre, emotion, rhythm, accent/dialect, and optional scene notes as a reusable local preset. Initial implementation stores one default description in Provider settings; a multi-preset library remains future work.
- Request shape: place the voice design description in the required `user` message and the preview/synthesis text in the `assistant` message. Done.
- Preview flow: synthesize a short sample, allow retrying the same description, then save the character voice preset for later phrase reading. Initial Provider "save and test" preview is implemented.
- Add one-click VoiceDesign description optimization through a MiMo chat model. Done.
- Optional text preview: support MiMo's `audio.optimize_text_preview` flag so the preview text can be improved for the designed voice when the user opts in. Done.
- Initial scope excludes voice cloning uploads; VoiceClone can be added later after privacy, file-size, and consent UX are designed.

## Phase 3 Polish

- Add audio cache controls and sharing.
- Add import/export for phrases as JSON. Done.
- Expand unit and UI state tests.
- Add screenshots and release docs.
