package org.aventyrs.core.sheet;

/**
 * The alternate physical shapes a combatant can take — a Forma, in the rules' own word. Authored
 * from the Talento and Raça clauses that name one; see {@code docs/rules/talentos.txt}.
 *
 * <p><b>There is no constant for "their own shape".</b> A character not transformed has {@code
 * CombatantSheet#getCurrentForm() == null}, the same "not applicable" reading a {@code null}
 * carries everywhere else in this core. That is why {@code HomemFera}'s Forma Híbrida, whose rules
 * text lists <i>Humanoide</i> alongside the three alternates as something to "switch between",
 * gets no {@code HUMANOIDE} constant: switching back to Humanoide is leaving the Forma, and a
 * state with two ways to spell "normal" would be a bug waiting to happen.
 *
 * <p><b>Named by shape, not by source.</b> {@link #MONSTRUOSA} is one constant even though three
 * unrelated traits reach it ({@code Gorgona}'s curse, {@code HomemFera}'s GranAventyr form,
 * {@code MonstruosoFeat#APARENCIA_MONSTRUOSA}), because a clause reading "enquanto em sua Forma
 * Monstruosa" cares about the shape and not about how its holder got there. Where a clause needs
 * to know <em>which</em> trait put them in it, that is the trait's own business — it knows whether
 * it is held.
 *
 * <p><b>A Forma is not a timed bonus.</b> {@code ElementalFeat#TRANSFORMACAO_ELEMENTAL} deliberately
 * has no constant here: its rules text makes the transformation <i>permanent</i>, not a Forma with
 * a Duração and a Custo, so it is an unconditional grant on the Talento rather than a state to
 * enter and leave.
 */
public enum FormType {

    /**
     * Forma Animal — a whole animal, not a blend. {@code BestialFeat#METAMORFOSE_SELVAGEM}'s
     * "transformar em um animal", {@code HomemFera}'s Animal rung.
     */
    ANIMAL(FormEquipmentPolicy.ALL_SUPPRESSED),

    /** Forma Híbrida — {@code HomemFera}'s Aventyr-only blend of Humanoide and Animal. */
    HIBRIDA,

    /**
     * Forma Monstruosa — {@code Gorgona}'s cursed shape, {@code HomemFera}'s GranAventyr rung,
     * {@code MonstruosoFeat#APARENCIA_MONSTRUOSA}'s.
     */
    MONSTRUOSA,

    /** Forma Feérica — the other half of {@code Gorgona}'s Monstros em pele de Fada toggle. */
    FEERICA,

    /** Forma Humana — an ordinary human shape a non-human puts on ({@code MonstruosoFeat#MIMETIZAR_FORMA_HUMANA}). */
    HUMANA,

    /** Draconato — {@code DraconicoFeat#DRACONATO}'s bipedal dragon. */
    DRACONATO,

    /** Anciente — {@code FeericoFeat#ANCIENTEFORME}'s living tree. */
    ANCIENTE,

    /** Névoa — one of {@code VampiricoFeat#METAMORFOSE_DRACULEA}'s Formas Metamórficas. */
    NEVOA(FormEquipmentPolicy.WEAPONS_SUPPRESSED),

    // The remaining five Formas Metamórficas. Each is its own shape rather than being folded into
    // ANIMAL, so a clause can ask "enquanto Morcego Atroz" — which the table's per-row Habilidades
    // need. Their Armas Naturais and Habilidades are authored on {@code FormaMetamorfica}, not
    // here: those belong to the Talento that grants them, while the shape belongs to the sheet.

    /** Aranha Gigante — Movimento Vertical e Vantagem em Furtividade. */
    ARANHA_GIGANTE(FormEquipmentPolicy.WEAPONS_SUPPRESSED),

    /** Cavalo de Chifres — ignora Terreno Difícil, Multiplicador de PV +1. */
    CAVALO_DE_CHIFRES(FormEquipmentPolicy.WEAPONS_SUPPRESSED),

    /**
     * Lobo Dentes-de-Sabre — Vantagem em Perícias de Ataque, and the table's one clawback: "pode
     * empunhar armas de uma mão com as presas", so {@link FormEquipmentPolicy#WEAPONS_SUPPRESSED}
     * stops at two-handed weapons here.
     */
    LOBO_DENTES_DE_SABRE(FormEquipmentPolicy.WEAPONS_SUPPRESSED, true),

    /** Morcego Atroz — Movimento de Voo, Roubo de Vida +2. */
    MORCEGO_ATROZ(FormEquipmentPolicy.WEAPONS_SUPPRESSED),

    /** Serpente Espinhosa — Corrente de Efeitos: Veneno Vampírico. */
    SERPENTE_ESPINHOSA(FormEquipmentPolicy.WEAPONS_SUPPRESSED);

    private final FormEquipmentPolicy equipmentPolicy;

    private final boolean oneHandedWeaponsPermitted;

    FormType() {
        this(FormEquipmentPolicy.UNRESTRICTED);
    }

    FormType(final FormEquipmentPolicy equipmentPolicy) {
        this(equipmentPolicy, false);
    }

    FormType(final FormEquipmentPolicy equipmentPolicy, final boolean oneHandedWeaponsPermitted) {
        this.equipmentPolicy = equipmentPolicy;
        this.oneHandedWeaponsPermitted = oneHandedWeaponsPermitted;
    }

    /**
     * What this shape lets its holder keep using — {@link FormEquipmentPolicy#UNRESTRICTED} for
     * every Forma whose rules text says nothing about equipment, which is most of them.
     *
     * <p>⚠️ {@link #ANIMAL} is {@link FormEquipmentPolicy#ALL_SUPPRESSED} on the strength of
     * {@code BestialFeat#METAMORFOSE_SELVAGEM}'s "é impossível usar equipamentos enquanto este
     * Talento estiver ativo" — a character who is wholly an animal has no hands for gear, whatever
     * put them there. {@code HomemFera}'s own Animal rung therefore <b>inherits</b> that
     * restriction, and its clause is not in this repo to check against; revisit when that text
     * lands.
     *
     * <p><b>The policy is not the whole answer for two shapes.</b> Both exceptions live inside
     * {@link FormEquipmentPolicy#WEAPONS_SUPPRESSED} and are per-shape rather than per-policy,
     * which is why they are columns here instead of new policy constants: {@link
     * #LOBO_DENTES_DE_SABRE} claws back one-handed weapons ({@link #permitsOneHandedWeapons()}),
     * and {@link #NEVOA} goes the other way — its holder can use no Arma Natural either, which
     * falls out of {@code Feat#replacesNaturalWeaponsWhileInForm} emptying the list rather than
     * from anything here.
     */
    public FormEquipmentPolicy getEquipmentPolicy() {
        return equipmentPolicy;
    }

    /**
     * Whether this shape keeps one-handed weapons usable despite suppressing weapons generally —
     * {@link #LOBO_DENTES_DE_SABRE}'s "pode empunhar armas de uma mão com as presas", the only
     * constant that answers {@code true}.
     *
     * <p>Handedness is <b>inferred</b>, not authored: {@code CombatantSheet#canAttackWith} reuses
     * the same {@code ItemWeightClass}-plus-category rule {@code CharacterSheet}'s two-hand
     * loadout budget applies, since {@code equipamentos.txt} gives no weapon a hands column.
     * Meaningless while the policy is {@link FormEquipmentPolicy#UNRESTRICTED} (nothing to claw
     * back) or {@link FormEquipmentPolicy#ALL_SUPPRESSED} (the clause that would grant it does not
     * exist on any such shape).
     */
    public boolean permitsOneHandedWeapons() {
        return oneHandedWeaponsPermitted;
    }
}
