package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.sheet.FormType;

/**
 * The six shapes {@code VampiricoFeat#METAMORFOSE_DRACULEA} offers — the {@code FORMA
 * METAMÓRFICA | ARMA NATURAL | HABILIDADE} table verbatim. A player picks two at acquisition
 * (one for a Dampiro, four for a Rakshasa, who cannot take Névoa), recorded on {@link
 * MetamorfoseDraculeaFeat}.
 *
 * <p><b>Feat-scoped on purpose.</b> These belong to this one Talento, so the catalog lives beside
 * it rather than in {@code org.aventyrs.core.sheet}. What <em>is</em> shared is the shape itself:
 * each constant names a {@link FormType}, which is where the sheet records the transformation and
 * where the equipment policy lives. Another Talento wanting activatable Formas reuses {@code
 * Feat#resolveActiveAbilities} and {@code FormType}, not this enum.
 *
 * <p><b>Only the Arma Natural column is mechanically live.</b> Each row's HABILIDADE is authored
 * here as text and granted by nothing — every one of them needs a system this core lacks (a
 * Movimento Base sub-stat for Vertical/Voo, a per-Rodada Multiplicador de PV, a Corrente de
 * Efeitos, damage-type immunity). Recording them keeps the table honest and gives each a place to
 * land; see {@link MetamorfoseActiveAbility} for what is actually applied.
 */
@Getter
public enum FormaMetamorfica {

    ARANHA_GIGANTE(FormType.ARANHA_GIGANTE, NaturalWeapon.PRESAS_LONGAS,
            "Movimento Base Vertical e Vantagem em Furtividade."),

    CAVALO_DE_CHIFRES(FormType.CAVALO_DE_CHIFRES, NaturalWeapon.CHIFRES_PODEROSOS,
            "Ignora Terreno Difícil e Multiplicador de PV +1."),

    LOBO_DENTES_DE_SABRE(FormType.LOBO_DENTES_DE_SABRE, NaturalWeapon.PRESAS_LONGAS,
            "Vantagem em Perícias de Ataque; pode empunhar armas de uma mão com as presas."),

    MORCEGO_ATROZ(FormType.MORCEGO_ATROZ, NaturalWeapon.PRESAS_LONGAS,
            "Movimento Base de Voo e Roubo de Vida aumentado em +2."),

    /**
     * The one row with no Arma Natural — "Nenhum" — and the one a Rakshasa may not take. Its
     * Habilidade is a damage-type immunity paid for by being unable to deal damage at all.
     */
    NEVOA(FormType.NEVOA, null,
            "Imune a dano físico (exceto fogo e armas de Dyospiros), mas é incapaz de causar danos."),

    SERPENTE_ESPINHOSA(FormType.SERPENTE_ESPINHOSA, NaturalWeapon.CAUDA_CONSTRITORA,
            "Corrente de Efeitos – Veneno Vampírico.");

    private final FormType form;

    /** The Arma Natural this shape fights with, or {@code null} for Névoa, which fights with none. */
    private final NaturalWeapon naturalWeapon;

    /** The row's HABILIDADE column, verbatim — authored data, granted by nothing yet. */
    private final String abilityDescription;

    FormaMetamorfica(final FormType form, final NaturalWeapon naturalWeapon, final String abilityDescription) {
        this.form = form;
        this.naturalWeapon = naturalWeapon;
        this.abilityDescription = abilityDescription;
    }
}
