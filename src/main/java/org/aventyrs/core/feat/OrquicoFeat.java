package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.TitleArchetype;

/**
 * Talentos Órquicos — the Orc's devotion to Epona, and the tectonic power it grants.
 *
 * <p>Two clauses are real, and both are Multiplicador de PV: {@link #TERRA_NAS_VEIAS} scales it
 * with the holder's Títulos Aventyr Despertos, and {@link #TREMOR}'s <i>passive</i> half adds one
 * more once two Títulos are held and one of them is Abençoado. Both are pure derivations off
 * {@code Character}, needing nothing this core lacks.
 *
 * <p>This is also the first tree to use {@code FeatRequirements#requiredDeity} — "Apenas
 * personagens Orcs <b>Devotos de Epona</b>" is a gate {@link Deity} and {@code
 * Character#getDeity()} can both answer, so it is enforced rather than left as a comment.
 */
public enum OrquicoFeat implements Feat {

    /**
     * "Após realizar uma Agnação Ancestral você recebe um Subordinado do tipo Peão, que te
     * auxiliará até seu próximo Descanso."
     */
    // TODO: triggered by Agnação Ancestral, which is itself unbuilt — Orc's own javadoc records
    //  it as needing a "spend a resource for a one-time roll effect" transaction this core has
    //  no equivalent of.
    // TODO: a Subordinado is a second creature acting for the holder. SummonedMonsterTemplate can
    //  build one, but nothing models the summoner then acting through it — CLAUDE.md's "A summon
    //  acting on its summoner's roll" gap — and "até seu próximo Descanso" needs a lifetime
    //  RestService would have to clear.
    AGNACAO_ANCESTRAL_SUPERIOR(
            "Após realizar uma Agnação Ancestral você recebe um Subordinado do tipo Peão, que te "
                    + "auxiliará até seu próximo Descanso.",
            FeatRequirements.builder()
                    .requiredRace(Orc.class)
                    .requiredDeity(Deity.EPONA)
                    .build()),

    /**
     * "Seu Multiplicador de PV aumenta em +1 para cada Título Aventyr desperto." Real, and
     * recomputed live — granting a Título raises the holder's PV on the next call, with nothing
     * to migrate.
     *
     * <p>Note this grants <b>zero</b> to a character holding no Título, so acquiring it early is
     * legal (its Pré-requisito names only Vigor 3) and simply inert until the first Título is
     * Desperto. That is the text, not an oversight.
     */
    TERRA_NAS_VEIAS(
            "Seu Multiplicador de PV aumenta em +1 para cada Título Aventyr desperto.",
            FeatRequirements.builder()
                    .requiredRace(Orc.class)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return character.getAllTitles().size();
        }
    },

    /**
     * "Seus ataques com Armas e Armas Naturais causam danos Físicos Elementais: Terra em
     * substituição aos seus tipos… Você pode adicionar Metade do Vigor às suas rolagens de danos
     * físicos."
     */
    // TODO: re-typing an attack's dano is not expressible — DamageType is a classification a
    //  caller supplies per hit, and nothing lets a held trait *override* what an attack deals.
    //  DamageBonus can carry ELEMENTAL + ElementalType.TERRA, but that types a bonus, not the attack.
    // TODO: the "+Metade do Vigor às rolagens de danos físicos" half is a flat dano bonus whose
    //  amount is computable, but Feat has no resolveDamageBonus hook — that lives on
    //  SkillCompetencyAbility and EgoAdvantage only, and both are reached through the skill
    //  Interaction rather than through character.getFeats().
    // TODO: the Magia half additionally needs a Magia's own damage type, which Spell has no
    //  column for at all.
    PALADINO_DE_EPONA(
            "Seus ataques com Armas e Armas Naturais causam danos Físicos Elementais: Terra em "
                    + "substituição aos seus tipos. Suas Magias, apenas Divinas e Elementais: "
                    + "Terra, capazes de infligir danos sempre causam Danos Físico ao invés de "
                    + "Mágicos. Você pode adicionar Metade do Vigor às suas rolagens de danos "
                    + "físicos, se um efeito puder alterar a natureza dos seus danos para mágico "
                    + "ela deixará de fazê-lo.",
            FeatRequirements.builder()
                    .requiredRace(Orc.class)
                    .requiredDeity(Deity.EPONA)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * An Efeito Ativo plus an Efeito Passivo. The <b>passive</b> half is real: "Se tiver 2
     * Títulos Aventyr Despertos e ao menos 1 deles for Abençoado seu Multiplicador de PV aumenta
     * em +1."
     *
     * <p>Both halves of that condition are ordinary questions about {@code
     * Character#getAllTitles()}, so it needs no new mechanism — and it stacks with {@link
     * #TERRA_NAS_VEIAS} for a holder of both, since {@code
     * HitPointsService#getLifeMultiplier} sums every Talento's contribution.
     */
    // TODO: the Efeito Ativo needs three things at once — an activated ability spending 3PA+3PM
    //  (nothing converts a spend into an attack), a dano bonus from a Feat (no hook, see
    //  PALADINO_DE_EPONA), and Área de Efeito — Explosão resolution, which is CLAUDE.md's "Area
    //  de Efeito" row part (a): AreaOfEffect describes a footprint that nothing turns into a set
    //  of targets.
    TREMOR(
            "Efeito Ativo - Ao Tempo de Ação de 3PA e Custo de 3PM, você pode realizar um ataque "
                    + "em um alvo em seu alcance com uma de suas Armas ou Armas Naturais. Para "
                    + "este ataque você recebe Vantagem em sua Rolagem de Ataque Corpo-a-Corpo e "
                    + "Bônus em rolagem de danos igual ao seus Multiplicadores de PV, o dano "
                    + "causado é Físico Elemental: Terra e tem Área de Efeito – Explosão. Efeito "
                    + "Passivo - Se tiver 2 Títulos Aventyr Despertos e ao menos 1 deles for "
                    + "Abençoado seu Multiplicador de PV aumenta em +1.",
            FeatRequirements.builder()
                    .requiredFeat(PALADINO_DE_EPONA)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            boolean twoTitles = character.getAllTitles().size() >= TREMOR_REQUIRED_TITLES;
            boolean oneAbencoado = character.getAllTitles().stream()
                    .map(AventyrTitle::getArchetype)
                    .anyMatch(archetype -> archetype == TitleArchetype.ABENCOADO);
            return twoTitles && oneAbencoado ? 1 : 0;
        }
    },

    /**
     * "Na Rodada após utilizar Tremor… você pode fazer tremer a Área de Efeito, causando Danos
     * Físico Primordial igual a Metade dos seus Multiplicadores de PV a todos os outros
     * personagens na área."
     */
    // TODO: triggered by TREMOR's Efeito Ativo, which does not exist.
    // TODO: recurring area damage on a following Rodada needs both Área de Efeito resolution and
    //  a delayed-effect mechanism — TemporaryEffect ticks a countdown on its holder's own sheet,
    //  it cannot re-damage a set of other combatants standing in a remembered footprint.
    // TODO: "apenas uma vez a cada Rodada" needs a per-Rodada activation counter, which
    //  CharacterSheet does not track.
    TREMOR_RESIDUAL(
            "Na Rodada após utilizar Tremor, como uma Ação Livre e ao Custo de 1PM, você pode "
                    + "fazer tremer a Área de Efeito, causando Danos Físico Primordial igual a "
                    + "Metade dos seus Multiplicadores de PV a todos os outros personagens na "
                    + "área. Você pode repetir este efeito por uma quantidade de Rodadas igual ao "
                    + "número de Títulos Aventyr Brutos que você possuir, mas apenas uma vez a "
                    + "cada Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(TREMOR)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.BRUTO)
                    .build());

    private static final int TREMOR_REQUIRED_TITLES = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    OrquicoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ORQUICO;
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
