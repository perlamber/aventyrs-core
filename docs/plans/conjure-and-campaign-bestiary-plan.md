# Conjure default monsters + campaign-scoped Bestiário

## Context
Right now the GM can only put a foe in a scene by first writing it in the Monster editor (the Bestiário), then adding that sheet in the Scene editor. The scene points at the Bestiário sheet itself.

The Bestiário also has no scoping:
- `GET /api/monster-sheets` returns everything, including summons.
- Monster sheets have no `campaignId`.
- The add-participant picker (`RestCharacterLookupService.listAllCharacters`) shows every foe, including ones already in another scene and dead ones.

Goal: faster sessions.
1. **Conjure:** from the live scene grid, the GM generates a monster that follows the manual from a preset (Modelo + GP + Regular/Exemplar). The result is a new `monsterSheets` entry placed straight into the scene and never listed in the Bestiário.
2. **Campaign binding:** a monster sheet gets an optional `campaignId`. `null` means lore. The Bestiário and the scene's monster picker offer only sheets from the selected campaign plus lore sheets.
3. **Scene binding:** a Bestiário sheet stays in the Bestiário while it sits in a scene. It is left out of the picker when adding monsters to any other scene. Fallen sheets (`FALLEN`/`COMMA`/`DEAD`) are left out too.
4. **Campaign dropdown:** sits on top of the GM console's scene listing. Its value is passed down to the Scene editor and the scene grid. Scenes are not stored in campaigns; the dropdown only filters monsters.

## Rulings (defaults; object at approval)
- **Scene binding is derived, not stored.** A Bestiário sheet counts as bound to a scene when that scene's `participants[].characterSheetId` contains it. Nothing can go stale and no migration is needed.
  - `SceneService.addParticipant` rejects a monster sheet that is already a participant of another scene (409).
  - Removing it from the scene unbinds it.
- **Conjured sheets** carry `conjured = true`, the scene's id as `conjuredInSceneId`, and the dropdown's `campaignId`.
  - They never appear in the Bestiário or the picker.
  - They are deleted when removed from the scene or when the scene is deleted, like summons.
  - Loot happens before removal, so `lootIntoBag` is unaffected.
- **The Bestiário shows** non-summon, non-conjured sheets. Its filter offers: All / one campaign / Lore only.
- **The picker shows** Bestiário sheets where `campaignId ∈ {selected, null}`, that are not in another scene and not fallen. With no campaign selected, it shows lore only.
- **Generator presets:** one recipe per Modelo, 7 in total. The GM chooses GP (1–80) and Kind. Optional extras: name, count 1–10 (copies get " 2", " 3" suffixes) and group.
  - The generator is deterministic for a given seed. A "Rerolar" button picks a new seed.
  - It uses one Modelo, i.e. a "plain default monster".
  - Abilities are taken from that Modelo only, preferring `APPLIED` over `PARTIAL` and never `TABLE_ONLY`. The first legal `ChoiceSpec` option is picked.

## Phase 1: core 0.1.7.5, `MonsterGenerator`
New `monster/generator/` package in `aventyrs-core/src/main/java/org/aventyrs/core/`:
- `ConjurePreset` enum, one constant per `MonsterModel`. Each holds:
  - attribute priority (`AttributeDomain` order)
  - preferred trained `SkillType`s, e.g. Luta/Esquiva-e-Aparar for brutes and Gnose-type Perícias for Alma Elemental
  - ability priority (an ordered list of that Modelo's `MonstrousAbility` constants)
  - feat category preference
  - default name and creature type
- `MonsterGenerator.generate(ConjurePreset, int powerDegree, MonsterKind, String name, long seed) → MonsterBlueprint`. It reuses the `MonsterRules` budgets instead of re-deriving them:
  1. Category comes from `MonsterCategory.forPowerDegree`.
  2. Attributes: spend `ATTRIBUTE_POINTS` along the priority, capping each at `MAXIMUM_ATTRIBUTE_BASE` and at the Categoria's `maximumAttribute`. The seed breaks ties and adds jitter.
  3. Abilities: the starting Presa-tier ability, then fill `abilityBudget` with tier-legal picks (`isAtLeast`).
  4. Skills: train the preset's Perícias, then use `gnoseUpgradeBudget` and `progressionUpgradeBudget` up to `maximumSkillLevel`.
  5. Feats: fill `featBudget` from `allowedFeatCategories`.
  6. Egos: spread `egoPointBudget`.
  7. The result must pass `MonsterRules.validate` with no violations. If it doesn't, drop the offending optional pick and retry. If it still fails, throw `IllegalStateException`; tests should make that impossible.
- Javadoc: "plain default per the manual". Don't touch `SampleMonster`.
- Tests in `MonsterGeneratorTest`:
  - every preset × GP in {1, 5, 12, 20, 26, 35, 46, 61, 80} × both kinds validates cleanly and `spawn(gm)` succeeds
  - the same seed gives an equal blueprint
  - abilities stay within the Modelo and the tier
- Bump `build.gradle` to `0.1.7.5` and publish to mavenLocal. Copy this plan to `docs/plans/conjure-and-campaign-bestiary-plan.md`.

## Phase 2: API (`aventyrs-api/.../api/`)
**Monster sheet fields.** Add `campaignId` (String, nullable), `conjured` (Boolean) and `conjuredInSceneId` to `monster/MonsterSheetDocument`.
- Add `campaignId` to `MonsterSheetCreateRequest`/`UpdateRequest`.
- Expose all three in `MonsterSheetResponse`.

**Bestiário endpoints.** Add `MonsterSheetController`:
- `GET /api/monster-sheets/bestiary?campaign={id|lore|all}`
  - excludes `summon != null` and `conjured == true`
  - `all` is the default
- `GET /api/monster-sheets/placeable?campaignId=&sceneId=`
  - returns Bestiário sheets with `campaignId ∈ {campaignId, null}` (lore only when no `campaignId` is given)
  - drops sheets with status `FALLEN`/`COMMA`/`DEAD`
  - drops sheets that are participants of any scene other than `sceneId`: one `SceneRepository` query on `participants.characterSheetId`, collected into a set
- Keep plain `GET /` unchanged for existing callers such as `listControllableMonsters` and the scene grid lookups.

**Conjure endpoint.** `POST /api/scenes/{id}/conjure` (GM-only matcher in `config/SecurityConfig`).
- Body: `ConjureRequest {preset, powerDegree, kind, name, count (required, 1–20), seed, group, concealment, campaignId, centre (GridPosition, required), tokenImageUrl}`.
  - A missing count or centre returns 400.
  - A blank `tokenImageUrl` becomes `bundled:conjured/<preset lower>.png`.
- **Placement around the GM's hex.**
  - The first copy lands on `centre` if it is free (`isFree`). Every other copy, and the first one when `centre` is occupied, goes to `freePositionNear(participants, centre)`. This is the same ring search summons use (`SceneService.java:603`).
  - The participants list is updated after each placement, so the copies fan out around the centre without overlapping.
  - Cells outside the scene's `width`/`height` are skipped as well, not just those outside `GRID_SIZE`; add the bound to that helper.
  - A `centre` outside the scene returns 400.
  - `addParticipant` gains an overload that takes an explicit position. It writes the position directly instead of using `firstFreePosition`.
- `SceneService.conjure` calls `MonsterGenerator.generate`. It then creates each sheet through `MonsterSheetService.create` (reusing the blueprint→character path, with `playerId` = principal and `conjured = true`) and calls `addParticipant` for it.
- It broadcasts the same scene update that `addParticipant` already sends and returns the created ids.

**Preview endpoint.** `POST /api/monster-sheets/generate-preview` (GM-only) returns the `MonsterBlueprintDto` without saving it, so the dialog can show the stats.

**Scene binding.**
- `SceneService.addParticipant`: if the sheet is a non-conjured monster that is a participant elsewhere, return 409.
- `removeParticipant`: also delete the sheet if it is `conjured`.
- Scene delete: delete the scene's conjured sheets.

**Tests.** Controller/service tests for the bestiary filters, placeable filtering (campaign, lore, other-scene, fallen), conjure (count, flags, participant added, GM-only 403, centre hex used when free, copies fan out without overlap, occupied centre falls to the nearest free hex, out-of-bounds centre returns 400), conjured cleanup on remove/delete, and the 409 on double placement.

## Phase 3: client (`aventyrs-game-client/src/main/java/com/aventyrs/client/`), core dep → 0.1.7.5
**Services.**
- `net/ApiClient`: `listBestiary(filter)`, `listPlaceableMonsters(campaignId, sceneId)`, `conjure(sceneId, req)`, `generatePreview(req)`.
- Add the new DTO fields.
- `monster/MonsterCatalogService` (+ `Rest`/`Mock`): `listBestiary`, `listPlaceable`, `conjure`, `preview`. `Monster` record gains `campaignId`.

**GM console.** `ui/screens/gm/GmConsoleController` + `gm-console.fxml` get a Campaign `ComboBox` above `scenesList`.
- Options: "Nenhuma (só lore)" plus `campaignService.listCampaigns()`.
- The selection lives in a small `GmCampaignContext` held by `SceneManager`, so it survives screen switches without being persisted.
- `showSceneEditor` and `enterScene` read it from there.

**Scene editor.** `SceneEditorController.onAddParticipant` keeps character candidates from `RestCharacterLookupService`, but foes come from `listPlaceable(context.campaignId, sceneId)` instead of the unscoped list. A small label shows the active campaign.

**Bestiário (`MonsterRosterController`).**
- Use `listBestiary` with a filter combo: All / campaign… / Lore.
- Show the campaign as a row tag.

**Monster editor (`MonsterEditorController`).** Add a "Campanha" combo defaulting to "Nenhuma (lore)", saved through `toCreateRequest`/`toUpdateRequest` in `monster/MonsterBlueprints`.

**Scene grid.** `SceneGridController` gets a new `sceneGmMenu` item, "Conjurar monstro", which opens a new `SceneGridConjureModal` (modelled on `SceneGridSubordinatesModal`).
- Fields: preset combo, GP spinner (shows the derived Categoria), Kind, name, count, group, a hidden checkbox, and "Rerolar".
- **Centre hex.**
  - A "Hex central" row shows the chosen cell, e.g. "Hex (12, 7)", next to a "Escolher no mapa" button.
  - The button hides the modal and puts the grid in a one-shot `conjurePicking` mode in `onGridClicked`. It follows the existing `looting`/`grantedMove` pattern: left click captures `new GridPosition(col, row)` and right click cancels, each with a log notice.
  - The modal then reopens with every field it had before. Keep the modal's state in a small `ConjureDraft` record passed to `open(...)`.
  - "Conjurar" stays disabled until a hex is chosen.
- **Pre-fill.** Opening the modal from a right-click on an empty hex isn't done today, so the first open always asks for a pick. Later opens remember the last centre.
- A preview pane shows attributes, PV/PM/PD, abilities, Perícias and Talentos from `generatePreview`.
- **Quantity is required.** There is a "Quantidade" spinner (1–20) with id `conjureCountSpinner`. "Conjurar" stays disabled unless it holds a valid value and a hex is chosen.
- **Token box.** A `FlowPane` gallery of the bundled conjure portraits (see *Token art* below).
  - Each portrait is a 64px circular-clipped thumbnail, drawn like the board does.
  - The preset's default portrait is preselected. Clicking another selects it (style class `token-option-selected`).
  - The choice is sent as `tokenImageUrl = "bundled:conjured/<file>.png"`.
- "Conjurar" calls the conjure endpoint with the context campaign, the centre, the count and the token.
- **Modal style.** Match the client's canonical scene modals, using `SceneGridHazardModal` and `SceneGridSubordinatesModal` as the reference:
  - a `Stage` with `initOwner` and `Modality.APPLICATION_MODAL`
  - a `Label` with class `modal-title`
  - a root `VBox(14)` with class `modal-root`, `Insets(24)` padding and an id
  - `field-label` labels and `grid-spinner` spinners
  - a right-aligned button row: `button-ghost` Cancel, `button-primary` Conjurar
  - `/com/aventyrs/client/css/app.css` attached to the modal scene
  - all strings from the bundle and ids on every control (for TestFX)
  
  No inline `setStyle`. Any new classes (e.g. the preview pane) go into `app.css` beside the existing modal rules. The wider modal style review is a separate later task.
- The grid then takes in the new participants through its existing participant-added flow, the same path as a Scene-editor add while the grid is open; `blueprintOf` resolves the new ids.

**Token art.** In `render/SummonTokenImages.java` and under `resources/com/aventyrs/client/tokens/`.
- **Bundled token keys.** A classpath URL is never sent to the server (see the class javadoc). Conjured sheets therefore store a portable key, `bundled:<dir>/<file>.png`.
  - `SummonTokenImages.resolve(...)` learns that prefix and turns it into the local classpath URL, falling back to `_default.png` if the file is missing.
  - Every client resolves the same art, the same way it already does for summon kinds.
  - The existing `SceneGridController.java:3667` call already goes through `resolve`, so conjured tokens need no other render change.
- **Conjured portraits.** New folder `tokens/conjured/`, holding:
  - `_default.png`, a copy of the summons default to start with
  - one default per preset, named after the `ConjurePreset` constant in lower case (`abencoado_de_cireneia.png`, `alma_elemental.png`, `aspecto_humanoide.png`, `mutante_monstruoso.png`, `besta_alada.png`, `brotos_de_mapinguari.png`, `escolhido_de_macaria.png`)
  - any extra portraits
  
  A classpath folder can't be listed inside a jar, so the gallery reads its entries from `tokens/conjured/index.txt`, one file name per line. Missing files are skipped and the preset defaults fall back to `_default.png`. A `CREDITS.txt` in that folder follows the summons one.
  
  The API's conjure endpoint writes the default key when the request has no `tokenImageUrl`. Conjured monsters therefore always get a default portrait.
- **Undead summons (Reanimar tree and monster-invoked).**
  - They already resolve by `SummonDto.kind`, but no art exists yet, so today they get the black-cat `_default.png`.
  - `urlFor` gains a family fallback: `CARRASCO_<CATEGORIA>` → `carrasco.png` → `_default.png`.
  - **Update `tokens/summons/CREDITS.txt`.** Add an "Undead (Reanimar tree)" section using the file's existing "(not added yet - uses _default.png)" entry format. Expected file names:
    - `zumbi.png`: Zumbi (Reanimar, `UndeadSummonKind.ZUMBI`; also corpse-raised Zumbis of any body size)
    - `carnical.png`: Carniçal (Criar Carniçal, `CARNICAL`; Açougueiro included)
    - `abantesma.png`: Abantesma (`ABANTESMA`)
    - `carrasco.png`: shared default for monster-invoked Carrascos Moribundos (Procissão dos Desencarnados, Cuspidor de Pragas, Praga Zumbi). Optional per-Categoria overrides: `carrasco_presa.png`, `carrasco_deviante.png`, `carrasco_predador.png`, `carrasco_apex.png`, `carrasco_abominacao.png`
    - `familiar_maior.png`: Bruxo's Familiar Maior (`FAMILIAR_MAIOR`), listed so the folder is complete
  - Also fix the file's opening paragraph: kinds are no longer only `NatureSummonKind`.
- **Tests.** `SummonTokenImagesTest` covers the `bundled:` key, the missing-file fallback, and `carrasco_*` → `carrasco.png` → `_default.png`.

**i18n.** Add keys in the existing bundles (pt-BR, plus en if present).

**Tests.** Update the mock services, and add/adjust the TestFX/controller tests for the editor picker filtering and the roster filter.

## Verification
1. Core: `./gradlew test` in `aventyrs-core/aventyrs-core`, including the generator test matrix, then `publishToMavenLocal`.
2. API: `./mvnw test` (or the Gradle equivalent), then run the dev profile and curl:
   - `/bestiary`
   - `/placeable` with a campaign and with lore only
   - `/conjure` as GM, then the same call as a player to check the 403
3. Client: the full test suite. Then run the app as GM:
   - pick a campaign on the console and open a scene's editor; only that campaign's foes plus lore foes are listed
   - place a foe in scene A; it disappears from scene B's picker but stays in the Bestiário
   - enter scene A, pick a hex next to an existing token, and conjure 3 × Mutante GP 12. The tokens appear on or around that hex without overlapping, and the Bestiário doesn't list them. Right-click while picking cancels the pick
   - the conjure modal looks like the Hazard and Subordinados modals
   - "Conjurar" is disabled until a quantity and a hex are set
   - the token box preselects the preset's default, and choosing another portrait shows it on every client's board
   - cast Reanimar: the Zumbi shows `zumbi.png` once the file is added, and `_default.png` until then
   - remove one; its sheet is deleted
   - kill a Bestiário foe; it leaves the picker
4. Save the rulings above to memory once approved.
