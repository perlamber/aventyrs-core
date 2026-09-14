package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Talentos Troll — three of the five extend <b>Regeneração Reativa</b>, the Troll's signature
 * Característica Racial; one extends Sono de Pedra, and the last is a plain Atributo grant.
 *
 * <p><b>Three of the five are real.</b> {@link #VIGOR_TROLLICO}'s "+1 em Vigor" half lands through
 * {@link Feat#resolveAttributeBonus}; {@link #REGENERACAO_REATIVA_SUPERIOR} and {@link
 * #REGENERACAO_REATIVA_INVERNAL} both act on a Regeneração Reativa that now genuinely runs —
 * {@code TrollsRacialAbility#REGENERACAO_REATIVA}, the Habilidade Racial itself, resolved by
 * {@code DamageService#notifyDamageTaken} (fired by the two paths that deliver an attack's
 * damage) into a {@code org.aventyrs.core.sheet.Regeneration} on whoever was hit. The remaining
 * two are each blocked on a system of their own — a sleep state for {@link #SONO_LEVE}, outward area damage for {@link
 * #REGENERACAO_REATIVA_ESPINHOSA} — as is the RD/RM half of {@link #VIGOR_TROLLICO}.
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
 * were deliberately not made a nested choice enum — every clause distinguishing them was blocked
 * on the missing elemental-vulnerability mechanism, so the enum would have carried nothing.
 *
 * <p><b>That is now one clause short of untrue</b>, and is the thing to revisit next in this tree.
 * {@link #VIGOR_TROLLICO}'s "Trolls da Floresta recebem RM, Trolls do Inverno recebem RD" needs no
 * missing mechanism — both stats are real — only the lineage itself, so a {@code Troll.Linhagem}
 * enum on the {@code Ogro.Aptidao} pattern would carry a live effect the day it lands. It is not
 * added here because it reaches further than this tree: {@code Troll} would take a {@code @NonNull}
 * constructor argument (every {@code new Troll()} in the codebase), and gating the two "apenas
 * Trolls do/da …" Pré-requisitos on it needs a {@code FeatRequirements} clause that does not exist
 * — "a held Race's own choice must be X", the sibling of the missing per-Talento one.
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
     *
     * <p><b>Real</b>, through {@link Feat#resolveSimultaneousRegenerationLimit}. The base
     * Característica is "Efeito não cumulativo" — a ceiling of one {@code
     * org.aventyrs.core.sheet.Regeneration} at a time, which is why the ceiling is a number rather
     * than a flag: this lifts it to 1 + Títulos Despertos.
     *
     * <p><b>The Habilidade Racial is what reads this</b> — {@code
     * TrollsRacialAbility#REGENERACAO_REATIVA} scans its holder's Talentos for the highest claim
     * and states the result on the {@code Blessing} it grants, so a hook named for Regeneração
     * Reativa can never widen an unrelated damage-taken Blessing. {@code
     * CombatantSheet#applyEffect} then trims the oldest instances down to fit. Each surviving
     * instance keeps its own Rodada count and its own recovery budget, so several hits genuinely
     * stack their healing instead of the newest replacing what came before.
     */
    REGENERACAO_REATIVA_SUPERIOR(
            "Os Efeitos regenerativos de sua Regeneração Reativa se tornam cumulativos, podendo "
                    + "somar uma quantidade de efeitos simultâneos igual 1+ número de Títulos "
                    + "Aventyr Despertos.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveSimultaneousRegenerationLimit(final Character character) {
            return BASE_REGENERATION_TERM + character.getAllTitles().size();
        }
    },

    /**
     * "Enquanto em Regeneração Reativa seu corpo exala esporos… Personagens adjacentes que não
     * sejam Trolls da Floresta sofrem Dano Físico Elemental: Natural igual 1+ número de Títulos
     * Aventyr Despertos."
     */
    // The "enquanto em Regeneração Reativa" gate is readable now — CombatantSheet
    // #hasActiveRegeneration() — but there is nothing to gate.
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
     *
     * <p><b>Real</b>, through {@link Feat#resolveDamageReduction(Character, CombatantSheet)} — RDS
     * is ordinary RD (see {@code ArtesCompetencyAbility}'s own "+1 RDS"), and both halves of the
     * condition are sheet-held state the longest overload can see: {@code
     * CombatantSheet#hasActiveRegeneration()} and {@code
     * CombatantSheet#getAttacksSufferedThisRound()}. A {@code null} holder — a {@code
     * Character}-only RD preview — reads as "condition not met", the usual convention.
     *
     * <p><b>"Não cumulativo com múltiplas ativações" needs no enforcement here.</b> The figure is
     * a property of <i>being</i> in Regeneração Reativa rather than of any one instance, so a
     * holder running three at once (via {@link #REGENERACAO_REATIVA_SUPERIOR}) still gets it once
     * — which is what asking the sheet a yes/no question buys.
     */
    REGENERACAO_REATIVA_INVERNAL(
            "Enquanto Regeneração reativa estiver ativa você recebe RDS igual à 1+ número de "
                    + "Títulos Aventyrs (não cumulativo com múltiplas ativações de Regeneração "
                    + "Reativa), esta RDS é aplicável somente ao primeiro ataque sofrido a cada "
                    + "Rodada.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .requiredFeat(REGENERACAO_REATIVA_SUPERIOR)
                    .build()) {
        @Override
        public int resolveDamageReduction(final Character character, final CombatantSheet holder) {
            if (holder == null || !holder.hasActiveRegeneration()
                    || holder.getAttacksSufferedThisRound() > 0) {
                return 0;
            }
            return BASE_REGENERATION_TERM + character.getAllTitles().size();
        }
    },

    /**
     * "Você recebe Bônus Racial de +1 em Vigor. Trolls da Floresta adicionalmente recebem RM,
     * Trolls do Inverno adicionalmente recebem RD."
     *
     * <p>Its Pré-requisito — "2 outros Talentos Raciais Troll" — is the first racial use of
     * {@code FeatRequirements#requiredFeatCategory}, and reads exactly as written: the Talento
     * being tested is never counted among the two.
     */
    /**
     * The "+1 em Vigor" half is <b>real</b>, through {@link Feat#resolveAttributeBonus} — an
     * unconditional grant to every Troll regardless of sub-lineage (the clause names no
     * condition), reaching every Atributo-total reader via {@code
     * Character#getEffectiveAttributeTotal}, exactly as {@code BestialFeat}'s Heranças.
     */
    // TODO: RD and RM are both real stats (Feat#resolveDamageReduction / resolveMagicReduction),
    //  so the only blocker left on this half is the unmodelled sub-lineage that picks between
    //  them — Troll da Floresta gets RM, Troll do Inverno RD. Granting either unconditionally
    //  would hand it to every Troll, including the half the clause gives the other to. This is
    //  the one Troll clause a Troll.Linhagem enum would make real on its own; see this enum's
    //  javadoc for what adding it costs.
    VIGOR_TROLLICO(
            "Você recebe Bônus Racial de +1 em Vigor. Trolls da Floresta adicionalmente recebem "
                    + "RM, Trolls do Inverno adicionalmente recebem RD.",
            FeatRequirements.builder()
                    .requiredRace(Troll.class)
                    .requiredFeatCategory(FeatCategory.TROLL)
                    .requiredFeatCategoryCount(2)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return domain == AttributeDomain.VIGOR ? VIGOR_TROLLICO_ATTRIBUTE_BONUS : 0;
        }
    };

    /** VIGOR_TROLLICO's own stated "+1 em Vigor". */
    private static final int VIGOR_TROLLICO_ATTRIBUTE_BONUS = 1;

    /**
     * The "1+" every Regeneração Reativa Talento in this tree opens its figure with — the
     * simultaneous-effect ceiling of {@link #REGENERACAO_REATIVA_SUPERIOR}, the RDS of {@link
     * #REGENERACAO_REATIVA_INVERNAL}, and the Dano of {@link #REGENERACAO_REATIVA_ESPINHOSA} were
     * it expressible. One constant because it is one phrase, repeated verbatim across the three.
     */
    private static final int BASE_REGENERATION_TERM = 1;

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
