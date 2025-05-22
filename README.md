# B1gBr0ther[b1gbr0ther](app/src/test/java/com/b1gbr0ther)

## Project Setup Instructions

### 1. Fixing the Gradle Wrapper

If you see errors like `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` or Gradle version errors, follow these steps:

1. Make sure you have these files in your project root:
    - `gradlew` (shell script)
    - `gradlew.bat` (Windows batch file)
    - `gradle/wrapper/gradle-wrapper.jar`
    - `gradle/wrapper/gradle-wrapper.properties`
2. If `gradle-wrapper.jar` is missing, download it from:
    - [https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.11.1/gradle-wrapper-8.11.1.jar](https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.11.1/gradle-wrapper-8.11.1.jar)
    - Place it in `gradle/wrapper/` and rename to `gradle-wrapper.jar` if needed.
3. Edit `gradle/wrapper/gradle-wrapper.properties` and set:
    - `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip`
4. Run `./gradlew --version` (or `gradlew.bat --version` on Windows) to verify Gradle 8.11.1 is used.

### 2. Downloading the Vosk Model

To download and unzip the Vosk speech recognition model automatically:

1. Make sure the Gradle wrapper is working (see above).
2. Run this command from the project root:
    ```
    ./gradlew :app:downloadVoskModel
    ```
    or on Windows:
    ```
    gradlew.bat :app:downloadVoskModel
    ```
3. This will download the Vosk model zip and unzip it to `app/src/main/assets/model`.

#### Troubleshooting
- If you see errors about missing files or classes, double-check the Gradle wrapper files and version.
- If the model does not appear in `assets/model`, check for errors in the Gradle output.

---

For further help, see the comments in the build files or ask a maintainer.