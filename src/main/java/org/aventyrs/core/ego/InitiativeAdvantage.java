package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;

import java.util.Optional;

/**
 * The Vantagem de Iniciativa chosen once at character creation — available only to
 * characters whose Iniciativa base reached {@value
 * org.aventyrs.core.character.services.CharacterCreationService#EGO_ADVANTAGE_MIN_BASE}
 * through the creation-time point distribution (see {@link
 * org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}).
 * Reaching that base any other way (Talentos, Títulos Aventyrs, other Habilidades) never
 * grants access to this choice, and it is never lost if Iniciativa later drops below it.
 */
@Getter
@AllArgsConstructor
public enum InitiativeAdvantage implements EgoAdvantage {

    /**
     * Fully wired: {@link #resolveConditionalRollBonus} grants Vantagem on every Perícia
     * roll while {@link SceneContext#isWithinFirstCombatRounds} is true (summed generically
     * for every skill by {@code org.aventyrs.core.skill.AbstractSkillInteraction}, so no
     * per-skill wiring was needed), and {@link #resolveDamageBonus} grants the same Vantagem
     * toward a dano roll once {@link SceneContext#hasWonInitiative()} is also true (summed for
     * every attack-skill roll the same generic way — this incidentally covers Ataque Corpo a
     * Corpo too, unlike {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s own dano bonus,
     * since this one needs no {@code attackTarget} to condition on).
     *
     * <p>TODO: the "Vantagem em... Efeitos de Magia" half of this Vantagem's rules text is
     * still unimplemented — this core has no "Efeito de Magia roll bonus" concept at all to
     * attach it to ({@code org.aventyrs.core.magic.SpellCastingService} only orchestrates the
     * delivery roll + Domínio do Mana roll, and {@code org.aventyrs.core.effect.EffectChainService}
     * only resolves margin thresholds — neither models a bonus toward some third "effect" roll
     * the way {@link DamageBonus} models one for dano), unlike the Dano half, which reuses that
     * already-real mechanism.
     */
    IMPETO("Você recebe Vantagem em suas Rolagens de Perícia nas duas primeiras Rodadas " +
            "de cada Cena de Combate. Se tiver ganho a iniciativa, você também recebe " +
            "Vantagem em suas rolagens de Dano e Efeitos de Magia.") {
        @Override
        public Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext) {
            if (sceneContext != null && sceneContext.isWithinFirstCombatRounds(FIRST_ROUNDS_COUNT)) {
                return Optional.of(Skill.ADVANTAGE_BONUS);
            }
            return Optional.empty();
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SceneContext sceneContext) {
            if (sceneContext != null && sceneContext.isWithinFirstCombatRounds(FIRST_ROUNDS_COUNT)
                    && sceneContext.hasWonInitiative()) {
                return Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO));
            }
            return Optional.empty();
        }
    },

    // TODO: during the first 2 Rounds of a Cena de Combate, your movement doesn't allow
    // enemy Reações; if you won initiative, your and your allies' Movimento Base also
    // increases by +2UD during those same Rounds. The Round/Cena-de-Combate/won-initiative
    // detection IMPETO needed is now real (SceneContext#isWithinFirstCombatRounds/
    // #hasWonInitiative), but this ability's own effects still have nothing to attach to:
    // there's no movement/positioning system to suppress a Reação from (this core doesn't
    // do geometry — see org.aventyrs.core.scene.Range's own javadoc) and no Movimento Base
    // stat on Character at all.
    POSICIONAMENTO_ESTRATEGICO("Apenas nas duas primeiras Rodadas de cada Cena de " +
            "Combate, seus movimentos não permitem Reações de seus inimigos. " +
            "Adicionalmente, se você tiver ganho a iniciativa, o seu Movimento Base e o " +
            "de seus aliados aumentam em +2UD nestas Rodadas."),

    // TODO: grants RA during the first 2 Rounds of a Cena de Combate; if you won
    // initiative, damage taken during that same window is also halved. Same story as
    // POSICIONAMENTO_ESTRATEGICO: the Round/Cena-de-Combate/won-initiative detection is now
    // real, but DamageService's RA/HALF_DAMAGE summation (DamageServiceImpl#sumAcrossSources)
    // takes only a Character, no SceneContext at all — it's a reflection-based @Modifier scan
    // with no per-roll/per-Round input, so scoping either reduction to specific Rounds of a
    // Cena de Combate still isn't wireable without first giving DamageService a SceneContext
    // parameter, which hasn't been done.
    TORRE_EM_MOVIMENTO("Nas duas primeiras Rodadas de cada Cena de Combate você recebe " +
            "RA. Se você tiver ganho a iniciativa, neste período dano causado a você é " +
            "reduzido à metade.");

    /** How many of a Cena de Combate's first Rounds IMPETO's Vantagem applies during. */
    private static final int FIRST_ROUNDS_COUNT = 2;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.INICIATIVA;
    }
}
