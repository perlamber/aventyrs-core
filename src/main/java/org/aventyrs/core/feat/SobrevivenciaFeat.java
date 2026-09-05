package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Talentos de Sobrevivência — staying alive: armour, favoured terrain, and refusing to drop.
 *
 * <p>The tree's recurring blocker is that <b>nothing intercepts damage on its way to zero</b>.
 * Five constants here trigger on PV reaching 0, hold a character at 1, halve the first hit of a
 * Cena, or keep them acting past death. {@code DamageService} computes a mitigated figure and
 * {@code CharacterSheet#applyDamage} applies it; there is no hook between the two, and no
 * mechanism reports that a threshold was crossed. {@code CharacterStatus} is derived on demand
 * precisely so nothing has to be notified.
 *
 * <p>The catalog's seven regional Talentos also carry this tree's tag ({@code Tellus/
 * Sobrevivência}). They are deliberately not authored: every one is an empty template in the
 * source, under a section marked "em produção".
 */
public enum SobrevivenciaFeat implements Feat {

    /**
     * "Você é mais resistente que o normal, seu Multiplicador de Pontos de Vida aumenta em +1."
     *
     * <p><b>The multiplier half is real</b>, through {@link Feat#resolveLifeMultiplierIncrease} —
     * an unconditional, permanent uplift consumed by {@code HitPointsService}. That hook was
     * added for this constant.
     */
    // TODO: the conditional RD half needs a Feat RD hook that can see the holder's current PV;
    //  DamageService sums ModifierType.DAMAGE_REDUCTION from three ability sources, none of which
    //  is Talentos, and a no-arg @Modifier could not read live PV anyway.
    VITALIDADE(
            "Você é mais resistente que o normal, seu Multiplicador de Pontos de Vida aumenta em "
                    + "+1. Enquanto sua quantidade de PV atuais for igual ou inferior ao seu valor "
                    + "de Multiplicador de PV, você recebe RD.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return 1;
        }
    },

    /**
     * "Escolha um tipo de armadura entre Leve, Média ou Pesada. Você recebe bônus de +1 em suas
     * Defesas enquanto utilizar uma armadura do tipo escolhido", rising to +5 at 10 Graduações.
     *
     * <p>The ladder is plain arithmetic {@link Feat#resolveDefenseBonus} could compute.
     */
    // TODO: ArmorItem exists but nothing marks an equipped Item as Leve/Média/Pesada in a way a
    //  Talento can test — ItemWeightClass is an Item column with no consumer — and the chosen
    //  type is an unrecorded acquisition-time choice. Granting unconditionally would hand the
    //  bonus to a character wearing nothing.
    ESPECIALISTA_EM_ARMADURAS(
            "Escolha um tipo de armadura entre Leve, Média ou Pesada. Você recebe bônus de +1 em "
                    + "suas Defesas enquanto utilizar uma armadura do tipo escolhido. Os bônus "
                    + "concedidos por este talento são aumentados para +2 quando o personagem "
                    + "tiver 4 graduações em ‘Esquiva e Aparar’, +3 quando tiver 7 graduações, "
                    + "então para +5 com 10 graduações.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Os Bônus em Defesas concedidos por Especialista em Armaduras se aplicam a qualquer armadura
     * que vestir, adicionalmente você recebe bônus de +2 em suas Defesas enquanto estiver vestindo
     * uma armadura da categoria que você se especializou."
     */
    // TODO: widens ESPECIALISTA_EM_ARMADURAS, which is itself unbuilt; the +2 half still needs the
    //  armour category comparison that blocks it.
    DOMINAR_ARMADURAS(
            "Os Bônus em Defesas concedidos por Especialista em Armaduras se aplicam a qualquer "
                    + "armadura que vestir, adicionalmente você recebe bônus de +2 em suas Defesas "
                    + "enquanto estiver vestindo uma armadura da categoria que você se "
                    + "especializou.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ESPECIALISTA_EM_ARMADURAS)
                    .build()),

    /**
     * "Se algum efeito iniciado por outro personagem for reduzir seus PV à 0 ou menos, ao invés
     * disso, aquele efeito reduzirá seu PV para 1."
     */
    // TODO: no hook intercepts damage before it lands — see this enum's own javadoc.
    // TODO: "não pode ser usado novamente até seu próximo Descanso Longo" needs a per-rest
    //  charge; RestService recovers resources but tracks no once-per-rest flags.
    // TODO: disjunctive Pré-requisito (Instinto *ou* Vigor 3); modelled as the Vigor branch only.
    DURO_DE_MATAR(
            "Se algum efeito iniciado por outro personagem for reduzir seus PV à 0 ou menos, ao "
                    + "invés disso, aquele efeito reduzirá seu PV para 1. Este efeito é ativado "
                    + "automaticamente, após sua ativação não pode ser usado novamente até seu "
                    + "próximo Descanso Longo.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Escolha um tipo de terreno. Você recebe Bônus de +2 em suas Defesas e Vantagem em rolagens
     * de Furtividade e de Conhecimentos: Natureza no terreno escolhido."
     *
     * <p><b>Real</b>, through {@link TerrenoPrediletoFeat} — the acquired, choice-carrying form
     * granted in place of this constant. The six terrenos the rules name are exactly {@link
     * TerrainType}'s six constants, and "no terreno escolhido" is read off the {@code
     * SceneContext} both hooks receive, the same way {@code ElficoFeat#GUARDIAO_DOS_BOSQUES}
     * reads it.
     */
    // TODO: its Pré-requisito names an Especialização (Conhecimentos: Natureza);
    //  FeatRequirements models a Habilidade de Competência but not a SkillSpecialization.
    TERRENO_PREDILETO(
            "Escolha um tipo de terreno entre Aquáticos, Cidades, Desertos, Florestas, Montanhas, "
                    + "Subterrâneo. Você recebe Bônus de +2 em suas Defesas e Vantagem em rolagens "
                    + "de Furtividade e de Conhecimentos: Natureza no terreno escolhido.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Sua Margem Crítica Menor aumenta em +1 número, então você recebe Vantagem em rolagens de
     * Perícias de Ataque e Danos enquanto no terreno escolhido."
     *
     * <p>The terrain match is real now ({@link TerrenoPrediletoFeat#chosenBy}), so both halves
     * this hook family can reach are wired: the Margem Crítica <b>Menor</b> half (all {@code
     * SkillRoll#getCriticalResult(int)}'s margin touches — Acerto Crítico Maior stays a literal
     * triple-6, and any Maior widening would need its own explicit mechanism) and the Perícias de
     * Ataque Vantagem half.
     */
    // TODO: "e Danos" — a Vantagem on a *dano* roll has no Feat hook. Granting a Corrente de
    //  Efeitos – Oprimir has no hook on the attack path either.
    MESTRE_DE_CACA(
            "Sua Margem Crítica Menor aumenta em +1 número, então você recebe Vantagem em rolagens "
                    + "de Perícias de Ataque e Danos enquanto no terreno escolhido. Nestes "
                    + "terrenos seus ataques recebem a Corrente de Efeitos – Oprimir.",
            FeatRequirements.builder()
                    .requiredFeat(TERRENO_PREDILETO)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character) {
            return inChosenTerrain(character, sceneContext) ? MESTRE_DE_CACA_MARGIN_INCREASE : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return inChosenTerrain(character, sceneContext) && skillType.isAttackSkill()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Você recebe Bônus de +1 em Vigor e Resistência à Críticos para resistir a ataques enquanto
     * estiver em seu Terreno Predileto."
     */
    // TODO: the terrain match is real now (TerrenoPrediletoFeat#chosenBy), but neither effect it
    //  would gate is: a round-scoped Attribute bonus does not exist — AttributeValue's three
    //  components are all permanent (gap catalog, "Round-scoped Attribute bonuses").
    // TODO: "Resistência à Críticos" is not a stat — CriticalEffect immunity is per named
    //  CriticalEffectType, not a general resistance.
    PROTETOR_TERRITORIALISTA(
            "Você recebe Bônus de +1 em Vigor e Resistência à Críticos para resistir a ataques "
                    + "enquanto estiver em seu Terreno Predileto. Você também pode estender os "
                    + "benefícios de Terreno Predileto e Talentos relacionados as áreas urbanas "
                    + "instaladas em seu terreno escolhido.",
            FeatRequirements.builder()
                    .requiredFeat(TERRENO_PREDILETO)
                    .build()),

    /**
     * "Você adquire 1 ponto permanente de 'Sorte'. Enquanto sua quantidade de PV for igual ou
     * menor que seu valor de 'Vigor', usos e perdas de 'Sorte' que seriam permanentes ao invés
     * disso são temporárias."
     */
    // TODO: a permanent Ego point is granted through CharacterEgos#withVariableBonus, reached only
    //  by AttributeAbility#resolvePermanentEgoGain — Feat has no equivalent hook.
    // TODO: redirecting a permanent Ego spend to the temporary pool has no representation:
    //  spendEgoPoints names its pool at the call site and deliberately has no fallback between
    //  the two (see CLAUDE.md's "Spending names its pool").
    // TODO: "qualquer Atributo com Valor Base 5" names no particular Attribute; FeatRequirements
    //  tests one named AttributeDomain, so only the Talento-count half is enforced.
    SORTE_DE_MOSES(
            "Você adquire 1 ponto permanente de ‘Sorte’. Enquanto sua quantidade de PV for igual "
                    + "ou menor que seu valor de ‘Vigor’, usos e perdas de ‘Sorte’ que seriam "
                    + "permanentes ao invés disso são temporárias.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.DESTINO)
                    .requiredFeatCategoryCount(1)
                    .build()),

    /** As {@link #SORTE_DE_MOSES}, for Autocontrole. */
    // TODO: same missing permanent-Ego-grant and pool-redirection as SORTE_DE_MOSES.
    DETERMINACAO_DE_MOSES(
            "Você adquire 1 ponto permanente de ‘Autocontrole’. Enquanto seus PV estiverem iguais "
                    + "ou menores que seu valor de ‘Vigor’, usos e perdas de ‘Autocontrole’ que "
                    + "seriam permanentes ao invés disso são temporárias.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(4)
                    .requiredFeat(DURO_DE_MATAR)
                    .build()),

    /**
     * "Os primeiros Danos que você sofrer a cada Cena de Combate são reduzidos à metade", for as
     * many attacks as the holder has Títulos.
     */
    // TODO: Meio-Dano exists as a DamageService flag, but nothing scopes it to the first N hits
    //  of a Cena — that needs damage instances to be counted per Cena, which nothing tracks.
    DURO_DE_FERIR(
            "Os primeiros Danos que você sofrer a cada Cena de Combate são reduzidos à metade. A "
                    + "quantidade de ataques que você pode reduzir danos desta forma é igual a "
                    + "quantidade de Títulos Aventyrs que você possuir, este é um efeito de "
                    + "Meio-Dano.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(4)
                    .requiredFeat(DURO_DE_MATAR)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Após ativar o Talento Duro de Matar, em seu próximo Turno você recupera uma quantidade de
     * PV iguais ao seu Vigor."
     */
    // TODO: chains off DURO_DE_MATAR's activation, which is itself unbuilt, and needs a
     //  start-of-Turn hook — CharacterSheet#startTurn exists but is a no-op with no effect
    //  registry behind it.
    // TODO: names two required Talentos; requiredFeat is singular, so only DURO_DE_FERIR is
    //  recorded (it already requires DURO_DE_MATAR transitively).
    DURO_DE_MATAR_SUPERIOR(
            "Após ativar o Talento Duro de Matar, em seu próximo Turno você recupera uma "
                    + "quantidade de PV iguais ao seu Vigor. Adicionalmente você reduz os danos "
                    + "dos próximos dois ataques sofridos à metade (Efeito de Meio-Dano).",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(4)
                    .requiredFeat(DURO_DE_FERIR)
                    .build()),

    /**
     * "Após ter seus PV reduzidos à zero ou menos você ainda pode agir por uma quantidade de
     * Rodadas igual a 1 + quantidade de Títulos Aventyrs que possuir."
     */
    // TODO: "Coma" here is CharacterStatus.COMMA, not a Condição — despite the rules text calling
    //  it a Malefício, it is a tier of the PV ladder, so ConditionType has no entry for it and
    //  should not gain one. CharacterStatus is derived from current PV on every call, so
    //  FALLEN/COMMA/DEAD cannot be suspended: there is no stored tier to override, and nothing
    //  gates acting on status. That one gap blocks both halves of this clause.
    // TODO: capping PA at 2 in a way effects cannot raise needs a ceiling stage;
    //  ActionPointsService sums additively and clamps only at 0.
    // TODO: disjunctive Pré-requisitos on both halves (Vigor *ou* Instinto 5; Duro de Ferir *ou*
    //  Duro de Matar); modelled as the Vigor and Duro de Ferir branches.
    PERMANECER_CONSCIENTE(
            "Após ter seus PV reduzidos à zero ou menos você ainda pode agir por uma quantidade de "
                    + "Rodadas igual a 1 + quantidade de Títulos Aventyrs que possuir. Você não é "
                    + "afetado pelo Malefício Coma durante estas Rodadas e, enquanto Permanecer "
                    + "Consciente estiver ativo, seus PA são reduzidos à 2 e não podem ser "
                    + "aumentados por efeitos.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(5)
                    .requiredFeat(DURO_DE_FERIR)
                    .requiredAwakenedTitles(1)
                    .build());

    /** MESTRE_DE_CACA's own stated "+1 número" to the Margem Crítica Menor. */
    private static final int MESTRE_DE_CACA_MARGIN_INCREASE = 1;

    /**
     * Whether character holds {@link TerrenoPrediletoFeat} and sceneContext is currently that
     * chosen terrain — the "enquanto no terreno escolhido" condition every dependent of {@link
     * #TERRENO_PREDILETO} shares.
     */
    private static boolean inChosenTerrain(final Character character, final SceneContext sceneContext) {
        if (sceneContext == null) {
            return false;
        }
        Optional<TerrainType> chosen = TerrenoPrediletoFeat.chosenBy(character);
        return chosen.isPresent() && sceneContext.isTerrain(chosen.get());
    }

    private final String description;
    private final FeatRequirements featRequirements;

    SobrevivenciaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.SOBREVIVENCIA;
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
