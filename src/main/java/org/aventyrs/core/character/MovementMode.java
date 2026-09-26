package org.aventyrs.core.character;

/**
 * The ways a character can move, each with its own Movimento Base per Ponto de Ação: on land (the
 * ordinary figure, {@code MovementService#getMovementBase}), in flight ("Movimento Base de Voo"),
 * swimming ("Movimento Base de Natação") and on walls ("Movimento Base Vertical", also "de
 * escalada").
 *
 * <p>Every character has {@link #LAND}. The other three are <b>possessed or not</b>: granted by a
 * Raça ({@code Race#grantsMovementMode}), a Talento ({@code Feat#grantsMovementMode}), a
 * Habilidade de Competência ({@code SkillCompetencyAbility#grantsMovementMode}) or a worn Forma.
 * Whether a character is <i>currently</i> flying or swimming is not this — that is the caller's
 * fact about the Scene ({@code EnvironmentalState#flying}/{@code atLeastHalfSubmerged}).
 */
public enum MovementMode {
    LAND,
    FLIGHT,
    SWIM,
    CLIMB
}
