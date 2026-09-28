package org.aventyrs.core.feat;

import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Talentos das Fadas — a tree of exactly one, the Fada's half of a matched pair with {@code
 * FuriasFeat#ASPECTO_DA_DECOMPOSICAO_NATURAL}. The two are mirror images: one cheapens Magias
 * Divinas e Naturais cast on allies, the other Magias Profanas e Encantamentos cast on enemies.
 */
public enum FadasFeat implements Feat {

    /**
     * "Conjurar Magias Divinas e Naturais em personagens aliados ou neutros custam 1PM ou 1PD à
     * menos (mínimo 1 Unidade)."
     */
    // The PM discount is real (Feat#resolveManaCostReduction): a Divina or Natural Magia whose
    // target is the caster, an ally, or anyone outside the enemy sub-group — "neutro" read as
    // in neither of the caster's SceneContext lists. A cast with no target or no Scene gets none.
    // TODO: the "ou 1PD" half — a mimetized Magia's PD cost is not reduced by any Talento.
    ASPECTO_DA_BONDADE_NATURAL(
            "Conjurar Magias Divinas e Naturais em personagens aliados ou neutros custam 1PM ou "
                    + "1PD à menos (mínimo 1 Unidade).",
            FeatRequirements.builder()
                    .requiredRace(Fada.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveManaCostReduction(final Spell spell, final CombatantSheet caster,
                                            final CombatantSheet target, final SceneContext casterContext) {
            boolean scope = spell.getTree().hasMagicType(MagicType.DIVINA) || spell.getTree().hasMagicType(MagicType.NATURAL);
            return scope && isAllyOrNeutral(caster, target, casterContext) ? 1 : 0;
        }
    };

    private final String description;
    private final FeatRequirements featRequirements;

    FadasFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FADAS;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }

    /** "Aliados ou neutros": the caster, a sub-group ally, or anyone in neither list. Unknown → false. */
    static boolean isAllyOrNeutral(final CombatantSheet caster, final CombatantSheet target, final SceneContext context) {
        if (target == null || context == null) {
            return false;
        }
        return target.getId().equals(caster.getId())
                || context.getEnemies().stream().noneMatch(enemy -> enemy.getId().equals(target.getId()));
    }

    /** "Inimigos ou neutros": anyone not the caster and not a sub-group ally. Unknown → false. */
    static boolean isEnemyOrNeutral(final CombatantSheet caster, final CombatantSheet target, final SceneContext context) {
        if (target == null || context == null || target.getId().equals(caster.getId())) {
            return false;
        }
        return context.getAllies().stream().noneMatch(ally -> ally.getId().equals(target.getId()));
    }
}
