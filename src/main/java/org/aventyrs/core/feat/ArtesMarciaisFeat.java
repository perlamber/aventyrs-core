package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Arte Marcial — what a character does fighting unarmed, or with Armas Naturais.
 *
 * <p>Two gaps run through the whole tree and are not repeated on every constant. The first is
 * the gap catalog's "Classifying an attack as Desarmado/Arma Natural": nothing marks an attack
 * as unarmed, and nothing marks a weapon as an Arma Natural. The second is narrower and only
 * affects the five <i>Dominar Arte Marcial</i> constants — each is a mutually exclusive style
 * ("nenhum outro Talento Dominar Arte Marcial"), and {@link FeatRequirements} combines its
 * clauses with <b>and</b>, with no way to express "and none of these". {@code ARTE_MARCIAL_MISTA}
 * exists precisely to lift that restriction to two, which is a second thing the record cannot
 * express. Both are noted per constant where they bite.
 */
public enum ArtesMarciaisFeat implements Feat {

    /**
     * "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base
     * cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui." A
     * Título Aventyr is "Desperto" simply by being held — see {@code
     * InstinctAbility#SENTIR_A_INTENCAO}'s own confirmed reading of this exact phrase, {@link
     * Character#getAllTitles()} non-empty/its size is the count.
     *
     * <p>The Título-count half of this bonus is real, computable arithmetic — see {@link
     * #resolveDamageBaseIncrease(Character)} — and now genuinely consumed: {@code
     * DamageBaseService} sums it into the wielded item's own {@link
     * org.aventyrs.core.character.DamageBase}. What's still missing is the <em>gate</em>: this
     * core has no way to classify whether a given attack is an Ataque Desarmado/Arma Natural
     * versus a weapon attack (an {@code Item} catalog exists, but nothing marks an attack as
     * unarmed beyond a caller passing no weapon at all), so the bonus currently applies to any
     * attack its holder makes rather than only to those two.
     */
    ARTISTA_MARCIAL(
            "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base "
                    + "cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(2)
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        // TODO: filter for Ataques Desarmados e Armas Naturais — DamageBaseService passes the
        // wielded Item (null for unarmed), but "Arma Natural" has no marker on Item at all, so
        // gating on weapon == null would silently drop the Armas Naturais half of the clause.
        @Override
        public int resolveDamageBaseIncrease(final Character character) {
            return BASE_DAMAGE_BASE_INCREASE + character.getAllTitles().size();
        }
    },

    /**
     * "Enquanto não estiver utilizando nenhuma arma, exceto Armas Naturais, você recebe Bônus de
     * +2 em suas Defesas, este Bônus aumenta cumulativamente em +1 para cada Título Aventyr que
     * você tenha Desperto."
     *
     * <p><b>Fully real</b>, and the only Arte Marcial constant that is. Both halves are
     * computable: "não estiver utilizando nenhuma arma" reads {@code Character#getEquipment()}
     * for any {@link Weapon}, and the Título count is {@code Character#getAllTitles()}. It grants
     * to both {@link DefenseType}s, since "suas Defesas" unqualified means DF and DM alike.
     *
     * <p>The "exceto Armas Naturais" exception is <b>vacuous today</b> rather than dropped:
     * nothing anywhere marks an {@code Item} as an Arma Natural, so no character this core can
     * build can hold one, and "wields no Weapon" is therefore exactly equivalent to the clause.
     * Same reasoning as {@code MonsterTemplate#isUndead()} — exact for every character
     * constructible now, and needing revisit the day the marker lands.
     */
    // TODO: revisit once an Arma Natural marker exists on Item — a character wielding one would
    //  then wrongly lose this bonus.
    DEFESA_DE_MAOS_LIMPAS(
            "Enquanto não estiver utilizando nenhuma arma, exceto Armas Naturais, você recebe "
                    + "Bônus de +2 em suas Defesas, este Bônus aumenta cumulativamente em +1 para "
                    + "cada Título Aventyr que você tenha Desperto.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ARTISTA_MARCIAL)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            boolean wieldsAWeapon = character.getEquipment().stream().anyMatch(Weapon.class::isInstance);
            return wieldsAWeapon ? 0 : BASE_DEFENSE_BONUS + character.getAllTitles().size();
        }
    },

    /**
     * "Enquanto não estiver utilizando nenhuma arma - exceto Armas Naturais - e nenhum item do
     * tipo Escudo seus ataques afetam um alvo adicional."
     */
    // TODO: needs multi-target attack resolution — AttackDelivery resolves exactly one target.
    // TODO: "nenhum item do tipo Escudo" needs a shield classification on Item; ItemCategory has
    //  no such value, and nothing else distinguishes a shield from other worn equipment.
    // TODO: the mutual-exclusion Pré-requisito ("nenhum outro Talento Dominar Arte Marcial")
    //  cannot be expressed — see this enum's own javadoc.
    DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA(
            "Enquanto não estiver utilizando nenhuma arma - exceto Armas Naturais - e nenhum item "
                    + "do tipo Escudo seus ataques afetam um alvo adicional. Alvos adicionais "
                    + "precisam estar adjacentes ao alvo primário. Enquanto houver mais de um alvo "
                    + "você sofre Desvantagem em rolagens de Danos, os danos no alvo adicional são "
                    + "reduzidos à metade.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Armas leves de combate corpo-a-corpo que você utilizar são consideradas Armas Naturais
     * para você" — the style that turns a real weapon into an Arma Natural so every other
     * Arma-Natural Talento reaches it.
     */
    // TODO: this is the Arma Natural marker's most direct consumer — it *sets* the property the
    //  gap catalog says nothing carries, so it cannot be expressed until Item has it.
    // TODO: "armas leves" needs a weight classification reaching Weapon; ItemWeightClass exists
    //  on Item but nothing scopes an effect by it yet.
    // TODO: mutual exclusion — see this enum's own javadoc.
    DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA(
            "Enquanto não estiver utilizando nenhum item do tipo Escudo, armas leves de combate "
                    + "corpo-a-corpo que você utilizar são consideradas Armas Naturais para você. "
                    + "Esta Arte Marcial permite que você utilize quaisquer outros Talentos ou "
                    + "Habilidades que afetem Armas Naturais para afetar sua arma, exceto para "
                    + "substituir explicitamente Ataques Desarmados.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Atacando apenas com Ataques Desarmados, você recebe Vantagem em suas Rolagens de Dano,
     * este benefício muda para +1d6 se o seu ataque for uma Reação."
     */
    // TODO: a Vantagem on a *dano* roll has no hook — Skill#ADVANTAGE_BONUS feeds a Perícia roll,
    //  and DamageBonus is a flat value with no advantage concept.
    // TODO: "se o seu ataque for uma Reação" needs the attack to know it was made as a Reação;
    //  neither AttackDelivery nor SkillRoll carries the action type it was spent as.
    // TODO: the Escudo half needs a shield classification; mutual exclusion — see enum javadoc.
    DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO(
            "Enquanto não estiver utilizando nenhuma arma, atacando apenas com Ataques "
                    + "Desarmados, você recebe Vantagem em suas Rolagens de Dano, este benefício "
                    + "muda para +1d6 se o seu ataque for uma Reação. Enquanto estiver utilizando "
                    + "um item do tipo Escudo você recebe Bônus de +2 em suas Defesas para "
                    + "resistir à Reações de outros personagens.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você não é considerado Desprevenido enquanto estiver Caído" — the grappling style.
     */
    // TODO: needs the Caído and Desprevenido conditions, neither of which exists; this core has
    //  no status-condition system at all.
    // TODO: Agarrar/Empurrar/Derrubar are manoeuvres with no representation, so a Vantagem
    //  scoped to them cannot be expressed (this core does not track what a roll is *for*).
    // TODO: "pode realizar uma Reação adicional" is conditional on being Caído, so it is not a
    //  flat @Modifier(REACTIONS); mutual exclusion — see enum javadoc.
    DOMINAR_ARTE_MARCIAL_SUBMISSAO(
            "Você não é considerado Desprevenido enquanto estiver Caído e não sofre Desvantagens "
                    + "em rolagens de Perícias de Ataque com Armas Naturais ou Desarmado nesta "
                    + "condição, você também recebe Vantagem em suas rolagens de Ataque "
                    + "Corpo-a-Corpo para Agarrar, Empurrar ou Derrubar. Enquanto estiver caído "
                    + "você recebe Bônus de +2 em sua DF, pode realizar uma Reação adicional e "
                    + "pode se levantar como Ação Livre.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "A Margem Crítica Menor de seus Ataques Corpo-a-Corpo aumenta em +1 e seus Acertos Críticos
     * ganham a Corrente de Efeitos – Rugido."
     *
     * <p>The margin half looks like {@code resolveCriticalMarginIncrease}, but that hook lives on
     * {@code EgoAdvantage}/{@code AttributeAbility}/{@code SkillCompetencyAbility} — not on
     * {@code Feat}, which no Interaction scans for it.
     */
    // TODO: promote resolveCriticalMarginIncrease to Feat and scan character.getFeats() in
    //  AbstractSkillInteraction#sumCriticalMarginIncrease before this can apply. Note the clause
    //  names the Margem Crítica *Menor* specifically, where SkillRoll#getCriticalResult widens
    //  both tiers together.
    // TODO: granting a Corrente de Efeitos to a critical needs a hook on the attack path;
    //  EffectChainService assembles chains from the roll, not from the attacker's Talentos.
    // TODO: "se o seu ataque for uma Ação Livre ou Reação" — see IMPACTO_ROCHOSO's own note.
    // TODO: mutual exclusion — see this enum's own javadoc.
    DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE(
            "A Margem Crítica Menor de seus Ataques Corpo-a-Corpo aumenta em +1 e seus Acertos "
                    + "Críticos ganham a Corrente de Efeitos – Rugido. O aumento na Margem Crítica "
                    + "Menor muda para +3 (ao invés de +1) se o seu ataque for uma Ação Livre ou "
                    + "Reação.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você recebe Bônus de +1 em rolagens de Danos Críticos de Armas Naturais e Defesas para
     * cada Talento de Arte Marcial que possuir." Also lifts the Dominar cap from one to two.
     */
    // TODO: a bonus scoped to *Danos Críticos* has no hook — DamageBonus applies to any dano
    //  roll, with no notion of the critical half.
    // TODO: the Defesas half is countable (Feat#resolveDefenseBonus over the holder's Arte
    //  Marcial Talentos), but the clause grants one figure to both Danos Críticos and Defesas;
    //  wiring only the reachable half would silently grant a different total than the text.
    // TODO: "Você pode possuir um segundo Talento de Dominar Arte Marcial" raises a cap that is
    //  not itself expressible — see this enum's own javadoc.
    ARTE_MARCIAL_MISTA(
            "Você recebe Bônus de +1 em rolagens de Danos Críticos de Armas Naturais e Defesas "
                    + "para cada Talento de Arte Marcial que possuir. Você pode possuir um segundo "
                    + "Talento de Dominar Arte Marcial.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(7)
                    .requiredAwakenedTitles(2)
                    .requiredFeatCategory(FeatCategory.ARTE_MARCIAL)
                    .requiredFeatCategoryCount(1)
                    .build());

    /** ARTISTA_MARCIAL's own flat "+1" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DAMAGE_BASE_INCREASE = 1;

    /** DEFESA_DE_MAOS_LIMPAS's own flat "+2" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DEFENSE_BONUS = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    ArtesMarciaisFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTE_MARCIAL;
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
