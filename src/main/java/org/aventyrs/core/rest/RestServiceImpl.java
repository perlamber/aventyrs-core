package org.aventyrs.core.rest;

import java.util.EnumSet;
import java.util.Set;
import org.aventyrs.core.sheet.ResourceType;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.HealingSource;

public class RestServiceImpl implements RestService {

    @Override
    public int getRecoveredHitPoints(final Character character, final RestType restType) {
        return getRecoveredHitPoints(character, restType, RecoveryOrigin.DESCANSO);
    }

    @Override
    public int getRecoveredHitPoints(final Character character, final RestType restType, final RecoveryOrigin origin) {
        boolean real = origin == RecoveryOrigin.DESCANSO;
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> real ? ability.resolveRestHitPointsBonus(restType)
                        : ability.resolveRestEquivalentHitPointsBonus(restType))
                .sum();
        int featBonus = character.getFeats().stream()
                // A Talento's Descanso bonus (Saúde de Ferro, Mente Expandida, Coração de Ferro do Destino)
                // is a Descanso's alone; no Talento yet writes one for any recovery effect.
                .mapToInt(feat -> real ? feat.resolveRestHitPointsBonus(restType, character) : 0)
                .sum();
        return recovered(character.getEffectiveAttributeTotal(AttributeDomain.VIGOR), restType) + bonus + featBonus;
    }

    @Override
    public int getRecoveredMagicPoints(final Character character, final RestType restType) {
        return getRecoveredMagicPoints(character, restType, RecoveryOrigin.DESCANSO);
    }

    @Override
    public int getRecoveredMagicPoints(final Character character, final RestType restType, final RecoveryOrigin origin) {
        if (prevented(character, ResourceType.MAGIC_POINTS)) {
            return 0;
        }
        boolean real = origin == RecoveryOrigin.DESCANSO;
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> real ? ability.resolveRestMagicPointsBonus(restType)
                        : ability.resolveRestEquivalentMagicPointsBonus(restType))
                .sum();
        int featBonus = character.getFeats().stream()
                // A Talento's Descanso bonus (Saúde de Ferro, Mente Expandida, Coração de Ferro do Destino)
                // is a Descanso's alone; no Talento yet writes one for any recovery effect.
                .mapToInt(feat -> real ? feat.resolveRestMagicPointsBonus(restType, character) : 0)
                .sum();
        return recovered(character.getEffectiveAttributeTotal(AttributeDomain.FOCUS), restType) + bonus + featBonus;
    }

    @Override
    public int getRecoveredDeterminationPoints(final Character character, final RestType restType) {
        return getRecoveredDeterminationPoints(character, restType, RecoveryOrigin.DESCANSO);
    }

    @Override
    public int getRecoveredDeterminationPoints(final Character character, final RestType restType,
                                               final RecoveryOrigin origin) {
        boolean real = origin == RecoveryOrigin.DESCANSO;
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> real ? ability.resolveRestDeterminationPointsBonus(restType)
                        : ability.resolveRestEquivalentDeterminationPointsBonus(restType))
                .sum();
        int featBonus = character.getFeats().stream()
                // A Talento's Descanso bonus (Saúde de Ferro, Mente Expandida, Coração de Ferro do Destino)
                // is a Descanso's alone; no Talento yet writes one for any recovery effect.
                .mapToInt(feat -> real ? feat.resolveRestDeterminationPointsBonus(restType, character) : 0)
                .sum();
        return recovered(character.getEffectiveAttributeTotal(AttributeDomain.INSTINCT), restType) + bonus + featBonus;
    }

    @Override
    public void applyRest(final Character character, final CharacterSheet characterSheet, final RestType restType) {
        applyRest(character, characterSheet, restType, false);
    }

    @Override
    public void applyRest(final Character character, final CharacterSheet characterSheet, final RestType taken,
                          final boolean verdadeiro) {
        applyRest(character, characterSheet, taken, verdadeiro, null);
    }

    @Override
    public Set<ResourceType> getRestBonusChoices(final Character character) {
        Set<ResourceType> choices = EnumSet.noneOf(ResourceType.class);
        character.getFeats().forEach(feat -> choices.addAll(feat.resolveRestBonusChoices(character)));
        return Set.copyOf(choices);
    }

    @Override
    public void applyRest(final Character character, final CharacterSheet characterSheet, final RestType taken,
                          final boolean verdadeiro, final ResourceType chosenBonus) {
        // Doutor de Eldur: "Sempre que descansar, seus Descansos contam como uma Categoria superior" —
        // everything below reads the upgraded category, recovery and cooldowns alike.
        RestType restType = character.getAllTitles().stream().anyMatch(title -> title.upgradesRests())
                ? taken.oneCategoryHigher()
                : taken;
        if (verdadeiro) {
            // Transferir Vitalidade's PV "podem ser recuperados apenas com Descansos Verdadeiros".
            characterSheet.releaseVitalityLock();
        }
        // Medicina Alternativa's "1d6PV, PM e PD adicionais ao fim do Descanso", owed before it began.
        int alternative = characterSheet.takePendingRestBonus();
        // A real Descanso is the one heal repeatable in Coma — 1PV each time — and reaches no one dead.
        characterSheet.heal(getRecoveredHitPoints(character, restType)
                + extraRecovery(character, restType, verdadeiro, chosenBonus, ResourceType.HIT_POINTS) + alternative,
                HealingSource.rest(restType));
        characterSheet.recoverMagicPoints(getRecoveredMagicPoints(character, restType)
                + extraRecovery(character, restType, verdadeiro, chosenBonus, ResourceType.MAGIC_POINTS) + alternative);
        characterSheet.recoverDeterminationPoints(getRecoveredDeterminationPoints(character, restType)
                + extraRecovery(character, restType, verdadeiro, chosenBonus, ResourceType.DETERMINATION_POINTS)
                + alternative);
        characterSheet.applyPendingEgoRecoveries(restType);
        // Frees every ability whose Resfriamento was measured in Descansos rather than Rodadas —
        // "não poderá ser reativado até que passe por um Descanso Longo".
        characterSheet.clearRestCooldowns(restType);
        // A Rei renews its Ego points "após … Descansos Longos" (core 0.0.92) — a Longo or anything stronger.
        if (restType.compareTo(RestType.LONGO) >= 0) {
            new org.aventyrs.core.subordinate.SubordinateServiceImpl().renewAfterLongRest(characterSheet);
        }
        if (verdadeiro) {
            characterSheet.completeTrueRest(restType);
        }
    }

    /** Nulificador's "nunca recupera PM com Descansos" — no Descanso returns resource, bonuses included. */
    private static boolean prevented(final Character character, final ResourceType resource) {
        return character.getFeats().stream().anyMatch(feat -> feat.preventsRestRecovery(resource, character));
    }

    /**
     * The Talento-granted recovery on top of the Atributo formula: the Descanso Verdadeiro bonus
     * ({@code Feat#resolveTrueRestBonus}) and the player's per-Descanso pick ({@code
     * Feat#resolveChosenRestBonus}), both for resource.
     */
    private static int extraRecovery(final Character character, final RestType restType, final boolean verdadeiro,
                                     final ResourceType chosenBonus, final ResourceType resource) {
        if (prevented(character, resource)) {
            return 0;
        }
        int extra = 0;
        for (org.aventyrs.core.feat.Feat feat : character.getFeats()) {
            if (verdadeiro) {
                extra += feat.resolveTrueRestBonus(resource, restType, character);
            }
            if (chosenBonus == resource && feat.resolveRestBonusChoices(character).contains(resource)) {
                extra += feat.resolveChosenRestBonus(resource, restType, character);
            }
        }
        return extra;
    }

    /**
     * Descanso Mínimo (x0.5) can yield a fractional result on an odd Attribute; rounded
     * down, as the ruleset does elsewhere when a half-value isn't specified further.
     */
    private int recovered(final int attributeTotal, final RestType restType) {
        return (int) Math.floor(attributeTotal * restType.getAttributeMultiplier());
    }
}
