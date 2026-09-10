package org.aventyrs.core.modifier;

/**
 * Registry of the kinds of numeric bonuses abilities, feats, titles or items can grant.
 * Adding a new kind of bonus to the system only requires a new constant here — nothing
 * that resolves or consumes modifiers needs to change.
 *
 * <p>{@code SKILL_ROLL_BONUS} applies broadly, to every Perícia roll — but a bonus that
 * should only apply to *one specific* Perícia (e.g. a temporary buff an ally received for
 * Atletismo specifically, not every roll they make) needs its own constant instead, since
 * {@code @Modifier} is a compile-time-fixed annotation value and {@link
 * org.aventyrs.core.modifier.ModifierResolver} has no way to filter a scan by "which skill is
 * this roll for". Every Perícia gets one of these — see {@code
 * org.aventyrs.core.skill.SkillType#getRollBonusType()} for the per-{@code SkillType} lookup —
 * summed *alongside*, not instead of, {@code SKILL_ROLL_BONUS} by {@code
 * org.aventyrs.core.skill.AbstractSkillInteraction}.
 *
 * <p>{@code DEFESAS} follows that exact same broad-plus-scoped shape one level down: it means
 * "both Defesas", while {@code PHYSICAL_DEFENSE}/{@code MAGIC_DEFENSE} scope a bonus to DF or DM
 * alone. All three are summed additively by {@code
 * org.aventyrs.core.character.services.DefenseService}, so a source can name whichever it needs
 * and a character holding some of each still totals correctly — see {@code
 * org.aventyrs.core.character.DefenseType}.
 *
 * <p>{@code LIFE_MULTIPLIER} and {@code HIT_POINTS} are the two ways to grow a creature's PV,
 * and they are not interchangeable: the first scales with Vigor (so it makes a tough creature
 * tougher in proportion to what it already is), while the second is the flat "recebe Bônus
 * Mágico de +NPV" shape, whose amount is stated outright and must not vary with the recipient's
 * Vigor. Both are read by {@code
 * org.aventyrs.core.character.services.HitPointsService#getMaxHitPoints}.
 */
public enum ModifierType {
    LIFE_MULTIPLIER,
    HIT_POINTS,
    SIZE_CATEGORY,
    MANA_MULTIPLIER,
    DETERMINATION_MULTIPLIER,
    ACTION_POINTS,
    SKILL_ROLL_COST,
    SKILL_ROLL_BONUS,
    REACTIONS,
    FREE_ACTIONS,
    INITIATIVE,
    MOVEMENT,
    /**
     * A flat modifier to a <b>dano roll</b> — not to a Perícia roll, and not damage reduction.
     * Its counterpart on the roll side is {@code SKILL_ROLL_BONUS}: "Vantagem em rolagens de
     * Dano" is the same flat +2 that Vantagem is anywhere else (see {@code Skill#ADVANTAGE_BONUS}),
     * and a Desvantagem the same -2.
     *
     * <p>Exists so a source that carries {@code ModifierType}-typed data rather than a typed
     * {@code DamageBonus} can still reach a dano roll — a {@code TemporaryBonus} granted by
     * another character's action, or a {@code ConditionType.ConditionEffect} (Caído's "Desvantagem
     * em rolagens de Dano Corpo-a-Corpo", the fear ladder's proximity-scoped one). Abilities that
     * grant <i>typed</i> extra damage keep returning a {@code DamageBonus} instead; both are
     * summed together by {@code AbstractSkillInteraction}.
     */
    DAMAGE_ROLL_BONUS,
    DAMAGE_REDUCTION,
    /**
     * Resistência à Magias (RM) — the magic-damage counterpart of {@link #DAMAGE_REDUCTION}. Per
     * {@code docs/rules/defesas-e-resistencias.txt} each instance reduces Dano Mágico
     * não-PRIMORDIAL by -2, so a bonus of {@code DamageService#DEFAULT_DAMAGE_REDUCTION} here is
     * one instance — the same figure a numberless "você recebe RM" clause grants.
     *
     * <p>Resolved by {@code DamageService#getTotalMagicReduction} from the same five sources RD
     * uses (the three-source {@code @Modifier} scan, equipped {@link
     * org.aventyrs.core.item.Item}s, held Talentos via {@code Feat#resolveMagicReduction}, and a
     * round-scoped {@code TemporaryBonus}), and added to the mitigation total by {@code
     * calculateFinalDamage} <b>only when the incoming damage is typed {@code
     * DamageType#MAGICO}</b> — an unclassified hit ({@code damageType} {@code null}, which is
     * what most callers still pass) gets none of it, since "caller didn't say" is not "this was
     * magic".
     *
     * <p><b>RD is still type-blind, so magic damage is currently over-mitigated.</b> The rules
     * give RD only to Dano Físico não-PRIMORDIAL e não-ELEMENTAL, but {@code
     * getTotalDamageReduction} applies it whatever the type — so a hit typed {@code MAGICO} takes
     * RD <em>and</em> RM today. Narrowing RD belongs to the damage-type system (CLAUDE.md's
     * "Damage-type-scoped mitigation" row), not here; this constant only closes the missing
     * resistance, not the over-broad one.
     *
     * <p>Resistência Elemental (RE) is the third sibling in that rules block and has no constant:
     * nothing in the catalog grants a plain RE that isn't also scoped to one {@code
     * org.aventyrs.core.magic.ElementalType}, which is the {@code DamageDescriptor} path already
     * built for equipment. Add it with its first real consumer.
     */
    MAGIC_REDUCTION,
    HALF_DAMAGE,
    ABSOLUTE_DAMAGE_REDUCTION,
    /**
     * Resistência à Críticos (RC) — a <b>defender-side</b> reduction of the Margem Crítica an
     * attack made against this combatant can reach. Per {@code
     * docs/rules/defesas-e-resistencias.txt}, each instance lowers the attacker's Margem Crítica
     * Menor by -2 (and Maior by -1), so a bonus of {@code 2} here is one instance.
     *
     * <p><b>This constant is only the round-scoped half of RC.</b> A {@code Blessing}/{@code
     * TemporaryBonus} typed with it ({@code AnaoFeat#VIGOR_DO_INVERNO}'s combat-start grant) is
     * one source; a <em>standing</em> grant from the holder's Raça ({@code
     * Race#getCriticalResistance()}) or a held Talento ({@code Feat#resolveCriticalResistance})
     * is the other, and carries no {@code ModifierType} at all. Both are summed by {@code
     * CombatantSheet#getTotalCriticalResistance}, which is what a consumer should call — reading
     * {@code getTemporaryBonus(CRITICAL_RESISTANCE)} alone sees only the timed half.
     *
     * <p><b>Partial reader.</b> Only {@code
     * org.aventyrs.core.skill.AbstractSkillInteraction} consumes that total — the attacker-rolls
     * path subtracts the attack target's figure from the summed Margem Crítica Menor widening
     * before {@code SkillRoll#getCriticalResult(int)}, so it also reaches {@code
     * org.aventyrs.core.combat.AttackDelivery}, which routes through that same interaction. Not
     * read on the {@code org.aventyrs.core.combat.AttackReceiver} mirror (the attacker rolls
     * nothing there). The "-1 à Margem Crítica Maior" clause has no expression — this ruleset
     * models no Acerto Crítico Maior margin at all — and the "até o mínimo de 17" floor is
     * approximated as "cannot push the attacker below their own baseline margin" (a net negative
     * widening is floored at 0 by {@code getCriticalResult}). No "não-PRIMORDIAL" scoping either;
     * the crit path carries no PRIMORDIAL marker.
     *
     * <p>Still without a home: an <b>item-scoped</b> RC, a value a produced or worn Equipamento
     * carries and passes to its wearer ({@code ProfissaoCompetencyAbility#FORJA_VULCANA}) —
     * neither {@code Item} nor the sheet total has a notion of one — and a {@code
     * SkillCompetencyAbility}/{@code AttributeAbility} standing grant, which no constant in the
     * catalog asks for.
     */
    CRITICAL_RESISTANCE,
    DEFESAS,
    PHYSICAL_DEFENSE,
    MAGIC_DEFENSE,
    ATTENTION_ROLL_BONUS,
    ARTES_ROLL_BONUS,
    ATLETISMO_ROLL_BONUS,
    DIRIGIR_E_CAVALGAR_ROLL_BONUS,
    DOMINIO_DO_MANA_ROLL_BONUS,
    ATAQUE_A_DISTANCIA_ROLL_BONUS,
    ATAQUE_CORPO_A_CORPO_ROLL_BONUS,
    ESQUIVA_E_APARAR_ROLL_BONUS,
    EMPATIA_SELVAGEM_ROLL_BONUS,
    FURTIVIDADE_ROLL_BONUS,
    MEDICINA_E_CURA_ROLL_BONUS,
    PERSUASAO_ROLL_BONUS,
    PROFISSAO_ROLL_BONUS,
    CONHECIMENTOS_ROLL_BONUS,

    /**
     * A round-scoped bonus to one Atributo — the vehicle for a temporary "recebe Bônus de +1 em
     * Carisma e Instinto" ({@code VampiricoFeat#DOM_DE_MIRCALLA}, a Poder Vampírico). One per
     * {@link org.aventyrs.core.character.AttributeDomain}, looked up via {@code
     * AttributeDomain#getBonusModifierType()}.
     *
     * <p><b>Partial reader.</b> Only {@code
     * org.aventyrs.core.skill.AbstractSkillInteraction} consumes these so far — a bonus reaches a
     * Perícia roll governed by that Atributo, and nothing else (HP/PM/PD/Defesa/Conjuração still
     * read {@code AttributeValue#getTotal()} directly). This is the "Round-scoped Attribute
     * bonuses" gap being closed one consumer at a time. The <b>permanent</b> {@code
     * Feat#resolveAttributeBonus} grant is <em>not</em> restricted this way — it is summed by
     * {@code Character#getEffectiveAttributeTotal}, which every Atributo-total reader calls.
     */
    STRENGTH_BONUS,
    VIGOR_BONUS,
    DEXTERITY_BONUS,
    FOCUS_BONUS,
    INSTINCT_BONUS,
    GNOSE_BONUS,
    CHARISMA_BONUS
}
