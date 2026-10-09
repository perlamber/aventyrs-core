package org.aventyrs.core.subordinate;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Subordinados (core 0.0.92) — {@code docs/rules/subordinados.txt}. */
class SubordinateTest {

    private final SubordinateService service = new SubordinateServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet commander(final int charisma) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(charisma).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static SceneContext combat(final CharacterSheet... allies) {
        return new SceneContext(List.of(allies), List.of(), Map.of(), TerrainType.FOREST, true, 1, false, null);
    }

    private static SceneContext calm() {
        return new SceneContext(List.of(), List.of(), Map.of(), TerrainType.FOREST, false, 0, false, null);
    }

    // ---------- command ----------

    /** "uma quantidade de Subordinados igual ao seu Carisma … não podem possuir Subordinados do mesmo tipo". */
    @Test
    void commandRespectsCarismaAndOneOfEachGradeUnlessProdigioso() {
        CharacterSheet sheet = commander(2);
        service.command(sheet, Subordinate.of(SubordinateBenefit.TORRE_DEFESAS, false, "a"), null);

        IllegalOperationException sameGrade = assertThrows(IllegalOperationException.class,
                () -> service.command(sheet, Subordinate.of(SubordinateBenefit.TORRE_RA, false, "b"), null));
        assertEquals(TranslatableMessages.SUBORDINATE_GRADE_HELD, sameGrade.getMessage());

        service.command(sheet, Subordinate.of(SubordinateBenefit.TORRE_RA, true, "c"), null);
        IllegalOperationException full = assertThrows(IllegalOperationException.class,
                () -> service.command(sheet, Subordinate.of(SubordinateBenefit.PEAO_SKILL, false, "d"), null));
        assertEquals(TranslatableMessages.SUBORDINATE_LIMIT_REACHED, full.getMessage());
    }

    // ---------- benefits ----------

    /** Torres "fornecem … +2 nas Defesas … ou RA", only in combat. */
    @Test
    void aTorreDefendsOnlyInCombat() {
        CharacterSheet sheet = commander(3);
        int calmDefense = new DefenseServiceImpl().getTotalDefense(sheet, DefenseType.PHYSICAL, calm());
        int combatRa = new DamageServiceImpl().getTotalAbsoluteDamageReduction(sheet, combat());
        service.command(sheet, Subordinate.of(SubordinateBenefit.TORRE_DEFESAS, false, "torre"), null);
        service.command(sheet, Subordinate.of(SubordinateBenefit.TORRE_RA, true, "torre2"), null);

        assertEquals(calmDefense, new DefenseServiceImpl().getTotalDefense(sheet, DefenseType.PHYSICAL, calm()));
        assertEquals(calmDefense + 2, new DefenseServiceImpl().getTotalDefense(sheet, DefenseType.PHYSICAL, combat()));
        assertEquals(combatRa + 2, new DamageServiceImpl().getTotalAbsoluteDamageReduction(sheet, combat()));
    }

    /** A Cavaleiro's Vantagem on Ataques in combat; a Peão's on anything else but Esquiva, always. */
    @Test
    void cavaleiroAndPeaoGiveVantagemWhereTheyReach() {
        CharacterSheet sheet = commander(3);
        int attack = roll(sheet, SkillType.ATAQUE_CORPO_A_CORPO, combat());
        int attention = roll(sheet, SkillType.ATTENTION, calm());
        int dodge = roll(sheet, SkillType.ESQUIVA_E_APARAR, calm());
        service.command(sheet, Subordinate.of(SubordinateBenefit.CAVALEIRO_ATTACK, false, "c"), null);
        service.command(sheet, Subordinate.of(SubordinateBenefit.PEAO_SKILL, false, "p"), null);

        assertEquals(attack + Skill.ADVANTAGE_BONUS, roll(sheet, SkillType.ATAQUE_CORPO_A_CORPO, combat()));
        assertEquals(attention + Skill.ADVANTAGE_BONUS, roll(sheet, SkillType.ATTENTION, calm()));
        assertEquals(dodge, roll(sheet, SkillType.ESQUIVA_E_APARAR, calm()), "no Peão on Esquiva e Aparar");
    }

    private static int roll(final CharacterSheet sheet, final SkillType skill, final SceneContext context) {
        return skill.newInteraction().applyTo(sheet, context, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
    }

    @Test
    void aRainhaGivesAPontoDeAcao() {
        CharacterSheet sheet = commander(3);
        int before = new ActionPointsServiceImpl().getMaxActionPoints(sheet, 1, calm());

        service.command(sheet, Subordinate.of(SubordinateBenefit.RAINHA_ACTION_POINT, false, "r"), null);

        assertEquals(before + 1, new ActionPointsServiceImpl().getMaxActionPoints(sheet, 1, calm()));
    }

    /** A Rei: "2 Pontos Temporários em Ego … renovando os bônus … após … Descansos Longos". */
    @Test
    void aReiGrantsEgoAtOnceAndAgainAfterALongRest() {
        CharacterSheet sheet = commander(3);
        int sorte = sheet.getTemporaryEgoPoints(EgoDomain.SORTE);

        service.command(sheet, Subordinate.of(SubordinateBenefit.REI_SORTE, false, "rei"), null);
        assertEquals(sorte + 2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.spendEgoPoints(EgoDomain.SORTE, org.aventyrs.core.sheet.EgoPointType.TEMPORARY, 2);
        new RestServiceImpl().applyRest(sheet.getCharacter(), sheet, RestType.LONGO);
        assertEquals(true, sheet.getTemporaryEgoPoints(EgoDomain.SORTE) >= sorte + 2);
    }

    /** "Subordinados Prodigiosos concedem seus benefícios a todos os Personagens do Grupo". */
    @Test
    void aProdigiosoReachesAllies() {
        CharacterSheet commander = commander(3);
        CharacterSheet ally = commander(1);
        int allyDefense = new DefenseServiceImpl().getTotalDefense(ally, DefenseType.PHYSICAL, combat(commander));

        service.command(commander, Subordinate.of(SubordinateBenefit.TORRE_DEFESAS, true, "torre"), null);
        assertEquals(allyDefense + 2, new DefenseServiceImpl().getTotalDefense(ally, DefenseType.PHYSICAL, combat(commander)));

        CharacterSheet other = commander(3);
        service.command(other, Subordinate.of(SubordinateBenefit.TORRE_DEFESAS, false, "common"), null);
        assertEquals(allyDefense + 2, new DefenseServiceImpl().getTotalDefense(ally, DefenseType.PHYSICAL,
                combat(commander, other)), "a common one serves its commander alone");
    }

    /** A Bispo's "2PV por Rodada", at each Rodada boundary of a Cena de Combate. */
    @Test
    void aBispoHealsEachRodadaInCombat() {
        CharacterSheet sheet = commander(3);
        sheet.applyDamage(6);
        service.command(sheet, Subordinate.of(SubordinateBenefit.BISPO_REGENERATION, false, "bispo"), null);
        Scene scene = new Scene();
        scene.addParticipant(sheet, 10, UUID.randomUUID());
        scene.startCombat();

        scene.next();   // its first Turn
        scene.next();   // the Rodada wraps

        assertEquals(4, sheet.getDamageTaken());
    }

    @Test
    void aSubordinadoWithADuracaoLapses() {
        CharacterSheet sheet = commander(3);
        service.command(sheet, new Subordinate(SubordinateBenefit.PEAO_SKILL, false, "cativar", null, 1), null);
        assertEquals(1, SubordinateBenefits.of(sheet).size());

        sheet.tickTemporaryEffects();

        assertEquals(0, SubordinateBenefits.of(sheet).size());
    }

    // ---------- the sombra conselheira and the Peão (core 0.0.98) ----------

    private static CharacterSheet devotee(final org.aventyrs.core.character.DevotionTier tier) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .feats(new java.util.ArrayList<>())
                .equipment(new java.util.ArrayList<>())
                .deity(org.aventyrs.core.character.Deity.A_ESQUECIDA)
                .devotionTier(tier)
                .build();
        character.grantFeat(org.aventyrs.core.feat.DevotoFeat.ABRACADO_PELA_ESQUECIDA);
        return CharacterSheet.of(character, new Player());
    }

    /** "Apenas uma vez por Cena … um Subordinado Peão, Cavaleiro ou Torre por Concentração +1 Rodada." */
    @Test
    void theShadowCounselServesWhileConcentratedOnThenOneRodada() {
        CharacterSheet fundamentalista = devotee(org.aventyrs.core.character.DevotionTier.FUNDAMENTALISTA);
        Scene scene = new Scene();
        scene.addParticipant(fundamentalista, 10, UUID.randomUUID());

        Subordinate shadow = service.summonShadowCounsel(fundamentalista, SubordinateBenefit.TORRE_DEFESAS, null);

        assertEquals(1, SubordinateBenefits.of(fundamentalista).size());
        assertEquals(null, shadow.getRemainingRounds(), "no countdown while concentrating");
        assertEquals(TranslatableMessages.SHADOW_COUNSEL_ALREADY_USED, assertThrows(IllegalOperationException.class,
                () -> service.summonShadowCounsel(fundamentalista, SubordinateBenefit.PEAO_SKILL, null)).getMessage());

        scene.breakConcentration(fundamentalista);
        assertEquals(1, shadow.getRemainingRounds());
        fundamentalista.tickTemporaryEffects();
        assertEquals(0, SubordinateBenefits.of(fundamentalista).size());
    }

    @Test
    void theShadowCounselNeedsTheFundamentalistaRungAndAnAllowedGrade() {
        assertEquals(TranslatableMessages.SHADOW_COUNSEL_NOT_HELD, assertThrows(IllegalOperationException.class,
                () -> service.summonShadowCounsel(devotee(org.aventyrs.core.character.DevotionTier.FIEL),
                        SubordinateBenefit.PEAO_SKILL, null)).getMessage());
        assertEquals(TranslatableMessages.SHADOW_COUNSEL_GRADE_NOT_ALLOWED, assertThrows(IllegalOperationException.class,
                () -> service.summonShadowCounsel(devotee(org.aventyrs.core.character.DevotionTier.FUNDAMENTALISTA),
                        SubordinateBenefit.REI_SORTE, null)).getMessage());
    }

    /** Agnação Ancestral Superior's Peão: "que te auxiliará até seu próximo Descanso". */
    @Test
    void aSubordinadoCanLastUntilTheNextDescanso() {
        CharacterSheet sheet = commander(3);
        service.command(sheet, Subordinate.of(SubordinateBenefit.PEAO_CRITICAL, false, "agnação"), null, RestType.MINIMO);
        assertEquals(1, SubordinateBenefits.of(sheet).size());

        new RestServiceImpl().applyRest(sheet.getCharacter(), sheet, RestType.MINIMO);

        assertEquals(0, SubordinateBenefits.of(sheet).size());
    }

    // ---------- GM grants (core 0.1.5.6) ----------

    private static SceneContext combatWith(final org.aventyrs.core.sheet.CombatantSheet... allies) {
        return new SceneContext(List.of(allies), List.of(), Map.of(), TerrainType.FOREST, true, 1, false, null);
    }

    /** "As many as he wants": a GM grant ignores the Carisma limit and the one-per-grade rule, and only warns. */
    @Test
    void aGmGrantIgnoresTheLimits() {
        CharacterSheet sheet = commander(1);
        assertEquals(false, service.exceedsLimits(sheet, SubordinateBenefit.TORRE_DEFESAS, false));

        Subordinate first = service.grant(sheet, SubordinateBenefit.TORRE_DEFESAS, false, null);
        assertEquals(true, service.exceedsLimits(sheet, SubordinateBenefit.TORRE_RA, false));
        service.grant(sheet, SubordinateBenefit.TORRE_RA, false, null);
        service.grant(sheet, SubordinateBenefit.TORRE_DEFESAS, false, null);

        assertEquals(3, SubordinateBenefits.of(sheet).size());
        assertEquals(SubordinateService.GM_GRANT, first.getSource());
        assertEquals(null, first.getRemainingRounds(), "until the GM dismisses it");
        assertEquals(2, SubordinateBenefits.count(sheet, combat(), SubordinateBenefit.TORRE_DEFESAS));
        assertThrows(IllegalOperationException.class,
                () -> service.command(sheet, Subordinate.of(SubordinateBenefit.PEAO_SKILL, false, "rule"), null),
                "a rule source still meets the limits");
    }

    @Test
    void aGrantedSubordinadoIsDismissedById() {
        CharacterSheet sheet = commander(3);
        Subordinate kept = service.grant(sheet, SubordinateBenefit.PEAO_SKILL, false, null);
        Subordinate gone = service.grant(sheet, SubordinateBenefit.CAVALEIRO_ATTACK, false, null);

        service.dismiss(sheet, gone.getId());

        assertEquals(List.of(kept), SubordinateBenefits.of(sheet));
    }

    /** A restored Subordinado keeps the id it was first held under, so another client can name it. */
    @Test
    void aRestoredSubordinadoKeepsItsId() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new Subordinate(id, SubordinateBenefit.TORRE_RA, false, "x", null, null).getId());
        assertEquals(id, Subordinate.sustained(id, SubordinateBenefit.TORRE_RA, false, "x", UUID.randomUUID(), 1).getId());
    }

    /** "apenas uma vez a cada dia": unspent Rei points don't pile up, and a Prodigioso Rei renews its allies too. */
    @Test
    void aReiRenewsWithoutPilingUpAndReachesAlliesOfAProdigioso() {
        CharacterSheet king = commander(3);
        CharacterSheet ally = commander(1);
        int kingSorte = king.getTemporaryEgoPoints(EgoDomain.SORTE);
        int allySorte = ally.getTemporaryEgoPoints(EgoDomain.SORTE);

        service.grant(king, SubordinateBenefit.REI_SORTE, true, combatWith(ally));
        assertEquals(kingSorte + 2, king.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(allySorte + 2, ally.getTemporaryEgoPoints(EgoDomain.SORTE));

        service.renewAfterLongRest(king, combatWith(ally));
        assertEquals(kingSorte + 2, king.getTemporaryEgoPoints(EgoDomain.SORTE), "unspent: nothing more");

        ally.spendEgoPoints(EgoDomain.SORTE, org.aventyrs.core.sheet.EgoPointType.TEMPORARY, allySorte + 2);
        service.renewAfterLongRest(ally, combatWith(king));
        assertEquals(2, ally.getTemporaryEgoPoints(EgoDomain.SORTE),
                "the ally gets the Prodigioso Rei's points back on its own Descanso");
    }

    /** A monster commands one too; its Prodigioso reaches allied monsters, not allied characters. */
    @Test
    void aMonstersProdigiosoReachesItsOwnKind() {
        var monster = org.aventyrs.core.monster.model.MonsterTestKit.spawn(1);
        var packmate = org.aventyrs.core.monster.model.MonsterTestKit.spawn(1);
        CharacterSheet character = commander(1);

        service.grant(monster, SubordinateBenefit.TORRE_RA, true, null);

        assertEquals(1, SubordinateBenefits.count(monster, combatWith(packmate, character), SubordinateBenefit.TORRE_RA));
        assertEquals(1, SubordinateBenefits.count(packmate, combatWith(monster, character), SubordinateBenefit.TORRE_RA));
        assertEquals(0, SubordinateBenefits.count(character, combatWith(monster, packmate), SubordinateBenefit.TORRE_RA));
        int ra = new DamageServiceImpl().getTotalAbsoluteDamageReduction(packmate, combatWith());
        assertEquals(ra + 2, new DamageServiceImpl().getTotalAbsoluteDamageReduction(packmate, combatWith(monster)));
    }

    /** A Prodigioso Rei seen on another client's stand-in gives its points to this client's ally, once. */
    @Test
    void aProdigiosoKingsEgoIsSharedOnce() {
        CharacterSheet ally = commander(1);
        int sorte = ally.getTemporaryEgoPoints(EgoDomain.SORTE);
        Subordinate king = Subordinate.of(SubordinateBenefit.REI_SORTE, true, "Mestre");

        service.shareKingsEgo(ally, king);
        service.shareKingsEgo(ally, king);
        service.shareKingsEgo(ally, Subordinate.of(SubordinateBenefit.REI_SORTE, false, "Mestre"));

        assertEquals(sorte + 2, ally.getTemporaryEgoPoints(EgoDomain.SORTE));
    }
}
