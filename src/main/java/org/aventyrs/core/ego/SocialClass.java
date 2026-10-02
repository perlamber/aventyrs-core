package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;

import java.util.Optional;

/**
 * One row of the Recursos tables (2.5 Ego › Recursos — "Classes Sociais e Pontos de Equipamentos" and
 * "Utilidades e Serviços"; see {@code docs/rules/ego.txt}), one constant per Recursos value 0–5.
 *
 * <p><b>Which value picks the row.</b> The book's "valor total neste Ego" is the Recursos a character still
 * has <em>permanently</em>: "Recursos 5 (3 disponíveis)" is 5 permanent points with 3 temporary left, and after
 * spending a permanent point it is "Recursos total 4". So {@link #current(CombatantSheet)} reads {@code
 * getPermanentEgoPoints(RECURSOS)} — never the temporary points left, and never above 5, since no Ego is (see
 * {@code Character#getEffectiveEgoTotal}). A spent permanent point therefore lowers every later point's
 * worth: the book's "a percepção de valor diminuiu com o poder aquisitivo".
 *
 * <p>{@link #getUtilityCount()}, {@link #getMaxUtilityRarity()} and {@link #getServicesPerDay()} are
 * <b>reference data only</b> — table ruling (2026-09-29): Utilidades e Serviços are the table's to judge,
 * never enforced.
 */
@Getter
@AllArgsConstructor
public enum SocialClass {
    FALENCIA(0, "Falência", 0, 3, 0, null, 1, ItemRarity.COMMON, 0),
    POBREZA(1, "Pobreza", 15, 7, 22, ItemRarity.COMMON, 3, ItemRarity.COMMON, 2),
    CLASSE_MEDIA(2, "Classe Média", 21, 10, 31, ItemRarity.COMMON, 6, ItemRarity.UNCOMMON, 3),
    BAIXA_NOBREZA(3, "Baixa Nobreza", 28, 14, 42, ItemRarity.UNCOMMON, 10, ItemRarity.RARE, 5),
    NOBREZA(4, "Nobreza", 36, 18, 54, ItemRarity.UNCOMMON, 15, ItemRarity.EPIC, 8),
    ALTA_NOBREZA(5, "Alta Nobreza", 45, 22, 67, ItemRarity.RARE, 21, ItemRarity.MYTHIC, 11);

    /** The Recursos value this row is for. */
    private final int resources;
    private final String displayName;
    /** "Pts. Eqp. Iniciais" — the PE a character starts with. */
    private final int startingEquipmentPoints;
    /** "Valor Pts Temp" — PE one temporary Recursos point buys. */
    private final int temporaryPointValue;
    /** "Valor Pts Perm" — PE one permanent Recursos point buys. */
    private final int permanentPointValue;
    /** "Raridade Inicial" — the rarest Equipamento buyable at creation; {@code null} is "Nenhum". */
    private final ItemRarity startingRarityOrNull;
    /** "Qtd Utilidades" — reference only. */
    private final int utilityCount;
    /** "Raridade Max" of Utilidades — reference only. */
    private final ItemRarity maxUtilityRarity;
    /** "Serviços/Dia" — reference only. */
    private final int servicesPerDay;

    /** The row for a Recursos value; clamped to 0–5, since no Ego is above 5 and none below 0. */
    public static SocialClass of(final int resources) {
        return values()[Math.max(0, Math.min(Character.MAX_EGO, resources))];
    }

    /** The row a live sheet's Recursos currently sits in — its permanent Recursos points left. */
    public static SocialClass current(final CombatantSheet sheet) {
        return of(sheet.getPermanentEgoPoints(EgoDomain.RECURSOS));
    }

    /** The rarest Equipamento buyable at creation — empty at Falência ("Nenhum"). */
    public Optional<ItemRarity> getStartingRarity() {
        return Optional.ofNullable(startingRarityOrNull);
    }

    /** PE one point of type buys at this row. */
    public int getPointValue(final EgoPointType type) {
        return type == EgoPointType.PERMANENT ? permanentPointValue : temporaryPointValue;
    }

    /** "Recursos a Zero": extrema pobreza. */
    public boolean isExtremePoverty() {
        return this == FALENCIA;
    }
}
