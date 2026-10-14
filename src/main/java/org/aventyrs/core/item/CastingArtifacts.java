package org.aventyrs.core.item;

import java.util.ArrayList;
import java.util.List;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;

/**
 * The one scan over a character's <b>drawn</b> Artefatos de Conjuração ({@link CastingArtifactItem}) — "utilizando"
 * is in hand, never merely worn — and only those whose Requisitos the wielder meets ({@link
 * Item#grantsFavorTo(Character)}, which also silences a destroyed one). Every casting reader asks here rather than
 * walking the drawn list itself.
 *
 * <p>Stacking: "Vantagem" is a flat {@link Skill#ADVANTAGE_BONUS} however many artefacts grant it (Vantagens do not
 * stack); a Margem, damage/heal or PA/PM figure sums; the post-cast Defesas bonus takes the largest, since both
 * Cajados name one bonus by one source.
 */
public final class CastingArtifacts {

    /** The source key of the post-cast Defesas bonus — shared, so Marcas da Proteção/Guerra replace it. */
    public static final String CAST_DEFESAS_SOURCE = "ARTEFATO_DE_CONJURACAO_DEFESAS";

    /** Esfera de AEther's Violento on an artefact host: "Dano e Cura mágica +3". */
    public static final int ESFERA_DE_AETHER_SPELL_EFFECT_BONUS = 3;

    private CastingArtifacts() {
    }

    /** One drawn artefact and the copy that is drawn. */
    public record Drawn(CastingArtifactItem artifact, Item copy) {
    }

    /** Every drawn artefact of character whose Requisitos it meets right now. */
    public static List<Drawn> empowered(final Character character) {
        List<Drawn> drawn = new ArrayList<>();
        if (character == null || character.getDrawnWeapons() == null) {
            return drawn;
        }
        for (Weapon weapon : character.getDrawnWeapons()) {
            CastingArtifactItem artifact = CastingArtifactItem.of(weapon);
            if (artifact != null && weapon.grantsFavorTo(character)) {
                drawn.add(new Drawn(artifact, weapon));
            }
        }
        return drawn;
    }

    /** Whether artifact is drawn by character and its Requisitos are met — its Favor's stronger figure applies. */
    public static boolean isEmpowered(final Character character, final CastingArtifactItem artifact) {
        return empowered(character).stream().anyMatch(drawn -> drawn.artifact() == artifact);
    }

    /** Whether character has artifact drawn at all, Requisitos or not — what offers its Efeito Ativo. */
    public static boolean isDrawn(final Character character, final CastingArtifactItem artifact) {
        return character != null && character.getDrawnWeapons() != null && character.getDrawnWeapons().stream()
                .anyMatch(weapon -> CastingArtifactItem.of(weapon) == artifact);
    }

    /** "Vantagem nas Rolagens de Perícias para Conjuração de Magias" — {@link Skill#ADVANTAGE_BONUS} or 0. */
    public static int castingRollBonus(final Character character) {
        return empowered(character).stream().anyMatch(drawn -> drawn.artifact().grantsCastingAdvantage())
                ? Skill.ADVANTAGE_BONUS : 0;
    }

    /** "Margem Crítica Menor das Magias que conjurar aumentam em +N", summed. */
    public static int spellCriticalMarginIncrease(final Character character) {
        return empowered(character).stream().mapToInt(drawn -> drawn.artifact().getSpellCriticalMarginIncrease()).sum();
    }

    /**
     * What every Magia's damage and healing gain: Cajado de Guerra's +1, plus +3 for an artefact hosting a bound Esfera
     * de AEther ("Pode ser anexo apenas à Artefatos de Conjuração. Dano e Cura mágica +3").
     */
    public static int spellEffectBonus(final Character character) {
        return empowered(character).stream()
                .mapToInt(drawn -> drawn.artifact().getSpellEffectBonus()
                        + (drawn.copy().getPowerStone() != null && !drawn.copy().isDestroyed()
                        && drawn.copy().getPowerStone().getStoneType() == PowerStoneType.ESFERA_DE_AETHER
                        ? ESFERA_DE_AETHER_SPELL_EFFECT_BONUS : 0))
                .sum();
    }

    /** "Bônus de +1 em suas Defesas por 1 Rodada após conjurar uma Magia" — the largest one drawn. */
    public static int castDefesasBonus(final Character character) {
        return empowered(character).stream().mapToInt(drawn -> drawn.artifact().getCastDefesasBonus()).max().orElse(0);
    }

    /** Grimório: the first Magia of each Rodada costs this many PA less. */
    public static int firstSpellActivationReduction(final Character character) {
        return empowered(character).stream()
                .mapToInt(drawn -> drawn.artifact().getFirstSpellActivationReduction()).sum();
    }

    /** Varinha: the first Magia of each Rodada costs this many PM less (never below 1). */
    public static int firstSpellManaReduction(final Character character) {
        return empowered(character).stream().mapToInt(drawn -> drawn.artifact().getFirstSpellManaReduction()).sum();
    }

    /** Grimório's Otimização Metamágica: a critical Magia's Efeitos Críticos trigger twice. */
    public static boolean doublesSpellCriticalEffects(final Character character) {
        return empowered(character).stream().anyMatch(drawn -> drawn.artifact().doublesSpellCriticalEffects());
    }

    /** Bola de Crista: no Efeito Crítico Menor from the dice's sum on a casting roll. */
    public static boolean suppressesSumMinorCriticals(final Character character) {
        return empowered(character).stream().anyMatch(drawn -> drawn.artifact().suppressesSumMinorCriticals());
    }

    /** Bola de Crista: UD added to a Toque Magia's reach. Reported — a Toque has no {@code Range} band to widen. */
    public static int touchReachIncreaseUd(final Character character) {
        return empowered(character).stream().mapToInt(drawn -> drawn.artifact().getTouchReachIncreaseUd()).sum();
    }

    /** Bola de Crista: Range steps added to a Magia à Distância. */
    public static int spellRangeSteps(final Character character) {
        return empowered(character).stream().mapToInt(drawn -> drawn.artifact().getSpellRangeSteps()).sum();
    }

    /** The Correntes a Magia cast by caster receives from the drawn artefacts, built for that cast. */
    public static List<EffectChain> spellEffectChains(final CombatantSheet caster, final Spell spell,
                                                      final boolean hostile) {
        List<EffectChain> chains = new ArrayList<>();
        for (Drawn drawn : empowered(caster.getCharacter())) {
            EffectChain chain = drawn.artifact().createSpellEffectChain(caster, spell, hostile);
            if (chain != null) {
                chains.add(chain);
            }
        }
        return chains;
    }

    /** The Efeitos Ativos the drawn artefacts offer — offered while drawn; Requisitos only change the figure. */
    public static List<ActiveAbility> activeAbilities(final Character character) {
        if (character == null || character.getDrawnWeapons() == null) {
            return List.of();
        }
        return character.getDrawnWeapons().stream()
                .map(CastingArtifactItem::of)
                .filter(java.util.Objects::nonNull)
                .map(CastingArtifactItem::getArtifactAbility)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
