package org.aventyrs.core.feat;

import org.aventyrs.core.item.ShieldItem;

/**
 * What {@link EscudeiroFeat#ESPECIALISTA_EM_ESCUDO} records as "um item escolhido do tipo ‘Escudo’":
 * one constant per {@link ShieldItem}, sharing its name so a choice recorded as the item round-trips
 * unchanged, plus {@link #ASAS_ADAMANTINAS} — the wings {@link EscudeiroFeat#ASAS_ADAMANTINAS} makes
 * "itens do tipo Escudo", which the table rules may be chosen too (2026-09-27). Offered only to a
 * holder of Asas Adamantinas ({@code EscudeiroFeat#ESPECIALISTA_EM_ESCUDO#resolveRequiredChoices}).
 */
public enum ShieldSpecialty {
    BRACADEIRAS(ShieldItem.BRACADEIRAS),
    BRACELETE_ARCANO(ShieldItem.BRACELETE_ARCANO),
    BROQUEL(ShieldItem.BROQUEL),
    ESCUDO_MEDIO(ShieldItem.ESCUDO_MEDIO),
    ESCUDO_DE_CORPO(ShieldItem.ESCUDO_DE_CORPO),
    REPULSOR(ShieldItem.REPULSOR),
    ASAS_ADAMANTINAS(null);

    private final ShieldItem shield;

    ShieldSpecialty(final ShieldItem shield) {
        this.shield = shield;
    }

    /** The catalog Escudo this names, or {@code null} for the wings. */
    public ShieldItem getShield() {
        return shield;
    }

    public boolean isWings() {
        return shield == null;
    }

    /** The specialty naming shield. */
    public static ShieldSpecialty of(final ShieldItem shield) {
        for (ShieldSpecialty specialty : values()) {
            if (specialty.shield == shield) {
                return specialty;
            }
        }
        throw new IllegalArgumentException("No specialty for " + shield);
    }
}
