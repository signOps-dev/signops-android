# SignOps for Android

[SignOps](https://signops.dev) in an Android app (Android 9 and later): Approve, passkeys and push open reporting. Approve keys are made in the Android Keystore (StrongBox when the phone has it) and never leave the phone. Passkeys go through Credential Manager; the same passkeys your website makes with `@signops/web` work here, and the other way round.

```kotlin
// build.gradle.kts: implementation("dev.signops:android:0.4.0")
val signops = SignOps(context, "pk_live_...")            // the publishable key; secret keys stay on your server

// Make a passkey. The token comes from your server: POST /v1/passkeys/registration-token
lifecycleScope.launch {
    val made = signops.passkeys.register(this@MainActivity, registrationToken)
}

// Sign in. Send r.signopsToken to your server, which checks it with POST /v1/passkeys/verify.
lifecycleScope.launch {
    val r = signops.passkeys.signIn(this@MainActivity)
}
```

## Approve

```kotlin
// After your own login. The token comes from your server: POST /v1/approve/enroll-token { user_id }.
signops.approve.enroll(enrolToken, FirebaseMessaging.getInstance().token.await())

// In your FirebaseMessagingService
override fun onMessageReceived(m: RemoteMessage) {
    Approve.requestId(m.data)?.let { signops.approve.open(this, it); return }   // the ready-made screen
}
override fun onNewToken(t: String) { scope.launch { signops.approve.updatePushToken(t) } }

// When the app opens (some phones hold pushes while the app sleeps)
if (signops.approve.isEnrolled && signops.approve.pending().isNotEmpty()) signops.approve.open(this)

// Or your own screen: an activity that is a FragmentActivity (AppCompatActivity is one)
val req = signops.approve.pending().first()
signops.approve.approve(this, req, picked = "47")   // fingerprint, face or screen lock, then a signed answer
signops.approve.deny(req)

SignOps.approveColors = ApproveColors(accent = 0xFF0F766E.toInt())   // the ready-made screen in your colours
```

## Push opens

```kotlin
signops.push.opened(remoteMessage.data)          // a notification your app drew
signops.push.opened(intent)                      // the launch intent of a notification the system drew
signops.push.opened(remoteMessage.data, "track") // one of its buttons
```

Errors are `SignOpsException` with `code`: the API's codes, or `cancelled` (the user closed the prompt), `no_passkey`, `passkey_error`, `not_enrolled`, `no_lock` (the phone has no screen lock), `auth_failed`.

## Before it works

1. In the SignOps dashboard, Passkeys, Setup: add the app's package name and the SHA-256 of its signing certificate (Play Console, App integrity, App signing, or `keytool -list -v -keystore <your keystore>`). Add the debug certificate too while you develop.
2. Host the `assetlinks.json` the dashboard shows at `https://<your domain>/.well-known/assetlinks.json`.

## Build

JDK 17 and the Android SDK (`local.properties` with `sdk.dir`), then `./gradlew :signops:assembleRelease :signops:testDebugUnitTest`. With `SIGNOPS_SK` and `SIGNOPS_PK` (test keys of one app) set, `ApproveTest.roundTripAgainstTheApi` enrols, fetches, approves and refreshes the push token against the real API with a software key in the same formats. The server half is checked by `app/tests/e2e.mjs` with a simulated Credential Manager passkey (origin `android:apk-key-hash:...`, no Origin header).
