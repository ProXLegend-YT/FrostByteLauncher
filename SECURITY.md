# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in FrostByte Launcher (for example:
insecure token storage, a way to exfiltrate account credentials, an
arbitrary-file-write via a crafted mod/shader/resource-pack ZIP, or a
supply-chain issue in the build), please report it privately rather than
opening a public issue.

- Open a **GitHub Security Advisory** (Security tab → "Report a
  vulnerability") on this repository, or
- Email the maintainers listed in the repository's contact information.

Please include:
- A description of the vulnerability and its impact
- Steps to reproduce (proof-of-concept if possible)
- The version/commit affected

We aim to acknowledge reports within 5 business days.

## Scope

In scope:
- The FrostByte Launcher Android application code in this repository
- The GitHub Actions build/release pipeline

Out of scope:
- Mojang/Microsoft's own authentication services and APIs
- Third-party mod/shader/resource-pack content hosted by external providers

## Handling of Sensitive Data

- Passwords are never stored.
- Authentication tokens (Microsoft/Xbox/Mojang, ely.by) are handled by
  `app_pojavlauncher`'s existing authenticator flow.
- Diagnostic/log exports should exclude passwords, tokens, and private keys.

Note: `docs/` in this repository describes a separate, unfinished Compose-based
rewrite (`app/`) that is not currently part of the shipping build (see
`settings.gradle` - only `app_pojavlauncher` and `forge_installer` are
included). Its documented security practices apply to that prototype, not
necessarily to `app_pojavlauncher`. If you find a discrepancy between this
policy and `app_pojavlauncher`'s actual behavior, that's the security bug to
report.
