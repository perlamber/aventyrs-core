package org.aventyrs.core.subordinate;

/**
 * The six kinds of Subordinado ({@code docs/rules/subordinados.txt}, core 0.0.92) — "Bispo, Cavaleiro, Peão, Rainha,
 * Rei e Torre". Each grants its commander one of two benefits, picked when it is gained ({@link SubordinateBenefit}).
 */
public enum SubordinateGrade {
    /** "Bispos curam os Personagens Jogadores". */
    BISPO,
    /** "Cavaleiros auxiliam os personagens em ações ofensivas de combate". */
    CAVALEIRO,
    /** "Peões auxiliam os personagens em suas ações de diversas formas". */
    PEAO,
    /** "Rainhas … motivam com seu carisma" — Pontos de Ação or Iniciativa. */
    RAINHA,
    /** "Reis … motivam com seu carisma" — temporary Ego. */
    REI,
    /** "Torres são os protetores". */
    TORRE
}
