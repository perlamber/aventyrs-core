package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.defect.Defect;
import org.aventyrs.core.defect.DefectSeverity;
import org.aventyrs.core.defect.HeldDefect;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.DEFECT_NOT_HELD;

public class DefectServiceImpl implements DefectService {

    @Override
    public HeldDefect grantDefect(@NonNull final Character character, @NonNull final Defect defect,
                                  @NonNull final DefectSeverity severity, final List<Object> choices)
            throws IllegalOperationException {
        List<Object> answers = choices == null ? List.of() : choices;
        CharacterCreationServiceImpl.validateChoices(defect.resolveChoices(severity, character, answers), answers);
        HeldDefect held = character.getActiveDefect(defect)
                .map(current -> current.changedTo(severity, answers))
                .orElseGet(() -> HeldDefect.duringPlay(defect, severity, answers));
        character.imposeDefect(held);
        return held;
    }

    @Override
    public BigDecimal getOvercomeCost(@NonNull final DefectSeverity severity) {
        return severity.getOvercomeCost();
    }

    @Override
    public HeldDefect overcome(@NonNull final Character character, @NonNull final CharacterSheet sheet,
                               @NonNull final Defect defect) throws IllegalOperationException {
        HeldDefect current = character.getActiveDefect(defect)
                .orElseThrow(() -> new IllegalOperationException(DEFECT_NOT_HELD));
        sheet.useExperience(getOvercomeCost(current.severity()));
        HeldDefect overcome = current.asOvercome();
        character.imposeDefect(overcome);
        return overcome;
    }
}
