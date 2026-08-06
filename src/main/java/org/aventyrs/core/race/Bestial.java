package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;

/**
 * Defines what the Bestial race can do under each rule-set. Unlike most other races, *nothing*
 * here is currently overridden — but that matches the rules text, not an oversight: Bestiais
 * explicitly have no Atributo bonus ("não possuem Bônus Raciais em Atributos") and Categoria de
 * Tamanho 0 ("também possuem Categoria de Tamanho 0", same baseline Humanos are), so {@link
 * Race}'s own defaults (empty fixed/choosable attribute bonuses, no override on {@link
 * #generateEmptyCharacter}) already express that correctly.
 *
 * <ul>
 *   <li><b>Talento Racial adicional concede ponto de Atributo</b> (acquiring a new Talento
 *   Racial may also grant a bonus Atributo point, "conforme a descrição") — same "no Feat
 *   catalog, no {@code Character.feats} list" gap as every other race's free Talentos/
 *   Especializações; this one compounds it with a per-Talento Atributo award that also has
 *   nowhere to attach.</li>
 *   <li><b>Idiomas</b> (Silvestre + um adicional per Antecedente, ambos trocáveis por Arcano
 *   dialeto Feérico ou Élfico) — same "no Language/Idioma concept exists" gap as every other
 *   race.</li>
 *   <li><b>Longevidade</b> (~60 anos) — same "no age/lifespan concept" gap as every other race;
 *   purely narrative today.</li>
 *   <li><b>1 Talento Bestial + 1 Talento adicional</b> (Geral ou Bestial) — same "no Feat
 *   catalog, no {@code Character.feats} list, {@link Race} has no hook for granting starting
 *   Perícia training" gap as every other race's free Talentos; {@code
 *   org.aventyrs.core.feat.FeatCategory#BESTIAL} already exists as a category (alongside
 *   {@code #MONSTRUOSO}/{@code #FERRICO}/{@code #ELFICO}), but {@code
 *   org.aventyrs.core.feat.Feat} still has no catalog of concrete named Feats within it.</li>
 *   <li><b>Corpo Quimérico</b> (spend 2PD, after touching a local animal, to copy its traits —
 *   retained for as long as desired — granting Vantagem on Atenção/Furtividade rolls in
 *   ambientes selvagens/não urbanos, and on Conhecimento na especialização Natureza) — spending
 *   PD itself is already supported ({@code
 *   org.aventyrs.core.sheet.CharacterSheet#spendDeterminationPoints}, same as {@code
 *   EmpatiaSelvagemCompetencyAbility#CHARME_FEERICO}), but everything else this needs is
 *   missing: a "retain an activated stance/transformation for as long as desired" toggle-state
 *   concept doesn't exist anywhere in this core; an urbano-vs-selvagem environment/terrain
 *   concept doesn't exist either (same gap {@code Anao}'s own Filhos da Montanha cites); and the
 *   Conhecimento half needs Especialização-scoped bonus resolution, which {@code
 *   org.aventyrs.core.skill.DifficultyLevel#reachedBy}'s own javadoc already flags as a
 *   separate, still-unbuilt concern (it deliberately ignores {@code expertValue}) — see {@code
 *   org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization#NATUREZA}. Whether "em
 *   ambientes selvagens" scopes only the Atenção/Furtividade pair or the Conhecimento bonus too
 *   is genuinely ambiguous in the source text itself; flagged, not guessed, same restraint
 *   {@code Gnomo}'s own "duas vs. três Perícias" inconsistency already documents.</li>
 *   <li><b>Falar com Animais</b> (spend 2PD to communicate with animals, limited by each
 *   animal's own intelligence, lasting a whole Cena) — this core has no notion of
 *   character-to-creature communication at all (nothing resembling it exists anywhere in this
 *   codebase, not even as a TODO on another ability), and no Cena-scoped duration tracking
 *   either (same gap {@code Gnomo}'s own Mente Veloz, Sorriso Veloz cites for Cena-scoped
 *   state). Spending PD is the only piece already real.</li>
 *   <li><b>Inocência Selvagem</b> (Desvantagem on Persuasão, exceto Intimidação, e em Atenção,
 *   exceto para resistir a Intimidações; Vantagem on Persuasão para Intimidação e em Atenção
 *   para resistir a Intimidações) — two separate gaps compound here: this core doesn't track
 *   what a roll is *for* (same "Intimidação" sub-purpose gap {@code
 *   PersuasaoCompetencyAbility#ESPALHAR_EMOCOES}/{@code #FORCA_OPRESSORA} already cite — a
 *   Persuasão or Atenção roll has no notion of "this one is/isn't an Intimidação"), so none of
 *   this trait's four sub-clauses can be granted without either leaking onto the wrong rolls or
 *   missing the right ones; and Desvantagem itself has no flat-bonus constant to mirror {@link
 *   org.aventyrs.core.skill.Skill#ADVANTAGE_BONUS} (this codebase's "Vantagem is a flat +2"
 *   convention has never needed a symmetric Desvantagem constant before, since no other
 *   ability grants a flat, unconditional Desvantagem today).</li>
 *   <li><b>Despertar Monstruoso</b> (2+ Talentos Bestiais changes creature type from Feérico to
 *   Monstruoso, unlocking Talentos Monstruosos and locking out further Talentos Feéricos) —
 *   {@link Character} has no creature-type field at all (Feérico/Monstruoso or otherwise), no
 *   {@code feats} list to count "2+ Talentos Bestiais" against in the first place (same gap as
 *   every race's free-Talento traits above), and no feat-eligibility-restriction service to
 *   enforce a category swap even once counting is possible — same unenforced-prerequisite
 *   treatment as every other "Requer N Graduações"-style clause elsewhere in this codebase
 *   would apply to the "Talentos Feéricos" lockout once it exists.</li>
 *   <li><b>Ego Primitivo</b> (never suffers Autocontrole loss from an enemy's abilities) — no
 *   mechanic for an *enemy's* ability to reduce a character's Autocontrole exists anywhere in
 *   this core yet ({@link org.aventyrs.core.ego.AutocontroleAdvantage}/{@code EgoAdvantage}
 *   only cover a character voluntarily *spending* their own Autocontrole points, never losing
 *   them to someone else's effect), so there's nothing yet for this trait to grant immunity
 *   against.</li>
 * </ul>
 *
 * <p>None of the racial traits above fit {@code SkillCompetencyAbility}'s shape well enough
 * today to catalog in a {@code BestialRacialAbility} enum (same reasoning as {@code Gigantes}/
 * {@code Pequenino}/Humanos/{@code Gnomo}/{@code Orc}) — several are acquisition-slot grants,
 * one needs a toggle-state/environment/Especialização combination this core can't express yet,
 * one needs a communication concept that doesn't exist, one spans multiple scoped Vantagem/
 * Desvantagem clauses this core can't distinguish, one needs a creature-type/feat-eligibility
 * concept, and one guards against a loss mechanic that doesn't exist — so {@link
 * #getRacialAbilities()} is left at {@link Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race: "quase
 * sempre Neutros... raramente assumem um Alinhamento de Tendência diferente" is advisory, not a
 * hard rule.
 */
public class Bestial implements Race {

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
