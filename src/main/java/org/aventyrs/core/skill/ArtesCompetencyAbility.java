package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Artes.
 */
@Getter
@AllArgsConstructor
public enum ArtesCompetencyAbility implements SkillCompetencyAbility {

    // TODO: activated ability (Artes roll vs GD Médio) granting nearby allies (Distância
    // Média), but not the user, +1 on Perícia rolls for 1 Round, scaling +1 per 5 points
    // over GD Médio — no roll-resolution-vs-DifficultyLevel engine or ally-range system
    // exists yet.
    DOM_BARDICO("Você pode utilizar sua arte para motivar seus aliados, GD Médio. Escolha " +
            "uma Perícia; em caso de sucesso, seus aliados em Distância Média, mas não você, " +
            "recebem +1 em rolagens de Perícias por 1 Rodada, bônus que aumenta em +1 para " +
            "cada 5 pontos acima da GD Médio obtida na rolagem."),

    // TODO: lets Artes substitute for Conhecimentos in the Cosmologia/Geo-história
    // specializations, with shallower results — no Conhecimentos Perícia or
    // specialization-substitution system exists yet.
    DOMINIO_CULTURAL("É possível utilizar rolagens de Artes ao invés de Conhecimentos " +
            "(somente nas especializações Cosmologia e Geo-história), mas informações " +
            "obtidas desta forma são mais superficiais."),

    // TODO: lets you perform Artes at Desvantagem without proper instruments, using
    // whatever's on hand — no Vantagem/Desvantagem roll system exists yet.
    IMPROVISO_ARTISTICO("Efetuando rolagens em Desvantagem, você é capaz de desenvolver sua " +
            "arte sem auxílio de instrumentos especiais, usando em substituição qualquer " +
            "coisa minimamente compatível que esteja no ambiente."),

    // TODO: grants Vantagem on Perícia rolls with people at the location after a successful
    // (GD Difícil) Artes roll in a peaceful/neutral social scene — no
    // roll-resolution-vs-DifficultyLevel engine or Cena/social-disposition system exists yet.
    ANIMADOR_DE_TAVERNAS("Enquanto estiver em um ambiente social pacífico ou neutro, após " +
            "uma rolagem de Artes bem-sucedida (GD Difícil), você recebe Vantagem em " +
            "rolagens de Perícias efetuadas com as pessoas do local."),

    // TODO: a successful Artes roll performing for a crowd turns listeners more favorable
    // or neutral — no roll-resolution engine or NPC-disposition system exists yet.
    ESPALHAR_REPUTACAO("Você pode fazer uma rolagem de Artes enquanto se apresenta para uma " +
            "multidão contando histórias suas ou de seu grupo; se for bem-sucedido, os " +
            "ouvintes se tornam mais favoráveis ou neutros.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ARTES;
    }
}
