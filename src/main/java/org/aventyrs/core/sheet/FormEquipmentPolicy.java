package org.aventyrs.core.sheet;

/**
 * What a {@link FormType} lets its holder keep using. A Forma reshapes the body, and the rules
 * are explicit — and inconsistent — about what survives that: {@code
 * BestialFeat#METAMORFOSE_SELVAGEM} says "é impossível usar equipamentos enquanto este Talento
 * estiver ativo", while {@code VampiricoFeat#METAMORFOSE_DRACULEA} says "seus Equipamentos se
 * adaptam ao seu corpo, itens defensivos continuam concedendo seus benefícios, armas não podem
 * ser utilizadas". Those are two different answers, so the policy is a property of the shape
 * rather than one blanket rule.
 *
 * <p>A third clause is neither: {@code DraconicoFeat#ASAS_DE_DRAGAO} "impede de usar Equipamentos
 * do tipo Capa" <b>permanently</b>, with no Forma involved at all. That one is a Talento-level
 * restriction and deliberately not modelled here.
 */
public enum FormEquipmentPolicy {

    /**
     * Everything keeps working — the default, and the answer for every Forma whose rules text
     * says nothing about equipment ({@code DRACONATO}, {@code ANCIENTE}, the Górgona shapes).
     */
    UNRESTRICTED,

    /**
     * Defensive items keep granting their benefits; weapons cannot be used. The Metamorfose
     * Dracúlea shapes — a wolf still wears its master's enchanted cloak, but cannot hold a sword.
     */
    WEAPONS_SUPPRESSED,

    /** No equipment at all — Metamorfose Selvagem's "é impossível usar equipamentos". */
    ALL_SUPPRESSED;

    /** Whether a Forma under this policy lets its holder attack with a wielded {@code Weapon}. */
    public boolean permitsWeapons() {
        return this == UNRESTRICTED;
    }
}
