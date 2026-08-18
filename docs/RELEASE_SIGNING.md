# Setting Up Release Signing

The `release.yml` workflow builds a **signed** release APK, which requires a
signing keystore and its credentials to exist as GitHub repository secrets.
This is optional — the Debug Build workflow works with zero setup and
produces an installable (unsigned/debug-signed) APK.

## 1. Generate a keystore (one-time, do this locally — never in CI)

```bash
keytool -genkeypair -v \
  -keystore frostbyte-release.keystore \
  -alias frostbyte \
  -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for a keystore password and a key password — remember
both. **Back up this file somewhere safe.** If you lose it, you can never
publish an update to an app that used it for a prior release.

## 2. Encode it as base64

```bash
base64 -i frostbyte-release.keystore -o frostbyte-release.keystore.base64
```

(On Linux: `base64 -w0 frostbyte-release.keystore > frostbyte-release.keystore.base64`)

## 3. Add GitHub repository secrets

Go to **Settings → Secrets and variables → Actions → New repository secret**
and add these four:

| Secret name | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | contents of `frostbyte-release.keystore.base64` |
| `KEYSTORE_PASSWORD` | the keystore password you set in step 1 |
| `KEY_ALIAS` | `frostbyte` (or whatever alias you used) |
| `KEY_PASSWORD` | the key password you set in step 1 |

## 4. Cut a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Pushing a tag matching `v*.*.*` triggers `release.yml`, which builds, signs,
checksums, and publishes a GitHub Release with the APK attached.

## Never do this

- Never commit the `.keystore`/`.jks` file itself to the repository (it's
  covered by `.gitignore`, but double-check).
- Never paste the base64 keystore or passwords directly into workflow YAML —
  always use repository secrets.
