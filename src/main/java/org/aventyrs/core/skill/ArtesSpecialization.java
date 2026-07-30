package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Artes — it defines which
 * artistic actions that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum ArtesSpecialization {
    ATUACAO("Interpretação de papéis, se passar por outras pessoas e atos diversos."),
    MUSICA("Técnicas de canto e vocalização rítmica, estilos de dança, interpretações " +
            "corporais rítmicas e uso de instrumentos musicais."),
    ESCULTURA("Habilidade de esculpir formas em materiais como pedra, argila, madeira etc."),
    PINTURA("Conhecimento de técnicas de desenho e pintura de criaturas, cenários, objetos " +
            "e ideias."),
    LITERATURA("Criação e conhecimentos sobre letras, histórias, poemas e outras formas de " +
            "registro escrito de ideias e do imaginário.");

    private final String description;
}
