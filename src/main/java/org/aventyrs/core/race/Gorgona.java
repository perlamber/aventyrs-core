package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Górgona race can do under each rule-set. Only one of this race's traits is
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Força, +1 Carisma) —
 * everything else needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Categoria de Tamanho 0</b> — no override needed, {@link
 *   org.aventyrs.core.character.SizeCategory#ZERO} is already {@link Character}'s own default,
 *   same treatment as {@code Human}/{@code Orc}/{@code Bestial}.</li>
 *   <li><b>Idiomas</b> (Continental + Élfico ou Arcano dialeto Feérico, à escolha do jogador) —
 *   same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~50 anos) — same "no age/lifespan concept" gap as every other race;
 *   purely narrative today.</li>
 *   <li><b>1 Talento adicional</b> (Monstruoso ou Racial) — same "no Feat catalog, no {@code
 *   Character.feats} list" gap as every other race's free Talentos; unlike most races' own
 *   version of this trait, the rules text here doesn't name one fixed category but a choice
 *   between {@code org.aventyrs.core.feat.FeatCategory#MONSTRUOSO} and "Talentos Raciais" in
 *   general (every {@code Type.RACIAL} constant — {@code MONSTRUOSO}/{@code FERRICO}/{@code
 *   ELFICO}/{@code BESTIAL} — not one specific one), which doesn't change the underlying gap
 *   but is flagged rather than silently narrowed to a guess.</li>
 * </ul>
 *
 * <p>None of this race's four Características Raciais fit {@code SkillCompetencyAbility}'s
 * shape well enough today to catalog in a {@code GorgonaRacialAbility} enum:
 *
 * <ul>
 *   <li><b>Abandonadas pelos Deuses</b> (Magias e Habilidades fortalecidas por maldições always
 *   affect Górgonas with amplified effects) — needs a "maldição" effect classification and an
 *   amplification mechanism, neither of which exists anywhere in this core (this codebase's
 *   familiar "doesn't track what an effect is *for*" gap, here for curses specifically rather
 *   than Encantamentos).</li>
 *   <li><b>Imunidade a Encantamentos</b> (+2 Bônus Racial em DM; immune to direct Encantamento
 *   effects, not indirect ones like an enchanted weapon's damage) — identical trait, wording,
 *   and mechanic to {@code Fada}'s/{@code Furia}'s own Imunidade a Encantamentos: DM (Defesa
 *   Mágica) isn't a concept this core computes at all (same gap {@code Elfo}'s own Linhagem
 *   Feérica already cites), and the immunity half needs the same missing Encantamento-effect
 *   classification (direct vs. indirect) neither of those two races' own version could reach
 *   either.</li>
 *   <li><b>Monstros em pele de Fada</b> (spend 2PA + 2PM to toggle between a Feérica form,
 *   granting +1 Carisma/+1 RM, and a Monstruosa form, granting +1 Força/+1 RD and a "Presas
 *   Longas" Arma Natural) — needs a "retain an activated stance for as long as desired" toggle-
 *   state concept this core has no equivalent of (same gap {@code Bestial}'s own Corpo Quimérico
 *   cites), plus RM (Redução Mágica) isn't a concept this core computes at all (unlike RD,
 *   which is real — {@code
 *   org.aventyrs.core.character.services.DamageService#getTotalDamageReduction} — but even RD
 *   can't be granted here since it would need to apply only while the toggle is in its
 *   Monstruosa state, which nothing tracks), and an Item/Equipamento entity for the Arma
 *   Natural itself (same gap {@code Gigantes}' own Tudo é Frágil and {@code
 *   ProfissaoCompetencyAbility#FORJA_VULCANA} cite).</li>
 *   <li><b>Olhar de Lacerto</b> (spend 2PA + 1PD for an Ataque Corpo-a-Corpo roll, em até
 *   Distância Muito Curta, against the target's DM, dealing 1d6+metade do Carisma Dano Mágico
 *   Elemental: Terra and a -2PA malus for 2 Rodadas; in Monstruosa form, gains the Corrente de
 *   Efeitos "Olhar Petrificador" — +1d6 dano, 2 Rodadas de petrificação, permanent
 *   petrification if it reduces the target to 0 PV or less, GD +1 nível per repeat attempt
 *   against the same target) — the single densest gap of any racial trait catalogued so far:
 *   {@code AtaqueCorpoACorpoInteraction} has no {@code attackTarget}-style parameter the way
 *   {@code AtaqueADistanciaInteraction} does (same "melee side isn't wired" gap CLAUDE.md's
 *   Abatedores de Gigantes section already flags), a target's DM lookup is "left to a layer
 *   above this core" (same as {@code Elfo}'s Linhagem Feérica), this core deliberately never
 *   rolls its own dice (the 1d6 itself is a caller's job, per the {@code skill} package-info) —
 *   {@link org.aventyrs.core.character.DamageType#ELEMENTAL} paired with {@link
 *   org.aventyrs.core.character.Element#TERRA} can now classify the dano itself as "Dano
 *   Mágico Elemental: Terra", but that alone doesn't unblock this trait given every other gap
 *   listed here — a temporary malus *inflicted on
 *   the target's* Pontos de Ação has no equivalent of {@code CharacterSheet
 *   #grantTemporaryBonus} (that mechanism only ever grants a bonus, self- or ally-side, never a
 *   malus on an opponent), Corrente de Efeitos is an entirely unbuilt system (same gap {@code
 *   AutocontroleAdvantage#RESOLUTO} already cites), the petrification's Encantamento
 *   classification needs the same missing piece as Imunidade a Encantamentos above, and the
 *   "GD aumentada em +1 nível por tentativa" clause needs a GD-*increase* expression and a
 *   per-target attempt counter, neither of which exist (this exact "GD-increase expression" gap
 *   is called out generically in CLAUDE.md's Range/SceneContext section, still with no concrete
 *   ability implementing it).</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core, same gap {@code
 *   Anao}'s/{@code Elfo}'s own Visão no Escuro already cite.</li>
 * </ul>
 *
 * <p>So {@link #getRacialAbilities()} is left at {@link Race}'s own empty default, same
 * reasoning as {@code Gigantes}/{@code Pequenino}/Humanos/{@code Gnomo}/{@code Orc}/{@code
 * Bestial}/{@code Fada}/{@code Furia}.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race — the
 * rules text's "muitos membros da Raça não se dão bem com" Fadas/Lacerto-aligned individuals
 * specifically (not a general Tendência skew) doesn't map onto {@link
 * Character#getTendencia()}'s plain 1-10 scale at all; this core has no faction/allegiance
 * concept to express a targeted dislike like this one either way (same "no
 * faction-relationship/allegiance concept" gap {@code Scene#getEnemies}'s own javadoc already
 * flags).
 */
public class Gorgona implements Race {

    /**
     * A judgment call, not a confirmed classification: Górgonas were originally Fadas, are
     * described as having "recuperando... graça feérica" recently, and the race's own "1
     * Talento adicional" trait offers Monstruoso *or* Talento Racial as alternatives (implying
     * her own Talento Racial category isn't itself Monstruoso) — so {@link CreatureType#FEERICO}
     * fits the text better than {@link CreatureType#MONSTRUOSO} despite her cursed,
     * monster-like physical traits. Flagged for confirmation rather than silently assumed.
     */
    @Override
    public CreatureType getCreatureType() {
        return CreatureType.FEERICO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.CHARISMA, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
