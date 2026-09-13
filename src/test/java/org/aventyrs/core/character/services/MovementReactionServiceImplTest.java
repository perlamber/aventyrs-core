package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who gets a Reação when someone moves past them.
 *
 * <p>The rule under test: an enemy threatens a mover if they have a <b>drawn</b> melee weapon (or
 * an Arma Natural, which cannot be sheathed) and the mover is within <b>their</b> attack range.
 * Ataque à Distância never threatens, however close it stands.
 *
 * <p>Every case here is driven through the service against real {@code SceneContext} distances and
 * real weapon Alcances rather than by stubbing a range — reach is the half of this rule most likely
 * to break, since it comes from {@code AttackRangeService} and varies by weapon and by the
 * threatener's own Categoria de Tamanho.
 */
class MovementReactionServiceImplTest {

    private final MovementReactionService movementReactionService = new MovementReactionServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon dagger() {
        return AbstractWeapon.builder()
                .name("Adaga").category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    /** Alcance Distância Muito Curta — the reach that makes the band test mean something. */
    private static Weapon spear() {
        return AbstractWeapon.builder()
                .name("Lança").category(ItemCategory.SPEAR)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .range(Range.DISTANCIA_MUITO_CURTA)
                .build();
    }

    private static Weapon bow() {
        return AbstractWeapon.builder()
                .name("Arco Curto").category(ItemCategory.BOW)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_MEDIA)
                .build();
    }

    private static CharacterSheet combatant() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .build(), new Player());
    }

    /** A combatant carrying weapons but with nothing in hand. */
    private static CharacterSheet carrying(final Weapon... weapons) {
        CharacterSheet sheet = combatant();
        for (Weapon weapon : weapons) {
            sheet.getCharacter().equip(weapon);
        }
        return sheet;
    }

    /** A combatant with those weapons carried <em>and</em> drawn. */
    private static CharacterSheet wielding(final Weapon... weapons) {
        CharacterSheet sheet = carrying(weapons);
        for (Weapon weapon : weapons) {
            sheet.getCharacter().drawWeapon(weapon);
        }
        return sheet;
    }

    private static SceneContext facing(final CombatantSheet enemy, final Range distance) {
        return new SceneContext(List.of(), List.of(enemy), Map.of(enemy, distance));
    }

    private List<CombatantSheet> reactorsTo(final CombatantSheet mover, final SceneContext context) {
        return movementReactionService.getProvokedReactors(mover, context, null);
    }

    // ---------- the baseline rule ----------

    @Test
    void anAdjacentEnemyWithADrawnMeleeWeaponMayReact() {
        CharacterSheet foe = wielding(dagger());

        assertEquals(List.of(foe), reactorsTo(combatant(), facing(foe, Range.ADJACENTE)));
    }

    /**
     * "Utilizando uma arma" means <b>drawn</b> throughout this core, so a sword still on the belt
     * threatens nobody. This is the single case most likely to be got wrong by reading
     * {@code getEquipment()} instead of {@code getDrawnWeapons()}.
     */
    @Test
    void aCarriedButSheathedWeaponThreatensNobody() {
        CharacterSheet foe = carrying(dagger());

        assertTrue(reactorsTo(combatant(), facing(foe, Range.ADJACENTE)).isEmpty());
    }

    /** "Isto não se aplica para Ataque à Distância" — the archer is not a threat at any band. */
    @Test
    void aDrawnRangedWeaponNeverThreatens() {
        CharacterSheet archer = wielding(bow());

        assertTrue(reactorsTo(combatant(), facing(archer, Range.ADJACENTE)).isEmpty());
        assertTrue(reactorsTo(combatant(), facing(archer, Range.DISTANCIA_CURTA)).isEmpty());
    }

    /**
     * An Arma Natural is part of a body: it is never in the equipment list, so it can never be
     * drawn, so requiring it to be drawn would silence every clawed creature in the game.
     */
    @Test
    void anArmaNaturalThreatensWithoutBeingDrawn() {
        CharacterSheet beast = combatant();
        beast.getCharacter().grantFeat(new NaturalWeaponFeat(NaturalWeapon.GARRAS_AFIADAS));

        assertEquals(List.of(beast), reactorsTo(combatant(), facing(beast, Range.ADJACENTE)));
    }

    // ---------- reach ----------

    /**
     * Reach is the threatener's own, read off their weapon — so a Lança covers a band an adaga
     * does not, and the mover is threatened at a distance where the dagger-wielder cannot touch
     * them.
     */
    @Test
    void reachIsTheThreatenersOwnWeaponRange() {
        CharacterSheet spearman = wielding(spear());
        CharacterSheet knifeman = wielding(dagger());

        assertEquals(List.of(spearman),
                reactorsTo(combatant(), facing(spearman, Range.DISTANCIA_MUITO_CURTA)));
        assertTrue(reactorsTo(combatant(), facing(knifeman, Range.DISTANCIA_MUITO_CURTA)).isEmpty());
    }

    @Test
    void anEnemyBeyondEveryReachDoesNotThreaten() {
        CharacterSheet foe = wielding(spear());

        assertTrue(reactorsTo(combatant(), facing(foe, Range.DISTANCIA_LONGA)).isEmpty());
    }

    // ---------- "cannot tell" ----------

    /** With no Scene there are no positions, and "cannot tell" reads as nobody, not everybody. */
    @Test
    void noSceneContextMeansNoOpportunity() {
        assertTrue(movementReactionService.getProvokedReactors(combatant(), null, Manoeuvre.INVESTIDA)
                .isEmpty());
    }

    // ---------- exemptions ----------

    /**
     * {@code DexterityAbility#IMPLACAVEL} — "suas investidas não provocam Reações". Scoped to that
     * manoeuvre: the same holder walking ordinarily is threatened exactly as anyone else is, which
     * is what stops a manoeuvre clause from quietly becoming a permanent one.
     */
    @Test
    void implacavelExemptsOnlyItsHoldersInvestidas() {
        Character relentless = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributeAbility(DexterityAbility.IMPLACAVEL)
                .build();
        CharacterSheet mover = CharacterSheet.of(relentless, new Player());
        CharacterSheet foe = wielding(dagger());
        SceneContext context = facing(foe, Range.ADJACENTE);

        assertTrue(movementReactionService.getProvokedReactors(mover, context, Manoeuvre.INVESTIDA)
                .isEmpty());
        assertEquals(List.of(foe),
                movementReactionService.getProvokedReactors(mover, context, null));
    }

    /** A character without the Habilidade provokes on an Investida like any other movement. */
    @Test
    void anOrdinaryChargerProvokes() {
        CharacterSheet foe = wielding(dagger());

        assertEquals(List.of(foe), movementReactionService.getProvokedReactors(
                combatant(), facing(foe, Range.ADJACENTE), Manoeuvre.INVESTIDA));
    }

    /**
     * A homebrew {@code Feat} granting one Arma Natural — the extension point {@code AbstractFeat}
     * exists for. Used here rather than a race so the test says exactly what it depends on.
     */
    private static final class NaturalWeaponFeat extends org.aventyrs.core.feat.AbstractFeat {
        private final NaturalWeapon weapon;

        private NaturalWeaponFeat(final NaturalWeapon weapon) {
            super(org.aventyrs.core.feat.FeatCategory.MOBILIDADE, "Arma Natural de teste",
                    org.aventyrs.core.feat.FeatRequirements.builder().build());
            this.weapon = weapon;
        }

        @Override
        public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
            return List.of(weapon);
        }
    }
}
