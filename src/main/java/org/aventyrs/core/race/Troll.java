package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines what the Trolls race can do under each rule-set — a stateless race, like {@code
 * Anao}/{@code Elfo}. Three of its traits are mechanically real today:
 *
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +2 Força.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#MONSTRUOSO}.</li>
 *   <li><b>{@link #getCriticalEffectImmunities()}</b> — the enumerable half of Anatomia Vegetal:
 *   immune to {@link CriticalEffectType#ATORDOANTE}, {@link CriticalEffectType#FERIDA_PROFUNDA}
 *   and {@link CriticalEffectType#SANGRAMENTO}. Trolls are the first player-facing consumer of
 *   {@link Race#getCriticalEffectImmunities()}, which {@code
 *   AbstractCombatantSheet#getCriticalEffectImmunities()} reads so the set reaches {@code
 *   CriticalEffect#applicableTo} with no per-race wiring. Only Sangramento has an implementation
 *   behind it today; the other two are named-only constants, and are resisted correctly the day
 *   someone builds them — exactly what {@link CriticalEffectType}'s own javadoc exists for.</li>
 * </ul>
 *
 * <p><b>Categoria de Tamanho is seeded at {@link SizeCategory#ZERO}</b>, the youngest rung of
 * Crescimento Constante ("Trolls jovens (até 120 anos) pertencem a Categoria de Tamanho 0"), not
 * because the +1/+2 rungs are wrong but because nothing can reach them: there is no age concept
 * on {@link Character} or {@link Race}, so a size that climbs with age has no input. Seeding the
 * first rung is the honest floor, and a Narrador wanting an ancião sets {@code
 * sizeCategory(PLUS_TWO)} on the builder directly, the same way any other authored deviation
 * works.
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Anatomia Vegetal's other three clauses</b> — <i>Resistência a Críticos</i> is not a
 *   stat this core computes at all (the same unbuilt piece {@code ProfissaoCompetencyAbility}
 *   cites, and distinct from the immunity list above, which is an all-or-nothing filter rather
 *   than a resistance value); the <b>vulnerabilities</b> (Fogo for every Troll, plus Natural for
 *   a Troll do Inverno and Gelo for one da Floresta) need damage-type-scoped mitigation, which
 *   {@code DamageService} has no notion of — and a <i>vulnerability</i> is a further missing
 *   stage beyond that, since nothing amplifies damage either; "recuperam danos sofridos por
 *   estes Elementos apenas com Descansos Verdadeiros" needs damage to remember what dealt it,
 *   which {@code CombatantSheet#getDamageTaken} is a single figure with no provenance; and the
 *   <b>Roubo de Vida immunity</b> ("de personagens que não tenham Anatomia Vegetal") has a real
 *   mechanism to attach to ({@code LifeStealService}) but no immunity hook on it, and no way to
 *   ask whether the <i>attacker</i> shares this trait.
 *
 *   <p>Note the two sub-lineages the vulnerabilities name — Troll do Inverno and Troll da
 *   Floresta — are deliberately <b>not</b> modeled as a nested choice enum the way {@code
 *   Aviano.Subtipo} or {@code Ogro.Aptidao} are: those two exist because the choice changes an
 *   Atributo map that is real today, whereas every clause distinguishing the two Trolls is
 *   blocked on the same missing vulnerability mechanism. Add the enum with the mechanism, not
 *   ahead of it.</li>
 *   <li><b>Regeneração Reativa</b> (after taking damage from an enemy source, recover 2PV per
 *   Rodada on the Troll's own Turn, for Vigor Rodadas, capped at the damage taken and
 *   non-cumulative) — {@code TemporaryEffect} and the Turn lifecycle could carry the recovery,
 *   but nothing triggers off <i>being damaged</i>: {@code DamageService#applyDamage} reports a
 *   figure and mutates the sheet, with no hook for the victim to react (the same direction {@code
 *   DamageService} has never modeled — see CLAUDE.md's "Reactive/retaliation damage" row, of
 *   which this is the healing counterpart). "De fontes inimigas" additionally needs the damage to
 *   name its source, which {@code applyDamage} does accept but never records.</li>
 *   <li><b>Sono de Pedra</b> (while asleep, damage taken is halved, and the Troll wakes only to
 *   damage exceeding its Vigor in PV; +2PV extra per Descanso Longo ou Superior) — sleep is a
 *   state nothing tracks (the same "no Fadiga/asfixia" gap CLAUDE.md names), so the {@code
 *   HALF_DAMAGE} half has no condition to fire on. The Descanso half is the closer of the two:
 *   {@code RestService#getRecoveredHitPoints} does sum a bonus, but only via {@code
 *   AttributeAbility#resolveRestHitPointsBonus} — and {@link Race} can grant no {@code
 *   AttributeAbility}, only {@code SkillCompetencyAbility}, the same "no race-granted
 *   AttributeAbility concept exists" gap {@code Orc}'s own Vigor de Epona cites.</li>
 *   <li><b>Crescimento Constante</b> (Categoria 0 until 120, +1 until 180, +2 beyond) — no
 *   age/lifespan concept exists on {@link Character} or {@link Race}. See the Categoria de
 *   Tamanho note above for what is seeded meanwhile.</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core.</li>
 *   <li><b>Idiomas</b> (Silvestre + Antecedente, trocável por Arcano ou Anão conforme a
 *   linhagem) — same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~200 anos) — same "no age/lifespan concept" gap as every other
 *   race.</li>
 *   <li><b>2 Talentos de Sobrevivência</b> — {@link Race} has no hook to grant a {@code Feat} at
 *   creation, same gap as every other race's free Talentos.</li>
 * </ul>
 *
 * <p>None of the Características above fit {@code SkillCompetencyAbility}'s shape — the immunity
 * list is not a roll modifier at all and has its own hook — so {@link #getRacialAbilities()} is
 * left at {@link Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Neutros" is advisory, not a hard rule.
 */
public class Troll implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.STRENGTH, 2);
    }

    @Override
    public Set<CriticalEffectType> getCriticalEffectImmunities() {
        return Set.of(CriticalEffectType.ATORDOANTE,
                CriticalEffectType.FERIDA_PROFUNDA,
                CriticalEffectType.SANGRAMENTO);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }
}
