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

Per the project requirements (see `docs/`):
- Passwords are never stored.
- Authentication tokens are stored only via Android Keystore-backed secure
  storage and are never logged.
- Diagnostic exports explicitly exclude passwords, tokens, and private keys.
- Telemetry is off by default; crash reporting is opt-in only.

If you find a place where this repository violates any of the above,
that's a security bug — please report it via the process above.
