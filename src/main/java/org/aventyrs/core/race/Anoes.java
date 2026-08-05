package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Anões (Dwarves) race can do under each rule-set. Three of this race's
 * traits are mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Vigor, +1
 * Gnose), {@link #generateEmptyCharacter} seeding {@link SizeCategory#MINUS_ONE}, and {@link
 * #getRacialAbilities()} granting {@link AnoesRacialAbility#ABATEDORES_DE_GIGANTES} — everything
 * else the rules text describes needs a system this core doesn't have yet, same "model it as
 * real data, defer the mechanic honestly" discipline as every ability enum in this codebase:
 *
 * <ul>
 *   <li><b>Idiomas</b> (Anão + a second per Antecedente, swappable for Continental) — no
 *   Language/Idioma concept exists anywhere in this core.</li>
 *   <li><b>Longevidade</b> (~300 anos) — no age/lifespan concept exists on {@link Character}
 *   or {@link Race}; purely narrative today.</li>
 *   <li><b>Talento gratuito</b> (Sobrevivência ou Destino) — {@code
 *   org.aventyrs.core.feat.FeatCategory#SOBREVIVENCIA}/{@code #DESTINO} exist as categories,
 *   but {@code org.aventyrs.core.feat.Feat} has no catalog of concrete named Feats within a
 *   category to grant one of (it's just a bare {@code featCategory} field), and {@link
 *   Character} has no {@code feats} list to hold one in the first place.</li>
 *   <li><b>Treinamento em Profissão</b>, with an extra Especialização chosen between {@code
 *   org.aventyrs.core.skill.profissao.ProfissaoSpecialization#ALVENARIA_E_CARPINTARIA} and
 *   {@code #METALURGIA} — the Perícia/Especialização vocabulary this needs already exists, but
 *   {@link Race} has no hook for granting starting Perícia training, and {@code
 *   org.aventyrs.core.character.services.CharacterCreationServiceImpl} only allocates
 *   Attributes/Egos today, not skills.</li>
 *   <li><b>Visão no Escuro</b> (monochromatic darkvision) — no vision/senses concept exists in
 *   this core.</li>
 *   <li><b>Filhos da Montanha</b> (Vantagem on Conhecimentos rolls, Natureza Especialização,
 *   while in mountains/caves) — needs both a terrain/environment concept this core doesn't
 *   track (same "doesn't track what a roll is for" gap documented for scoped Vantagem
 *   elsewhere) and Especialização-scoped bonus resolution, which {@code
 *   org.aventyrs.core.skill.DifficultyLevel#reachedBy}'s own javadoc already flags as a
 *   separate, still-unbuilt concern (it deliberately ignores {@code expertValue}). See {@code
 *   org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization#NATUREZA}.</li>
 *   <li><b>Pequenos Gigantes</b> (Vigor Base 3+ grants one extra {@code
 *   org.aventyrs.core.ability.AttributeAbility}, restricted to {@code
 *   org.aventyrs.core.ability.VigorAbility#SANGUE_DE_GIGANTE}/{@code #SOBRE_HUMANO}) — {@code
 *   org.aventyrs.core.character.services.AttributeAbilityService#getUnlockedAbilitySlots}
 *   already unlocks a slot at Vigor base 3, but only counts slots from the raw base, with no
 *   notion of a race-granted *extra* slot, and no way to restrict a slot's choice to a named
 *   subset of one Attribute's abilities. Same unenforced-prerequisite treatment as every
 *   "Requer N Graduações"-style clause elsewhere in this codebase applies to the "Vigor Base 3
 *   ou mais" condition itself — no eligibility-validation service exists for this either.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained: the rules text only says Anões "raramente"
 * pick extremist tendências (Samaritano/Destruidor) — advisory, not a hard rule — so {@link
 * Character#getTendencia()} stays a plain, unvalidated 1-10 value for this race like any
 * other.
 */
public class Anoes implements Race {

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.VIGOR, 1, AttributeDomain.GNOSE, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(SizeCategory.MINUS_ONE);
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES);
    }
}
