package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Furia race can do under each rule-set. Only one of this race's traits is
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Carisma, +1 Foco) —
 * everything else needs a system this core doesn't have yet. Fúrias are, per the rules text
 * itself, still Fadas in most respects ("eram Fadas originalmente, de certa forma ainda são"),
 * so this race shares essentially every trait and every gap with {@link Fada} — see that
 * class's own javadoc for Categoria de Tamanho, Idiomas, Longevidade, and Perícias e Talentos
 * (identical between the two); this javadoc instead covers the shared Características Raciais
 * in full, since those apply to both races equally (Natureza Mágica aside, see below).
 *
 * <ul>
 *   <li><b>Feromônio Encantador</b> (spend 1PD as an Ação Livre for Vantagem on Persuasão ou
 *   Artes rolls against targets at Distância Curta, for 1 Rodada; an Encantamento effect) —
 *   three of this trait's four pieces are individually real today: spending PD ({@code
 *   org.aventyrs.core.sheet.CombatantSheet#spendDeterminationPoints}), proximity ({@code
 *   org.aventyrs.core.scene.SceneContext#getDistanceTo}/{@code Range.DISTANCIA_CURTA}), and a
 *   Rodada-scoped bonus ({@code CombatantSheet#grantTemporaryBonus}'s {@code rounds}
 *   parameter) — but nothing in this core can *combine* them into a self-targeted, opt-in
 *   activation: every existing use of {@code grantTemporaryBonus} is the *result* of a skill
 *   roll ({@code ArtesCompetencyAbility#DOM_BARDICO}, via {@code InteractionResult}'s
 *   temporary-bonus fields), not a standalone Ação Livre a character can trigger outside of
 *   any roll. Nor do {@code PersuasaoInteraction}/{@code ArtesInteraction} take an {@code
 *   attackTarget}-style parameter the way {@code AtaqueADistanciaInteraction} does, so even a
 *   granted bonus would have nothing to check "against a target at Distância Curta" against
 *   for those two specific skills. Being an "efeito de Encantamento" also ties into Imunidade a
 *   Encantamentos below — neither trait has an Encantamento-effect classification to plug
 *   into.</li>
 *   <li><b>Natureza Mágica</b> (mimic a spell at 2PD without knowing it — Fadas mimic 'Piscar',
 *   Fúrias mimic 'Transformação Monstruosa', restricted to female Humanoid targets of Categoria
 *   de Tamanho 0 ou inferior) — needs a Magia entity with a concrete catalog to look either
 *   named spell up in, which doesn't exist yet ({@code
 *   org.aventyrs.core.magic.SpellCastingService}'s own "No Magia entity/list exists yet"), plus
 *   a "cast without knowing/training" override this core has no equivalent of. The PD-cost half
 *   is otherwise the same already-real piece Feromônio Encantador and {@code
 *   EmpatiaSelvagemCompetencyAbility#CHARME_FEERICO} share.</li>
 *   <li><b>Magia Natural</b> (casting characters spend 0.5 EXP less to learn additional Natural-
 *   type spells) — same missing Magia entity as Natureza Mágica, plus this core has no
 *   spell-*learning*-cost system at all (only {@code SkillGraduationService}/{@code
 *   CharacterAttributeService}'s upgrade-cost formulas exist, neither of which concerns
 *   learning a new spell) — a different missing system than the Feat-cost discount below, not
 *   the same gap twice.</li>
 *   <li><b>Conexão com o Mana</b> (Talentos Metamágicos custam 2.5 EXP ao invés de 3) —
 *   identical trait, same name, and same mechanic as {@code Elfo}'s own Conexão com o Mana: {@link
 *   Race#getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code int}, which
 *   can't represent a genuinely fractional 2.5 — the same int-vs-fractional mismatch already
 *   flagged there (and on Humanos'/{@code Pequenino}'s own Adaptação).</li>
 *   <li><b>Imunidade a Encantamentos</b> (+2 Bônus Racial em DM; immune to direct Encantamento
 *   effects, but not indirect ones like damage from an enchanted weapon) — DM (Defesa Mágica)
 *   is now computable — {@code ModifierType#MAGIC_DEFENSE} plus {@code DefenseService} — so the
 *   flat +2 needs only a {@code *RacialAbility} enum to hang it on, the same state {@code
 *   Elfo}'s own Linhagem Feérica is in; the immunity half also needs an Encantamento-effect
 *   classification (direct vs. indirect) that doesn't exist anywhere in this core, the same
 *   missing piece Feromônio Encantador's own "efeito de Encantamento" tag needs.</li>
 * </ul>
 *
 * <p>None of the five Características Raciais above fit {@code SkillCompetencyAbility}'s shape
 * well enough today to catalog in a {@code FuriaRacialAbility} enum (or a shared one with
 * {@code Fada}, since neither race can grant any of them yet either) — so {@link
 * #getRacialAbilities()} is left at {@link Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "frequentemente assumem tendências de alinhamento malignos ou neutro" is advisory, not a hard
 * rule, same as {@link Fada}'s own more benevolent advisory text.
 */
public class Furia implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.FEERICO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
