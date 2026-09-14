package org.aventyrs.core.race;

/**
 * How much of a character's {@link Race} is temporarily silenced — the answer {@code
 * org.aventyrs.core.feat.Feat#resolveRacialTraitSuppression} returns, folded across every held
 * Talento by {@code CombatantSheet#getRacialTraitSuppression()} and consulted wherever a racial
 * trait is aggregated.
 *
 * <p><b>A strictly ordered ladder, not a set of flags.</b> Each constant silences everything the
 * one before it does and more, so a consumer asks one {@code suppresses*} question rather than
 * switching on members, and {@link #strongest} can fold several Talentos by taking the highest.
 * Declaration order <em>is</em> the ordering, which is why nothing may be inserted in the middle.
 *
 * <p><b>Four rungs because four clauses ask for four different things</b> — the usual "promotion is
 * earned" restraint. Two of them share wording and land on {@link #ALL}; the other two are
 * genuinely narrower, and collapsing either into a stronger rung would silently widen a Talento:
 *
 * <ul>
 *   <li>{@link #NATURAL_WEAPONS_ONLY} — {@code VampiricoFeat#METAMORFOSE_DRACULEA}. Its text never
 *   says "abandonando traços raciais"; this rung exists because reading its "são substituídas por
 *   armas naturais" as also dropping the holder's Resistência a Críticos or shrinking them to
 *   human size would be an invention, not a reading.</li>
 *   <li>{@link #PHYSICAL} — {@code MonstruosoFeat#MIMETIZAR_FORMA_HUMANA}'s "perde características
 *   raciais físicas, como escamas, chifres, garras".</li>
 *   <li>{@link #ALL} — {@code DraconicoFeat#DRACONATO} ("abandonando quaisquer traços raciais
 *   existente") and {@code FeericoFeat#ANCIENTEFORME} ("abandonando seus traços raciais").</li>
 * </ul>
 *
 * <p><b>What no rung ever touches</b>, because none of these is a trait a body carries:
 * {@link Race#getCreatureType()} and {@link Race#getPrerequisiteCreatureType()} — looking human
 * does not stop you <em>being</em> a monster, and {@code MIMETIZAR_FORMA_HUMANA}'s own
 * Pré-requisito is {@code requiredCreatureType(MONSTRUOSO|FEERICO)}, so suppressing it would make
 * the Talento retroactively ineligible for its own holder; {@link Race#isMestico()}, an ancestry
 * fact; and the progression economy ({@link Race#getNewFeatCost}, {@link Race#getNewSkillCost},
 * {@link Race#resolveSpellAcquisitionCostReduction}), which is XP bookkeeping rather than anything
 * a transformation could hide.
 *
 * <p><b>Only the <em>racial</em> contribution goes.</b> Every trait below is aggregated from
 * several sources, and suppression drops exactly the {@code Race} term: a Talento-granted
 * Resistência a Críticos and a round-scoped {@code TemporaryBonus} both survive {@link #ALL}.
 */
public enum RacialTraitSuppression {

    /** Nothing is suppressed — the default, and every Talento but three. */
    NONE,

    /**
     * The holder's racial Armas Naturais are silenced, and nothing else. {@code
     * VampiricoFeat#METAMORFOSE_DRACULEA}'s "armas não podem ser utilizadas, são substituídas por
     * armas naturais": the shape brings its own, so the ones its holder was born with go — but
     * that sentence is about weapons and says nothing about the rest of a body.
     */
    NATURAL_WEAPONS_ONLY,

    /**
     * Every racial trait a body wears — Armas Naturais, plus the anatomy pair (Resistência a
     * Críticos and {@code CriticalEffectType} immunities) and the base Categoria de Tamanho.
     * Racial Habilidades and Atributo bonuses survive: a Gnomo passing for human has no horns to
     * show, but is no less clever for it.
     *
     * <p>⚠️ <b>Base Categoria de Tamanho here is an inference.</b> The clause enumerates
     * appendages — "escamas, chifres, garras etc." — and never mentions stature; it is suppressed
     * on the strength of "assumindo uma forma humana comum", a human form being human-sized. It is
     * the one trait on this rung the text does not name, and the first thing to revisit if the
     * rules are clarified.
     */
    PHYSICAL,

    /**
     * The whole race falls silent — everything {@link #PHYSICAL} covers, plus racial Habilidades
     * and racial Atributo bonuses (both the race's fixed grant and the points its player chose at
     * creation: both come of being a Gnomo, whoever directed them).
     */
    ALL;

    /** Whether this rung silences the holder's racial Armas Naturais — true of every rung but {@link #NONE}. */
    public boolean suppressesNaturalWeapons() {
        return this != NONE;
    }

    /**
     * Whether this rung silences the traits a body wears beyond its weapons — the Resistência a
     * Críticos / {@code CriticalEffectType} anatomy pair, and the base Categoria de Tamanho.
     */
    public boolean suppressesPhysicalTraits() {
        return ordinal() >= PHYSICAL.ordinal();
    }

    /**
     * Whether this rung silences racial Habilidades and Atributo bonuses — {@link #ALL} alone.
     * Named for the rung rather than per-trait because nothing distinguishes the two: the clause
     * that reaches them reaches both.
     */
    public boolean suppressesInnateTraits() {
        return this == ALL;
    }

    /**
     * The stronger of two rungs — how {@code CombatantSheet#getRacialTraitSuppression()} folds
     * several held Talentos. A {@code null} reads as {@link #NONE}, so a hook that declined to
     * answer cannot drag the total down.
     */
    public static RacialTraitSuppression strongest(final RacialTraitSuppression first,
                                                   final RacialTraitSuppression second) {
        RacialTraitSuppression left = first == null ? NONE : first;
        RacialTraitSuppression right = second == null ? NONE : second;
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
