# Acorde 2.0 Mobile

Aplicativo Flutter para colaboradores registrarem deslocamentos casa–trabalho, consultarem o próprio histórico e avançarem ações atribuídas. A API valida os dados e calcula emissões; o app não implementa fatores nem cálculos ambientais. A barra superior permite alternar entre tema claro e escuro. Sessões ficam em memória e exigem novo login após expiração ou reinício.

## Desenvolvimento

Na raiz do repositório, inicie PostgreSQL e API conforme o [README principal](../README.md). Depois:

```powershell
cd mobile
flutter pub get
flutter analyze
flutter test
flutter run
```

Use `--dart-define=ACORDE_API_URL=...` para trocar a URL da API. Android Emulator usa `http://10.0.2.2:8080`; iOS Simulator usa `http://localhost:8080`; dispositivo físico deve usar o endereço acessível do computador. HTTP é permitido somente em desenvolvimento local.

## Build Android

`flutter build apk --release` gera `build/app/outputs/flutter-apk/app-release.apk`. Sem assinatura configurada, o build usa a chave debug apenas para demonstração local e imprime um aviso; esse APK não deve ser distribuído como release oficial.

Para assinatura de distribuição, configure fora do repositório as quatro variáveis abaixo antes do build. O arquivo keystore também deve permanecer fora do repositório e ser protegido por backup seguro.

```powershell
$env:ACORDE_ANDROID_KEYSTORE_PATH = 'C:\caminho-seguro\acorde-release.jks'
$env:ACORDE_ANDROID_KEYSTORE_PASSWORD = '<senha-do-keystore>'
$env:ACORDE_ANDROID_KEY_ALIAS = '<alias>'
$env:ACORDE_ANDROID_KEY_PASSWORD = '<senha-da-chave>'
flutter build appbundle --release
```

O Gradle rejeita uma configuração parcial. Gere e guarde a chave conforme o processo de release da equipe; não versione segredos nem o keystore. O identificador Android atual é `br.com.acorde.acorde_mobile`.

## Limites da validação

Analyze, testes de widget e build APK não substituem a validação em emulador/dispositivo Android e iOS. A API precisa estar acessível no ambiente-alvo e com HTTPS fora do desenvolvimento local.
