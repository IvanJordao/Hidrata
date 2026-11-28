# Hidrata

App pessoal de controle diário de água. O dono do projeto está aprendendo desenvolvimento
Android: explique as mudanças em português, de forma didática, e comente o código em português.

- Raiz do repositório: PWA original (`index.html`, `sw.js`, `manifest.webmanifest`) — é a referência das regras.
- `android/`: app nativo em Kotlin + Jetpack Compose (branch `kotlin-android`). Veja `android/README.md`.
- O APK é compilado pelo GitHub Actions (`.github/workflows/android.yml`); não há Android SDK local.
- Regras de negócio ficam em `android/app/src/main/java/com/ivanjordao/hidrata/logic/` e têm testes em `app/src/test/`.
- Próximos passos planejados: widget (Jetpack Glance) e, mais adiante, anúncios (AdMob) e publicação.
