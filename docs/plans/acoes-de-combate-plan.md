# Ações de Combate, Danos e Resistências — import plan

Source: `V19 - Ações de Combate, Danos e Resistências 20261012.docx`, converted verbatim to
`docs/rules/acoes-de-combate.txt` (2026-10-13). Its resistance section supersedes the matching part of
`docs/rules/defesas-e-resistencias.txt`, which was updated to agree with it.

## Table rulings (2026-10-13)

- **One table of resistance values.** Every RD/RM/RE/RA/RDS/RC figure is a count of *instances* times the
  per-instance value in `character.Resistance`. "RD" (with no number) or "RD 1" is one instance; "RDS 5" or
  "RDS igual a 1 + …" is that many instances. No source writes a raw point figure any more.
- **An unnamed item reduction is RDS.** An item Favor that says only "Dano … sofrido é reduzido em -N"
  (Armadura Completa, Couraça, Meia-Armadura, Escudo Médio/de Corpo, the Capas) grants **N RDS
  instances**. This deliberately reaches more damage types than RD does. Items whose text names RD/RM/RA keep
  that resistance.
- **Queda and Asfixia deal Dano Primordial, and RA does not reduce Primordial** — this follows the text. It replaces
  the 2026-10-12 interim "RA only" ruling.

## Phases

### Phase 0 — resistance constants and the rule changes — *built (core 0.1.6.13)*
Behaviour changes it made, all by the rulings above: Escudo de Sombras and Pele Rija give one RDS instead of 3/2
(the number in "+3 em suas Defesas e RDS" is the Defesas'); every item "reduzido em -N" Favor moved from RD to RDS
(Armadura Completa/de Justa/Couraça/Meia-Armadura/Robe de Guerra, Escudo Médio/de Corpo, Manto Nortenho, the
Equilibrada and Defensora Obras-Primas); Vigor de Épona ("-1 todo dano sofrido") is RDS; the Sobretudo do
Inquisidor's Favor lost its "RM 1" (it is a Duração clause — Phase 2). Non-item unnamed reductions that scope a type
(Rigidez da Montanha's Físico, the Nature summons' Físico, Benção Vulcana/Elduriana, Opala's Profano) keep their
scope, sized in RDS instances. Devoto's "Margem Crítica Menor de seus inimigos -1" stays an enumerated 1, not RC.

1. `character.Resistance`: `RD(2)`, `RM(2)`, `RE(3)`, `RA(2)`, `RDS(1)`, `RC(2)`, with `instances(n)`. Also
   `RC_MAJOR_MARGIN` (1), `RA_CRITICAL_MARGIN` (1), `DURATION_REDUCTION` (1) and `EFFECT_CHAIN_BASE` (5).
2. Retire the scattered constants: `DamageService.DEFAULT_DAMAGE_REDUCTION`/`DAMAGE_TAKEN_REDUCTION_INSTANCE`,
   `DamageServiceImpl.ELEMENTAL_RESISTANCE_PER_INSTANCE`, `CombatantSheet.CRITICAL_RESISTANCE_INSTANCE`, the
   three `MonstrousTraits` ones, `EffectChainService.BASE_REQUIRED_MARGIN`. Each one either delegates to
   `Resistance` or is removed, with its callers moved over.
3. Audit every grant site: `ItemBonus`, `TemporaryBonus`/`Blessing`, `@Modifier` methods, and the `resolve*`
   resistance hooks on `Feat`/`AttributeAbility`/`SkillCompetencyAbility`/`EgoAdvantage`/Título abilities/monster
   abilities. Rewrite each value as `Resistance.X.instances(n)`, with `n` read from its rules text. Unnamed item
   reductions → RDS.
4. Rule changes: RE −3; RDS skips Profano/Sagrado/Umbral hits; RA skips Primordial; `QUEDA`/`ASFIXIA` are
   Primordial (unreduced).
5. An architecture test that fails on a raw numeric literal beside a resistance `ModifierType`.

### Phase 1 — Danos de Queda and de Asfixia
Resistência à Quedas (2 × Categoria, min 2UD); half the excess height as Primordial damage; +1d6 per 10UD excess
with Feridas Dolorosas until the next Descanso; death past 40UD excess. Asfixia: from Vigor Rodadas on, 1d6 per
instance growing to 3d6, Feridas Dolorosas at the 4th instance, death at the 6th. Caller-driven like every other
geometry (height, submersion).

### Phase 2 — Resistance durations
RD/RM/RE/RA: −1 Unidade on the Duração of a matching continuous effect. Not cumulative between instances.

### Phase 3 — Movements
Movimento Padrão (1PA, provokes Defender o Perímetro), Correr (2PA, Atletismo GD Médio+, ×2 Movimento, +2UD per GD
beaten, adjacent-only provocation, a Falha Crítica means Caído, not in Terreno Difícil), Escalar and Nadar (2PA, half
Movimento, the Vertical/Natação mode skips the roll, Desprevenido while threatened, failure bands at margin 5),
Saltar (2PA, half Movimento +2UD per GD), Recuo Defensivo (2PA, once per Rodada, away only, half Movimento, no
Defender o Perímetro), Reposicionar (now half the Categoria de Tamanho, min 1UD — replaces the 1UD ruling), Voar
(starting provokes).

### Phase 4 — Manoeuvres
Encontrão (3PA, a straight-line charge that pushes 1UD/2UD on a critical, carries Lançar ao Chão, −2 Defesas on a
miss), Atacar Objetos / Desarmar (2PA, GD +1 nível, Meio-Dano to the item, carries Desarmar), Imobilizar (3PA,
only a Rendido/Inconsciente target). Deltas to the built ones: Investida's minimum distance; Agarrar's
Categoria bonus.

### Phase 5 — Conjurar Defensivamente
+1PA, GD +1 nível, blocks the Impedir Conjuração Reação (not modelled yet), Desvantagem on the Magia's damage and healing.

### Phase 6 — API and client wiring
The actions from Phases 1–5, plus the draw-weapon / ready-shield buttons left open by the 20261012 Correntes.
