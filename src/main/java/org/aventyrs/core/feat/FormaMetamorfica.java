package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
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
 * <p><b>The Arma Natural column is live, and it replaces.</b> While its holder wears the shape,
 * that row's weapon is the <em>only</em> Arma Natural they have — a Nosferatu in {@link
 * #SERPENTE_ESPINHOSA} fights with the Cauda Constritora and not the Presas Longas their lineage
 * grants; in {@link #NEVOA}, whose column reads "Nenhum", they have none at all. Surfaced by
 * {@code CombatantSheet#getNaturalWeapons()} through {@code Feat#getGrantedNaturalWeapons(Character,
 * CombatantSheet)} and {@code Feat#replacesNaturalWeaponsWhileInForm}.
 *
 * <p><b>That replacement is a deliberate reading, not a transcription</b>, and the source argues
 * the other way on two counts, both recorded here so a later reader sees a decision rather than a
 * slip:
 * <ul>
 *   <li>the subject of "armas não podem ser utilizadas, <i>são substituídas</i> por armas naturais"
 *   is <b>armas</b> — the equipment weapons the preceding clause just forbade. The sentence says
 *   what natural weapons replace, and is silent on what replaces <i>them</i>; and</li>
 *   <li>"abandonando seus traços raciais", the phrase that authorises stripping racial traits,
 *   appears twice in the corpus ({@code DraconicoFeat#DRACONATO}, {@code
 *   FeericoFeat#ANCIENTEFORME}) and <b>not</b> in Metamorfose Dracúlea.</li>
 * </ul>
 * It is read as replacement anyway because the shapes are whole animals and a snake has no fangs.
 * This is emphatically <b>not</b> the racial-trait-suppression gap being closed: it cancels Armas
 * Naturais only, only while the shape is worn, and only for this Talento.
 *
 * <p><b>The HABILIDADE column is three-sixths live.</b> {@link #ARANHA_GIGANTE}'s Furtividade
 * Vantagem, {@link #LOBO_DENTES_DE_SABRE}'s Perícias de Ataque Vantagem and {@link
 * #MORCEGO_ATROZ}'s "Roubo de Vida +2" are granted for real, Forma-gated. The rest are authored
 * text granted by nothing, each blocked on its own missing system — see the per-constant javadoc
 * rather than assuming one shared blocker.
 */
@Getter
public enum FormaMetamorfica {

    /**
     * The Furtividade half is granted for real, Forma-gated.
     *
     * <p>TODO: "Movimento Base Vertical" needs a movement sub-stat this core does not have —
     *  {@code MovementService} resolves one undifferentiated Movimento Base, with no vertical or
     *  flight channel to raise.
     */
    ARANHA_GIGANTE(FormType.ARANHA_GIGANTE, NaturalWeapon.PRESAS_LONGAS,
            "Movimento Base Vertical e Vantagem em Furtividade."),

    /**
     * Neither half is granted, and they are blocked on different things.
     *
     * <p>TODO: "Ignora Terreno Difícil" — {@code TerrainType} describes a whole Scene, not a
     *  per-movement cost there is anything to ignore.
     * <p>TODO: "Multiplicador de PV +1" — {@code HitPointsService#getLifeMultiplier} takes a
     *  {@code Character} and cannot see a Forma, which lives on the sheet. The arithmetic is
     *  ordinary (a permanent per-Título multiplier is real — {@code OrquicoFeat#TERRA_NAS_VEIAS});
     *  the reach is what is missing.
     */
    CAVALO_DE_CHIFRES(FormType.CAVALO_DE_CHIFRES, NaturalWeapon.CHIFRES_PODEROSOS,
            "Ignora Terreno Difícil e Multiplicador de PV +1."),

    /**
     * Both halves are real. The Vantagem is granted Forma-gated; "pode empunhar armas de uma mão
     * com as presas" is a per-shape exception to the weapons suppression, carried by {@code
     * FormType#permitsOneHandedWeapons()} and honoured by {@code CombatantSheet#canAttackWith} —
     * the only row in the table that claws back part of what {@code
     * FormEquipmentPolicy#WEAPONS_SUPPRESSED} takes away.
     */
    LOBO_DENTES_DE_SABRE(FormType.LOBO_DENTES_DE_SABRE, NaturalWeapon.PRESAS_LONGAS,
            "Vantagem em Perícias de Ataque; pode empunhar armas de uma mão com as presas."),

    /**
     * The Roubo de Vida half is granted for real, Forma-gated — and like every Roubo de Vida
     * figure in this core it only amplifies an already-active {@code LifeSteal}, never grants one
     * from nothing.
     *
     * <p>TODO: "Movimento Base de Voo" needs the flight sub-stat {@code Aviano}/{@code
     * DraconicoFeat#ASAS_DE_DRAGAO}/{@code FeericoFeat} all cite — the same missing channel as
     * {@link #ARANHA_GIGANTE}'s Vertical.
     */
    MORCEGO_ATROZ(FormType.MORCEGO_ATROZ, NaturalWeapon.PRESAS_LONGAS,
            "Movimento Base de Voo e Roubo de Vida aumentado em +2."),

    /**
     * The one row with no Arma Natural — "Nenhum" — and the one a Rakshasa may not take. Under the
     * replacement reading that empties its holder's Armas Naturais outright, so {@code
     * CombatantSheet#canAttackWith} refuses every one of them: half of "é incapaz de causar danos"
     * falls out of the weapon path with nothing added for it.
     *
     * <p>TODO: only half. Nothing stops a Névoa holder casting a damaging Magia, and an Ataque
     *  Desarmado is still permitted ({@code canAttackWith(null)} is unconditionally true — a
     *  punch is not a weapon to suppress). A blanket "deals no damage" prohibition exists nowhere
     *  in this core.
     * <p>TODO: "Imune a dano físico (exceto fogo e armas de Dyospiros)" needs damage-type
     *  immunity, which does not exist at any strength — {@code DamageService} has RD and RM but no
     *  nullifying stage, and no per-{@code ElementalType} carve-out to hang the fogo exception on.
     */
    NEVOA(FormType.NEVOA, null,
            "Imune a dano físico (exceto fogo e armas de Dyospiros), mas é incapaz de causar danos."),

    /**
     * TODO: "Corrente de Efeitos – Veneno Vampírico" is blocked twice over. {@code EffectChain} is
     *  a bare marker interface with one concrete implementation ({@code Sangramento}), and the
     *  Malefício it would inflict is inert — {@code ConditionType#ENVENENADO} has no effect
     *  because "Multiplicador de Bônus Base" exists nowhere in this core.
     */
    SERPENTE_ESPINHOSA(FormType.SERPENTE_ESPINHOSA, NaturalWeapon.CAUDA_CONSTRITORA,
            "Corrente de Efeitos – Veneno Vampírico.");

    /**
     * The activatable shape this row <em>is</em> — one stable instance per constant, built here so
     * that the ability a client is offered through {@code Feat#resolveRequiredChoices} is the
     * very object the acquired Talento ends up holding. {@code ActiveAbilityService#activate}
     * matches a held ability by {@code ==}, so anything less than a singleton would make an
     * offered choice unactivatable.
     */
    private final ActiveAbility transformation = new MetamorfoseActiveAbility(this);

    private final FormType form;

    /** The Arma Natural this shape fights with, or {@code null} for Névoa, which fights with none. */
    private final NaturalWeapon naturalWeapon;

    /** The row's HABILIDADE column, verbatim — authored data, granted by nothing yet. */
    private final String abilityDescription;

    /**
     * The row for form, or {@code null} for a {@link FormType} this table does not name — every
     * shape outside Metamorfose Dracúlea ({@code ANIMAL}, {@code DRACONATO}, the Górgona shapes),
     * and {@code null} itself, which is "not transformed".
     *
     * <p>The reverse of the {@link #getForm()} column, walked rather than cached: six constants,
     * and a {@code Map} would have to be built in a static initialiser that runs before the
     * instances it indexes exist.
     */
    static FormaMetamorfica of(final FormType form) {
        if (form == null) {
            return null;
        }
        for (FormaMetamorfica forma : values()) {
            if (forma.form == form) {
                return forma;
            }
        }
        return null;
    }

    /** Every Forma a holder may legally choose — the whole table, minus Névoa for a Rakshasa. */
    static java.util.Set<FormaMetamorfica> availableTo(final org.aventyrs.core.character.Character holder) {
        java.util.Set<FormaMetamorfica> available = java.util.EnumSet.allOf(FormaMetamorfica.class);
        if (holder.getRace() instanceof org.aventyrs.core.race.Vampiro vampiro
                && vampiro.getLineage() == org.aventyrs.core.race.Vampiro.VampiroLineage.RAKSHASA) {
            available.remove(NEVOA);
        }
        return available;
    }

    FormaMetamorfica(final FormType form, final NaturalWeapon naturalWeapon, final String abilityDescription) {
        this.form = form;
        this.naturalWeapon = naturalWeapon;
        this.abilityDescription = abilityDescription;
    }
}
