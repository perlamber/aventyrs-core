package org.aventyrs.core.skill.profissao;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The specialization a player must choose when a character trains Profissão — it defines
 * which trade that training actually covers.
 */
@Getter
@AllArgsConstructor
public enum ProfissaoSpecialization {
    MECANICA("Habilidade de construir e efetuar a manutenção de peças e artefatos " +
            "mecânicos ou tecnológicos."),
    ALFAIATARIA_E_CURTUME("Técnicas de confecção e costura de itens e equipamentos de " +
            "pano e couro, como roupas, armas e armaduras."),
    METALURGIA("Técnicas de forja de itens e equipamentos de metal, como utensílios " +
            "domésticos, ferramentas de lavouras, armas, armaduras."),
    JOALHERIA("Lapidação e criação de joias."),
    ALVENARIA_E_CARPINTARIA("Domínio da prática de realizar construções feitas de pedra ou " +
            "madeira, como casas, pontes, e cavernas não naturais.");

    private final String description;
}
