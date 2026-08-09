package org.aventyrs.core.character;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The deity (if any) a character worships or is devoted to. {@link #NENHUMA} covers both a
 * character with no patron and one who follows the Adépto do Equilíbrio path instead of any
 * single god — the rules text lists them as one and the same option, not two.
 */
@Getter
@AllArgsConstructor
public enum Deity {
    NENHUMA(DeityCategory.NENHUMA, "Adépto do Equilíbrio ou Nenhuma"),

    A_ESQUECIDA(DeityCategory.DEUS_PRIMORDIAL, "A Esquecida (Umbra)"),
    DEUSES_DO_VAZIO(DeityCategory.DEUS_PRIMORDIAL, "Deuses do Vazio (Assimilação/Expansão)"),
    DRAGAO_DO_LESTE(DeityCategory.DEUS_PRIMORDIAL, "Dragão do Leste (Drak'arya/Dragões)"),
    ESCURIDAO_PROFUNDA(DeityCategory.DEUS_PRIMORDIAL, "Escuridão Profunda (Transformação/Morte)"),
    LUZ_PRIMORDIAL(DeityCategory.DEUS_PRIMORDIAL, "Luz Primordial (Criação/Vida)"),

    EPONA(DeityCategory.DEUS_ELEMENTAL, "Epona (Terra/Proteção)"),
    GAEA(DeityCategory.DEUS_ELEMENTAL, "Gaea (Natureza/Colheita)"),
    GAEA_E_FLORA(DeityCategory.DEUS_ELEMENTAL, "Gaea e Flora (Natureza/Fadas/Fauna e Flora)"),
    GAEA_E_LACERTO(DeityCategory.DEUS_ELEMENTAL, "Gaea e Lacerto (Natureza/Caça/Monstros)"),
    SURT_ELDUR(DeityCategory.DEUS_ELEMENTAL, "Surt'Eldur (Fogo/Justiça)"),
    SURT_ELDUR_E_BOROS(DeityCategory.DEUS_ELEMENTAL, "Surt'Eldur e Boros (Fogo/Misericórdia)"),
    SYLPH(DeityCategory.DEUS_ELEMENTAL, "Sylph (Vento/Trapaça)"),
    TESLA(DeityCategory.DEUS_ELEMENTAL, "Tesla (Eletricidade/Conhecimentos)"),
    UNDINE_E_HALOI(DeityCategory.DEUS_ELEMENTAL, "Undine e Haloi (Água/Adaptação)"),
    UNDINE(DeityCategory.DEUS_ELEMENTAL, "Undine (Água/Cuidado)"),
    HALOI(DeityCategory.DEUS_ELEMENTAL, "Haloi (Água/Vingança)"),
    VULCANO(DeityCategory.DEUS_ELEMENTAL, "Vulcano (Magma/Forja)"),
    YMIR(DeityCategory.DEUS_ELEMENTAL, "Ymir (Gelo/Força)"),

    MORPHEUS(DeityCategory.DEUS_GUIA, "Morpheus (Sonhos/Ambição)"),
    TYKHE(DeityCategory.DEUS_GUIA, "Tykhé (Sorte/Destino)"),

    CHAMAS_NEGRAS(DeityCategory.SENHOR_UMBRAL, "Chamas Negras (Inclemência)"),
    DRAGAO_NEGRO(DeityCategory.SENHOR_UMBRAL, "Dragão Negro (Poder)"),
    O_DECAPTADO(DeityCategory.SENHOR_UMBRAL, "Decaptado, O (Vendeta)"),
    MUITOS_OLHOS(DeityCategory.SENHOR_UMBRAL, "Muitos-Olhos (Segredos)"),
    PASSARO_AFOGADO(DeityCategory.SENHOR_UMBRAL, "Pássaro Afogado (Intriga)"),
    SERPENTE_DEVORA_MUNDOS(DeityCategory.SENHOR_UMBRAL, "Serpente-Devora-Mundos (Destruição)"),
    TECE_MORTES(DeityCategory.SENHOR_UMBRAL, "Tece-Mortes (Calamidade)");

    private final DeityCategory category;
    private final String description;
}
