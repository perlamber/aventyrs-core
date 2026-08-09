package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Esquiva e Aparar Perícia test. See {@link AbstractSkillInteraction} for how the
 * roll bonus/difficultyReduction are actually computed.
 *
 * <p>TODO: the rules text also has the character's equipped armor Categoria reduce or zero
 * out this roll's Destreza contribution (full value with Leve/no armor, half with Média,
 * none with Pesada — see {@link EsquivaEApararCompetencyAbility#ENCOURACADO_E_VELOZ} for how
 * that's lessened). No Equipamento/Armadura tracking exists on {@code Character} yet, so
 * {@link AbstractSkillInteraction} always uses the full attribute total via {@link
 * org.aventyrs.core.character.services.CharacterSkillService#getValueForRoll} — it doesn't
 * yet know what, if anything, the character has equipped.
 */
public class EsquivaEApararInteraction extends AbstractSkillInteraction {

    public EsquivaEApararInteraction() {
        super(SkillType.ESQUIVA_E_APARAR);
    }

    public EsquivaEApararInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ESQUIVA_E_APARAR, characterSkillService, modifierResolver);
    }
}
