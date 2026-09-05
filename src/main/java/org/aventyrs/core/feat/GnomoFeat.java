package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
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
    // TODO: "Categoria de Tamanho muda para -2" is an absolute *set*, not the shift
    //  ModifierType.SIZE_CATEGORY expresses, and Feat is outside every ModifierResolver scan
    //  anyway — there is no resolveSizeCategoryIncrease hook. Worth noting the two happen to
    //  agree here: this Talento is Gnomo-only and Gnomo's base is MINUS_ONE, so the effect is
    //  exactly one step down. A hook would still have to be a shift, and a later race-size
    //  change would silently break the equivalence.
    // TODO: Mimetizar has no mechanism — SpellCastingService cannot cast a Magia the caster does
    //  not know, and there is no per-Descanso use counter for the "1 + Títulos Despertos" limit.
    //  Same gap NascidoDoDragao's own Magia Dracônica cites. "Que não sejam Profanas" would
    //  additionally need a Magia classification MagicType does not carry.
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
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.MAGIC ? DUENDE_MAGIC_DEFENSE_BONUS : 0;
        }
    },

    /**
     * "Você aprende uma Habilidade de Competência de uma Perícia treinada. Você estende os
     * Benefícios de Aprendizado Rápido até a sétima Graduação."
     */
    // TODO: the free Habilidade de Competência is the "grant an extra acquisition slot" gap.
    // TODO: Aprendizado Rápido is itself unbuilt — Gnomo's own javadoc records it, and so do
    //  Human's and Pequenino's: SkillGraduationService#getUpgradeCost takes no Race and has no
    //  notion of a per-race discount, and nothing records which Perícias were chosen for it. A
    //  Talento extending its reach has nothing to extend.
    SABICHAO(
            "Você aprende uma Habilidade de Competência de uma Perícia treinada. Você estende os "
                    + "Benefícios de Aprendizado Rápido até a sétima Graduação.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(4)
                    .build()),

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
    // TODO: the passive half is the "grant an extra acquisition slot" gap, here restricted to a
    //  Perícia with 2+ Graduações.
    // TODO: the active half needs a temporary *ability* grant, which is a different mechanism
    //  from TemporaryBonus — that carries a ModifierType and a value, not a trait. Nothing can
    //  add a SkillCompetencyAbility to a character for a limited time. It also needs a
    //  per-Cena activation counter, which CharacterSheet does not track (it counts Rodadas via
    //  TemporaryEffect, not activations, and has no notion of a Cena boundary).
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
