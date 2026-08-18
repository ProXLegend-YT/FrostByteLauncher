This directory intentionally does NOT contain a committed gradle-wrapper.jar
binary, because it was generated in a sandboxed environment with no network
access to Gradle's distribution servers.

This is handled automatically:
- CI (.github/workflows/*.yml) runs `gradle wrapper --gradle-version 8.9`
  on the runner if the jar is missing, before calling ./gradlew.
- Locally: open the project in Android Studio and it will regenerate the
  wrapper jar automatically, OR run `gradle wrapper --gradle-version 8.9`
  once from a terminal if you have any Gradle/Android Studio installed.

Once generated, commit the resulting gradle/wrapper/gradle-wrapper.jar so
future clones don't need to regenerate it.
