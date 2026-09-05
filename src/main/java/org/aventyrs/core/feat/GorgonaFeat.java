package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.race.Gorgona;

/**
 * Talentos Górgonas — a tree structured entirely around the race's own curse: two Talentos that
 * resolve it in opposite directions (embrace the monstrous form, or be freed from it), two
 * mutually-exclusive protections, and three Aventyr-tier Talentos.
 *
 * <p>Two constants carry real effects, both RD, through the {@code Feat#resolveDamageReduction}
 * hook this batch added: {@link #PROTECAO_DO_DEUS_DOS_MONSTROS} and {@link
 * #PROTECAO_DA_RAINHA_DAS_FADAS}.
 *
 * <p><b>Every remaining constant is blocked on the same thing: Górgona's forms.</b> The race's
 * own Monstros em pele de Fada Característica — toggling between a Feérica and a Monstruosa form
 * — is unbuilt, and {@code Gorgona}'s javadoc records why (no "retain an activated stance" state,
 * and RM is not a concept this core computes). Six of these seven Talentos either presuppose a
 * form, change which form the holder is locked into, or scope an effect to one.
 *
 * <p><b>Three exclusion pairs, none enforceable.</b> Marca da Maldição ↔ Acolhida por Flora, and
 * Proteção do Deus dos Monstros ↔ Proteção da Rainha das Fadas. {@code FeatRequirements} carries
 * only thresholds that must be met, never one that must not — so a character can legally hold
 * both halves of either pair, which the text forbids.
 */
public enum GorgonaFeat implements Feat {

    /**
     * "Você está sempre em sua forma monstruosa e é incapaz de alternar para a forma humanoide."
     */
    // TODO: locks the holder into a form that does not exist — see the class javadoc.
    // TODO: Olhar de Lacerto is itself unbuilt (Gorgona's javadoc calls it the densest gap of any
    //  racial trait catalogued), so widening its Alcance widens nothing.
    // TODO: Corrente de Efeitos is an unbuilt system, and "Enrijecer Musculatura" is not among
    //  the 13 EffectChainService resolves.
    // TODO: "sempre considerado Amaldiçoado" now has a classification to name
    //  (ConditionType.AMALDICOADO, appliable open-ended with a null duration), but nothing applies
    //  a Condition from a held Talento — Feat has no condition hook, and "sempre" is a standing
    //  state rather than a triggered one. Losing Imunidade a Encantamentos additionally needs a
    //  mechanism for a Talento to *suppress* a Característica Racial — Race#getRacialAbilities()
    //  is read live with no way to suspend it — and no Encantamento condition is authored.
    MARCA_DA_MALDICAO(
            "Você está sempre em sua forma monstruosa e é incapaz de alternar para a forma "
                    + "humanoide. O Alcance de seu Olhar de Lacerto aumenta para Distância Curta e "
                    + "suas Presas Longas recebem a Corrente de Efeitos – Enrijecer Musculatura. "
                    + "Para efeitos diversos você é sempre considerado um personagem Amaldiçoado e "
                    + "não possui a Característica Racial Imunidade a Encantamentos. Um mesmo "
                    + "personagem não pode possuir os Talentos Marca da Maldição e Acolhida por "
                    + "Flora.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .build()),

    /**
     * "Você está completamente liberta da maldição e não pode acessar a forma monstruosa."
     */
    // TODO: the mirror of MARCA_DA_MALDICAO, and blocked on the same missing form.
    // TODO: suppressing Abandonadas pelos Deuses and substituting Feromônio Encantador both need
    //  a Talento to replace a Característica Racial, which nothing can do.
    // TODO: "+2 em Conjuração, Danos e Curas de suas Magias Naturais" needs a Magia to have
    //  numeric effects — Spell has no damage or healing column — and a conjuração bonus hook,
    //  which SpellCastingService has never had (see its own javadoc: the ability that once
    //  justified building it was dropped in a rules revision).
    ACOLHIDA_POR_FLORA(
            "Você está completamente liberta da maldição e não pode acessar a forma monstruosa. "
                    + "Você não possui a Característica Racial Abandonada pelos Deuses, ao invés "
                    + "disso recebe a Habilidade Feromônio Encantador, também recebe Bônus de +2 "
                    + "em Conjuração, Danos e Curas de suas Magias Naturais. Um mesmo personagem "
                    + "não pode possuir os Talentos Marca da Maldição e Acolhida por Flora.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .build()),

    /**
     * "Você recebe RDS e RD, enquanto em sua Forma Monstruosa você recebe Resistência à
     * Críticos." The RD half is real.
     *
     * <p><b>The source text is redundant here</b> — RDS <i>is</i> RD (Redução de Danos Sofridos;
     * see {@code ArtesCompetencyAbility}'s own "+1 RDS"), so "RDS e RD" names one stat twice.
     * Read as a single grant rather than doubled, and since the clause states no figure it uses
     * {@code DamageService#DEFAULT_DAMAGE_REDUCTION}, the convention for an RD clause with no
     * number in its rules text.
     */
    // TODO: Resistência a Críticos is form-gated *and* is not a stat this core computes —
    //  distinct from Race#getCriticalEffectImmunities(), which is an identity-keyed filter.
    PROTECAO_DO_DEUS_DOS_MONSTROS(
            "Você recebe RDS e RD, enquanto em sua Forma Monstruosa você recebe Resistência à "
                    + "Críticos. Um mesmo personagem não pode possuir os Talentos Proteção do Deus "
                    + "dos Monstros e Proteção da Rainha das Fadas.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public int resolveDamageReduction(final Character character) {
            return DamageService.DEFAULT_DAMAGE_REDUCTION;
        }
    },

    /**
     * "Você recebe RDS e RM, enquanto em sua forma Feérica você recebe Resistência a Críticos."
     * The RDS half is real.
     */
    // TODO: RM (Redução Mágica) is not a concept this core computes at all — the same gap
    //  Gorgona's own Monstros em pele de Fada and TrollFeat#VIGOR_TROLLICO cite. Only the RDS
    //  half lands, which makes this Talento strictly weaker than its Monstros twin today even
    //  though the two are written as equals.
    // TODO: Resistência a Críticos is form-gated and uncomputed, same as its twin.
    PROTECAO_DA_RAINHA_DAS_FADAS(
            "Você recebe RDS e RM, enquanto em sua forma Feérica você recebe Resistência a "
                    + "Críticos. Um mesmo personagem não pode possuir os Talentos Proteção do Deus "
                    + "dos Monstros e Proteção da Rainha das Fadas.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public int resolveDamageReduction(final Character character) {
            return DamageService.DEFAULT_DAMAGE_REDUCTION;
        }
    },

    /**
     * "Você recebe Desvantagens em rolagens de Persuasão, mas recebe Vantagem em suas Rolagens de
     * Ataque e Danos de seu Olhar de Lacerto."
     */
    // TODO: withheld whole rather than half-implemented. The Desvantagem em Persuasão *is*
    //  expressible today through Feat#resolveSkillRollBonus (a flat Skill#DISADVANTAGE_MALUS),
    //  but the Vantagem that pays for it is scoped to Olhar de Lacerto, which is unbuilt —
    //  granting only the malus would leave a Górgona strictly worse off for acquiring the
    //  Talento. Same reasoning as GiganteFeat's two Clã Talentos.
    CABELO_SERPENTINO(
            "Seu cabelo está sempre em forma de Serpente, o que assusta ou incomoda outros "
                    + "personagens. Você recebe Desvantagens em rolagens de Persuasão, mas recebe "
                    + "Vantagem em suas Rolagens de Ataque e Danos de seu Olhar de Lacerto.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você pode adquirir Talentos Monstruosos que não sejam Raciais e recebe a Habilidade Racial
     * Ferocidade de Lacerto (ver Indômitos) como se fosse um Impuro."
     */
    // TODO: Ferocidade de Lacerto is unbuilt — Indomito's own javadoc records why it is withheld
    //  rather than approximated (it is a state that can be declined, and nothing tracks it).
    // TODO: "pode adquirir Talentos Monstruosos que não sejam Raciais" would widen what this
    //  character may acquire, but FeatCategory#MONSTRUOSO is itself a racial category, so the
    //  carve-out names a distinction the enum does not draw. Nothing gates acquisition on a
    //  category anyway — only requiredRace/requiredCreatureType do.
    // TODO: "efeitos desencadeados apenas durante a Forma Monstruosa" needs the form.
    ACEITAR_A_SELVAGERIA(
            "Você pode adquirir Talentos Monstruosos que não sejam Raciais e recebe a Habilidade "
                    + "Racial Ferocidade de Lacerto (ver Indômitos) como se fosse um Impuro. "
                    + "Talentos Monstruosos adquiridos tem seus efeitos desencadeados apenas "
                    + "durante a Forma Monstruosa.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Escolha duas Árvores de Magia Natural, você pode Mimetizar as magias Semente das Árvores
     * escolhidas."
     */
    // TODO: mimetizar has no mechanism, spending PD in place of PM has no cost step to redirect,
    //  and the whole effect is form-gated. (The two chosen Árvores could be recorded now — a
    //  choice-carrying AbstractFeat subclass, see FocoEmPericiaFeat — but mimetizar is the
    //  blocker.)
    ABENCOADA_PELO_CONCLAVE(
            "Você pode adquirir Talentos Feéricos. Escolha duas Árvores de Magia Natural, você "
                    + "pode Mimetizar as magias Semente das Árvores escolhidas. Ao Despertar seu "
                    + "primeiro Título poderá conjurar as magias Broto destas árvores ao custo de "
                    + "2PD. Ao Despertar seu segundo Título Aventyr também poderá conjurar as "
                    + "magias do Tipo Muda (ao custo de 3PD). Magias das Árvores escolhidas só "
                    + "podem ser Mimetizadas enquanto em sua forma Feérica.",
            FeatRequirements.builder()
                    .requiredRace(Gorgona.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    GorgonaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.GORGONA;
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
