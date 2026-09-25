package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.race.HomemFera;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.race.TrollsRacialAbility;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regeneração Reativa end to end — the Troll's Característica Racial, the {@code FeralFeat}
 * Talento that lends it out, and the two {@code TrollFeat} Talentos that act on it. Every
 * assertion reads PV off the sheet or RD off {@link DamageService}, never a hook's return value:
 * what is being tested is that taking a hit starts the cycle and that the cycle changes numbers a
 * player can see.
 */
class ReactiveRegenerationTest {

    private final FeatService featService = new FeatServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    // ---------- The Característica itself ----------

    /**
     * The whole cycle, on the sheet: a hit lands, and from the Troll's next Turn on their damage
     * falls by their own per-Rodada figure — for as many Rodadas as their Vigor, and no longer.
     */
    @Test
    void aTrollRecoversTwoHitPointsEachTurnForAsManyRodadasAsTheirVigor() {
        CharacterSheet sheet = sheetFor(troll(3));

        damageService.applyDamage(sheet, 20, true);
        assertEquals(20, sheet.getDamageTaken());

        assertEquals(18, damageAfterOneTurn(sheet));
        assertEquals(16, damageAfterOneTurn(sheet));
        assertEquals(14, damageAfterOneTurn(sheet));
        // Vigor 3 bought exactly three Rodadas; the fourth Turn recovers nothing.
        assertEquals(14, damageAfterOneTurn(sheet));
        assertFalse(sheet.hasActiveRegeneration());
    }

    /**
     * "A quantidade de PV recuperados desta forma não pode superar os danos sofridos" — the cap is
     * on the <i>total</i>, not on one Rodada, so a scratch regenerates less than a full Vigor's
     * worth of Rodadas would otherwise pay out.
     */
    @Test
    void regenerationNeverRecoversMoreThanTheDamageThatTriggeredIt() {
        CharacterSheet sheet = sheetFor(troll(3));

        damageService.applyDamage(sheet, 3, true);

        assertEquals(1, damageAfterOneTurn(sheet));
        assertEquals(0, damageAfterOneTurn(sheet));
        assertEquals(0, damageAfterOneTurn(sheet));
    }

    /** A race without the Característica takes the same hit and regenerates nothing. */
    @Test
    void aCharacterWithoutTheCaracteristicaDoesNotRegenerate() {
        CharacterSheet sheet = sheetFor(character().race(new Human()).build());

        damageService.applyDamage(sheet, 20, true);

        assertFalse(sheet.hasActiveRegeneration());
        assertEquals(20, damageAfterOneTurn(sheet));
    }

    /** Damage that never reached PV gives a Troll nothing to regenerate from. */
    @Test
    void aFullyMitigatedHitStartsNoRegenerationButIsStillCountedAsAnAttackSuffered() {
        CharacterSheet sheet = sheetFor(troll(3));

        damageService.applyDamage(sheet, 0, true);

        assertFalse(sheet.hasActiveRegeneration());
        assertEquals(1, sheet.getAttacksSufferedThisRound());
    }

    /**
     * Losing PV any way other than an attack triggers nothing — the trait reads "após sofrer danos
     * de fontes inimigas", and {@code CombatantSheet#applyDamage} is the path every ongoing effect
     * and self-inflicted cost takes.
     */
    @Test
    void losingHitPointsOutsideAnAttackStartsNoRegeneration() {
        CharacterSheet sheet = sheetFor(troll(3));

        sheet.applyDamage(10);

        assertFalse(sheet.hasActiveRegeneration());
        assertEquals(0, sheet.getAttacksSufferedThisRound());
    }

    /** "Efeito não cumulativo": a second hit refreshes the effect rather than doubling it. */
    @Test
    void aSecondHitReplacesTheRunningRegenerationRatherThanStackingWithIt() {
        CharacterSheet sheet = sheetFor(troll(3));

        damageService.applyDamage(sheet, 20, true);
        damageService.applyDamage(sheet, 20, true);

        assertEquals(38, damageAfterOneTurn(sheet));
    }

    /**
     * Not accumulating is only half the rule — the replacement <b>renews the duration</b>, so a
     * Troll hit again on its third Rodada keeps regenerating past where the first grant would have
     * lapsed.
     */
    @Test
    void aSecondHitRenewsTheDurationRatherThanLettingTheFirstGrantLapse() {
        CharacterSheet sheet = sheetFor(troll(3));

        damageService.applyDamage(sheet, 40, true);
        assertEquals(38, damageAfterOneTurn(sheet));
        assertEquals(36, damageAfterOneTurn(sheet));

        // Two of the three Rodadas are spent; this hit puts the count back to three.
        damageService.applyDamage(sheet, 40, true);

        assertEquals(74, damageAfterOneTurn(sheet));
        assertEquals(72, damageAfterOneTurn(sheet));
        assertEquals(70, damageAfterOneTurn(sheet));
        assertEquals(70, damageAfterOneTurn(sheet));
        assertFalse(sheet.hasActiveRegeneration());
    }

    /**
     * The trigger is generic: {@code DamageService} applies whatever Blessings a held ability
     * declares, so a damage-triggered clause that is not Regeneração Reativa needs no change
     * there. Written with a homebrew ability, since Regeneração Reativa is the only authored one.
     */
    @Test
    void anyAbilityCanDeclareADamageTakenBlessing() {
        SkillCompetencyAbility guarded = new SkillCompetencyAbility() {
            @Override
            public SkillType getSkillType() {
                return SkillType.ESQUIVA_E_APARAR;
            }

            @Override
            public String getDescription() {
                return "Homebrew: RD for two Rodadas after being hit";
            }

            @Override
            public List<Blessing> resolveDamageTakenBlessings(final CombatantSheet holder, final int finalDamage) {
                return List.of(new Blessing(ModifierType.DAMAGE_REDUCTION, 3, 2,
                        TargetScope.SELF, "HOMEBREW_GUARD"));
            }
        };
        Character character = character().race(new Human()).build();
        character.grantFeat(new AbstractFeat(FeatCategory.SOBREVIVENCIA, "Homebrew: guards after a hit",
                FeatRequirements.builder().build()) {
            @Override
            public List<SkillTrait> getGrantedSkillTraits(final Character holder) {
                return List.of(guarded);
            }
        });
        CharacterSheet sheet = sheetFor(character);
        assertEquals(0, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));

        damageService.applyDamage(sheet, 5, true);

        assertEquals(3, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));
    }

    // ---------- Regeneração Reativa Superior ----------

    /**
     * Superior turns "não cumulativo" into a ceiling of 1 + Títulos Aventyr Despertos, so two hits
     * leave two effects running and the Troll recovers both figures on the same Turn.
     */
    @Test
    void superiorLetsSeveralRegenerationsRunAtOnce() throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        CharacterSheet sheet = sheetFor(troll);

        damageService.applyDamage(sheet, 20, true);
        damageService.applyDamage(sheet, 20, true);

        // One Título Desperto → a ceiling of two, and 2PV apiece.
        assertEquals(36, damageAfterOneTurn(sheet));
    }

    /**
     * The ceiling is a scaling term, not a constant: with no Título it is 1 (the Característica's
     * own non-cumulative floor), with one it is 2. Read from the Talento's own hook, since the
     * effect count is not otherwise observable.
     */
    @Test
    void theSuperiorCeilingScalesWithAwakenedTitles() {
        Character withoutTitle = troll(3);
        Character withTitle = troll(3);
        grantTitle(withTitle);

        assertEquals(1, TrollFeat.REGENERACAO_REATIVA_SUPERIOR.resolveSimultaneousRegenerationLimit(withoutTitle));
        assertEquals(2, TrollFeat.REGENERACAO_REATIVA_SUPERIOR.resolveSimultaneousRegenerationLimit(withTitle));
    }

    /** Past the ceiling the oldest effect is the one that goes, so the count never exceeds it. */
    @Test
    void aThirdHitDisplacesTheOldestRegenerationOnceTheCeilingIsReached() throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        CharacterSheet sheet = sheetFor(troll);

        damageService.applyDamage(sheet, 20, true);
        damageService.applyDamage(sheet, 20, true);
        damageService.applyDamage(sheet, 20, true);

        // Still two effects, not three.
        assertEquals(56, damageAfterOneTurn(sheet));
    }

    // ---------- Regeneração Reativa Invernal ----------

    /**
     * Invernal's RDS is doubly conditional, and both halves are read off the sheet: it applies
     * only while Regeneração Reativa is running, and only to the Rodada's first incoming attack.
     */
    @Test
    void invernalGrantsDamageReductionOnlyWhileRegeneratingAndOnlyOnTheRodadasFirstAttack()
            throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR, TrollFeat.REGENERACAO_REATIVA_INVERNAL);
        CharacterSheet sheet = sheetFor(troll);

        // Not regenerating yet — nothing granted.
        assertEquals(0, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));

        damageService.applyDamage(sheet, 20, true);
        // Regenerating now, but this Rodada's first attack has already been suffered.
        assertEquals(0, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));

        sheet.startNewRound();
        // 1 + one Título Desperto.
        assertEquals(2, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));

        damageService.applyDamage(sheet, 20, true);
        assertEquals(0, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));
    }

    /** That RDS reaches a real hit: the Rodada's first attack lands for less. */
    @Test
    void invernalsDamageReductionComesOffTheRodadasFirstHit() throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR, TrollFeat.REGENERACAO_REATIVA_INVERNAL);
        CharacterSheet sheet = sheetFor(troll);

        damageService.applyDamage(sheet, 20, true);
        sheet.startNewRound();

        damageService.applyDamage(sheet, 10, false);
        assertEquals(28, sheet.getDamageTaken());

        damageService.applyDamage(sheet, 10, false);
        assertEquals(38, sheet.getDamageTaken());
    }

    /** A {@code Character}-only RD preview cannot see the sheet, and reads as "condition not met". */
    @Test
    void invernalGrantsNothingWithoutASheetToAsk() throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR, TrollFeat.REGENERACAO_REATIVA_INVERNAL);

        assertEquals(0, damageService.getTotalDamageReduction(troll));
        assertEquals(0, TrollFeat.REGENERACAO_REATIVA_INVERNAL.resolveDamageReduction(troll));
    }

    // ---------- Benção de Mapinguari ----------

    /**
     * The Talento hands a Homem-Fera the Troll's Característica itself — the same {@code
     * TrollsRacialAbility} constant, so the same 2PV. See that constant for the rulebook
     * divergence this resolves.
     */
    @Test
    void bencaoDeMapinguariGrantsTheCaracteristicaToSomeoneNotBornWithIt() throws IllegalOperationException {
        Character homemFera = character()
                .race(new HomemFera(HomemFera.EspiritoAnimal.LICANTROPO, HomemFera.Criacao.HUMANOS))
                .attributes(vigor(3))
                .build();
        CharacterSheet before = sheetFor(homemFera);
        damageService.applyDamage(before, 20, true);
        assertFalse(before.hasActiveRegeneration());

        acquire(homemFera, FeralFeat.BENCAO_DE_MAPINGUARI);
        CharacterSheet after = sheetFor(homemFera);
        damageService.applyDamage(after, 20, true);

        assertTrue(after.hasActiveRegeneration());
        assertEquals(18, damageAfterOneTurn(after));
    }

    // ---------- The ceiling belongs to the ability ----------

    /**
     * <b>The headline.</b> Superior buffs Regeneração Reativa and nothing else, so a second,
     * unrelated damage-taken Blessing held by the same Troll keeps its own ceiling of one. Both
     * halves are asserted together on purpose: separately, either could pass for the wrong reason
     * — RD staying at 3 proves nothing unless the regeneration beside it demonstrably <i>did</i>
     * stack.
     */
    @Test
    void superiorsCeilingDoesNotReachAnUnrelatedDamageTakenBlessing() throws IllegalOperationException {
        Character troll = troll(3);
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        troll.grantFeat(grantingDamageTakenBlessing(
                new Blessing(ModifierType.DAMAGE_REDUCTION, 3, 2, TargetScope.SELF, "HOMEBREW_GUARD")));
        CharacterSheet sheet = sheetFor(troll);

        damageService.applyDamage(sheet, 20, true);
        damageService.applyDamage(sheet, 20, true);

        assertEquals(3, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));
        assertEquals(36, damageAfterOneTurn(sheet));
    }

    /**
     * The ceiling is resolved by the ability itself, with no {@code DamageService} in the picture —
     * and it is a scaling term, not a constant.
     */
    @Test
    void theCeilingIsResolvedByTheAbilityNotByTheDamagePath() throws IllegalOperationException {
        assertEquals(1, statedCeiling(troll(3)));

        Character oneTitle = troll(3);
        grantTitle(oneTitle);
        acquire(oneTitle, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        assertEquals(2, statedCeiling(oneTitle));

        Character twoTitles = troll(3);
        grantTitle(twoTitles);
        acquire(twoTitles, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        twoTitles.grantTitle(new org.aventyrs.core.title.santo.Santo(List.of(), List.of()),
                TitleSlot.SECONDARY);
        assertEquals(3, statedCeiling(twoTitles));
    }

    /** What the 22 Blessings that say nothing about stacking inherit. */
    @Test
    void aBlessingStatesACeilingOfOneByDefault() {
        assertEquals(Blessing.DEFAULT_MAXIMUM_SIMULTANEOUS,
                new Blessing(ModifierType.DEFESAS, 2, 3, TargetScope.SELF, "X").getMaximumSimultaneous());
        assertEquals(Blessing.DEFAULT_MAXIMUM_SIMULTANEOUS,
                new Blessing(ModifierType.DEFESAS, 2, 3, TargetScope.SELF, "X", null).getMaximumSimultaneous());
    }

    /**
     * A stated ceiling is honoured whatever the {@code ModifierType}, not only for the one kind
     * that acts each Rodada — which is what makes {@link
     * #superiorsCeilingDoesNotReachAnUnrelatedDamageTakenBlessing}'s RD of 3 a real closure rather
     * than an accident of the conversion dropping the figure. The two together are the
     * discrimination: same type, same two hits, default ceiling → 3 and a stated ceiling of 2 → 6.
     */
    @Test
    void aNonRegenerationBlessingHonoursItsOwnStatedCeiling() {
        Character character = character().race(new Human()).build();
        character.grantFeat(grantingDamageTakenBlessing(
                new Blessing(ModifierType.DAMAGE_REDUCTION, 3, 2, TargetScope.SELF, "SRC", null, 2)));
        CharacterSheet sheet = sheetFor(character);

        damageService.applyDamage(sheet, 5, true);
        damageService.applyDamage(sheet, 5, true);

        assertEquals(6, damageService.getTotalDamageReduction(sheet, (DamageType) null, null));
    }

    private static int statedCeiling(final Character troll) {
        return TrollsRacialAbility.REGENERACAO_REATIVA
                .resolveDamageTakenBlessings(sheetFor(troll), 10)
                .get(0)
                .getMaximumSimultaneous();
    }

    /** A homebrew Talento handing its holder an ability that declares blessing when hit. */
    private static Feat grantingDamageTakenBlessing(final Blessing blessing) {
        SkillCompetencyAbility ability = new SkillCompetencyAbility() {
            @Override
            public SkillType getSkillType() {
                return SkillType.ESQUIVA_E_APARAR;
            }

            @Override
            public String getDescription() {
                return "Homebrew: grants " + blessing.getModifierType() + " after being hit";
            }

            @Override
            public List<Blessing> resolveDamageTakenBlessings(final CombatantSheet holder, final int finalDamage) {
                return List.of(blessing);
            }
        };
        return new AbstractFeat(FeatCategory.SOBREVIVENCIA, "Homebrew: reacts to a hit",
                FeatRequirements.builder().build()) {
            @Override
            public List<SkillTrait> getGrantedSkillTraits(final Character holder) {
                return List.of(ability);
            }
        };
    }

    // ---------- Controls ----------

    /**
     * "Abandonando seus traços raciais" silences the Característica — it is a Habilidade Racial,
     * which is the {@code ALL} rung alone; the narrower rungs, which name Armas Naturais and
     * anatomy, leave it running. No authored Talento reaches both a Troll and that rung
     * (Draconato is a Nascido do Dragão's, Ancienteforme a Fada's), so the rung is declared by a
     * homebrew {@code AbstractFeat} and held through the plain mutator: the subject here is the
     * suppression ladder, not an acquisition. A Forma is what turns any rung on at all, so the
     * sheet enters one.
     */
    @Test
    void suppressingEveryRacialTraitSilencesTheCaracteristica() {
        Character troll = troll(3);
        troll.grantFeat(suppressing(RacialTraitSuppression.ALL));
        CharacterSheet sheet = sheetFor(troll);
        sheet.enterForm(FormType.DRACONATO);

        damageService.applyDamage(sheet, 20, true);

        assertFalse(sheet.hasActiveRegeneration());
        assertEquals(20, damageAfterOneTurn(sheet));
    }

    /** The two narrower rungs name a body's weapons and anatomy, and leave the healing alone. */
    @Test
    void theNarrowerSuppressionRungsLeaveTheCaracteristicaRunning() {
        for (RacialTraitSuppression rung : List.of(RacialTraitSuppression.NATURAL_WEAPONS_ONLY,
                RacialTraitSuppression.PHYSICAL)) {
            Character troll = troll(3);
            troll.grantFeat(suppressing(rung));
            CharacterSheet sheet = sheetFor(troll);
            sheet.enterForm(FormType.DRACONATO);

            damageService.applyDamage(sheet, 20, true);

            assertTrue(sheet.hasActiveRegeneration(), rung + " should not silence Regeneração Reativa");
            assertEquals(18, damageAfterOneTurn(sheet));
        }
    }

    private static Feat suppressing(final RacialTraitSuppression rung) {
        return new AbstractFeat(FeatCategory.TROLL, "Homebrew: suppresses " + rung,
                FeatRequirements.builder().build()) {
            @Override
            public RacialTraitSuppression resolveRacialTraitSuppression(final Character character,
                                                                        final CombatantSheet holder) {
                return rung;
            }
        };
    }

    /**
     * Coming by the same Habilidade Racial twice is still one ability — {@code
     * SkillCompetencyAbility#allFor} deduplicates, so the figure is not summed with itself. No
     * authored pair can reach this (Mapinguari is a Homem-Fera's, and a Troll already has the
     * ability), so a homebrew Talento stands in for the second source.
     */
    @Test
    void comingByTheCaracteristicaTwiceStillRegeneratesOnce() {
        Character troll = troll(3);
        troll.grantFeat(new AbstractFeat(FeatCategory.TROLL, "Homebrew: grants the Troll ability",
                FeatRequirements.builder().build()) {
            @Override
            public List<SkillTrait> getGrantedSkillTraits(final Character character) {
                return List.of(TrollsRacialAbility.REGENERACAO_REATIVA);
            }
        });
        CharacterSheet sheet = sheetFor(troll);

        damageService.applyDamage(sheet, 20, true);

        assertEquals(18, damageAfterOneTurn(sheet));
    }

    /** No other Talento in the catalog hands out the Habilidade Racial or raises the ceiling. */
    @Test
    void noOtherConstantResolvesRegeneration() {
        Character troll = troll(3);
        for (Feat feat : FeatCatalog.all()) {
            if (feat != FeralFeat.BENCAO_DE_MAPINGUARI) {
                assertFalse(feat.getGrantedSkillTraits(troll)
                                .contains(TrollsRacialAbility.REGENERACAO_REATIVA),
                        feat + " should grant no Regeneração Reativa");
            }
            if (feat != TrollFeat.REGENERACAO_REATIVA_SUPERIOR) {
                assertEquals(0, feat.resolveSimultaneousRegenerationLimit(troll),
                        feat + " should raise no simultaneous-effect ceiling");
            }
        }
    }

    /** The two still-gap-blocked Troll Talentos change nothing that can be observed. */
    @Test
    void theGapBlockedTrollTalentosChangeNothing() throws IllegalOperationException {
        // Instinto 3 is SONO_LEVE's own Pré-requisito, met exactly.
        Character troll = character().race(new Troll())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .build();
        grantTitle(troll);
        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        CharacterSheet baseline = sheetFor(troll);
        damageService.applyDamage(baseline, 20, true);
        int damageAfterATurn = damageAfterOneTurn(baseline);

        acquire(troll, TrollFeat.SONO_LEVE, TrollFeat.REGENERACAO_REATIVA_ESPINHOSA);
        CharacterSheet after = sheetFor(troll);
        damageService.applyDamage(after, 20, true);

        assertEquals(damageAfterATurn, damageAfterOneTurn(after));
        assertEquals(0, damageService.getTotalDamageReduction(after, (DamageType) null, null));
    }

    // ---------- Fixtures ----------

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .mimetizedSpells(new ArrayList<MimetizedSpell>());
    }

    private static CharacterAttributes vigor(final int base) {
        return CharacterAttributes.builder()
                .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(base).build())
                .build();
    }

    /** Sturdy enough that no hit here drops it into Coma, where a Regeneração heals only 1PV. */
    private static Character troll(final int vigorBase) {
        return character().race(new Troll()).attributes(vigor(vigorBase)).lifeMultiplier(20).build();
    }

    private static void grantTitle(final Character character) {
        character.grantTitle(new org.aventyrs.core.title.santo.Santo(List.of(), List.of()), TitleSlot.PRIMARY);
    }

    private static CharacterSheet sheetFor(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    private void acquire(final Character character, final Feat... feats) throws IllegalOperationException {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        for (Feat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
    }

    /** Ends the combatant's Turn — the Rodada boundary a {@code Regeneration} heals on. */
    private static int damageAfterOneTurn(final CharacterSheet sheet) {
        sheet.finishTurn();
        return sheet.getDamageTaken();
    }
}
