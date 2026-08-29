package org.aventyrs.core.magic;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.MagiaAlternativaAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * A Magia. It is an {@link AttackSource}: casting one at somebody is an attack delivered by
 * {@link #getAttackSkillType()}, exactly as swinging a {@code Weapon} is — which is what lets a
 * delivery-scoped ability such as {@code AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO}
 * cover "armas de arremessos <b>e magias</b>" without either side knowing about the other.
 *
 * <h2>One method per authored descriptor</h2>
 *
 * Every member below is a column of a Magia's rules-text block in {@code docs/rules/magias.txt},
 * and the mapping is one-to-one. Authoring one means filling in that block; see {@link
 * SpellData}, which is the builder the whole catalog is written against, and {@link
 * AuthoredSpell}, which turns one into a {@code Spell} with no per-tree boilerplate.
 *
 * <h2>Two descriptors this interface deliberately answers differently from the document</h2>
 *
 * <ul>
 *   <li><b>The category tag belongs to the {@link SpellTree}, not to the Magia.</b> The catalog
 *       prints it once per Árvore ({@code PIROMANCIA (Divina/Elemental: Fogo)}) and never on an
 *       individual entry, so {@link #getPrimaryType()}/{@link #getSecondaryType()} default to
 *       delegating to the tree and no Magia authors them.</li>
 *   <li><b>{@code Perícia Chave para Conjuração} is the <i>delivery</i> roll.</b> The document
 *       gives one field, assigned by the rule at L47 — Corpo-a-Corpo for Toque, à Distância for
 *       Alvo Distante/Área, Domínio do Mana for Pessoal or unavoidable — and that is {@link
 *       #getAttackSkillType()}. {@link #getConjurationSkillType()} has no column to read from
 *       because under {@code SpellCastingService}'s two-roll model the second roll is
 *       <i>always</i> Domínio do Mana, so it defaults to exactly that. The two are equal for the
 *       109 Magias whose key Perícia already is Domínio do Mana.</li>
 * </ul>
 */
public interface Spell extends AttackSource {

    /** This Magia's own name, e.g. "Cativar Animal" — matches its rules-text identity line. */
    String getName();

    /**
     * The {@code GD da Conjuração:} tier. {@code null} for the handful of entries that state no
     * fixed tier: three source-document blanks (Invocar Traje de Batalha, Estalo Primordial,
     * Proteção Primordial, plus Aumentar Passos), the one {@code Variável}, and the two whose GD
     * is a table over the <em>target effect's</em> rung — see {@link
     * #getCastingDifficultyAgainst(BranchLevel)}.
     */
    DifficultyLevel getCastingDifficultyLevel();

    /**
     * Whether the GD is "{@code ou DM do Alvo (maior)}" — the stated tier acts as a <b>floor</b>
     * and the target's Defesa Mágica wins when it is higher.
     *
     * <p>It combines with a {@code null} {@link #getCastingDifficultyLevel()} to express the one
     * catalog entry whose GD is written as bare "{@code DM do alvo}" with no tier at all ({@code
     * MorteSpell#IMPOR_ARREPSIA}): a floor of nothing is always beaten by the target's Defesa
     * Mágica, which is exactly what that line means. Nothing resolves any of it yet
     * (a foe's DM is an authored flat number and this core does not compare the two anywhere),
     * so it is authored data with no reader, per the usual "can't apply it yet doesn't mean
     * can't compute it yet" discipline.
     */
    default boolean isCastingDifficultyFlooredByTargetMagicDefense() {
        return false;
    }

    /**
     * The GD for a Magia whose difficulty scales to the rung of the <em>effect it targets</em>
     * rather than being fixed — "Sementes: Fácil, Brotos: Médio, Mudas: Difícil, Emergentes:
     * Muito Difícil, Florescentes: Improvável". Two Magias (Remover Maldição, Anulação); {@link
     * Optional#empty()} for every other, which states a fixed {@link
     * #getCastingDifficultyLevel()} instead. Override it with {@link
     * #castingDifficultyAgainst(BranchLevel)}, which is that exact table.
     */
    default Optional<DifficultyLevel> getCastingDifficultyAgainst(final BranchLevel targetLevel) {
        return Optional.empty();
    }

    /**
     * The rung-to-GD ladder — Semente/Fácil, Broto/Médio, Muda/Difícil, Emergente/Muito Difícil,
     * Florescente/Improvável. Ordinal-aligned by construction: both ladders are ordered
     * shallowest-first and {@link BranchLevel} has exactly five rungs starting where {@link
     * DifficultyLevel#EASY} does.
     *
     * <p>Written here rather than in the <b>three</b> constants that call it ({@code
     * AnulacaoSpell#IDENTIFICACAO}, {@code VidaSpell#TOQUE_CURATIVO}, {@code
     * VidaSpell#REMOVER_MALDICAO}), which scale their GD to the rung of the effect they
     * <em>target</em> rather than their own.
     *
     * <p><b>It is the catalog's general rule, not a table peculiar to those three.</b> Of the 145
     * authored Magias, 134 state a plain tier and <em>all 134 follow this ladder against their own
     * rung, with zero deviations</em>; the other 11 are the 4 blanks, the 3 bare-{@code DM do
     * Alvo}, these 3 tables and 1 allegiance-conditioned entry. The {@code ou DM do Alvo (maior)}
     * floor is orthogonal — all 46 of those still state their rung's tier.
     *
     * <p>So a Magia with a blank {@code GD da Conjuração:} is derivable rather than unknowable.
     * The four such Magias are still authored as {@code null}: filling them by rule is a separate
     * decision, and this method deliberately does not make it.
     */
    static DifficultyLevel castingDifficultyAgainst(final BranchLevel targetLevel) {
        return DifficultyLevel.EASY.harder(targetLevel.ordinal());
    }

    /** The {@code Descrição:} line — one sentence saying what the Magia is for. */
    String getDescription();

    /** The {@code Efeito:} block — what the Magia actually does. */
    String getPrimaryEffectDescription();

    /**
     * The {@code Efeito Alternativo – ‹name›:} block, or {@code null} for the 82 Magias with
     * none. "Um personagem que aprenda a versão base automaticamente aprende sua segunda versão"
     * — it is not separately acquired, which is why it is a column here rather than its own
     * catalog entry.
     */
    default String getSecondaryEffectDescription() {
        return null;
    }

    /**
     * The {@code Corrente de Efeitos – ‹name›:} block, or {@code null} for the ~85 Magias with
     * none. Applied when the delivery roll "superar a DM do alvo em 5 ou mais" (L49).
     *
     * <p><b>Prose, not a reference into a catalog.</b> The 145 complete Magias name 69 distinct
     * Correntes inline, and <em>none</em> of them appears in the shared 13-entry Corrente
     * catalog — nor is any of those 13 named by a Magia. The two populations are completely
     * disjoint, so unlike {@link #getCriticalEffectType()} this cannot be an enum reference.
     */
    default String getEffectChainDescription() {
        return null;
    }

    /**
     * The {@code Efeito Crítico:} tier this Magia applies. {@code null} only for the two
     * source-document blanks (Necropotência, Reanimar). Every other value in the catalog
     * resolves — all 23 Efeitos Críticos Ofensivos are constants.
     */
    default CriticalEffectType getCriticalEffectType() {
        return null;
    }

    /**
     * The second of {@code SpellCastingService}'s two rolls, which is always Domínio do Mana —
     * see the class javadoc. No Magia authors this.
     */
    default SkillType getConjurationSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }

    /** The {@code Perícia Chave para Conjuração:} field — the delivery roll. See the class javadoc. */
    @Override
    SkillType getAttackSkillType();

    /** The {@code Tempo de Ativação:} field — PA, or a Reação/Ação Livre. */
    ActivationTime getActivationTime();

    /**
     * How deep in {@link #getTree()} this Magia sits — both its Mana cost and its rung on the
     * tree, which is what {@link #isEligible}'s cap and climb gates read.
     */
    BranchLevel getBranchLevel();

    /** Pontos de Mana this Magia costs to cast, which is purely a function of its rung. */
    default int getManaCost() {
        return getBranchLevel().getManaCost();
    }

    /** The Árvore de Magia this Magia belongs to. */
    SpellTree getTree();

    /**
     * Which ramificação of {@link #getTree()} this Magia sits on, or {@link Optional#empty()}
     * when it sits on the <b>trunk</b> — before the tree diverges, or after its branches
     * converge again. A trunk Magia is on every path, which is the whole convergence mechanism;
     * see {@link SpellTree}.
     */
    default Optional<SpellBranch> getBranch() {
        return Optional.empty();
    }

    /** The first half of the tree's category tag — see the class javadoc. */
    default MagicType getPrimaryType() {
        return getTree().getMagicType();
    }

    /** The second half of the tree's category tag, or {@code null} for a single-typed tree. */
    default MagicType getSecondaryType() {
        return getTree().getSecondaryMagicType().orElse(null);
    }

    /** The {@code Duração:} field, keeping the unit it was authored in — see {@link SpellDuration}. */
    SpellDuration getDuration();

    /**
     * How this Magia reaches what it affects — the {@link SpellReach} together with the
     * {@code Range}/{@code AreaOfEffect} that reach requires. The bare classification is still
     * reachable as {@code getTargeting().reach()}.
     */
    SpellTargeting getTargeting();

    /**
     * The <b>second</b> way this Magia may be aimed, for the 19 catalog entries whose {@code
     * Alcance:} names two — 18 read {@code Pessoal ou Toque} and one {@code Alvo Único – Toque
     * ou Pessoal}. A {@link SpellTargeting} holds exactly one {@link SpellReach} by design (it
     * pairs a reach with the parameters <em>that</em> reach takes), so a dual-reach Magia
     * authors both and the caster picks per cast. {@link Optional#empty()} for the other 126.
     */
    default Optional<SpellTargeting> getAlternateTargeting() {
        return Optional.empty();
    }

    /**
     * Whether character may acquire this Magia right now — <b>three independent gates</b>, all
     * of which must hold, mirroring {@code org.aventyrs.core.feat.Feat#isEligible}/{@code
     * AventyrTitleAbility#isEligible}'s own combine-every-prerequisite shape. Checked by {@code
     * org.aventyrs.core.character.services.SpellService#grantSpell} before granting.
     *
     * <ol>
     *   <li><b>Cap</b> — maxBranchLevel must reach this Magia's own {@link #getBranchLevel()}.
     *       Until a Talento raises it, a Conjurador spends sideways: more Magias at their cap
     *       level, from other Árvores.</li>
     *   <li><b>Climb</b> — unless this is a {@link BranchLevel#SEMENTE} Magia (a tree's entry
     *       point, which rests on nothing), character must already hold a Magia <em>in this same
     *       tree</em> at the immediately shallower rung. A foothold in a different tree never
     *       counts.</li>
     *   <li><b>Branch</b> — character must hold no Magia in this tree on a <em>different</em>
     *       ramificação. Trunk Magias belong to no branch, so they neither commit a Conjurador
     *       nor can ever be refused — which is exactly how a tree's branches converge.</li>
     * </ol>
     *
     * <p>The branch gate alone has an exemption: {@code
     * org.aventyrs.core.ability.MagiaAlternativaAbility} held for <b>either</b> of this tree's
     * Tipos de Magia ("você pode aprender magias de ambas as ramificações dos tipos de magia
     * escolhidos"). Either, rather than only the primary, because the catalog's two-part tag is
     * not a precedence statement — {@code ALIADOS DA NATUREZA (Natural/Invocação)} is as much an
     * Invocação tree as a Natural one. It does not loosen the cap or the climb.
     *
     * <p><b>All three are derived, never stored.</b> There is no "chosen branch" field and no
     * unlocked-levels counter — a Conjurador's branch in a tree simply <em>is</em> whichever
     * ramificação their acquired Magias sit on, so the answer changes by itself as they acquire.
     * Same recompute-on-demand discipline as {@code HitPointsService#getStatus} and {@code
     * InitiativeEntry#getEffectiveInitiativeValue}.
     *
     * @param maxBranchLevel the character's general cap, already resolved by {@code
     *     SpellService#getMaxBranchLevel} — passed in rather than scanned here so this stays a
     *     pure function over data in hand, the same shape {@code DamageServiceImpl} uses when it
     *     resolves {@code hasLowerPvAdjacentAlly} for a hook.
     */
    default boolean isEligible(final Character character, final BranchLevel maxBranchLevel) {
        return withinCap(maxBranchLevel)
                && hasFootholdAtPreviousLevel(character)
                && branchIsAvailable(character);
    }

    /** Gate 1 — the character's general cap reaches this Magia's own depth. */
    private boolean withinCap(final BranchLevel maxBranchLevel) {
        return maxBranchLevel != null && maxBranchLevel.isAtLeast(getBranchLevel());
    }

    /** Gate 2 — a Magia of this same tree is already held one rung shallower. */
    private boolean hasFootholdAtPreviousLevel(final Character character) {
        Optional<BranchLevel> requiredLevel = getBranchLevel().previous();
        if (requiredLevel.isEmpty()) {
            return true;
        }
        return spellsInSameTree(character)
                .anyMatch(held -> held.getBranchLevel() == requiredLevel.get());
    }

    /** Gate 3 — no Magia of this tree is held on a different ramificação, or the type is exempt. */
    private boolean branchIsAvailable(final Character character) {
        SpellTree.validateBranches(getTree());

        Optional<SpellBranch> branch = getBranch();
        if (branch.isEmpty() || isExemptFromBranchLock(character)) {
            return true;
        }
        return spellsInSameTree(character)
                .flatMap(held -> held.getBranch().stream())
                .allMatch(held -> held == branch.get());
    }

    /** Whether MAGIA_ALTERNATIVA was chosen for either of this tree's Tipos de Magia. */
    private boolean isExemptFromBranchLock(final Character character) {
        return character.getAttributeAbilities().stream()
                .anyMatch(this::exempts);
    }

    private boolean exempts(final AttributeAbility ability) {
        return ability instanceof MagiaAlternativaAbility alternativa
                && getTree().hasMagicType(alternativa.getMagicType());
    }

    private java.util.stream.Stream<Spell> spellsInSameTree(final Character character) {
        return character.getSpells().stream().filter(held -> held.getTree() == getTree());
    }
}
