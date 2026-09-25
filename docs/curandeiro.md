# Curandeiro — caller guide

Every Curandeiro trait is real. Passive traits are `Curandeiro` hooks that core services scan.
Activated traits go through `AventyrTitle#activateAbility(trait, TitleAbilityActivationRequest)`.
Core **reports** PA and PM and never deducts them. It **spends** PD, PV and Ego points itself.

## The limits on healing the fallen

`CombatantSheet#heal(int amount, HealingSource source)`:

| Target's status (before the heal) | Result |
| --- | --- |
| Coma (`COMMA`) | At most `COMA_HEAL_CAP` (1) PV per distinct `HealingSource#key()`, **total** while the Coma lasts. A used key heals nothing more. A real Descanso (`HealingSource.rest`) is capped too, but repeatable. |
| Dead (`DEAD`) | Refused, unless the target is not `isBeyondRevival()` and a Título of the healer's `claimRevival`s it. |
| Anything else | Unlimited, as before. |

Regeneração is subject to both limits, and its whole budget is one effect. `healFromLifeSteal` and
the unsourced `heal(int)` are exempt. No heal ever recovers PV locked by Transferir Vitalidade.

## What a caller must always pass and do

- **The caster**, wherever a heal is built. There are three places:
  - `SpellEffectContext.of(hostile, casterSheet)`; `SpellCastingServiceImpl` already does this;
  - the 5-arg `SpellHealingEffect` constructor;
  - `new Sobrecura(casterSheet, parentSpell, d6)`.

  Without it, the Curandeiro's hooks never see the heal.
- Call **`startNewRound()`** and **`startNewScene()`** on each sheet at those boundaries. `Scene`
  does this for its participants. They drive:
  - the death and fallen windows;
  - "Coma begun in this Cena";
  - Ego-loan settlement;
  - Transferir Rancor's "dealt damage this Cena";
  - clearing unspent charges.
- Call **`tickTemporaryEffects()`** at Turn end. That lets Curandeiro Veloz's once-per-Rodada mark lapse.

## Trait by trait

| Trait | How to use it |
| --- | --- |
| **Despertar: Vantagem** | Nothing to do. Every Medicina e Cura roll of the holder gets +2 (`resolveSkillRollBonus`). |
| **Despertar: the touch** (`CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO`, 3PA) | Roll Medicina e Cura against `DonsDoCurandeiroInteraction.DIFFICULTY` (Difícil) yourself. Activate with `target`, `choice(Outcome.SUCCEEDED/FAILED)`, and optionally `choices(Corrente.BEIJO_DE_BOROS)` (needs a non-combat `sceneContext`). Success heals as a Descanso Curto (Longo with Beijo). Either way the target is locked out until the **Curandeiro's** Descanso Longo. |
| **Domínio da Cura** (Primário) | Nothing to do. `Character#getEffectiveSkill(DOMINIO_DO_MANA)` picks Medicina e Cura when its Graduação is higher. The Domínio do Mana roll and Graduação-based effects use it. Prerequisites don't. |
| **Médico de Guerra** | Nothing to do. Every heal the holder makes offers +2 before halving or the Coma cap. |
| **Mártir Altruísta** | Nothing to do. A healing Magia that can reach others gets −1 GD level (`SpellCastingResult#getCastingDifficultyLevel`). |
| **Curandeiro Veloz** (2PD, Ação Livre) | Activate once per Rodada. The next heal-others Habilidade de Curandeiro (reported `actionPointCost`) or healing Magia Natural/Divina (`SpellCastingResult#getActivationTime`) costs −2PA, floored at 1, and spends the charge. |
| **Levantar os Caídos** | Nothing to do. The holder's Magias and Curandeiro heals ignore the Coma cap on a Coma begun this Cena. |
| **Curar os Mortos** (2PD, +1PA) | Activate **before** the heal (`activateCurarOsMortos`). One activation pays for one heal on a dead target. The heal must be a Magia Divina/Natural or a Habilidade de Curandeiro, within ½ Medicina e Cura Rodadas of the death. If the target is still dead afterwards, activate again. Once revived, they are in a Coma begun this Cena, so Levantar os Caídos applies. |
| **Benção de Boros** | Nothing to do. While the holder is at 0 PV or below, within ½ Medicina e Cura Rodadas of falling, Curandeiro activations cost double PD and Magias double PM (`SpellCastingResult#getManaCost`). Core refuses no one an action at 0 PV, so the "may act" half needs nothing. |
| **Tratamento Furtivo** | Nothing to do. No Reação in core is triggered by a heal. |
| **Cura Protetora** (2PD, +1PA) | Activate beside the heal it rides on (a Broto+ Magia or a Habilidade de Curandeiro), with that heal's `target`. Healer and target get +4 Defesas for 1 Rodada. A second activation renews it and does not stack. |
| **Encanto Regenerativo** (2PD, +1PA) | Activate together with the Magia or Encantamento cast on an ally, with that ally as `target`. The ally heals its Vigor in PV. |
| **Doutor de Eldur** | Nothing to do. The holder's Descansos count one category higher (`RestService#applyRest`). Médico de Guerra activations and healing Magias cost −1PA, floored at 1. |
| **Transferir Vitalidade** | **Habilidade:** pass `choices(TitleCostPayment.HIT_POINTS)` when activating a Curandeiro trait aimed at another character; its PD is paid as locked PV. **Magia:** set `SpellCastRequest#payManaWithHitPoints` for a single-target Divina/Natural Magia on a non-hostile other character, then pay `SpellCastingResult#getVitalityCost()` with `caster.payWithVitality(n)`. Locked PV come back only through `applyRest(…, verdadeiro = true)`. |
| **Transferir Determinação / Essência** (1PD, 3PA) | `target` = the ally. Either pass `choice(Mode.POINTS)` with the amount as an `Integer` among `choices`. The whole amount is given; the ally keeps what it is missing (PD or PM) and the rest is lost. Or pass `choice(Mode.EGO_POINT)`, which lends 1 temporary Autocontrole or Sorte until the end of the Cena. |
| **Transferir Rancor** | Nothing to do beyond delivering damage through `DamageService` with a `SceneContext`. Each enemy hit on the holder gives every ally, cumulatively for 1 Rodada, −1 GD level on attack and Domínio do Mana rolls and +1d6 dano, reported as `InteractionResult#getExtraDamageDice()` for you to roll. It stays silent in a combat Cena once the holder has dealt damage. |

## Across clients

When a trait's target is another player's character, the activating client holds only a stand-in for it.
aventyrs-game-client then relays the effect (see its `docs/wiring-a-titulo.md`, layer 9):

- `HealingSource#relay(targetIsDead)` resolves the healer's half on the healer's real sheet. It spends a
  Curar os Mortos charge when the target is dead.
- The owner's client applies it with `DonsDoCurandeiroInteraction#healTarget`,
  `EncantoRegenerativoInteraction#healTarget`, `CuraProtetoraInteraction#protect`,
  `TransferenciaInteraction#receivePoints` or `receiveEgoLoan`.

`CombatantSheet#heal` judges a relayed source against the target's own Coma and death window.

## Readings

- "adquiridos na mesma Cena" (Levantar) is read as *the Coma* being acquired in this Cena.
- "Suas Magias" (Levantar) is any Magia of the holder's. Curar os Mortos names only Divinas and
  Naturais.
- "Magias que permitam que outros personagens recuperarem PV" is a Magia with a heal whose Alcance,
  either half of a "Pessoal ou Toque", reaches past its caster.
- Benção de Boros' "Custo: passiva" beside "Tempo: +1PA" is read as passive. Its doubling covers all
  Magias and all Habilidades de Curandeiro while fallen.
- An Ego loan counts as "unused" if the ally's temporary points have not fallen since it landed.
- A heal reduced to 0 PV by the Coma cap or halvings does not use its key up.
- A Curar os Mortos charge is spent when the heal is claimed, even if halvings then leave it at 0 PV.
