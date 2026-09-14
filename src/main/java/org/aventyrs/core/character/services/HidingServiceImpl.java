package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.skill.DifficultyLevel;

public class HidingServiceImpl implements HidingService {

    @Override
    public Hidden hide(@NonNull final CombatantSheet hider, final int furtividadeTotal) {
        return hide(hider, furtividadeTotal, false);
    }

    @Override
    public Hidden hide(@NonNull final CombatantSheet hider, final int furtividadeTotal,
                       final boolean rolledWithSpecialization) {
        return conceal(hider, Hidden.fromRoll(furtividadeTotal, rolledWithSpecialization));
    }

    @Override
    public Hidden hide(@NonNull final CombatantSheet hider, @NonNull final DifficultyLevel difficultyLevel,
                       final int bonus) {
        return conceal(hider, new Hidden(difficultyLevel, bonus));
    }

    private Hidden conceal(final CombatantSheet hider, final Hidden hidden) {
        hider.applyCondition(hidden);
        return hidden;
    }

    @Override
    public Integer getConcealmentValue(@NonNull final CombatantSheet hider) {
        return getConcealmentValue(hider, false);
    }

    @Override
    public Integer getConcealmentValue(@NonNull final CombatantSheet hider, final boolean observerIsExpert) {
        return hider.getHidden().map(hidden -> hidden.getConcealmentValue(observerIsExpert)).orElse(null);
    }

    @Override
    public boolean isHiddenFrom(@NonNull final CombatantSheet hider, final CombatantSheet observer,
                                final SceneContext hiderContext) {
        return hider.getHidden()
                .filter(hidden -> observer != null && !isSelf(hider, observer))
                .filter(hidden -> !isAlly(observer, hiderContext))
                .filter(hidden -> !hidden.isDetectedBy(observer))
                .isPresent();
    }

    @Override
    public boolean resolveDetection(@NonNull final CombatantSheet hider, final CombatantSheet observer,
                                    final Integer attentionTotal, final SceneContext hiderContext) {
        return resolveDetection(hider, observer, attentionTotal, false, hiderContext);
    }

    @Override
    public boolean resolveDetection(@NonNull final CombatantSheet hider, final CombatantSheet observer,
                                    final Integer attentionTotal, final boolean observerIsExpert,
                                    final SceneContext hiderContext) {
        if (!isHiddenFrom(hider, observer, hiderContext)) {
            return true;
        }
        Hidden hidden = hider.getHidden().orElseThrow();
        Integer perception = perceptionOf(observer, attentionTotal);
        if (perception == null || perception < hidden.getConcealmentValue(observerIsExpert && !isFoe(observer))) {
            return false;
        }
        hidden.markDetectedBy(observer);
        return true;
    }


    @Override
    public boolean reveals(@NonNull final CombatantSheet hider, @NonNull final RevealTrigger trigger) {
        if (hider.getHidden().isEmpty()) {
            return false;
        }
        return trigger != RevealTrigger.MOVEMENT || !movesWhileHidden(hider);
    }

    @Override
    public boolean reveal(@NonNull final CombatantSheet hider, @NonNull final RevealTrigger trigger) {
        if (!reveals(hider, trigger)) {
            return false;
        }
        hider.removeCondition(ConditionType.ESCONDIDO);
        return true;
    }

    /**
     * What observer opposes the concealment with: a foe's authored flat Atenção, or the total a
     * rolling observer was handed in. {@code null} — a rolling observer with no total — is
     * "cannot tell", and the caller above reads it as no detection.
     *
     * <p>A total supplied for a {@code MonsterSheet} is ignored rather than preferred. A foe never
     * rolls anywhere in this core; letting a caller slip one in here would make the one sheet type
     * whose numbers are authored behave differently depending on who asked.
     */
    private Integer perceptionOf(final CombatantSheet observer, final Integer attentionTotal) {
        if (observer instanceof MonsterSheet foe) {
            return foe.getPerception();
        }
        return attentionTotal;
    }

    /**
     * A foe never rolls, so it never rolled with an Especialização either: both {@code
     * attentionTotal} and {@code observerIsExpert} are ignored for one, and its authored flat
     * Atenção is compared against the tier's ordinary threshold.
     */
    private boolean isFoe(final CombatantSheet observer) {
        return observer instanceof MonsterSheet;
    }

    /**
     * Whether any Talento the hider holds lets them move without giving themselves away — {@code
     * MobilidadeFeat#MOVIMENTO_FURTIVO}. No {@code AttributeAbility}/{@code
     * SkillCompetencyAbility} pass: no Habilidade in the catalogue states this, and one would be
     * added with its first constant rather than ahead of it.
     */
    private boolean movesWhileHidden(final CombatantSheet hider) {
        return hider.getCharacter().getFeats().stream()
                .anyMatch((Feat feat) -> feat.movesWhileHidden(hider.getCharacter()));
    }

    private boolean isSelf(final CombatantSheet hider, final CombatantSheet observer) {
        return hider.getId().equals(observer.getId());
    }

    /**
     * Whether observer shares the hider's sub-group, read off the hider's own snapshot — matched by
     * {@code CombatantSheet#getId()}, the way {@code org.aventyrs.core.scene.Scene} identifies its
     * participants. A {@code null} context cannot establish the group and answers {@code false};
     * see {@link HidingService#isHiddenFrom} for why that direction.
     */
    private boolean isAlly(final CombatantSheet observer, final SceneContext hiderContext) {
        return hiderContext != null && hiderContext.getAllies().stream()
                .anyMatch(ally -> ally.getId().equals(observer.getId()));
    }
}
