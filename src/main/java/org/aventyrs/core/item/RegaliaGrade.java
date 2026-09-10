package org.aventyrs.core.item;

import org.aventyrs.core.skill.DifficultyLevel;

/**
 * How powerful a <b>Regalia</b> is — the tier a {@code org.aventyrs.core.feat.ArtificeFeat}
 * Talento lets its holder forge. <b>Not an {@link ItemRarity}</b>: "Regalia" is a property of the
 * owned copy ({@link Item#isRegalia()} / {@link Item#getRegaliaGrade()}), orthogonal to how rare
 * the base Equipamento is — a Regalia Menor can be built on a Comum blade or an Épico one.
 *
 * <p>The three grades are a ladder: {@link #MENOR} → {@link #SUPERIOR} → {@link #DIVINA}, each
 * costlier in Grau de Dificuldade, in days of work, and in the Centelha sacrifice its creation
 * demands (see {@code docs/rules/talentos.txt}, the "O Olho de Deus / Artesão de Regalias" block).
 * Everything the crafting flow needs off a grade lives here; {@code
 * org.aventyrs.core.character.services.EquipmentCraftingService#forgeRegalia} reads it.
 */
public enum RegaliaGrade {

    /**
     * "Criar uma Regalia Menor exige uma rolagem de Profissão na GD Inimaginável … um personagem
     * sacrifique voluntariamente uma de suas Centelhas … leva até 90 dias de trabalho."
     */
    MENOR(DifficultyLevel.UNIMAGINABLE, 90, false, false),

    /**
     * "Criar uma Regalia Superior exige uma rolagem de Profissão na GD Milagre … sacrifique
     * voluntariamente todas as suas Centelhas … leva até 145 dias de trabalho."
     */
    SUPERIOR(DifficultyLevel.MIRACLE, 145, false, true),

    /**
     * "Criar uma Regalia Divina exige uma rolagem de Profissão na GD Milagre que tenha por
     * resultado obrigatório um Acerto Crítico … é necessário que um Dragão, Elemental, Abissal ou
     * Celestial sacrifique voluntariamente suas Centelhas … leva até 180 dias de trabalho."
     */
    DIVINA(DifficultyLevel.MIRACLE, 180, true, true);

    private final DifficultyLevel craftingDifficulty;
    private final int craftingTimeInDays;
    private final boolean requiresCriticalResult;
    private final boolean requiresAllCentelhas;

    RegaliaGrade(final DifficultyLevel craftingDifficulty, final int craftingTimeInDays,
                 final boolean requiresCriticalResult, final boolean requiresAllCentelhas) {
        this.craftingDifficulty = craftingDifficulty;
        this.craftingTimeInDays = craftingTimeInDays;
        this.requiresCriticalResult = requiresCriticalResult;
        this.requiresAllCentelhas = requiresAllCentelhas;
    }

    /**
     * The Grau de Dificuldade of the Profissão (ofício) roll to forge a Regalia of this grade —
     * Inimaginável for {@link #MENOR}, Milagre for {@link #SUPERIOR}/{@link #DIVINA}. The rules
     * add "não é possível reduzir GD desta rolagem … exceto Habilidades de Artífice"; this core
     * carries no GD-reduction source that targets a crafting roll and no Artífice Habilidade that
     * would be exempt, so that carve-out is currently exempt from nothing — see {@code
     * EquipmentCraftingService#getRegaliaCraftingDifficulty}.
     */
    public DifficultyLevel getCraftingDifficulty() {
        return craftingDifficulty;
    }

    /** The raw Tempo de Criação in days — 90 / 145 / 180 — before any Habilidade de Artífice reduction. */
    public int getCraftingTimeInDays() {
        return craftingTimeInDays;
    }

    /**
     * Whether the crafting roll must land an Acerto Crítico outright, not merely reach the GD —
     * {@link #DIVINA} only. This core never rolls dice, so the flag is reported for the caller to
     * enforce, the same caller-drives-it contract the rest of {@code EquipmentCraftingService} uses.
     */
    public boolean requiresCriticalResult() {
        return requiresCriticalResult;
    }

    /**
     * Whether the donor gives up <b>all</b> their Centelhas ({@link #SUPERIOR}/{@link #DIVINA}),
     * rather than a single one ({@link #MENOR}). This core does not track a character's Centelhas
     * (the gap {@code DestinoFeat#FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH} cites), so the donation is
     * a caller assertion — see {@code EquipmentCraftingService.RegaliaDonation}.
     */
    public boolean requiresAllCentelhas() {
        return requiresAllCentelhas;
    }

    /**
     * Whether the Centelhas must come from a <b>Dragão, Elemental, Abissal ou Celestial</b>
     * rather than from any willing personagem — {@link #DIVINA} only.
     */
    public boolean requiresExternalDonor() {
        return this == DIVINA;
    }
}
