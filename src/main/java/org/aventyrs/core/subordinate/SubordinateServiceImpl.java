package org.aventyrs.core.subordinate;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.SUBORDINATE_GRADE_HELD;
import static org.aventyrs.core.util.TranslatableMessages.SUBORDINATE_LIMIT_REACHED;

public class SubordinateServiceImpl implements SubordinateService {

    /** The sombra conselheira's source — on the Subordinado, and the once-per-Cena mark. */
    public static final String SHADOW_COUNSEL = "Sombra Conselheira";

    @Override
    public void command(@NonNull final CombatantSheet commander, @NonNull final Subordinate subordinate,
                        final SceneContext sceneContext) {
        command(commander, subordinate, sceneContext, null);
    }

    @Override
    public Subordinate summonShadowCounsel(@NonNull final CombatantSheet holder, @NonNull final SubordinateBenefit benefit,
                                           final SceneContext sceneContext) {
        if (!org.aventyrs.core.feat.DevotoFeat.ABRACADO_PELA_ESQUECIDA.reached(holder.getCharacter(),
                org.aventyrs.core.character.DevotionTier.FUNDAMENTALISTA)) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.SHADOW_COUNSEL_NOT_HELD);
        }
        SubordinateGrade grade = benefit.getGrade();
        if (grade != SubordinateGrade.PEAO && grade != SubordinateGrade.CAVALEIRO && grade != SubordinateGrade.TORRE) {
            throw new IllegalOperationException(
                    org.aventyrs.core.util.TranslatableMessages.SHADOW_COUNSEL_GRADE_NOT_ALLOWED);
        }
        if (holder.isAffectedThisCombat(SHADOW_COUNSEL)) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.SHADOW_COUNSEL_ALREADY_USED);
        }
        Subordinate shadow = Subordinate.sustained(benefit, false, SHADOW_COUNSEL, holder.getId(),
                SHADOW_COUNSEL_TRAILING_ROUNDS);
        command(holder, shadow, sceneContext);
        holder.beginConcentration();
        holder.markAffectedThisCombat(SHADOW_COUNSEL);
        return shadow;
    }

    @Override
    public void command(@NonNull final CombatantSheet commander, @NonNull final Subordinate subordinate,
                        final SceneContext sceneContext, final org.aventyrs.core.rest.RestType endsAtRest) {
        var held = SubordinateBenefits.of(commander);
        int limit = commander.getAttributeTotal(AttributeDomain.CHARISMA);
        if (held.size() >= limit) {
            throw new IllegalOperationException(SUBORDINATE_LIMIT_REACHED);
        }
        boolean sameGradeCommon = held.stream()
                .anyMatch(other -> other.getGrade() == subordinate.getGrade() && !other.isProdigious());
        if (!subordinate.isProdigious() && sameGradeCommon) {
            throw new IllegalOperationException(SUBORDINATE_GRADE_HELD);
        }
        if (endsAtRest == null) {
            commander.applyEffect(subordinate);
        } else {
            commander.applyEffectUntilRest(subordinate, endsAtRest);
        }
        grantKingsEgo(commander, subordinate);
        if (subordinate.isProdigious() && sceneContext != null) {
            sceneContext.getAllies().stream()
                    .filter(ally -> ally != commander && ally instanceof CharacterSheet)
                    .forEach(ally -> grantKingsEgo(ally, subordinate));
        }
    }

    @Override
    public void dismiss(@NonNull final CombatantSheet commander, @NonNull final UUID id) {
        SubordinateBenefits.of(commander).stream()
                .filter(held -> held.getId().equals(id))
                .findFirst()
                .ifPresent(commander::removeEffect);
    }

    @Override
    public void renewAfterLongRest(@NonNull final CombatantSheet commander) {
        SubordinateBenefits.of(commander).forEach(held -> grantKingsEgo(commander, held));
    }

    /** A Rei's "2 Pontos Temporários em Ego", received like any granted point; nothing for another grade. */
    private static void grantKingsEgo(final CombatantSheet recipient, final Subordinate subordinate) {
        EgoDomain domain = switch (subordinate.getBenefit()) {
            case REI_SORTE -> EgoDomain.SORTE;
            case REI_AUTOCONTROLE -> EgoDomain.AUTOCONTROLE;
            default -> null;
        };
        if (domain != null) {
            recipient.receiveTemporaryEgoPoints(domain, "SUBORDINADO_REI:" + subordinate.getId(),
                    SubordinateBenefit.EGO_POINTS);
        }
    }
}
