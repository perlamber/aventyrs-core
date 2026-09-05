package org.aventyrs.core.item;

import org.aventyrs.core.skill.DifficultyLevel;

/**
 * How rare an {@link Item} is — the second half of an item's own "(Pesado/Raro)"-style
 * heading, alongside {@link ItemWeightClass}. Already referenced by rules text elsewhere in
 * this codebase before any Item entity existed: {@code
 * org.aventyrs.core.ego.ResourcesAdvantage#HERANCA_FAMILIAR}'s "um Equipamento Comum
 * Ofensivo de qualquer Raridade" and its "Obra-Prima Comum ou Incomum" upgrade tier.
 *
 * <p>Only the tiers actually confirmed by rules text so far are modeled — {@link
 * ArmorItem#ARMADURA_DE_JUSTA}'s own "(Pesado/Épico)" heading added {@link #EPIC}, while the
 * Couro de Dragão and Espírito Umbral Masterpieces add {@link #MYTHIC} and the "(Leve/Natural)"
 * heading every Arma/Defesa Natural carries adds {@link #NATURAL}. If the ruleset has further
 * tiers, add a constant once its real text exists.
 *
 * <p>Obra-Prima tiers and Aprimoramentos are deliberately *not* modeled here: those are
 * per-acquired-copy upgrades, not a property of the catalog entry (see {@link Item}'s own
 * javadoc on the catalog-versus-owned-copy split).
 */
public enum ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    MYTHIC,

    /**
     * The "(Leve/Natural)"-style heading every Equipamento Natural carries — an Arma Natural
     * ({@link NaturalWeapon}) or a Defesa Natural — rather than a scarcity tier. It marks an
     * item that is part of a creature's body: not bought, not forged, and (unlike every other
     * tier) not something a PE economy would ever price.
     */
    NATURAL;

    /**
     * The Grau de Dificuldade a player rolls Conhecimentos/Profissão against to <b>fabricate</b>
     * an item of this Raridade — "o Grau de Dificuldade varia de acordo com a sua Raridade":
     * COMMON→Médio, UNCOMMON→Difícil, RARE→Muito Difícil, EPIC→Improvável, MYTHIC→Inimaginável.
     * Fabricating it as an Obra-Prima raises this by {@code harder(1)} (see {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#getFabricationDifficulty}).
     *
     * @throws IllegalStateException for {@link #NATURAL} — a body part is never forged.
     */
    public DifficultyLevel getFabricationDifficulty() {
        return switch (this) {
            case COMMON -> DifficultyLevel.MEDIUM;
            case UNCOMMON -> DifficultyLevel.HARD;
            case RARE -> DifficultyLevel.VERY_HARD;
            case EPIC -> DifficultyLevel.UNLIKELY;
            case MYTHIC -> DifficultyLevel.UNIMAGINABLE;
            case NATURAL -> throw naturalUnsupported();
        };
    }

    /**
     * The Grau de Dificuldade to <b>install an Aprimoramento</b> on an Obra-Prima of this
     * Raridade — COMMON→Difícil, UNCOMMON→Muito Difícil, RARE→Improvável, EPIC→Inimaginável,
     * MYTHIC→Milagre. Each Aprimoramento already fitted adds cumulative Desvantagem on top
     * (see {@code EquipmentCraftingService#getImprovementInstallDisadvantage}).
     *
     * @throws IllegalStateException for {@link #NATURAL}.
     */
    public DifficultyLevel getImprovementInstallDifficulty() {
        return switch (this) {
            case COMMON -> DifficultyLevel.HARD;
            case UNCOMMON -> DifficultyLevel.VERY_HARD;
            case RARE -> DifficultyLevel.UNLIKELY;
            case EPIC -> DifficultyLevel.UNIMAGINABLE;
            case MYTHIC -> DifficultyLevel.MIRACLE;
            case NATURAL -> throw naturalUnsupported();
        };
    }

    /**
     * The Grau de Dificuldade a player rolls Conhecimentos against to <b>repair</b> an ordinary
     * (non-Obra-Prima) item of this Raridade — "o GD do reparo pode variar conforme a sua
     * Raridade": COMMON→Muito Fácil, UNCOMMON→Fácil, RARE→Médio, EPIC→Difícil, MYTHIC→Muito
     * Difícil. An Obra-Prima repairs at the harder {@link #getMasterpieceRepairDifficulty()}.
     *
     * @throws IllegalStateException for {@link #NATURAL}.
     */
    public DifficultyLevel getRepairDifficulty() {
        return switch (this) {
            case COMMON -> DifficultyLevel.VERY_EASY;
            case UNCOMMON -> DifficultyLevel.EASY;
            case RARE -> DifficultyLevel.MEDIUM;
            case EPIC -> DifficultyLevel.HARD;
            case MYTHIC -> DifficultyLevel.VERY_HARD;
            case NATURAL -> throw naturalUnsupported();
        };
    }

    /**
     * The Grau de Dificuldade to repair an <b>Obra-Prima</b> of this Raridade — "Obras-Primas,
     * devido a sua maior riqueza de detalhes, possuem dificuldades maiores": COMMON→Fácil,
     * UNCOMMON→Médio, RARE→Difícil, EPIC→Muito Difícil, MYTHIC→Improvável.
     *
     * @throws IllegalStateException for {@link #NATURAL}.
     */
    public DifficultyLevel getMasterpieceRepairDifficulty() {
        return switch (this) {
            case COMMON -> DifficultyLevel.EASY;
            case UNCOMMON -> DifficultyLevel.MEDIUM;
            case RARE -> DifficultyLevel.HARD;
            case EPIC -> DifficultyLevel.VERY_HARD;
            case MYTHIC -> DifficultyLevel.UNLIKELY;
            case NATURAL -> throw naturalUnsupported();
        };
    }

    /**
     * The minimum Graduação in the crafting Perícia a player needs to fabricate an Obra-Prima of
     * this Raridade — "Criar Obras-Primas exige um valor mínimo de Graduações": COMMON 1,
     * UNCOMMON 3, RARE 5, EPIC 7, MYTHIC 10.
     *
     * @throws IllegalStateException for {@link #NATURAL}.
     */
    public int getMinimumMasterpieceGraduation() {
        return switch (this) {
            case COMMON -> 1;
            case UNCOMMON -> 3;
            case RARE -> 5;
            case EPIC -> 7;
            case MYTHIC -> 10;
            case NATURAL -> throw naturalUnsupported();
        };
    }

    private IllegalStateException naturalUnsupported() {
        return new IllegalStateException("Equipamentos Naturais are part of a body — never fabricated or repaired.");
    }
}
