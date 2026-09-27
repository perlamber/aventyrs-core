package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArvoresMimetizadasFeat;
import org.aventyrs.core.feat.ElficoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.feat.FeericoFeat;
import org.aventyrs.core.feat.GnomoFeat;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.feat.MetamagicoFeat;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_FORM_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_STORAGE_FULL;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_STORAGE_NOT_GRANTED;
import static org.aventyrs.core.util.TranslatableMessages.STORED_SPELL_RELEASE_COST_NOT_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase F: mimetizar with a choice and a live grant, Mana-cost reductions, per-cast opt-ins, and
 * Magia storage.
 */
class SpellcastingExtensionsTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet holding(final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .mimetizedSpells(new ArrayList<>())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static List<Spell> rung(final MagicTree tree, final BranchLevel level) {
        return tree.getSpells().stream().filter(spell -> spell.getBranchLevel() == level).toList();
    }

    private static MagicTree aNaturalTree() {
        return ArvoresMimetizadasFeat.naturalTrees().stream()
                .filter(tree -> !rung(tree, BranchLevel.BROTO).isEmpty() && !rung(tree, BranchLevel.MUDA).isEmpty()
                        && !rung(tree, BranchLevel.EMERGENTE).isEmpty() && !rung(tree, BranchLevel.SEMENTE).isEmpty())
                .findFirst().orElseThrow();
    }

    private static MimetizedSpell mimetized(final CharacterSheet sheet, final Spell spell) {
        return sheet.getCharacter().getMimetizedSpells().stream()
                .filter(held -> held.getSpell().equals(spell)).findFirst().orElseThrow();
    }

    // ---------- mimetizar: a chosen Árvore, a live grant ----------

    @Test
    void almaFeericaOffersOneNaturalTree() {
        FeatChoice<?> choice = ElficoFeat.ALMA_FEERICA.resolveRequiredChoices(holding().getCharacter()).get(0);

        assertEquals(MagicTree.class, choice.type());
        assertEquals(1, choice.picks());
        assertEquals(ArvoresMimetizadasFeat.naturalTrees(), choice.options());
    }

    @Test
    void almaFeericaMimetizesBrotoAndMudaForTheirManaCostInPd() {
        MagicTree tree = aNaturalTree();
        CharacterSheet elf = holding(ArvoresMimetizadasFeat.of(ElficoFeat.ALMA_FEERICA, tree));

        Spell broto = rung(tree, BranchLevel.BROTO).get(0);
        Spell muda = rung(tree, BranchLevel.MUDA).get(0);
        assertEquals(BranchLevel.BROTO.getManaCost(), mimetized(elf, broto).getDeterminationPointCost());
        assertEquals(BranchLevel.MUDA.getManaCost(), mimetized(elf, muda).getDeterminationPointCost());
        assertTrue(elf.getCharacter().getMimetizedSpells().stream()
                .noneMatch(held -> held.getSpell().getBranchLevel() == BranchLevel.EMERGENTE));
    }

    /** "Caso possua 2 Títulos Despertos também poderá mimetizar Emergentes" — the grant grows live. */
    @Test
    void theEmergenteAppearsOnceTwoTitulosAreDesperto() {
        MagicTree tree = aNaturalTree();
        CharacterSheet elf = holding(ArvoresMimetizadasFeat.of(ElficoFeat.ALMA_FEERICA, tree));
        elf.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        elf.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);

        assertTrue(elf.getCharacter().getMimetizedSpells().stream()
                .anyMatch(held -> held.getSpell().getBranchLevel() == BranchLevel.EMERGENTE));
    }

    @Test
    void aMimetizedMagiaIsCastForItsPd() {
        MagicTree tree = aNaturalTree();
        CharacterSheet elf = holding(ArvoresMimetizadasFeat.of(ElficoFeat.ALMA_FEERICA, tree));
        MimetizedSpell broto = mimetized(elf, rung(tree, BranchLevel.BROTO).get(0));

        new MimetizedSpellCastingServiceImpl().cast(elf, broto);

        assertEquals(broto.getDeterminationPointCost(), elf.getDeterminationSpent());
    }

    @Test
    void conclaveMagiasAreCastOnlyInFormaFeerica() {
        MagicTree tree = aNaturalTree();
        CharacterSheet gorgona = holding(ArvoresMimetizadasFeat.of(GorgonaFeat.ABENCOADA_PELO_CONCLAVE, tree));
        MimetizedSpell semente = mimetized(gorgona, rung(tree, BranchLevel.SEMENTE).get(0));

        assertEquals(0, semente.getDeterminationPointCost());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> new MimetizedSpellCastingServiceImpl().cast(gorgona, semente));
        assertEquals(MIMETIZED_SPELL_FORM_REQUIRED, refused.getMessage());

        gorgona.enterForm(FormType.FEERICA);
        new MimetizedSpellCastingServiceImpl().cast(gorgona, semente);
    }

    @Test
    void conclaveBrotoWaitsForTheFirstTitulo() {
        MagicTree tree = aNaturalTree();
        CharacterSheet gorgona = holding(ArvoresMimetizadasFeat.of(GorgonaFeat.ABENCOADA_PELO_CONCLAVE, tree));

        assertTrue(gorgona.getCharacter().getMimetizedSpells().stream()
                .noneMatch(held -> held.getSpell().getBranchLevel() == BranchLevel.BROTO));
        gorgona.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertEquals(2, mimetized(gorgona, rung(tree, BranchLevel.BROTO).get(0)).getDeterminationPointCost());
    }

    @Test
    void assombrosaOffersTheMudasOfAmplasTrees() {
        MagicTree tree = aNaturalTree();
        CharacterSheet arcanist = holding(ArvoresMimetizadasFeat.of(MetamagicoFeat.APTIDAO_MAGICA_AMPLA, tree));

        FeatChoice<?> choice = MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA
                .resolveRequiredChoices(arcanist.getCharacter()).get(0);

        assertEquals(rung(tree, BranchLevel.MUDA), choice.options());
    }

    @Test
    void espiritoDaFlorestaMimetizesEveryNaturalBrotoAtTwoPd() {
        CharacterSheet sprite = holding(FeericoFeat.ESPIRITO_DA_FLORESTA);
        MagicTree tree = aNaturalTree();

        assertEquals(2, mimetized(sprite, rung(tree, BranchLevel.BROTO).get(0)).getDeterminationPointCost());
    }

    @Test
    void duendeMimetizesNoProfanaSemente() {
        List<MimetizedSpell> held = holding(GnomoFeat.DUENDE).getCharacter().getMimetizedSpells();

        assertFalse(held.isEmpty());
        assertTrue(held.stream().allMatch(spell -> spell.getSpell().getBranchLevel() == BranchLevel.SEMENTE
                && !spell.getSpell().getTree().hasMagicType(MagicType.PROFANA)));
    }

    // ---------- Mana cost and per-cast opt-ins ----------

    /** A Magia of the given rung that affects only its caster — castable with no target named. */
    private static Spell personal(final BranchLevel level) {
        return Arrays.stream(MagicTree.values()).flatMap(tree -> tree.getSpells().stream())
                .filter(spell -> spell.getBranchLevel() == level
                        && spell.getTargeting().reach() == SpellReach.PESSOAL
                        && spell.getActivationTime() != null
                        && spell.getActivationTime().type() == ActivationType.PONTOS_DE_ACAO
                        && spell.getActivationTime().actionPoints() >= 2)
                .findFirst().orElseThrow();
    }

    private static SpellCastingResult cast(final CharacterSheet caster, final Spell spell, final Feat... activated) {
        Scene scene = new Scene();
        scene.addParticipant(caster, 1);
        SpellCastRequest.SpellCastRequestBuilder request = SpellCastRequest.builder()
                .caster(caster).spell(spell).scene(scene).sceneContext(scene.buildContext(caster, Map.of()));
        for (Feat feat : activated) {
            request.activatedFeat(feat);
        }
        return new SpellCastingServiceImpl().castSpell(request.build());
    }

    @Test
    void engenheiroDoManaTakesOnePmOff() {
        Spell muda = personal(BranchLevel.MUDA);

        assertEquals(cast(holding(), muda).getManaCost() - 1,
                cast(holding(MetamagicoFeat.ENGENHEIRO_DO_MANA), muda).getManaCost());
    }

    @Test
    void conjuracaoRapidaTradesADesvantagemForAPontoDeAcao() {
        Spell spell = personal(BranchLevel.MUDA);
        CharacterSheet caster = holding(MetamagicoFeat.CONJURACAO_RAPIDA);
        SpellCastingResult plain = cast(caster, spell);
        SpellCastingResult quick = cast(caster, spell, MetamagicoFeat.CONJURACAO_RAPIDA);

        assertEquals(plain.getActivationTime().actionPoints() - 1, quick.getActivationTime().actionPoints());
        assertEquals(plain.getDominioDoManaResult().getSkillRollBonus() + org.aventyrs.core.skill.Skill.DISADVANTAGE_MALUS,
                quick.getDominioDoManaResult().getSkillRollBonus());
    }

    @Test
    void procrastinarDelaysTheEffectARodada() {
        Spell spell = personal(BranchLevel.MUDA);
        CharacterSheet caster = holding(MetamagicoFeat.PROCRASTINAR_CONJURACAO);

        assertEquals(0, cast(caster, spell).getEffectDelayRounds());
        assertEquals(1, cast(caster, spell, MetamagicoFeat.PROCRASTINAR_CONJURACAO).getEffectDelayRounds());
    }

    @Test
    void anOptInNotNamedDoesNothing() {
        Spell spell = personal(BranchLevel.MUDA);
        CharacterSheet caster = holding(MetamagicoFeat.CONJURACAO_RAPIDA);

        assertEquals(cast(holding(), spell).getActivationTime(), cast(caster, spell).getActivationTime());
    }

    // ---------- Magia storage ----------

    private final SpellStorageService storage = new SpellStorageServiceImpl();

    private static SpellCastingResult aCast() {
        return SpellCastingResult.builder().build();
    }

    @Test
    void storingNeedsArmazenarMagia() {
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> storage.store(holding(), personal(BranchLevel.BROTO), aCast()));

        assertEquals(SPELL_STORAGE_NOT_GRANTED, error.getMessage());
    }

    @Test
    void armazenarHoldsOneShallowMagiaReleasedAsAnAcaoLivre() {
        CharacterSheet caster = holding(MetamagicoFeat.ARMAZENAR_MAGIA);
        Spell broto = personal(BranchLevel.BROTO);
        storage.store(caster, broto, aCast());

        assertEquals(SPELL_STORAGE_FULL, assertThrows(IllegalOperationException.class,
                () -> storage.store(caster, broto, aCast())).getMessage());
        assertEquals(STORED_SPELL_RELEASE_COST_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> storage.release(caster, broto, ActionCost.REACTION)).getMessage());

        assertEquals(broto, storage.release(caster, broto, ActionCost.FREE_ACTION).spell());
        assertTrue(caster.getStoredSpells().isEmpty());
    }

    @Test
    void armazenarRefusesAMuda() {
        assertEquals(SPELL_STORAGE_FULL, assertThrows(IllegalOperationException.class,
                () -> storage.store(holding(MetamagicoFeat.ARMAZENAR_MAGIA), personal(BranchLevel.MUDA), aCast()))
                .getMessage());
    }

    @Test
    void superiorHoldsTwoShallowOrOneMudaAndReleasesAsAReacao() {
        CharacterSheet caster = holding(MetamagicoFeat.ARMAZENAR_MAGIA, MetamagicoFeat.ARMAZENAR_MAGIA_SUPERIOR);
        Spell broto = personal(BranchLevel.BROTO);
        storage.store(caster, broto, aCast());
        storage.store(caster, broto, aCast());
        assertEquals(2, caster.getStoredSpells().size());

        storage.release(caster, broto, ActionCost.REACTION);
        assertEquals(1, caster.getReactionsSpentThisRound());

        CharacterSheet other = holding(MetamagicoFeat.ARMAZENAR_MAGIA_SUPERIOR);
        storage.store(other, personal(BranchLevel.MUDA), aCast());
        assertThrows(IllegalOperationException.class, () -> storage.store(other, broto, aCast()));
    }

    @Test
    void aDescansoDissipatesEveryStoredMagia() {
        CharacterSheet caster = holding(MetamagicoFeat.ARMAZENAR_MAGIA);
        storage.store(caster, personal(BranchLevel.BROTO), aCast());

        caster.clearRestCooldowns(RestType.values()[0]);

        assertTrue(caster.getStoredSpells().isEmpty());
    }
}
