package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.feat.BestialFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.LacertoFerocity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_MIMIC_ALREADY_USED;
import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_MIMIC_NOT_GRANTED;
import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_MIMIC_TOO_EARLY;
import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;

public class LacertoFerocityServiceImpl implements LacertoFerocityService {

    /** The combat-scoped marker for "declined this combat". */
    private static final Object DECLINED = LacertoFerocityServiceImpl.class.getName() + "#DECLINED";

    /** "Com 1 ou mais PV" — every status above zero PV. */
    private static final Set<CharacterStatus> STANDING = EnumSet.of(CharacterStatus.CLEAN,
            CharacterStatus.HIGH_LIFE, CharacterStatus.MEDIUM_LIFE, CharacterStatus.LOW_LIFE);

    private final HitPointsService hitPointsService;

    public LacertoFerocityServiceImpl() {
        this(new HitPointsServiceImpl());
    }

    public LacertoFerocityServiceImpl(@NonNull final HitPointsService hitPointsService) {
        this.hitPointsService = hitPointsService;
    }

    @Override
    public Optional<Integer> getFerocityRound(@NonNull final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        if (character == null) {
            return Optional.empty();
        }
        Integer racial = character.getRace() == null || sheet.getRacialTraitSuppression().suppressesInnateTraits()
                ? null : character.getRace().resolveLacertoFerocityRound(character);
        return Stream.concat(Stream.of(racial),
                        character.getFeats().stream().map(feat -> feat.resolveLacertoFerocityRound(character)))
                .filter(Objects::nonNull)
                .min(Integer::compare);
    }

    @Override
    public Optional<LacertoFerocity> refresh(@NonNull final CombatantSheet sheet, final SceneContext holderContext) {
        Optional<LacertoFerocity> running = sheet.getLacertoFerocities().stream()
                .filter(ferocity -> !ferocity.isMimicked())
                .findFirst();
        if (running.isPresent()) {
            return running;
        }
        if (holderContext == null || !holderContext.isCombatScene() || sheet.isAffectedThisCombat(DECLINED)) {
            return Optional.empty();
        }
        Optional<Integer> round = getFerocityRound(sheet);
        if (round.isEmpty() || holderContext.getCurrentRound() < round.get()) {
            return Optional.empty();
        }
        Character character = sheet.getCharacter();
        Predicate<CombatantSheet> standing = ally -> STANDING.contains(hitPointsService.getStatus(ally));
        for (Feat feat : character.getFeats()) {
            if (feat.preventsEnteringLacertoFerocity(character, holderContext, standing)) {
                return Optional.empty();
            }
        }
        LacertoFerocity ferocity = LacertoFerocity.natural();
        sheet.applyEffectUntilCombatEnds(ferocity);
        return Optional.of(ferocity);
    }

    @Override
    public void decline(@NonNull final CombatantSheet sheet) {
        if (getFerocityRound(sheet).isEmpty()) {
            throw new IllegalOperationException(LACERTO_FEROCITY_NOT_HELD);
        }
        spendAutocontrole(sheet);
        sheet.endNaturalLacertoFerocity();
        sheet.markAffectedThisCombat(DECLINED);
    }

    @Override
    public LacertoFerocity mimic(@NonNull final CombatantSheet sheet, final SceneContext holderContext) {
        Character character = sheet.getCharacter();
        boolean held = character != null && character.getFeats().stream()
                .anyMatch(feat -> feat.catalogEntry() == BestialFeat.ACEITAR_A_LACERTO);
        if (!held) {
            throw new IllegalOperationException(LACERTO_FEROCITY_MIMIC_NOT_GRANTED);
        }
        if (holderContext == null || !holderContext.isCombatScene() || holderContext.getCurrentRound() < MIMIC_FROM_ROUND) {
            throw new IllegalOperationException(LACERTO_FEROCITY_MIMIC_TOO_EARLY);
        }
        if (sheet.isAffectedThisCombat(BestialFeat.ACEITAR_A_LACERTO)) {
            throw new IllegalOperationException(LACERTO_FEROCITY_MIMIC_ALREADY_USED);
        }
        spendAutocontrole(sheet);
        sheet.markAffectedThisCombat(BestialFeat.ACEITAR_A_LACERTO);
        LacertoFerocity ferocity = LacertoFerocity.mimicked(
                Math.max(1, character.getEffectiveAttributeTotal(AttributeDomain.INSTINCT)));
        sheet.applyEffectUntilCombatEnds(ferocity);
        return ferocity;
    }

    private static void spendAutocontrole(final CombatantSheet sheet) {
        if (sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE) < 1) {
            throw new IllegalOperationException(NOT_ENOUGH_EGO_POINTS);
        }
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1);
    }
}
