package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;
import java.util.Optional;

/**
 * The {@link ActiveAbility} a Poder Vampírico grants — one class, parameterized by which {@link
 * VampiricoFeat} constant it belongs to, because the six Poderes share every cost and Duração:
 * an <b>Ação Livre</b> ({@link #getActionPointCost()} == 0), no Pontos de Magia, <b>3PV</b>
 * ({@code Vampiro}'s Sangue, Poder e Dependência: "consomem 3PV cada"), and a base <b>2 Rodadas</b>
 * ({@link #BASE_DURATION_IN_ROUNDS}) extended by one Rodada per Título Aventyr while {@link
 * VampiricoFeat#PODER_VAMPIRICO_DURADOURO} is held.
 *
 * <p>Triggered through {@code org.aventyrs.core.character.services.ActiveAbilityService#activate},
 * which spends the 3PV (plain damage — its "recuperados exclusivamente com Roubo de Vida"
 * provenance is untracked) and applies every effect from {@link #resolveEffects(Character)}.
 *
 * <p>{@link VampiricoFeat#PODER_VAMPIRICO_DURADOURO}'s own first clause, "a Duração de seus
 * Poderes Vampíricos aumenta para 2 Rodadas", is a no-op here: the race Característica already
 * sets the base to 2, and only its "Adicionalmente … +1 Rodada para cada Título" rider has an
 * effect. A source inconsistency, resolved toward the more specific race text.
 */
final class PoderVampiricoActiveAbility implements ActiveAbility {

    static final int HIT_POINT_COST = 3;
    static final int BASE_DURATION_IN_ROUNDS = 2;

    private static final int OSTEOMANCIA_BASE_DEFESAS = 2;
    private static final int CELERIDADE_ACTION_POINTS = 1;
    private static final int ORLOK_ROLL_VANTAGEM = Skill.ADVANTAGE_BONUS;
    private static final int ORLOK_LIFE_STEAL = 1;
    private static final int MIRCALLA_BASE_ATTRIBUTE_BONUS = 1;

    private final VampiricoFeat poder;

    PoderVampiricoActiveAbility(final VampiricoFeat poder) {
        this.poder = poder;
    }

    @Override
    public String getDescription() {
        return poder.getDescription();
    }

    /** Ação Livre — no Pontos de Ação. */
    @Override
    public int getActionPointCost() {
        return 0;
    }

    @Override
    public int getMagicPointCost() {
        return 0;
    }

    @Override
    public int getHitPointCost() {
        return HIT_POINT_COST;
    }

    @Override
    public int getDurationInRounds() {
        return BASE_DURATION_IN_ROUNDS;
    }

    private int durationFor(final Character character) {
        boolean hasDuradouro = character.getFeats().stream()
                .anyMatch(feat -> feat.catalogEntry() == VampiricoFeat.PODER_VAMPIRICO_DURADOURO);
        return BASE_DURATION_IN_ROUNDS + (hasDuradouro ? character.getAllTitles().size() : 0);
    }

    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return resolveEffects(character).get(0);
    }

    @Override
    public List<TemporaryEffect> resolveEffects(final Character character) {
        int duration = durationFor(character);
        int titles = character.getAllTitles().size();
        return switch (poder) {
            case OSTEOMANCIA -> List.of(
                    new TemporaryBonus(ModifierType.DEFESAS, OSTEOMANCIA_BASE_DEFESAS + titles, duration));
            case CELERIDADE_VAMPIRICA -> List.of(
                    new TemporaryBonus(ModifierType.ACTION_POINTS, CELERIDADE_ACTION_POINTS, duration),
                    new TemporaryBonus(ModifierType.MOVEMENT, titles, duration));
            case ARMAMENTO_DE_ORLOK -> List.of(
                    new TemporaryBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, ORLOK_ROLL_VANTAGEM, duration),
                    new TemporaryBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, ORLOK_ROLL_VANTAGEM, duration),
                    new TemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS, ORLOK_ROLL_VANTAGEM, duration),
                    new LifeSteal(ORLOK_LIFE_STEAL, Optional.of(duration)));
            case DOM_DE_MIRCALLA -> List.of(
                    new TemporaryBonus(ModifierType.CHARISMA_BONUS, MIRCALLA_BASE_ATTRIBUTE_BONUS + titles, duration),
                    new TemporaryBonus(ModifierType.INSTINCT_BONUS, MIRCALLA_BASE_ATTRIBUTE_BONUS + titles, duration));
            default -> throw new IllegalStateException(poder + " is not a Poder Vampírico with an activation effect");
        };
    }
}
