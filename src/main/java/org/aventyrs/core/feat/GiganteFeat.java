package org.aventyrs.core.feat;

import org.aventyrs.core.race.Gigantes;

/**
 * Talentos Gigantes — two about protecting smaller allies, two about awakening an ancestral clã.
 *
 * <p>No constant carries a mechanical effect, but two of the four are worth reading for *why*,
 * because neither is blocked in the ordinary way.
 *
 * <p><b>{@link #ESCUDO_QUE_ANDA} is the second real consumer of ally-facing Defesas.</b> Its
 * "aliados adjacentes recebem Bônus de +1 em Defesas" is the same shape as Santo's Despertar,
 * which CLAUDE.md's gap catalog already names as the case that would justify generalising the
 * ally-scanning mechanism. That mechanism half-exists: {@code
 * DamageServiceImpl#sumAllyGrantedAbsoluteDamageReduction} already walks a target's adjacent
 * allies and asks each what it grants outward, for RA. Defesas has no equivalent scan, and
 * {@code DefenseService} would need one plus a {@code Feat} hook to be asked through.
 *
 * <p><b>The two Clã Talentos are withheld deliberately, not merely blocked.</b> Each pairs
 * bonuses this core cannot grant (a Categoria de Tamanho uplift, a per-Título Atributo bonus)
 * with a malus it <i>could</i> — Empusa's "-2 em suas Defesas" is expressible today as a negative
 * {@code resolveDefenseBonus}. Implementing only the half that hurts would leave a character
 * strictly worse off for having acquired the Talento, which is further from the rules text than
 * granting nothing. Both halves wait together.
 */
public enum GiganteFeat implements Feat {

    /**
     * "A característica Cuidado Para Não Quebrar não te concede mais quaisquer Desvantagens. Seu
     * Movimento Base aumenta em +2UD para cada Título Aventyr Desperto, mas apenas para se
     * aproximar de aliados feridos que pertençam à Categorias de Tamanhos inferiores à sua."
     */
    // TODO: Cuidado Para Não Quebrar is itself unbuilt — Gigantes' own javadoc records why: its
    //  Desvantagem is scoped to "Perícias baseadas em Força ou Destreza", an AttributeDomain
    //  scope no hook expresses. A Talento suppressing it has nothing to suppress, and there is
    //  no mechanism for suppressing a Desvantagem either.
    // TODO: the Movimento uplift is scoped to a *purpose* ("apenas para se aproximar de aliados
    //  feridos" of a smaller Categoria de Tamanho), and this core does not track what movement
    //  is for. Granting it through resolveMovementIncrease would raise the holder's Movimento
    //  Base for every purpose, which the clause explicitly excludes.
    ZELO_PELOS_FRAGEIS(
            "Você se acostumou a viver com as criaturas de tamanhos inferiores, não ferir ninguém "
                    + "acidentalmente se tornou natural. A característica Cuidado Para Não Quebrar "
                    + "não te concede mais quaisquer Desvantagens. Seu Movimento Base aumenta em "
                    + "+2UD para cada Títulos Aventyr Despertos, mas apenas para se aproximar de "
                    + "aliados feridos que pertençam à Categorias de Tamanhos inferiores à sua.",
            FeatRequirements.builder()
                    .requiredRace(Gigantes.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Seus aliados recebem Bônus de +1 em Defesas enquanto estiverem adjacentes a você. Este
     * Bônus aumenta em +1 para cada Título Aventyr Desperto, então em +1 para cada Título
     * Abençoado desperto."
     *
     * <p><b>Source-document defect.</b> Its {@code Descrição:} line opens with the bare text
     * "Talento Zelo pelos Frágeis" — a prerequisite that slipped into the description field, the
     * same defect {@code docs/rules/talentos-index.md} records at L635 for <i>Se Mover e
     * Atacar</i>. Read as a prerequisite and recorded as {@code requiredFeat}; the description
     * below is the remaining, genuine text.
     */
    // TODO: an ally-facing continuous Defesas grant — see the class javadoc. The amount is fully
    //  computable off Character#getAllTitles() (one per Título, one more per Abençoado); what is
    //  missing is the scan, and a Feat hook for it to reach. Second real consumer of the shape,
    //  after Santo's Despertar.
    ESCUDO_QUE_ANDA(
            "Seus aliados recebem Bônus de +1 em Defesas enquanto estiverem adjacentes a você. "
                    + "Este Bônus aumenta em +1 para cada Título Aventyr Desperto, então em +1 "
                    + "para cada Título Abençoado desperto.",
            FeatRequirements.builder()
                    .requiredRace(Gigantes.class)
                    .requiredFeat(ZELO_PELOS_FRAGEIS)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Sua Categoria de Tamanho aumenta em +1, então recebe Bônus de +1 Força para cada Título
     * Aventyr Desperto, mas recebe Redutor de -2 em suas Defesas."
     */
    // TODO: withheld as a whole rather than half-implemented — see the class javadoc. The -2
    //  Defesas alone would be expressible through resolveDefenseBonus.
    // TODO: a Talento cannot raise Categoria de Tamanho (Feat is outside every ModifierResolver
    //  scan, and there is no resolveSizeCategoryIncrease hook) nor grant an Atributo bonus.
    // TODO: "não pode possuir mais de um Talento de Clã" is an exclusion, and FeatRequirements
    //  carries only thresholds that must be met.
    GIGANTE_DO_CLA_EMPUSA(
            "O Despertar de sua Centelha também desperta um poder latente em seu sangue, fruto da "
                    + "descendência Abissal. Você se torna maior e mais poderoso, mas se torna "
                    + "mais frágil. Sua Categoria de Tamanho aumenta em +1, então recebe Bônus de "
                    + "+1 Força para cada Título Aventyr Desperto, mas recebe Redutor de -2 em "
                    + "suas Defesas. Um mesmo personagem não pode possuir mais um de um Talento "
                    + "de Clã.",
            FeatRequirements.builder()
                    .requiredRace(Gigantes.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Sua Categoria de Tamanho aumenta em +1, então recebe Bônus de +1 em Vigor para cada Título
     * Aventyr Desperto, mas recebe Desvantagens em rolagens de Perícias Físicas."
     */
    // TODO: same Categoria de Tamanho, Atributo and exclusion gaps as its Empusa twin.
    // TODO: its malus is not even expressible, unlike Empusa's — a Desvantagem on "Perícias
    //  Físicas (baseadas em Força e Destreza), exceto Esquiva e Aparar" is scoped by
    //  AttributeDomain with a named exception, which is the same shape Gigantes' own Cuidado
    //  para não Quebrar is blocked on.
    GIGANTE_DO_CLA_JOTUN(
            "O Despertar de sua Centelha também desperta um poder latente em seu sangue, fruto da "
                    + "descendência Titânica. Você se torna maior e mais resistente, mas seus "
                    + "movimentos se tornam mais rígidos e lentos. Sua Categoria de Tamanho "
                    + "aumenta em +1, então recebe Bônus de +1 em Vigor para cada Título Aventyr "
                    + "Desperto, mas recebe Desvantagens em rolagens de Perícias Físicas "
                    + "(baseadas em Força e Destreza), exceto Esquiva e Aparar. Um mesmo "
                    + "personagem não pode possuir mais um de um Talento de Clã.",
            FeatRequirements.builder()
                    .requiredRace(Gigantes.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    GiganteFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.GIGANTE;
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
