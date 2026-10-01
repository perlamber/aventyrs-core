package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EnrijecerMusculatura;
import org.aventyrs.core.effect.ExplosaoCataclismica;
import org.aventyrs.core.effect.FeridaInfecciosa;
import org.aventyrs.core.effect.Oprimir;
import org.aventyrs.core.effect.VenenoVampirico;
import org.aventyrs.core.feat.FeralFeat;
import org.aventyrs.core.feat.FormaMetamorfica;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.feat.MetamorfoseDraculeaFeat;
import org.aventyrs.core.feat.SobrevivenciaFeat;
import org.aventyrs.core.feat.TerrenoPrediletoFeat;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each Talento that grants a Corrente de Efeitos (core 0.0.85) hands it to a real attack: {@code AttackDelivery} chains
 * it behind the damage once the Corrente threshold is cleared, and leaves it off otherwise.
 */
class CorrenteConsumersTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** Força 6 and Graduação 6 in Ataque Corpo-a-Corpo, so a 6-6-6 clears any Corrente threshold. */
    private static Character.CharacterBuilder fighter() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(6);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill);
    }

    private DeliveredAttackResult attack(final CharacterSheet attacker, final AttackSource source,
                                         final List<Integer> dice, final Scene scene) {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        DeliveredAttack.DeliveredAttackBuilder builder = DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(source)
                .attackRoll(new SkillRoll(dice));
        if (scene != null) {
            scene.addParticipant(attacker, 10);
            builder.sceneContext(scene.buildContext(attacker, Map.of()));
        }
        return attackDelivery.resolve(builder.build());
    }

    /** Every stage behind the attack's damage, in order. */
    private static List<Interaction<CombatantSheet>> stages(final DeliveredAttackResult result) {
        List<Interaction<CombatantSheet>> stages = new ArrayList<>();
        Interaction<CombatantSheet> next = result.getAttackResult().getNextInteraction().getNextInteraction();
        while (next != null) {
            stages.add(next);
            next = next.getNextInteraction();
        }
        return stages;
    }

    private static boolean carries(final DeliveredAttackResult result, final Class<?> corrente) {
        return stages(result).stream().anyMatch(corrente::isInstance);
    }

    @Test
    void marcaDaMaldicaosPresasLongasCarryEnrijecerMusculatura() {
        Character gorgona = fighter().build();
        gorgona.grantFeat(GorgonaFeat.MARCA_DA_MALDICAO);
        CharacterSheet sheet = CharacterSheet.of(gorgona, new Player());

        assertTrue(gorgona.getNaturalWeapons().contains(NaturalWeapon.PRESAS_LONGAS));
        assertTrue(carries(attack(sheet, NaturalWeapon.PRESAS_LONGAS, List.of(6, 6, 6), null),
                EnrijecerMusculatura.class));
        assertFalse(carries(attack(sheet, NaturalWeapon.GARRAS_AFIADAS, List.of(6, 6, 6), null),
                EnrijecerMusculatura.class));
    }

    @Test
    void desprezoNaturalsArmasNaturaisCarryFeridaInfecciosa() {
        Character feral = fighter().build();
        feral.grantFeat(GorgonaFeat.MARCA_DA_MALDICAO);   // for a pair of Presas Longas to bite with
        feral.grantFeat(FeralFeat.DESPREZO_NATURAL);
        CharacterSheet sheet = CharacterSheet.of(feral, new Player());

        assertTrue(carries(attack(sheet, NaturalWeapon.PRESAS_LONGAS, List.of(6, 6, 6), null),
                FeridaInfecciosa.class));
        assertFalse(carries(attack(sheet, null, List.of(6, 6, 6), null), FeridaInfecciosa.class),
                "an Ataque Desarmado names no Arma Natural");
    }

    /** "Nestes terrenos seus ataques recebem a Corrente de Efeitos – Oprimir" — 2 on the critical. */
    @Test
    void mestreDeCacaOppressesOnlyInTheChosenTerrain() {
        Character hunter = fighter().build();
        hunter.grantFeat(TerrenoPrediletoFeat.of(TerrainType.FOREST));
        hunter.grantFeat(SobrevivenciaFeat.MESTRE_DE_CACA);
        CharacterSheet sheet = CharacterSheet.of(hunter, new Player());
        Scene forest = new Scene();
        forest.setTerrainType(TerrainType.FOREST);
        Scene cave = new Scene();
        cave.setTerrainType(TerrainType.CAVE);

        DeliveredAttackResult inForest = attack(sheet, null, List.of(6, 6, 6), forest);

        Oprimir oprimir = stages(inForest).stream().filter(Oprimir.class::isInstance).map(Oprimir.class::cast)
                .findFirst().orElseThrow();
        assertTrue(oprimir.isCritical());
        assertEquals(Oprimir.CRITICAL_STEAL, oprimir.steal());
        assertFalse(carries(attack(sheet, null, List.of(6, 6, 6), cave), Oprimir.class));
    }

    @Test
    void serpenteEspinhosasCaudaCarriesVenenoVampiricoWhileTheShapeIsWorn() {
        Character vampiro = fighter().race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        vampiro.grantFeat(MetamorfoseDraculeaFeat.of(vampiro,
                EnumSet.of(FormaMetamorfica.SERPENTE_ESPINHOSA, FormaMetamorfica.NEVOA)));
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());

        assertFalse(carries(attack(sheet, NaturalWeapon.CAUDA_CONSTRITORA, List.of(6, 6, 6), null),
                VenenoVampirico.class), "out of the shape");

        sheet.enterForm(FormType.SERPENTE_ESPINHOSA);
        assertTrue(carries(attack(sheet, NaturalWeapon.CAUDA_CONSTRITORA, List.of(6, 6, 6), null),
                VenenoVampirico.class));
    }

    /** "… e Cataclismo como um Efeito Crítico adicional" — on the critical a triggered Explosão rides. */
    @Test
    void explosaoCataclismicaAddsCataclismoToTheCritical() {
        CharacterSheet hero = CharacterSheet.of(fighter().build(), new Player());
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .effectChain(new ExplosaoCataclismica())
                .build());

        assertTrue(carries(result, ExplosaoCataclismica.class));
        assertTrue(stages(result).stream().anyMatch(stage -> stage instanceof org.aventyrs.core.effect.CriticalEffect effect
                        && effect.getType() == CriticalEffectType.CATACLISMO)
                        || result.getUnappliedCriticalEffects().contains(CriticalEffectType.CATACLISMO),
                "Cataclismo is applied, or reported for want of its dice");
    }
}
