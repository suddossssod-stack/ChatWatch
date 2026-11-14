# ChatWatch — GitHub Actions build (Minecraft 1.21.11)

This is the ChatWatch Fabric client-mod source project plus a GitHub Actions workflow. GitHub's hosted runner will install Java 21 and Gradle, compile the project, and publish the resulting JAR as a downloadable workflow artifact.

## Build the JAR on GitHub (Windows-friendly)

1. Sign in to GitHub and create a **new public repository** named `ChatWatch` (a private repository also works). Do not initialize it with a README, license, or `.gitignore`.
2. Extract this ZIP on your Windows computer.
3. Open the extracted `ChatWatch-1.21.11-Ready` folder. Upload **the contents of that folder** to the empty GitHub repository, keeping the folder structure. Make sure the `.github/workflows/build.yml` file is included. If Windows hides `.github`, enable **View → Show → Hidden items** in File Explorer before uploading.
4. Commit the uploaded files to the `main` branch.
5. In the GitHub repository, open **Actions**. Select **Build ChatWatch** on the left, then click **Run workflow** → **Run workflow**. (A push to `main` also starts a build automatically.)
6. Wait for the run to finish. A green check means the workflow succeeded. Open that run and scroll to **Artifacts**.
7. Download `ChatWatch-Minecraft-1.21.11`, then extract the downloaded artifact ZIP. The mod JAR will be inside `build/libs/` (usually `chatwatch-1.0.0.jar`).
8. Put that JAR in your Minecraft 1.21.11 Fabric profile's `mods` folder. Also install the matching Fabric API for Minecraft 1.21.11.

## If the build fails

Open the failed workflow run, select the **Build mod** step, and copy the error text. Send it here and I can help fix the source. A successful upload/commit is not proof that the mod compiled; only a successful Actions run and a JAR artifact confirm that.

## What the mod does

ChatWatch is client-side. It highlights matching chat lines locally and can log flagged lines to `.minecraft/config/chatwatch-log.txt`. It does not delete messages, report players, or punish anyone on a server. Logs may contain other players' chat, so review them before sharing.

## In-game commands

- `/chatwatch toggle` — turn filtering on or off
- `/chatwatch list` — show the word list and allow-list
- `/chatwatch add <word>` / `/chatwatch remove <word>` / `/chatwatch allow <word>`
- `/chatwatch fuzzy` — enable one-character-near-match detection (may create false positives)
- `/chatwatch reload` — reload `config/chatwatch.json`

The project targets Minecraft 1.21.11, Java 21, Fabric Loader, and Fabric API. The first hosted build is also a compatibility check; if Fabric/Minecraft API names differ, the build log will identify the errors to fix.
