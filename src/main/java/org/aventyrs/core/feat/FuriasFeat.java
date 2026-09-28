package org.aventyrs.core.feat;

import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.race.Furia;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Talentos das Fúrias — a tree of exactly one, the mirror of {@code
 * FadasFeat#ASPECTO_DA_BONDADE_NATURAL} and blocked on precisely the same missing piece.
 */
public enum FuriasFeat implements Feat {

    /**
     * "Conjurar Magias Profanas e Encantamentos em personagens inimigos ou neutros custam 1PM ou
     * 1PD à menos (mínimo 1 Unidade)."
     */
    // The PM discount is real (Feat#resolveManaCostReduction): a Profana or Encantamento Magia
    // whose target is anyone outside the caster's own sub-group — FadasFeat's allegiance reading.
    // TODO: the "ou 1PD" half — a mimetized Magia's PD cost is not reduced by any Talento.
    ASPECTO_DA_DECOMPOSICAO_NATURAL(
            "Conjurar Magias Profanas e Encantamentos em personagens inimigos ou neutros custam "
                    + "1PM ou 1PD à menos (mínimo 1 Unidade).",
            FeatRequirements.builder()
                    .requiredRace(Furia.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveManaCostReduction(final Spell spell, final CombatantSheet caster,
                                            final CombatantSheet target, final SceneContext casterContext) {
            boolean scope = spell.getTree().hasMagicType(MagicType.PROFANA) || spell.getTree().hasMagicType(MagicType.ENCANTAMENTO);
            return scope && FadasFeat.isEnemyOrNeutral(caster, target, casterContext) ? 1 : 0;
        }
    };

    private final String description;
    private final FeatRequirements featRequirements;

    FuriasFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FURIAS;
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
