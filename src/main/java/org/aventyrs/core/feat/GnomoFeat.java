package org.aventyrs.core.feat;

import java.util.List;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.race.Gnomo;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Gnomos — the small tinkerer's tree: a smaller body, a sharper trade, and a knack for
 * borrowing other people's competence.
 *
 * <p>Two clauses are real — {@link #DUENDE}'s +1 DM and {@link #FAVORITOS_DE_TESLA}'s −1 nível de
 * GD em Profissão. The rest of the tree runs into one wall repeatedly: <b>"you acquire a
 * Habilidade de Competência"</b> is an extra acquisition slot, and no notion of a race- or
 * Talento-granted extra slot exists ({@code AttributeAbilityService#getUnlockedAbilitySlots}
 * counts slots from a raw Atributo base). Three of the four constants cite it.
 */
public enum GnomoFeat implements Feat {

    /**
     * "Sua Categoria de Tamanho muda para -2, você recebe Bônus de +1 na DM e você pode
     * Mimetizar Sementes, que não sejam Profanas, de qualquer Árvore de Magias." The DM half is
     * real.
     *
     * <p>Unconditional and scoped to one Defesa, so it uses the narrow {@link DefenseType#MAGIC}
     * branch rather than the broad both-Defesas form {@code DraconicoFeat#ASAS_DE_DRAGAO} grants.
     */
    // "Categoria de Tamanho muda para -2" is real, through Feat#resolveSizeCategoryOverride —
    // an absolute *set*, which is why it needs that hook rather than the shift
    // ModifierType.SIZE_CATEGORY expresses. Stated as the value it is, not as the one step down
    // from Gnomo's own MINUS_ONE it happens to equal today: a later race-size change must not
    // silently move a Duende.
    // The mimicry is real: every Semente of every Árvore that is not Profana (MagicType.PROFANA),
    // free — a Semente costs 0 PM (getGrantedMimetizedSpells, derived live).
    // TODO: "o número de vezes que você pode mimetizar uma mesma Semente é igual à 1 + Títulos",
    //  renewed each Descanso Longo Verdadeiro — nothing counts casts of one mimetized Magia.
    DUENDE(
            "Sua Categoria de Tamanho muda para -2, você recebe Bônus de +1 na DM e você pode "
                    + "Mimetizar Sementes, que não sejam Profanas, de qualquer Árvore de Magias. "
                    + "O Número de vezes que você pode mimetizar uma mesma Semente é igual à 1 + "
                    + "Número de Títulos Aventyr Despertos, este limite é renovado sempre que "
                    + "passar por um Descanso Longo Verdadeiro.",
            FeatRequirements.builder()
                    .requiredRace(Gnomo.class)
                    .build()) {
        @Override
        public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
            return java.util.Arrays.stream(MagicTree.values())
                    .filter(tree -> !tree.hasMagicType(MagicType.PROFANA))
                    .flatMap(tree -> tree.getSpells().stream())
                    .filter(spell -> spell.getBranchLevel() == BranchLevel.SEMENTE)
                    .map(spell -> MimetizedSpell.builder().spell(spell).determinationPointCost(0).build())
                    .toList();
        }

        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public SizeCategory resolveSizeCategoryOverride(final Character character) {
            return SizeCategory.MINUS_TWO;
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.MAGIC ? DUENDE_MAGIC_DEFENSE_BONUS : 0;
        }
    },

    /**
     * "Você aprende uma Habilidade de Competência de uma Perícia treinada. Você estende os
     * Benefícios de Aprendizado Rápido até a sétima Graduação."
     */
    // The free Habilidade de Competência is real, recorded on ChosenSkillTraitsFeat.
    // The extension is real: resolveQuickLearningMaxGraduation carries Aprendizado Rápido's
    // discount to the seventh Graduação (SkillGraduationService#getUpgradeCost(Character, SkillType)).
    SABICHAO(
            "Você aprende uma Habilidade de Competência de uma Perícia treinada. Você estende os "
                    + "Benefícios de Aprendizado Rápido até a sétima Graduação.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public Integer resolveQuickLearningMaxGraduation(final Character character) {
            return EXTENDED_QUICK_LEARNING_GRADUATION;
        }
    },

    /**
     * "A GD de suas rolagens de Profissão é reduzida em -1 nível." Real — unconditional, one
     * named Perícia, exactly the shape {@link Feat#resolveDifficultyReduction} exists for.
     */
    FAVORITOS_DE_TESLA(
            "A GD de suas rolagens de Profissão é reduzida em -1 nível.",
            FeatRequirements.builder()
                    .requiredRace(Gnomo.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
            return skillType == SkillType.PROFISSAO ? PROFISSAO_DIFFICULTY_REDUCTION : 0;
        }
    },

    /**
     * An Efeito Passivo granting a Habilidade de Competência outright, plus an Efeito Ativo
     * borrowing one temporarily for a Cena.
     */
    // The passive half is real, recorded on ChosenSkillTraitsFeat. Its "de uma Perícia Treinada
    // qual tenha pelo menos 2 Graduações" restriction is not validated there — the usual
    // builders-aren't-gatekeepers restraint.
    // TODO: the active half needs a temporary *ability* grant, which is a different mechanism
    //  from TemporaryBonus — that carries a ModifierType and a value, not a trait. Nothing can
    //  add a SkillCompetencyAbility to a character for a limited time. (Its once-per-Cena limit
    //  is not the blocker: CombatantSheet#getActionsThisCena and startNewScene give a Cena
    //  boundary to count against.)
    MIMETIZAR_COMPETENCIA(
            "Efeito Passivo – Você adquire uma Habilidade de Competência de uma Perícia Treinada "
                    + "qual tenha pelo menos 2 Graduações. Efeito Ativo – Apenas uma vez por Cena, "
                    + "você pode gastar 2PM, ao Tempo de 2PA, para adquirir temporariamente uma "
                    + "Habilidade de Competência de uma Perícia Treinada que você possua, este "
                    + "efeito dura até o final da cena.",
            FeatRequirements.builder()
                    .requiredRace(Gnomo.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private static final int DUENDE_MAGIC_DEFENSE_BONUS = 1;
    private static final int PROFISSAO_DIFFICULTY_REDUCTION = 1;

    /** Sabichão: "Você estende os Benefícios de Aprendizado Rápido até a sétima Graduação." */
    private static final int EXTENDED_QUICK_LEARNING_GRADUATION = 7;

    private final String description;
    private final FeatRequirements featRequirements;

    GnomoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.GNOMO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
