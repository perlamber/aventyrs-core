package org.aventyrs.core.feat;

import java.util.Optional;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageBonus;
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
    // Three halves real. The retyping is Feat#resolveDamageRetype, reported on the attack roll:
    // a Weapon (an Arma Natural included) deals Físico Elemental: Terra; a Divina or Elemental:
    // Terra Magia delivered as an attack deals Físico. The "+Metade do Vigor às rolagens de danos
    // físicos" is a dano bonus on those weapon attacks. ⚠️ "Você pode adicionar" is read as always
    // taken, since it costs nothing.
    // TODO: "se um efeito puder alterar a natureza dos seus danos para mágico ela deixará de
    //  fazê-lo" — a precedence over other retyping; this retype is simply the first held
    //  Talento's, and no Talento retypes to Mágico today.
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
                    .build()) {
        @Override
        public DamageDescriptor resolveDamageRetype(final Character attacker, final SkillType attackSkill,
                                                    final AttackSource attackSource) {
            if (attackSource instanceof Weapon) {
                return new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.TERRA);
            }
            if (attackSource instanceof Spell spell && (spell.getTree().hasMagicType(MagicType.DIVINA)
                    || spell.getTree().getElementalType().filter(el -> el == ElementalType.TERRA).isPresent())) {
                return new DamageDescriptor(DamageType.FISICO);
            }
            return null;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource) {
            return attackSource instanceof Weapon
                    ? Optional.of(new DamageBonus(actor.getEffectiveAttributeTotal(AttributeDomain.VIGOR) / 2,
                            DamageType.FISICO))
                    : Optional.empty();
        }
    },

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
    // TODO: the Efeito Ativo is *not* an ActiveAbility, and that is the point: activate() spends
    //  a cost and applies TemporaryEffects lasting N Rodadas, while this clause fires a single
    //  attack with no Duração at all. Adding a Resfriamento to that transaction (Phase 4) changed
    //  nothing here. What it needs: an "activation that delivers one attack" shape; a dano bonus
    //  scoped to "this one activated attack" (Feat#resolveDamageBonus is real but unscoped); and
    //  Área de Efeito — Explosão resolution, CLAUDE.md's "Area de Efeito" row part (a).
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
