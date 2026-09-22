package org.aventyrs.core.title.santo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.EmpoweredAttack;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Fúria dos Deuses — Abraçado pela Escuridão's own per-attack effect: "Sempre que realizar um
 * ataque você pode pagar o Custo de 1PV, 3PV ou 5PV", each tier buying more for that one attack.
 *
 * <p><b>Three tiers, not three abilities</b> ({@link Tier}), because the rules text prices one
 * effect three ways rather than naming three. The player names the tier on the request as its
 * {@code choice}, the base class charges its PV through {@link #resolveHitPointCost} — refusing an
 * activation that would be self-fatal, like every other PV-priced Título trait — and the result
 * reports what the attack gains.
 *
 * <p><b>Reported, not applied</b>, and for the reason every per-attack Título clause here is: the
 * activation happens <em>before</em> the attack is built ("sempre que realizar um ataque você
 * <i>pode</i>" is a declaration, made while choosing the attack), so the caller reads {@link
 * InteractionResult#getEmpoweredAttack()} and folds the figures into the {@code SkillRoll} and
 * {@code DeliveredAttack} it then constructs. That is what closed the "spend a resource for a
 * one-time roll effect" gap for this clause: the transaction is an ordinary Título activation, and
 * the roll it modifies has not happened yet, so nothing has to reach into a resolution in flight.
 *
 * <p>The {@code ActionCost} is {@link org.aventyrs.core.sheet.ActionCost#NONE} and the {@code
 * PDCost} zero because this costs neither — it rides on an attack the holder was making anyway,
 * and is paid for entirely in PV.
 *
 * <p>TODO "O dano deste ataque aumenta 1d6+Vigor" is reported as {@link
 * EmpoweredAttack#extraDamageDice()} 1 plus {@link EmpoweredAttack#extraDamageFlat()} Vigor, and
 * the caller rolls the die — this core never rolls. The rules call it a Corrente de Efeitos, but
 * nothing about it needs one: it is a flat addition to the same dano roll, not a further stage.
 *
 * <p>TODO "Pontos de Vida perdidos desta forma só podem ser recuperados com Descansos Verdadeiros
 * ou efeitos de Roubo de Vida" — no locked-PV subtype exists, the same gap {@code
 * SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} cites.
 */
public class FuriaDosDeusesInteraction extends AbstractTitleAbilityInteraction {

    /**
     * The three prices Fúria dos Deuses can be paid at, and what each buys for the one attack.
     *
     * <p>The 5PV tier is <b>cumulative with the 3PV one</b> ("adicionalmente à redução de GD"),
     * which is why it carries the same {@code difficultyReduction} rather than replacing it. The
     * 1PV tier is the odd one out: it grants Vantagem on the roll instead of easing the GD, and
     * the text never says the two combine — paying 1PV is choosing a different, cheaper effect.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Tier {

        /** "Se utilizar 1PV você recebe Vantagem na rolagem de Perícia de Ataque." */
        VANTAGEM(1, Skill.ADVANTAGE_BONUS, 0, false),

        /** "Se utilizar 3PV reduz a GD da Rolagem de Perícia de Ataque em -1 Nível." */
        REDUCAO_DE_GD(3, 0, 1, false),

        /** "Ao utilizar 5PV, adicionalmente à redução de GD, … O dano deste ataque aumenta 1d6+Vigor." */
        FURIA_MAIOR(5, 0, 1, true);

        private final int hitPointCost;
        private final int attackRollBonus;
        private final int difficultyReduction;
        private final boolean grantsDamage;
    }

    /** "O dano deste ataque aumenta 1d6+Vigor" — the die; the Vigor is resolved per holder. */
    static final int FURIA_MAIOR_DAMAGE_DICE = 1;

    private final HitPointsService hitPointsService;

    public FuriaDosDeusesInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public FuriaDosDeusesInteraction(final DeterminationPointsService determinationPointsService) {
        super(SantoSpecialization.ABRACADO_PELA_ESCURIDAO, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /**
     * The tier is the player's call and the three cost different amounts, so an activation naming
     * none is refused before anything is spent — there is no safe default between them.
     */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getChoice(Tier.class).isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
    }

    /** "você pode pagar o Custo de 1PV, 3PV ou 5PV" — whichever tier the request named. */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return request.getChoice(Tier.class).map(Tier::getHitPointCost).orElse(0);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        Tier tier = request.getChoice(Tier.class).orElseThrow();
        int extraDice = tier.isGrantsDamage() ? FURIA_MAIOR_DAMAGE_DICE : 0;
        int extraFlat = tier.isGrantsDamage()
                ? activator.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR)
                : 0;

        return InteractionResult.builder()
                .empoweredAttack(EmpoweredAttack.ofRoll(tier.getAttackRollBonus(), tier.getDifficultyReduction(),
                        extraDice, extraFlat))
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
