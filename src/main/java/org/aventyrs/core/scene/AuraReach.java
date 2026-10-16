package org.aventyrs.core.scene;

/** Whom a {@link MonsterAura} reaches inside its radius (core 0.1.7.4). The holder itself is never "another". */
public enum AuraReach {
    /** "Inimigos … em Distância Curta" — the holder's enemies. */
    ENEMIES,
    /** "Inimigos vivos" / "Personagens vivos" — enemies without the Anatomia de Morto-Vivo. */
    LIVING_ENEMIES,
    /** "Todos os outros personagens no Alcance" / "Outros personagens na área" — allies, enemies and neutrals. */
    ALL_OTHERS,
    /** "Personagens vivos em sua Área de Efeito" — everyone else without the Anatomia de Morto-Vivo. */
    LIVING_OTHERS,
    /** "O Monstro e seus aliados" — read as its monster allies (table ruling 2026-10-15, Dádiva de Gaea). */
    SELF_AND_MONSTER_ALLIES
}
