# Spell casting — client → API → core audit (VIDA and ALIADOS DA NATUREZA)

**Status (2026-10-01):** steps 1–4 are built — core 0.0.93–0.0.96, the client, and the API (spell relay and summon
participant). What stays open is listed under each step.

**Step 1, as built:**

| Gap | Fix |
| --- | --- |
| 1. Efeito Alternativo | A second CONJURAR button (`castAlternateSpell-*`) on each Magia that has one; the cast sends `useAlternateVersion`. |
| 2. "Pessoal ou Toque" | Core accepts a target fitting either reach; the client offers the caster plus everyone adjacent, and naming oneself casts it as Pessoal. |
| 3. DM floor | `SpellCastingResult#castingTargetValue`, judged by `SpellCastingService#castSucceeds`. |
| 4. Corrente | `#isEffectChainTriggered` per target, `#resolveEffectChain` (Sobrecura via `Spell#getEffectChainKind`), chained onto that target's effect. Area occupants each get their own effect, so Nova Rejuvenescedora now halves enemies. |
| 5. Casting critical | `#resolveCastingCriticalEffect`, applied to each target. Potencializar's +Duração is logged only — nothing tracks a running Duração. |

**Step 2 (VIDA), as built — core 0.0.94 + client + API relay:**

| Gap | Fix |
| --- | --- |
| A Magia's effect on someone else's sheet | Relayed (`/app/scenes/{id}/spells` → `/topic/.../spells`, `SpellLandedMessage`); the owner's client rebuilds it from core with the caster's dice (`client.magic.SpellLanding`) and broadcasts the sheet. |
| Concentração (gap 6) | `Scene#breakConcentration` on the caster's own cast or attack, and on every client for a remote spell/attack action; it releases summons and `sheet.Sustained` effects. |
| Aliviar a Dor once per Descanso Longo | `SpellHealing#oncePerLongRest`. |
| Procrastinar Ferimento | A Reação (spent from the live pool) leaving a `PostponedWoundWard` the next hit spends; Estancar refuses that hit's Sangramento. |
| Benção Bifurcada / Corrente Abençoada | `SpellCastRequest#additionalTargets` (cap, +3PM); the modal offers who is in touch (and, uncapped, one step further). |
| Toque Curativo / Remover Maldição GD | The modal asks what is removed; `SpellCastRequest#opposedBranchLevel`. |
| Cura em Massa / Nova Rejuvenescedora / Fonte da Juventude | Caster-centred areas that reach (or spare) the caster; each occupant its own effect; Fonte restores PV/PM/PD (hostiles at a Mínimo). |
| Corpo Fechado | Cleansing plus a sustained `MaleficioWard`. |

**Still open in VIDA** (narrative-tier, no mechanism asks): Transferir Doenças e Venenos (Toque Curativo's Corrente), Barganha
Negra (Exorcizar's), "apenas doenças de origem mundana", and Potencializar's +Duração (nothing tracks a running Duração —
logged only).

## How a cast flows today

- **Client** (`SceneGridController#onCastSpell` → `completeSpellCast` → `landSpell`/`landSpellOn`, helpers in
  `client.magic.SpellCasting`):
  1. picks a target if `SpellCasting.needsTarget(spell.getTargeting())`, plus the Metamágico opt-ins;
  2. prices it with core and calls `SpellCastingService#castSpell`;
  3. pays PM/PA, rolls Domínio do Mana against `castingDifficultyLevel`;
  4. on a success, lands it: an `AttackDelivery` vs DM for a Perícia-de-Ataque Magia on a foe, else
     `primaryDamage` and `spellEffect` straight onto the target(s).
- **API**: no cast endpoint. The client broadcasts the roll and the target's status; a Scene's participants are
  characters or blueprint-built monsters (`SceneParticipantEntry`).
- **Core**: `castSpell` reports cost, GD, Duração, damage, `spellEffect`, granted chains; it applies nothing.

## Gaps common to both trees

1. **No Efeito Alternativo can be cast.**
   - The client never sets `SpellCastRequest#useAlternateVersion`.
   - Affected: Procrastinar Ferimento, Benção Bifurcada, Cura em Massa, Fonte da Juventude, Corrente Abençoada,
     Falsa Matilha, Predador Regional, Laboratório de Lacerto.
2. **"Pessoal ou Toque" is cast as Pessoal only.**
   - Every Vida Magia has `alternateTargeting(TOQUE)`, but the client reads only `getTargeting()` (PESSOAL), so it
     never asks for a target and always heals or cleanses the caster.
3. **"GD … ou DM do alvo (maior)" is never applied.**
   - `Spell#isCastingDifficultyFlooredByTargetMagicDefense` has no reader in core or client.
   - Affected: Revigorar, Revigorar Maior, Cativar Animal.
4. **Correntes de Efeitos on a cast are never judged.**
   - `SpellCastingResult#spellEffect` expects the caller to chain a triggered Corrente (`Sobrecura`).
   - The client never compares the Conjuração margin to the Corrente margin, so Sobrecura (Revigorar, Revigorar
     Maior, Nova Rejuvenescedora, Benção Bifurcada, Cura em Massa) never fires.
5. **Casting criticals are ignored.**
   - The Efeito Crítico of the Conjuração roll (Amenizar, Imunizar, Potencializar) is never applied by the client.
   - Core: Potencializar's application is still TODO ("no running Magia Duração").
6. **Concentração.**
   - Nothing reports a caster's focus breaking (cast another Magia, or attack).
   - Core's `Scene#breakConcentration` covers summons only; a Concentração effect on a sheet (Corpo Fechado) has no
     transition.

## VIDA

| Magia | Gap |
| --- | --- |
| Aliviar a Dor | "O efeito de cura … só afeta o alvo 1 vez, voltando a afetá-lo somente após … Descanso Longo" is not enforced (`markAffectedUntilRest` exists). |
| Procrastinar Ferimento (alt.) | Reação to an incoming attack, on self or an ally in Distância Curta. Core's `DamageInteraction#postponing` exists; the client offers no Reação and builds no effect (`authoredKinds` empty). Estancar (no Sangramento from that hit) is not modelled. |
| Benção Bifurcada (alt.) | Two targets; the client names one. Its GD "muda pra Médio" — verify the alternate's column. |
| Toque Curativo / Remover Maldição | GD varies by the affliction's origin (Semente…Florescente, Muito Difícil for Habilidades Monstruosas/Aventyrs); check what is authored. With no GD the client counts any roll as a success. Transferir Doenças e Venenos is not modelled. |
| Cura em Massa (alt.) | "você e todos os outros personagens à até 2m de você" — authored as Pessoal, not a caster-centred area. |
| Nova Rejuvenescedora | Caster-centred area, ok. The client's area path builds one effect with `isHostile=false`, so enemies are healed in full, not halved (core says the caller should build per occupant via `resolveEffect`). |
| Fonte da Juventude (alt.) | No effect at all: PV, PM and PD as a Descanso Curto for everyone affected but the caster, hostiles as a Mínimo. |
| Exorcizar | Cleansing ok. Barganha Negra not modelled. |
| Corrente Abençoada (alt.) | Extra creatures adjacent to you or to one healed, +3PM each — no multi-target or extra PM. |
| Corpo Fechado | Cleansing ok. "Imune à Malefícios" while it lasts is not built (gap catalog). Concentração + 2 is not tracked on the target. |

## ALIADOS DA NATUREZA

1. **The client never calls `NatureInvocationService`.**
   - A cast of any of the seven Magias lands nothing: no effect is authored, so the target only gets an "effect" log
     line.
   - Each Magia needs its own call after a successful Conjuração:
     - Predador Regional / Falsa Matilha / Laboratório via the alternate choice;
     - Canção's extra and strengthened animals and its Fauna Flora (the Corrente margin);
     - the Experimento's and Orgulho's dice.
2. **Targeting is wrong for an invocation.**
   - Aliados, Canção, Experimento, Totem and Orgulho are "Distância (Adjacente)", so the client asks for a combatant;
     an invocation names an empty hex next to the caster for its token.
   - Despertar Anciente is Toque on a tree (narrative).
3. **Cativar Animal.**
   - The client rolls it as an attack vs DM on a foe, then lands no effect.
   - It needs:
     - `captivate` with a Cavaleiro/Torre pick;
     - the animal leaving the turn order while it serves;
     - a blueprint foe being able to be an `ANIMAL` (check the GM monster editor exposes `CreatureType`).
4. **Summons in the Scene (invocations plan Phase 4–5).**
   - **API:** a summon participant shape — template kind, Conjurador Graduação, rolled powers, caster id; controlled by
     the caster's `playerId`; spent PV/PM/PD; Duração/concentration/exclusivity state — with create/dismiss
     endpoints and STOMP events.
   - **Client:**
     - place the token, rebuild the `NatureSummon` locally, give the caster's player its Turn;
     - on expiry, a fall, or replacement, dismiss it and broadcast the dismissal;
     - Totem: ask which animals are in Distância Longa each Rodada and call `blessFromTotem`;
     - report `breakConcentration` when the caster casts or attacks.
   - **Subordinados:** stored on the character sheet (API) and shown in the hub and the Cena.
5. **Extra PM** (`Invocation#extraManaCost`, Predador +1, Canção +2/+1) must be priced before the cast is allowed and
   paid with it.

**Steps 3–4 (ALIADOS end to end), as built — core 0.0.95/0.0.96, API, client:**

- **API summon participant.** An invoked creature is a `MonsterSheetDocument` with a `summon` descriptor (kind,
  Graduação, powers, caster) in place of a blueprint, owned by the caster's player.
  - The Scene keeps `SceneSummonEntry` (Duração, Concentração's trailing Rodadas, exclusivity group, replaced) and
    `SceneSummonSpawnerEntry` (Totem).
  - `SceneService` runs core's rules:
    - placed after its caster, with the caster's Iniciativa, group and rotation;
    - replaced when the caster's Turn ends;
    - ticks and leaves at the Rodada boundary;
    - spawners invoke there;
    - a caster leaving takes its summons;
    - `releaseConcentration`.
  - Endpoints: `POST /scenes/{id}/summons`, `DELETE /scenes/{id}/summons/{summonId}`,
    `POST /scenes/{id}/summons/release/{casterId}`, `POST /scenes/{id}/summon-spawners`. A turn advance that changes the
    roster re-broadcasts it.
- **Client.**
  - A successful Aliados cast becomes a core `InvocationPlan` posted to the server. Predador/Laboratório come from the
    Efeito Alternativo button, Canção's counts from the modal, Fauna Flora from the Corrente margin, Potencializar adds
    Rodadas, and the extra PM is priced and paid with the cast.
  - The caster's player controls each summon (`syncControlledSummons`), and it is chosen automatically on its Turn.
  - A Lacerto creature's Correntes ride its attacks, and a summon at 0 PV is dismissed.
  - Concentração breaks release on the server.
  - Totem de Gaea is a spawner, and its blessing applies at the caster's Turn to the animals they control in
    Distância Longa.
  - Cativar Animal picks a Cavaleiro/Torre benefit and judges the target by its stat block's creature type.

**Closed in core 0.0.97 + client:**
- Membros Múltiplos' pair (`CriaturaFeat`, an attack-row checkbox, the second attack offered).
- Sopro Elemental (an action on the summon: cone aimed at a foe, falloff per UD, Refrigeração 1).
- Aura Elemental (adjacent foes at the summon's Turn start).
- The Anciente's Bênçãos (regeneration at its Turn; even-Rodada shared heal, relayed as `fixedHealing`).
- Subordinados and the Corpo Fechado/Procrastinar wards shown on the character info card.
- Breath and aura are Natural (table ruling, 2026-10-01).

**Still open in ALIADOS:**
- Subordinados are not persisted: like every in-Cena timed effect, Cativar's Subordinado lives on the owning client's
  sheet.
- Cativar's "until a Descanso" mark lands on the caster's stand-in, not the GM's real foe.
- A summon's sheet is a core `CharacterSheet` on its controller's client, so it is not typed `ANIMAL` for other clients'
  checks.
- The Totem blesses only animals its caster's client controls.
- Damage a summon deals to a GM-run foe (attack, breath, aura) lands the way every player attack does today.
