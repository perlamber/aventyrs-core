package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Guampos race can do under each rule-set — a stateless race, like {@code
 * Anao}/{@code Elfo}. Five of its traits are mechanically real today:
 *
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Gnose and +1 Instinto.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#MONSTRUOSO}; the rules text calls
 *   them "a única raça monstruosa bem quista no Império Elduriano", which settles it despite
 *   their standing.</li>
 *   <li><b>{@link #getBaseSizeCategory()}/{@link #generateEmptyCharacter}</b> — {@link
 *   SizeCategory#PLUS_ONE}.</li>
 *   <li><b>Vigor de Epona's PV half</b> ("+1 Multiplicador de PV") — seeded by {@link
 *   #generateEmptyCharacter} as {@code HitPointsService.DEFAULT_LIFE_MULTIPLIER + 1}, which
 *   {@code HitPointsServiceImpl#getLifeMultiplier} reads as its base. Same builder-seeding path
 *   as Categoria de Tamanho, and the same one {@code NascidoDoDragao}'s Armadura Dracônica
 *   uses.</li>
 *   <li><b>Vigor de Epona's RD half</b> ("reduzem em -1 todo dano sofrido") — {@link
 *   GuamposRacialAbility#VIGOR_DE_EPONA}, the first racial ability in this codebase to grant RD.
 *   See its javadoc for what that required.</li>
 * </ul>
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Benção Divina</b> (Resistência Elemental to every Elemento; the first Magia Elemental
 *   cast on an even Rodada costs -1PM, minimum 1PM) — two independent gaps. RE is not a stat this
 *   core computes at all, the same one {@code NascidoDoDragao}'s Escamas Cromática cites (and
 *   here it needs no element chosen, which makes the missing mechanism the <i>whole</i> of the
 *   clause). The PM discount has all three of its inputs available — {@code Spell#getManaCost},
 *   {@code SceneContext#getCurrentRound}, and the Magia's own {@code
 *   SpellTree#getElementalType()} — but {@code SpellCastingService#castSpell} has no cost step at
 *   all: it rolls the delivery Perícia and Domínio do Mana and spends nothing, so there is no
 *   figure to reduce, no "first cast this Rodada" counter, and no floor to clamp to.</li>
 *   <li><b>Chifres Majestosos</b> (Chifres Poderosos as an Arma Natural; investidas deal +1d6
 *   dano) — the Arma Natural half is the usual two-markers-missing gap (no weapon catalog,
 *   nothing marks a weapon as natural). The dano half needs an "investida"/charge classification
 *   that nothing models, and is expressed as a <i>die</i> rather than a flat figure, which {@code
 *   DamageBonus} cannot hold — this core never rolls dice, so a +1d6 has no representation
 *   distinct from the {@code DamageBase} scale, and Dano Base and a dano bonus deliberately never
 *   merge.</li>
 *   <li><b>Memória Eidética</b> (never forgets anything learned) and <b>Senso de Direção
 *   Apurado</b> (always identifies North; never lost in a labyrinth, natural or magical) — both
 *   are narrative guarantees rather than roll modifiers: neither grants Vantagem nor reduces a
 *   GD, they assert an outcome. This core has no auto-success hook for a Perícia roll at all (the
 *   same still-unbuilt piece {@code MedicinaECuraExcellency#FOCADO} and {@code
 *   AttentionCompetencyAbility#PERCEPCAO_DE_FOXM} both wait on), and no memory/knowledge state to
 *   assert against either.</li>
 *   <li><b>Idiomas</b> (Continental como idioma natural) — same "no Language/Idioma concept
 *   exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~150 anos) — same "no age/lifespan concept" gap as every other race;
 *   purely narrative today.</li>
 *   <li><b>Treinamento em Conhecimentos com as Especializações Cosmologia e Metamágico, uma
 *   Habilidade de Competência dessa Perícia, e um Talento de Sobrevivência ou Metamágico</b> —
 *   {@link Race} has no hook to grant starting Perícia training/abilities nor a {@code Feat} at
 *   creation, same gap as every other race's free Talentos/Especializações. Both Especializações
 *   named do exist as {@code ConhecimentosSpecialization} constants, so only the granting hook is
 *   missing here, not the vocabulary.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race.
 */
public class Guampo implements Race {

    private static final int VIGOR_DE_EPONA_LIFE_MULTIPLIER_BONUS = 1;

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.GNOSE, 1, AttributeDomain.INSTINCT, 1);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return SizeCategory.PLUS_ONE;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder()
                .sizeCategory(getBaseSizeCategory())
                .lifeMultiplier(HitPointsService.DEFAULT_LIFE_MULTIPLIER + VIGOR_DE_EPONA_LIFE_MULTIPLIER_BONUS);
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(GuamposRacialAbility.VIGOR_DE_EPONA);
    }
}
