# Hidrata — app Android nativo (Kotlin + Jetpack Compose)

Versão nativa do PWA que está na raiz do repositório (`index.html`). As regras são as mesmas;
a novidade são os **lembretes em segundo plano**, que funcionam com o app fechado.

## Onde está cada coisa (comparando com o PWA)

| No PWA (`index.html`)                      | No Android                                              |
| ------------------------------------------ | ------------------------------------------------------- |
| `schedule()`, `expectedAt()`, `compute()`  | `logic/Hydration.kt`                                    |
| `localStorage` (`hidrata.settings`, `.log`) | `data/HidrataRepository.kt` (SharedPreferences + JSON) |
| HTML + CSS das telas                       | `ui/HidrataApp.kt`, `ui/Components.kt`, `ui/SettingsForm.kt` |
| Variáveis de cor `:root`                   | `ui/Theme.kt`                                           |
| `setInterval(check, 60000)` + notificação  | `notify/Reminders.kt` (WorkManager) e `notify/Notifier.kt` |
| Botões do banner                           | Botões na notificação → `ActionReceiver` em `Notifier.kt` |
| `manifest.webmanifest`                     | `app/src/main/AndroidManifest.xml`                      |

## Como funcionam os lembretes

- A cada **15 minutos** (o menor intervalo que o Android permite para tarefas repetidas),
  o WorkManager roda `Reminders.check()`, mesmo com o app fechado ou depois de reiniciar o celular.
- **Prestes a ficar abaixo:** se, no ritmo atual, você vai ficar abaixo da meta antes da próxima
  checagem, chega o aviso "Quase na hora de beber água ⏳".
- **Abaixo da meta:** "Hora de beber água 💧", com os botões **Bebi 300 ml** e **Lembrar em 30 min**.
- Para não incomodar: o mesmo tipo de aviso se repete no máximo a cada 30 min. Fora do horário
  acordado ou com a meta batida, nada é enviado.
- O Android pode atrasar alguns minutos para economizar bateria. Em alguns celulares
  (Xiaomi, Samsung, Motorola), vale tirar o Hidrata da "otimização de bateria".

## Como gerar e instalar o APK

1. Faça push na branch `kotlin-android`.
2. No GitHub, abra **Actions → Android APK →** a execução mais recente.
3. Em **Artifacts**, baixe `hidrata-debug-apk` (vem em .zip), extraia e abra o `app-debug.apk` no celular.

Os APKs de teste usam sempre a mesma chave (`app/debug.keystore`), então cada versão nova
instala por cima da anterior sem perder os dados. Essa chave é só para testes.

## Abrir no Android Studio (opcional)

`File → Open…` e escolha a pasta `android/`. Para rodar os testes: `./gradlew test`.
