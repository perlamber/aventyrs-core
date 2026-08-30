package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.race.Troll;

/**
 * Talentos Troll — four of the five extend <b>Regeneração Reativa</b>, the Troll's signature
 * Característica Racial, and the fifth extends Sono de Pedra.
 *
 * <p>None carries a mechanical effect, and all four regeneration Talentos are blocked on the
 * same missing piece already recorded on {@code Troll} itself: <b>nothing triggers off being
 * damaged</b>. {@code DamageService#applyDamage} reports a figure and mutates the sheet with no
 * hook for the victim to react — the healing counterpart of CLAUDE.md's "Reactive/retaliation
 * damage" row. Every constant here adjusts a figure of an effect that never starts.
 *
 * <p><b>Declaration order is not the document's.</b> {@link #REGENERACAO_REATIVA_ESPINHOSA} and
 * {@link #REGENERACAO_REATIVA_INVERNAL} both name {@link #REGENERACAO_REATIVA_SUPERIOR} as their
 * {@code requiredFeat}, and Java forbids a forward reference between enum constants — so Superior
 * is declared before the two that depend on it, where the source document prints it after. Same
 * constraint {@code MetamagicoFeat} documents, and {@code FeatCatalogIntegrityTest} pins that
 * every {@code requiredFeat} actually resolves.
 *
 * <p><b>The two Troll sub-lineages are not modelled</b>, so two Pré-requisitos are looser here
 * than in the text: "apenas Trolls da Floresta" and "apenas Trolls do Inverno" are dropped, and
 * only the shared clauses are enforced. {@code Troll}'s own javadoc explains why the sub-lineages
 * were deliberately not made a nested choice enum — every clause distinguishing them is blocked
 * on the missing elemental-vulnerability mechanism, so the enum would carry nothing today.
 */
public enum TrollFeat implements Feat {

    /**
     * "Enquanto no Sono de Pedra você pode despertar ao sofrer qualquer quantidade de danos ou ao
     * perceber anomalias ao seu redor."
     */
    // TODO: Sono de Pedra needs a sleep state, which nothing tracks — the same "no Fadiga/asfixia"
    //  gap Troll's own javadoc records. This Talento only lowers the damage threshold that wakes
    //  the sleeper ("danos superiores ao seu valor de Vigor"), so with no sleep there is no
    //  threshold to lower.
    SONO_LEVE(
            "Enquanto no Sono de Pedra você pode despertar ao sofrer qualquer quantidade de danos "
                    + "ou ao perceber anomalias ao seu redor.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Os Efeitos regenerativos de sua Regeneração Reativa se tornam cumulativos, podendo somar
     * uma quantidade de efeitos simultâneos igual 1+ número de Títulos Aventyr Despertos."
     */
    // TODO: Regeneração Reativa is unbuilt — nothing triggers off taking damage. This Talento
    //  removes that Característica's own "Efeito não cumulativo" clause, so it is a modifier to
    //  a rule that is not enforced either.
    REGENERACAO_REATIVA_SUPERIOR(
            "Os Efeitos regenerativos de sua Regeneração Reativa se tornam cumulativos, podendo "
                    + "somar uma quantidade de efeitos simultâneos igual 1+ número de Títulos "
                    + "Aventyr Despertos.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Enquanto em Regeneração Reativa seu corpo exala esporos… Personagens adjacentes que não
     * sejam Trolls da Floresta sofrem Dano Físico Elemental: Natural igual 1+ número de Títulos
     * Aventyr Despertos."
     */
    // TODO: gated on Regeneração Reativa being active, which never happens.
    // TODO: damage to everyone adjacent is an outward, area-shaped effect nothing models —
    //  DamageService only ever computes damage *to* one target *from* an attacker, and Área de
    //  Efeito has no footprint resolution. Same shape as DraconicoFeat#AURA_DRACONICA.
    // TODO: "que não sejam Trolls da Floresta" needs the unmodelled sub-lineage, so even the
    //  exemption could not be honoured.
    REGENERACAO_REATIVA_ESPINHOSA(
            "Enquanto em Regeneração Reativa seu corpo exala esporos que inflige dor e danos a "
                    + "quem estiver próximo. Personagens adjacentes que não sejam Trolls da "
                    + "Floresta, sofrem Dano Físico Elemental: Natural igual 1+ número de Títulos "
                    + "Aventyr Despertos que você possuir (não cumulativo com múltiplas ativações "
                    + "de Regeneração Reativa).",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .requiredFeat(REGENERACAO_REATIVA_SUPERIOR)
                    .build()),

    /**
     * "Enquanto Regeneração Reativa estiver ativa você recebe RDS igual à 1+ número de Títulos
     * Aventyrs… aplicável somente ao primeiro ataque sofrido a cada Rodada."
     */
    // TODO: gated on Regeneração Reativa being active, which never happens. RDS is ordinary RD
    //  (ModifierType.DAMAGE_REDUCTION — see ArtesCompetencyAbility's own "+1 RDS"), and the
    //  amount is computable, but a Feat has no conditional RD hook and the scope is doubly
    //  narrow: only while an unbuilt effect runs, and only on the first attack of each Rodada —
    //  which is the "this one delivered attack" scoping gap, on the defence side.
    REGENERACAO_REATIVA_INVERNAL(
            "Enquanto Regeneração reativa estiver ativa você recebe RDS igual à 1+ número de "
                    + "Títulos Aventyrs (não cumulativo com múltiplas ativações de Regeneração "
                    + "Reativa), esta RDS é aplicável somente ao primeiro ataque sofrido a cada "
                    + "Rodada.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .requiredFeat(REGENERACAO_REATIVA_SUPERIOR)
                    .build()),

    /**
     * "Você recebe Bônus Racial de +1 em Vigor. Trolls da Floresta adicionalmente recebem RM,
     * Trolls do Inverno adicionalmente recebem RD."
     *
     * <p>Its Pré-requisito — "2 outros Talentos Raciais Troll" — is the first racial use of
     * {@code FeatRequirements#requiredFeatCategory}, and reads exactly as written: the Talento
     * being tested is never counted among the two.
     */
    // TODO: a Talento cannot grant an Atributo bonus — Race#getFixedAttributeBonuses() is the
    //  only racial-bonus hook and it belongs to the Race, not to an acquired Talento. Same gap
    //  as AnaoFeat#CONSELHEIRO_DE_GUERRA_YMIRIANO.
    // TODO: the RD/RM half needs the unmodelled sub-lineage to pick between them, and RM
    //  (Redução Mágica) is not a concept this core computes at all — the same gap Gorgona's own
    //  Monstros em pele de Fada cites. Granting the RD half unconditionally would hand it to
    //  every Troll, including the Floresta ones the clause gives RM instead.
    VIGOR_TROLLICO(
            "Você recebe Bônus Racial de +1 em Vigor. Trolls da Floresta adicionalmente recebem "
                    + "RM, Trolls do Inverno adicionalmente recebem RD.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .requiredFeatCategory(FeatCategory.TROLL)
                    .requiredFeatCategoryCount(2)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    TrollFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.TROLL;
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
