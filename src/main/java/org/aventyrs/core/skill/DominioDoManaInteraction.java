package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests a Domínio do Mana Perícia test. Domínio do Mana rolls only ever happen after an
 * already-separate, already-successful Magia casting roll; this Interaction only ever
 * computes Domínio do Mana's own bonus/difficultyReduction, never that separate casting roll,
 * which this core doesn't model yet (no {@code Magia} entity or casting-resolution engine
 * exists) — see {@link org.aventyrs.core.magic.SpellCastingService}. If the character has a
 * {@code SkillCompetencyAbility} for this same skill whose {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * DominioDoManaCompetencyAbility.MAGIA_SELVAGEM}), that Attribute is used in place of Domínio
 * do Mana's normal Foco — see {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class DominioDoManaInteraction extends AbstractSkillInteraction {

    public DominioDoManaInteraction() {
        super(SkillType.DOMINIO_DO_MANA);
    }

    public DominioDoManaInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.DOMINIO_DO_MANA, characterSkillService, modifierResolver);
    }
}
