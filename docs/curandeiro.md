# Curandeiro — caller guide

Curandeiro is scaffolded, not complete. Two Abilities are real: **Levantar os Caídos** and **Curar os
Mortos**, the ones that bend the limits on healing the fallen. Every other trait is authored with a
TODO naming its blocker (see `title/curandeiro/*`).

## The limits they bend

`CombatantSheet#heal(int amount, HealingSource source)`:

| Target's status (before the heal) | Result |
| --- | --- |
| Coma (`COMMA`) | At most `COMA_HEAL_CAP` (1) PV per distinct `HealingSource#key()`, **total** while the Coma lasts. A used key heals nothing more. A real Descanso (`HealingSource.rest`) is capped too, but repeatable. |
| Dead (`DEAD`) | Refused, unless the target is not `isBeyondRevival()` and a Título of the healer's `claimRevival`s it. |
| Anything else | Unlimited, as before. |

Regeneração is subject to both limits, and its whole budget is one effect. `healFromLifeSteal` and
the unsourced `heal(int)` are exempt.

## What a caller must pass

- **The caster**, wherever a heal is built. There are three places:
  - `SpellEffectContext.of(hostile, casterSheet)`; `SpellCastingServiceImpl` already does this;
  - the 5-arg `SpellHealingEffect` constructor;
  - `new Sobrecura(casterSheet, parentSpell, d6)`.

  Without it, the heal is limited as if no Título could help.
- **A `HealingSource`** for any heal effect you apply by hand.

## What a caller must do

- Call **`startNewRound()`** and **`startNewScene()`** on each sheet at those boundaries. `Scene`
  does this for its participants.
  - The Rodada count drives Curar os Mortos' window: ⌊Medicina e Cura / 2⌋ Rodadas since death.
  - A new Cena closes that window, and ends "Coma begun in this Cena" for Levantar os Caídos.
- **Activate Curar os Mortos before the heal** (`Curandeiro#activateCurarOsMortos`, 2PD, "+1PA"
  reported). Each activation banks one charge on the Curandeiro, and the next qualifying heal on a
  dead target spends it.
  - A qualifying heal is a Magia Divina/Natural or a Habilidade de Curandeiro, inside the window.
  - If the heal left them still dead (PV not above −max), activate again.
  - Once they are back, they are in a Coma begun this Cena, so Levantar os Caídos heals them in full.
- Unspent charges are dropped at the next Cena.

## Readings

- "adquiridos na mesma Cena" (Levantar) is read as *the Coma* being acquired in this Cena.
- "Suas Magias" (Levantar) is any Magia of the holder's. Curar os Mortos names only Divinas and
  Naturais.
- Benção de Boros' "Custo: passiva" beside "Tempo: +1PA" is read as passive.
- A heal that the Coma cap and halvings reduce to 0PV does not use its key up.
- A Curar os Mortos charge is spent when the heal is claimed, even if halvings then leave it at 0PV.
