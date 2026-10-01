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

    @Override
    public void command(@NonNull final CombatantSheet commander, @NonNull final Subordinate subordinate,
                        final SceneContext sceneContext) {
        var held = SubordinateBenefits.of(commander);
        int limit = commander.getCharacter().getEffectiveAttributeTotal(AttributeDomain.CHARISMA, commander);
        if (held.size() >= limit) {
            throw new IllegalOperationException(SUBORDINATE_LIMIT_REACHED);
        }
        boolean sameGradeCommon = held.stream()
                .anyMatch(other -> other.getGrade() == subordinate.getGrade() && !other.isProdigious());
        if (!subordinate.isProdigious() && sameGradeCommon) {
            throw new IllegalOperationException(SUBORDINATE_GRADE_HELD);
        }
        commander.applyEffect(subordinate);
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
