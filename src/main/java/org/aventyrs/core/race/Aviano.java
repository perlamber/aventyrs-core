package org.aventyrs.core.race;

import org.aventyrs.core.character.MovementMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatPool;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Avianos race can do under each rule-set. Like {@code Agastias}' Linhagem, an
 * Aviano carries one acquisition-time choice — its {@link Subtipo}, "escolhido no momento da
 * criação do personagem, feita a escolha não é possível alterá-la" — so this is an instance-based
 * race with a constructor field, the {@code ArtesAprimorarComArteAbility} pattern rather than
 * {@code AcquiredChoice} (the choice feeds this object's own {@link #getFixedAttributeBonuses()}).
 * It is <b>not</b> a Mestiço: nothing about it picks a parent {@link Race}.
 *
 * <p>Three of this race's traits are mechanically real today:
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Destreza for every Aviano, plus the
 *   chosen {@link Subtipo}'s own: +1 Força for a {@link Subtipo#RAPINANTE}, +1 Vigor for a
 *   {@link Subtipo#CORRENUVENS}.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#MONSTRUOSO}. The rules text says
 *   so outright ("apesar de serem considerados monstros"), and the Império's hostility "a raças
 *   não humanoides" settles the other direction.</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — {@link
 *   AvianosRacialAbility#VISAO_ALEM_DO_ALCANCE}'s Vantagem em Atenção, with the "percepções
 *   visuais" narrowing documented there as an accepted simplification.</li>
 * </ul>
 *
 * <p>Categoria de Tamanho 0 needs no override — {@link
 * org.aventyrs.core.character.SizeCategory#ZERO} is already {@link Character}'s own default, same
 * treatment as {@code Human}/{@code Orc}/{@code Gorgona}. Everything else needs a system this
 * core doesn't have yet:
 *
 * <ul>
 *   <li><b>Movimento Base while flying</b> (+2UD for a Rapinante, +1UD for a Correnuvens) — <b>real</b>,
 *   on the flight axis ({@link #resolveModeMovementIncrease}): ⚠️ "enquanto voando seu Movimento
 *   Base aumenta" is read as the Movimento Base de Voo, the figure used while flying, never as
 *   ground movement.</li>
 *   <li><b>Braços Alados</b> (asas in place of arms; both hands count as "membros inábeis para
 *   rolagens de Perícia"; flying in a Cena de Combate costs 1PD and lasts 1d6 + metade da Destreza
 *   Rodadas) — the flight itself is <b>real</b> ({@link #grantsMovementMode}); still missing: no
 *   limb/anatomy concept to mark a member inábil and no per-Perícia penalty keyed on one, flying
 *   as a timed state (whether an Aviano is flying is the caller's {@code EnvironmentalState}), and no
 *   "spend a resource to enter a timed state" transaction (Pontos de Determinação are spent
 *   through {@code CombatantSheet}, but nothing converts a spend into a {@code TemporaryEffect}
 *   the way this needs — the same gap {@code Gorgona}'s Monstros em pele de Fada cites). This
 *   core also never rolls the 1d6 itself.</li>
 *   <li><b>Pés Hábeis e Poderosos</b> (holding items and performing manual actions with the feet)
 *   — the flip side of Braços Alados' penalty, so it is blocked on the same missing limb concept;
 *   with neither modeled, the pair currently nets out to no mechanical effect at all.</li>
 *   <li><b>Garras Afiadas</b> (the feet count as the Arma Natural "Garras Afiadas") — {@code
 *   org.aventyrs.core.item.Weapon} exists, but no weapon <i>catalog</i> does (only {@code
 *   ArmorItem} is authored), and nothing marks a weapon as an Arma Natural in the first place —
 *   the same two-markers-missing gap CLAUDE.md's "Classifying an attack as Desarmado/Arma
 *   Natural" row names.</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core, same gap {@code
 *   Anao}'s/{@code Elfo}'s own already cite. Carried in {@link
 *   AvianosRacialAbility#VISAO_ALEM_DO_ALCANCE}'s description text rather than dropped.</li>
 *   <li><b>Idiomas</b> (dialeto Aviano, Continental, e um adicional per Antecedente) — same "no
 *   Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~100 anos) — same "no age/lifespan concept" gap as every other race.</li>
 *   <li><b>1 Talento Geral adicional + Ossos Ocos como Talento adicional + uma Especialização e
 *   uma Habilidade de Competência em até uma mesma Perícia treinada</b> — both Talentos are built
 *   ({@link #getStartingFeatSlots()}); Ossos Ocos is {@code MonstruosoFeat#OSSOS_OCOS}, offered as a
 *   single-option slot. The Perícia half is the same "no hook for granting starting Perícia
 *   training" gap as every other race.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "costumam ser Neutros" is advisory, not a hard rule.
 */
@Getter
public class Aviano implements Race {

    /**
     * The two Avianos subtypes, each fixing the second half of {@link
     * #getFixedAttributeBonuses()}. A nested enum on the race, following {@code
     * Agastias.Linhagem}'s own shape — the choice space is small, fixed at compile time, and
     * meaningless outside this race.
     *
     * <p>Each carries the flight-Movimento figure its rules text names, read by {@link
     * Aviano#resolveModeMovementIncrease}.
     */
    @Getter
    @AllArgsConstructor
    public enum Subtipo {

        /** "Mais fortes e raramente possuem bicos... favorece voos curtos e rápidos." */
        RAPINANTE(AttributeDomain.STRENGTH, 2),

        /** "Corpos mais resistentes, capazes de voar por horas sem parar." */
        CORRENUVENS(AttributeDomain.VIGOR, 1);

        private final AttributeDomain attributeBonus;

        /** "Enquanto voando seu Movimento Base aumenta em +NUD". */
        private final int flightMovementBonus;
    }

    private final Subtipo subtipo;

    public Aviano(@NonNull final Subtipo subtipo) {
        this.subtipo = subtipo;
    }

    /**
     * Braços Alados — "os utilizam para voar livremente". The Movimento Base de Voo is real; the
     * 1PD activation in a Cena de Combate and its 1d6 + metade da Destreza Duração are not (whether
     * the Aviano is flying right now is the caller's {@code EnvironmentalState#flying}).
     */
    @Override
    public boolean grantsMovementMode(final MovementMode mode) {
        return mode == MovementMode.FLIGHT;
    }

    /** The subtype's "enquanto voando seu Movimento Base aumenta em +2UD/+1UD". */
    @Override
    public int resolveModeMovementIncrease(final MovementMode mode) {
        return mode == MovementMode.FLIGHT ? subtipo.getFlightMovementBonus() : 0;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.DEXTERITY, 1, subtipo.getAttributeBonus(), 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE);
    }

    @Override
    public List<StartingFeatSlot> getStartingFeatSlots() {
        return List.of(
                StartingFeatSlot.race(FeatPool.Categories.ofType(FeatCategory.Type.GERAL)),
                StartingFeatSlot.race(FeatPool.Named.of(MonstruosoFeat.OSSOS_OCOS)));
    }
}
