# Releasing Aventyrs

One procedure for all three repositories. This file is identical in each repo; when you change
it, copy it to the other two.

| Repo | What a release produces | Released by |
|---|---|---|
| `aventyrs-core` | Library jar (in mavenLocal / CI) | Tag `v<version>` |
| `aventyrs-api` | Server on aventyrs-srv (port 22696) | `scripts/deploy-prod.sh` |
| `aventyrs-game-client` | `.dmg` (macOS) + `.exe` (Windows) on a GitHub Release | Tag `v<version>` (CI) |

**Always release in this order: core → API → client.** The API and client depend on core,
and the client depends on the API being live.

Each repo has a `release` branch. Merge the work you want to ship into it first, then follow
the steps below on `release`.

---

## 1. Core

```bash
cd aventyrs-core
git switch release && git pull
# merge the work to ship, e.g.: git merge <feature-branch>
```

1. Bump `version` in `build.gradle` by adding or raising a fourth number (e.g. `0.1.4` → `0.1.4.1`,
   `0.1.4.1` → `0.1.4.2`, up to `0.1.4.99`). Moving to the next minor (`0.1.5`) is a manual
   decision — never bump it automatically.
2. Move the previous release's changelog into the history folder, then add
   `<version>.CHANGELOG.md` at the root describing the changes — only the latest
   changelog lives at the root:
   ```bash
   git mv 0.1.4.CHANGELOG.md docs/changelog/
   git add 0.1.4.1.CHANGELOG.md
   ```
3. Test and publish locally:
   ```bash
   ./gradlew test publishToMavenLocal
   ```
4. Commit, tag and push:
   ```bash
   git commit -am "Release 0.1.4.1"
   git tag -a v0.1.4.1 -m "aventyrs-core 0.1.4.1"
   git push origin release v0.1.4.1
   ```

> The tag is required: client CI builds core from tag `v<version>`, and fails if it's missing.

## 2. API

```bash
cd aventyrs-api
git switch release && git pull
```

1. Point `build.gradle` at the new core: `implementation 'org.aventyrs.core:aventyrs-core:0.1.4.1'`.
2. Test: `./gradlew test`
3. Deploy (VPN must be on; prompts for the server's sudo password):
   ```bash
   scripts/deploy-prod.sh
   ```
   The script publishes core, builds, uploads, restarts the `aventyrs-api` service and prints
   `active` when it is up. If core was ahead, it updates `build.gradle` for you.
4. Commit and push:
   ```bash
   git commit -am "Release with core 0.1.4.1"
   git push origin release
   ```

Check: `curl -s -o /dev/null -w '%{http_code}\n' http://7dd208931cad.sn.mynetname.net:22696/api/`
returns `401`.

## 3. Client

```bash
cd aventyrs-game-client
git switch release && git pull
```

1. In `build.gradle`, point the core dependency at the new core version. **The client's version is
   always the core version** — `version` is read from that dependency line, so there is nothing
   else to bump.
2. Test: `./gradlew test`
3. Commit, tag and push (the tag is the core version):
   ```bash
   git commit -am "Release 0.1.6.1"
   git tag -a v0.1.6.1 -m "Aventyrs client 0.1.6.1"
   git push origin release v0.1.6.1
   ```
4. Pushing the tag starts **Actions → Package client**. When it finishes, the GitHub Release
   `v0.1.6.1` has `Aventyrs-<installer version>.dmg` and `.exe` attached (see Troubleshooting for
   the installer version).
5. Install one of them and check that the initial screen shows **Cliente 0.1.6.1 · Core 0.1.6.1**
   and the connection status reads **Ligado**.

To build installers without releasing (e.g. to test a branch): **Actions → Package client →
Run workflow**, optionally setting `core_ref` to a core branch. The files appear as workflow
artifacts instead of a Release.

---

## Checklist

- [ ] Core: version bumped, changelog added, tests pass, tag `v<core>` pushed
- [ ] API: core dependency updated, tests pass, deployed (`active`), pushed
- [ ] Client: core dependency updated (client version follows it), tests pass, tag `v<core>` pushed
- [ ] GitHub Release has `.dmg` and `.exe`; installed app shows the right versions and connects

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| CI: "aventyrs-core has no tag v…" | Tag and push core (step 1.4), then rerun the workflow. |
| Installer is named `x.y.z`, not `0.x.y.z` | jpackage takes at most three numbers and macOS rejects a leading `0`, so `0.1.6` ships as `1.6.0` and `0.1.6.1` as `1.6.1` (both OSes). In-app version is unaffected. |
| macOS: "app can't be opened" | The app isn't signed. Right-click → Open the first time. |
| API service won't start | `ssh administrador@192.168.99.99 'journalctl -u aventyrs-api -n 50'`. Don't run the fat jar directly: it must run extracted (the deploy script does this). |
| Deploy: `KeyError: 'ContainerConfig'` | docker-compose v1 bug on the server. `docker rm` the Mongo/SeaweedFS containers and rerun. Data is in named volumes and is kept. |
| Client `./gradlew test` freezes | `SceneGridControllerTest` occasionally deadlocks in the full suite. Rerun it alone: `./gradlew test --tests '*SceneGridControllerTest'`. |
