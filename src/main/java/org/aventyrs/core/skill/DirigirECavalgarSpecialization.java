package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Dirigir e Cavalgar — it
 * defines which mounts/vehicles that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum DirigirECavalgarSpecialization {
    AQUATICOS("Animais e Veículos (não-tecnológicos) Aquáticos."),
    TERRESTRES("Animais e Veículos (não-tecnológicos) Terrestres."),
    VOADORES("Animais e Veículos (não-tecnológicos) Voadores."),
    MONTARIAS_MONSTRUOSAS("Montarias Monstruosas."),
    VEICULOS_TECNOLOGICOS("Veículos Tecnológicos.");

    private final String description;
}
