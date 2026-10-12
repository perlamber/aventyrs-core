package org.aventyrs.core.item;

import lombok.Getter;

/**
 * An Aprimoramento fitted to a Pedra do Poder's Obra-Prima — "concedendo a ela um único Aprimoramento de Obra-Prima",
 * so a stone holds at most one, and only once it is an Obra-Prima. From {@code docs/rules/pedras-do-poder.txt} (V19),
 * "Aprimoramentos de Obras-Primas", each with its own Preço in PE. Read by {@code PowerStoneService}, and — the three
 * charge-spending ones — by the payment paths named on each constant.
 */
@Getter
public enum PowerStoneImprovement {

    /** Anima Selvagem [+20PE] — Cargas as a reserve of PD for Habilidades de Títulos Aventyrs. */
    ANIMA_SELVAGEM("Anima Selvagem", 20),

    /** Arcanum [+20PE] — Cargas as a reserve of PM to Conjurar or Mimetizar Magias. */
    ARCANUM("Arcanum", 20),

    /** Conexão Veloz [+10PE] — Tempo de Vinculação reduzido para Ação Livre. */
    CONEXAO_VELOZ("Conexão Veloz", 10),

    /** Dissipador de Calor [+15PE] — once per Cena, on the first use, Resfriamento -1 Rodada. */
    DISSIPADOR_DE_CALOR("Dissipador de Calor", 15),

    /** Mecanismo de Sobrecarga [+15PE] — may spend 3 Cargas instead of 1; Duração and Resfriamento are doubled. */
    MECANISMO_DE_SOBRECARGA("Mecanismo de Sobrecarga", 15),

    /** Última Carga [+5PE] — at zero Cargas, one last use for 2 Rodadas, with the Danos to Equipamentos doubled. */
    ULTIMA_CARGA("Última Carga", 5),

    /** Vitalitas Immanis [+20PE] — spend Cargas to heal twice their number in PV. */
    VITALITAS_IMMANIS("Vitalitas Immanis", 20);

    private final String name;
    /** This Aprimoramento's Preço in PE, as V19 prints it beside its name. */
    private final int priceModifier;

    PowerStoneImprovement(final String name, final int priceModifier) {
        this.name = name;
        this.priceModifier = priceModifier;
    }
}
