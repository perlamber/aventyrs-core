package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.race.Vampiro.VampiroLineage;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The acquired, per-character form of {@link VampiricoFeat#METAMORFOSE_DRACULEA}, carrying the
 * {@link FormaMetamorfica}s the player chose — "Escolha 2 Formas Metamórficas (Dampiros podem
 * escolher apenas 1, Rakshasa podem escolher 4, mas não podem se transformar em Névoa)". Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant.
 *
 * <p>Each chosen Forma becomes its own activatable shape, surfaced through {@link
 * #resolveActiveAbilities()} — the plural hook this Talento is the reason for. A single
 * {@code Optional} could not carry four.
 *
 * <p><b>The two lineage rules are validated here, not left as prose</b>, because both are exact
 * and both are checkable from the holder's own {@code Vampiro}: how many may be picked ({@link
 * #choicesFor}) and Rakshasa's Névoa exclusion. That is a deliberate exception to this codebase's
 * builders-aren't-gatekeepers restraint — the same one {@link ArmamentoDraconicoFeat} makes for
 * its own "escolha duas armas", and for the same reason: a choice class exists precisely to hold a
 * legal choice, so an illegal one has no meaning to record.
 */
@Getter
public final class MetamorfoseDraculeaFeat extends AbstractFeat {

    /** "Escolha 2 Formas Metamórficas" — the figure for every lineage but the two named below. */
    private static final int DEFAULT_CHOICES = 2;

    /** {@code FormaMetamorfica#MORCEGO_ATROZ}'s "Roubo de Vida aumentado em +2". */
    private static final int MORCEGO_LIFE_STEAL_BONUS = 2;

    private static final Map<VampiroLineage, Integer> CHOICES_BY_LINEAGE = new EnumMap<>(Map.of(
            VampiroLineage.DAMPIRO, 1,
            VampiroLineage.RAKSHASA, 4));

    private final Set<FormaMetamorfica> chosenFormas;

    /** One ability per chosen Forma, built once — {@code activate} matches by {@code ==}. */
    private final List<ActiveAbility> transformations;

    public MetamorfoseDraculeaFeat(@NonNull final Set<FormaMetamorfica> chosenFormas) {
        super(VampiricoFeat.METAMORFOSE_DRACULEA.getFeatCategory(),
                VampiricoFeat.METAMORFOSE_DRACULEA.getDescription(),
                VampiricoFeat.METAMORFOSE_DRACULEA.getFeatRequirements());
        this.chosenFormas = EnumSet.copyOf(chosenFormas);
        this.transformations = this.chosenFormas.stream()
                .map(FormaMetamorfica::getTransformation)
                .toList();
    }

    /**
     * The validated factory — use this at grant time. Checks both lineage rules against holder:
     * the number of Formas ({@link #choicesFor}) and, for a Rakshasa, that Névoa is not among
     * them.
     *
     * <p>The raw constructor stays unvalidated for the builder-bypass convention (fixtures, tests
     * staging a state directly), the same split {@code ConselheiroDeGuerraYmirianoFeat} keeps.
     */
    public static MetamorfoseDraculeaFeat of(@NonNull final Character holder,
                                             @NonNull final Set<FormaMetamorfica> chosenFormas) {
        int allowed = choicesFor(holder);
        if (chosenFormas.size() != allowed) {
            throw new IllegalArgumentException(
                    "Metamorfose Dracúlea must pick exactly " + allowed + " Formas, got " + chosenFormas.size());
        }
        if (lineageOf(holder) == VampiroLineage.RAKSHASA && chosenFormas.contains(FormaMetamorfica.NEVOA)) {
            throw new IllegalArgumentException("A Rakshasa cannot take Névoa");
        }
        return new MetamorfoseDraculeaFeat(chosenFormas);
    }

    /**
     * The same thing, from the {@link ActiveAbility}s a client actually picked — the options a
     * {@code Feat#resolveRequiredChoices} offering handed it. Resolves each back to its
     * {@link FormaMetamorfica} and validates exactly as {@link #of(Character, Set)} does, so a
     * client never has to know the enum behind the ability it was shown.
     */
    public static MetamorfoseDraculeaFeat ofChosenAbilities(@NonNull final Character holder,
                                                            @NonNull final List<ActiveAbility> chosen) {
        Set<FormaMetamorfica> formas = EnumSet.noneOf(FormaMetamorfica.class);
        for (ActiveAbility ability : chosen) {
            if (!(ability instanceof MetamorfoseActiveAbility metamorfose)) {
                throw new IllegalArgumentException("Not a Forma Metamórfica: " + ability);
            }
            formas.add(metamorfose.getForma());
        }
        return of(holder, formas);
    }

    /**
     * How many Formas holder may choose — 1 for a Dampiro, 4 for a Rakshasa, {@link
     * #DEFAULT_CHOICES} for every other lineage. A non-Vampiro gets the default; the race gate is
     * {@code FeatRequirements#requiredRace}'s job, not this method's.
     */
    public static int choicesFor(final Character holder) {
        VampiroLineage lineage = lineageOf(holder);
        return lineage == null ? DEFAULT_CHOICES : CHOICES_BY_LINEAGE.getOrDefault(lineage, DEFAULT_CHOICES);
    }

    private static VampiroLineage lineageOf(final Character holder) {
        return holder.getRace() instanceof Vampiro vampiro ? vampiro.getLineage() : null;
    }

    @Override
    public Feat catalogEntry() {
        return VampiricoFeat.METAMORFOSE_DRACULEA;
    }

    /** One activatable shape per chosen Forma — see {@link MetamorfoseActiveAbility}. */
    @Override
    public List<ActiveAbility> resolveActiveAbilities() {
        return transformations;
    }

    /**
     * The {@link FormaMetamorfica} sheet's holder is currently wearing, or {@code null} — out of
     * any Forma, in a shape this Talento does not name, or in one the holder never picked.
     *
     * <p><b>The chosen set is part of the test, deliberately.</b> A shape the player did not pick
     * is not theirs, which mirrors {@link #resolveActiveAbilities()} — they have no way to enter
     * it. The visible consequence: a GM who bare-{@code enterForm}s someone into Aranha Gigante
     * when they picked Lobo and Morcego gets no fangs and no replacement, which is the honest
     * answer rather than a silent grant.
     */
    private FormaMetamorfica wornForma(final CombatantSheet sheet) {
        if (sheet == null) {
            return null;
        }
        FormaMetamorfica worn = FormaMetamorfica.of(sheet.getCurrentForm());
        return worn != null && chosenFormas.contains(worn) ? worn : null;
    }

    /**
     * The worn shape's ARMA NATURAL column — Cauda Constritora for Serpente Espinhosa, nothing at
     * all for Névoa, whose column reads "Nenhum".
     *
     * <p><b>Only the long form is overridden</b>, so the sheet-less {@code
     * getGrantedNaturalWeapons(Character)} stays empty for this Talento. That is correct and not
     * an omission: out of any Forma, Metamorfose Dracúlea grants no Arma Natural whatsoever.
     */
    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character,
                                                        final CombatantSheet sheet) {
        FormaMetamorfica worn = wornForma(sheet);
        if (worn == null || worn.getNaturalWeapon() == null) {
            return List.of();
        }
        return List.of(worn.getNaturalWeapon());
    }

    /**
     * "Armas não podem ser utilizadas, são substituídas por armas naturais" — read as replacing
     * the holder's own Armas Naturais too, for as long as the shape is worn. See {@link
     * FormaMetamorfica} for the two counts on which the source argues otherwise, and why it is
     * read this way regardless.
     *
     * <p><b>{@link RacialTraitSuppression#NATURAL_WEAPONS_ONLY}, and that rung exists for this
     * clause.</b> Metamorfose Dracúlea's text never says "abandonando traços raciais" — that is
     * {@code DraconicoFeat#DRACONATO} and {@code FeericoFeat#ANCIENTEFORME}. Reading "são
     * substituídas" as also dropping the holder's Resistência a Críticos, their anatomy
     * immunities or their racial size would be an invention layered on a reading that is already
     * debatable, so the ladder has a rung that stops at weapons rather than promoting this clause
     * to {@link RacialTraitSuppression#PHYSICAL}.
     *
     * <p>Declared for <em>every</em> worn row including Névoa, which is the point: Névoa grants no
     * weapon and suppresses the rest, leaving a holder who can strike with nothing.
     */
    @Override
    public RacialTraitSuppression resolveRacialTraitSuppression(final Character character,
                                                                final CombatantSheet sheet) {
        return wornForma(sheet) == null
                ? RacialTraitSuppression.NONE
                : RacialTraitSuppression.NATURAL_WEAPONS_ONLY;
    }

    /**
     * The HABILIDADE column's two Vantagem rows — Aranha Gigante's Furtividade and Lobo
     * Dentes-de-Sabre's Perícias de Ataque. Both are worth nothing until their holder is actually
     * wearing that shape, which is why this reads the sheet rather than the {@link Character}.
     */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                     final SkillTrait requestedAbility, final Character character,
                                     final AttackSource attackSource, final CombatantSheet holder) {
        FormaMetamorfica worn = wornForma(holder);
        if (worn == null) {
            return 0;
        }
        boolean applies = switch (worn) {
            case ARANHA_GIGANTE -> skillType == SkillType.FURTIVIDADE;
            case LOBO_DENTES_DE_SABRE -> skillType != null && skillType.isAttackSkill();
            default -> false;
        };
        return applies ? Skill.ADVANTAGE_BONUS : 0;
    }

    /**
     * Morcego Atroz's "Roubo de Vida aumentado em +2". An amplifier like every other {@code
     * resolveLifeStealBonus} — {@code LifeStealService} applies it only when the holder already
     * has an active {@code LifeSteal}, so a bat with no Roubo de Vida to amplify still steals
     * nothing.
     */
    @Override
    public int resolveLifeStealBonus(final Character character, final CombatantSheet sheet) {
        return wornForma(sheet) == FormaMetamorfica.MORCEGO_ATROZ ? MORCEGO_LIFE_STEAL_BONUS : 0;
    }
}
