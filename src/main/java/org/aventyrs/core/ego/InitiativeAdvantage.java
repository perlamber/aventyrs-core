package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;

import java.util.List;
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

    /**
     * The "o seu Movimento Base e o de seus aliados aumentam em +2UD" half is fully wired:
     * {@link #resolveInitiativeBlessings} grants a {@link Blessing}
     * ({@link ModifierType#MOVEMENT}, +{@value #MOVEMENT_BONUS}UD, {@value
     * #FIRST_ROUNDS_COUNT} Rounds, {@link TargetScope#SELF_AND_ALLIES}) — resolved by {@code
     * org.aventyrs.core.character.services.InitiativeBlessingService} and applied by {@code
     * org.aventyrs.core.scene.Scene#applyInitiativeBlessings} as a {@code TemporaryBonus} on
     * the winner and every Scene ally, consumed by summing {@code
     * org.aventyrs.core.character.services.MovementService#getTotalMovement} with {@code
     * CharacterSheet#getTemporaryBonus(ModifierType.MOVEMENT)}.
     *
     * <p>TODO: "seus movimentos não permitem Reações de seus inimigos" stays unimplemented —
     * this core has no movement/positioning system to suppress a Reação from at all (it
     * doesn't do geometry — see {@code org.aventyrs.core.scene.Range}'s own javadoc), a
     * different gap than the movement-*amount* half above, which only needed a stat to add UD
     * to, not a whole Reação-triggering mechanism.
     */
    POSICIONAMENTO_ESTRATEGICO("Apenas nas duas primeiras Rodadas de cada Cena de " +
            "Combate, seus movimentos não permitem Reações de seus inimigos. " +
            "Adicionalmente, se você tiver ganho a iniciativa, o seu Movimento Base e o " +
            "de seus aliados aumentam em +2UD nestas Rodadas.") {
        @Override
        public List<Blessing> resolveInitiativeBlessings() {
            return List.of(new Blessing(ModifierType.MOVEMENT, MOVEMENT_BONUS, FIRST_ROUNDS_COUNT, TargetScope.SELF_AND_ALLIES, name()));
        }
    },

    /**
     * Fully wired, the same shape as {@link #IMPETO} (a per-calculation {@link SceneContext}
     * check, not {@link #POSICIONAMENTO_ESTRATEGICO}'s group-blessing mechanism — both of this
     * Vantagem's clauses are about the holder's own defense, never granted to allies):
     * {@link #resolveAbsoluteDamageReduction} grants RA ({@value
     * org.aventyrs.core.character.services.DamageService#DEFAULT_DAMAGE_REDUCTION}, this rules
     * text's own number isn't spelled out — see {@code DamageService.DEFAULT_DAMAGE_REDUCTION}'s
     * own javadoc for why RD's unspecified-bonus default is reused here) while {@link
     * SceneContext#isWithinFirstCombatRounds} is true, and {@link #resolveHalfDamage} halves
     * damage taken once {@link SceneContext#hasWonInitiative()} is also true — both summed by
     * {@code DamageServiceImpl}'s new {@code SceneContext}-accepting overloads across every
     * held {@code EgoAdvantage}, reached end-to-end via {@code DamageInteraction}'s own new
     * {@code SceneContext}-accepting overloads.
     */
    TORRE_EM_MOVIMENTO("Nas duas primeiras Rodadas de cada Cena de Combate você recebe " +
            "RA. Se você tiver ganho a iniciativa, neste período dano causado a você é " +
            "reduzido à metade.") {
        @Override
        public int resolveAbsoluteDamageReduction(final SceneContext sceneContext) {
            if (sceneContext != null && sceneContext.isWithinFirstCombatRounds(FIRST_ROUNDS_COUNT)) {
                return DamageService.DEFAULT_DAMAGE_REDUCTION;
            }
            return 0;
        }

        @Override
        public boolean resolveHalfDamage(final SceneContext sceneContext) {
            return sceneContext != null && sceneContext.isWithinFirstCombatRounds(FIRST_ROUNDS_COUNT)
                    && sceneContext.hasWonInitiative();
        }
    };

    /** How many of a Cena de Combate's first Rounds IMPETO's Vantagem/POSICIONAMENTO_ESTRATEGICO's blessing apply during. */
    private static final int FIRST_ROUNDS_COUNT = 2;

    /** POSICIONAMENTO_ESTRATEGICO's own Movimento Base bonus — a coincidentally-equal-to-Vantagem number, not Vantagem itself. */
    private static final int MOVEMENT_BONUS = 2;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.INICIATIVA;
    }
}
