package org.aventyrs.core.skill.dirigirecavalgar;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Dirigir e Cavalgar — it
 * defines which mounts/vehicles that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum DirigirECavalgarSpecialization {
    AQUATICOS("Você recebeu treinamento para guiar animais marinhos e embarcações."),
    TERRESTRES("Você sabe cavalgar animais terrestres e guiar veículos tracionados por " +
            "estes animais."),
    VOADORES("Você recebeu treinamento de cavalaria aérea e sabe como montar Dragonetes, " +
            "Grifos, Pégasus, pássaros atrozes e outros animais alados."),
    MONTARIAS_MONSTRUOSAS("Você sabe como montar Dragões, criaturas Sauróides e " +
            "primitivas, monstros ou monstruosas, sejam eles aladas, terrestres ou " +
            "marinhas."),
    VEICULOS_TECNOLOGICOS("Você foi treinado para dirigir ou pilotar veículos " +
            "tecnológicos, como carros, monociclos, dirigíveis, bancos à vapor, dentre " +
            "outros.");

    private final String description;
}
