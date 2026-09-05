package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.magic.SpellBranch;
import org.aventyrs.core.magic.SpellTree;

import java.util.Arrays;
import java.util.List;

/**
 * Every ramificação of every diverging {@link MagicTree} — two apiece, for the eighteen of the
 * twenty trees that split. Anulação and Ira de Vulcano run straight through and appear here not
 * at all, which is exactly why their branch gate can never refuse anything.
 *
 * <h2>Named by role, because the document names them by nothing</h2>
 *
 * A ramificação is never given a name anywhere in {@code docs/rules/magias.txt} — a diverging
 * rung is simply two entries, one after the other. What the document <em>does</em> state (L30) is
 * what the two of them are for: "Um deles aprofunda o efeito principal da magia, enquanto outro
 * foca na evolução dos Efeitos Alternativos". So each constant is a {@link MagicTree} paired with
 * a {@link BranchRole}, which is source-backed, rather than a name invented for it.
 *
 * <p><b>Which entry got which role is a reading.</b> The trace is always the same: find the
 * {@code Efeito Alternativo} of the last Magia before the divergence, then see which of the two
 * paths develops it. Each constant records that trace, and says so plainly where the two paths
 * are genuinely hard to tell apart.
 *
 * <h2>One enum, not eighteen</h2>
 *
 * {@link SpellBranch}'s own javadoc calls for a per-tree enum, rejecting "one shared {@code
 * PRIMEIRA}/{@code SEGUNDA} enum reused by every tree" — and this is not that. Every constant
 * here belongs to exactly one tree and is returned by exactly that tree's {@link
 * SpellTree#getBranches()}, so no two trees ever share a branch object and {@code
 * Spell#isEligible}'s identity comparison stays correct. Eighteen two-constant files would carry
 * the same information with no additional guarantee.
 */
public enum MagicBranch implements SpellBranch {

    /**
     * Canção de Flora → Totem de Gaea. Canção de Flora restates the Broto's principal effect
     * outright ("Como Aliado da Natureza"), and Totem de Gaea keeps producing that same Magia's
     * creatures.
     *
     * <p><b>A reading, and a close one.</b> Both paths invoke creatures, and the Alternativo path
     * arguably develops the principal effect too. It is traced this way because only this path
     * names the base Magia in its own text.
     */
    ALIADOS_DA_NATUREZA_PRINCIPAL(MagicTree.ALIADOS_DA_NATUREZA, BranchRole.PRINCIPAL),

    /**
     * Experimento de Larcerto → Orgulho de Lacerto, developing Aliados da Natureza's Efeito
     * Alternativo <i>Predador Regional</i> ("um forte exemplar da sua espécie") into a monstrous
     * one and then a true monster. See the sibling constant — the trace is a reading.
     */
    ALIADOS_DA_NATUREZA_ALTERNATIVO(MagicTree.ALIADOS_DA_NATUREZA, BranchRole.ALTERNATIVO),

    /**
     * Arma Elemental → Fúria Elemental — the offensive line, deepening the Semente's own principal
     * effect (elemental damage dealt to whoever attacks you).
     */
    ARSENAL_ELEMENTAL_PRINCIPAL(MagicTree.ARSENAL_ELEMENTAL, BranchRole.PRINCIPAL),

    /**
     * Bastião Elemental → Armadura Elemental, evolving Retaliação Elemental's Efeito Alternativo
     * <i>Proteção Elemental</i> ("Ao invés de causar dano você pode receber Bônus Elemental de
     * +Metade do Foco em suas Defesas"). An unusually clean trace: every rung of this branch is
     * about Defesas and Resistência, and every rung of the other about damage.
     */
    ARSENAL_ELEMENTAL_ALTERNATIVO(MagicTree.ARSENAL_ELEMENTAL, BranchRole.ALTERNATIVO),

    /**
     * Restaurar Objetos — the line that keeps acting on equipment the character already owns,
     * which is Aprimorar's own principal effect (turning a mundane item into an Obra-Prima).
     * <b>A weak trace.</b> Aprimorar's Efeito Alternativo <i>Reforjar</i> also acts on an existing
     * item, so the L30 rule separates these two rungs less cleanly than it does elsewhere; what
     * decides it is that only the other branch introduces a capability Aprimorar does not have.
     */
    ARTESAO_PRINCIPAL(MagicTree.ARTESAO, BranchRole.PRINCIPAL),

    /**
     * Invocar Traje de Batalha — the line that creates equipment outright rather than improving
     * what is already there. See the sibling constant: this branch is the one holding something
     * Aprimorar's principal effect cannot do at all.
     */
    ARTESAO_ALTERNATIVO(MagicTree.ARTESAO, BranchRole.ALTERNATIVO),

    /**
     * Corpo Rochoso → Corpo Diamantino — the buff path, deepening Rigidez Térrea's own principal
     * effect of protecting whoever is touched (RD, then half physical damage, then half magical).
     * <b>Traced from the GD line, not from an Efeito Alternativo</b>, because this tree has none
     * anywhere. Neither of these two states "ou DM do Alvo (Maior)"; both rungs of the other branch
     * do, and that floor clause only means anything when the target is unwilling.
     */
    CORPO_ROCHOSO_PRINCIPAL(MagicTree.CORPO_ROCHOSO, BranchRole.PRINCIPAL),

    /**
     * Enrijecer Articulações → Caixão Rochoso — the debuff path, turning the same petrification
     * against an enemy (lost Movimento, then lost Pontos de Ação and crushing damage). Both of its
     * rungs carry the "ou DM do Alvo (Maior)" floor; see the sibling constant.
     */
    CORPO_ROCHOSO_ALTERNATIVO(MagicTree.CORPO_ROCHOSO, BranchRole.ALTERNATIVO),

    /**
     * Projétil Primordial — a ranged Primordial hit, which is Estalo Primordial's principal effect
     * made stronger and longer-ranged.
     * <b>Traced from the principal effect only.</b> This tree diverges at Broto, so the one Magia
     * before the split is its Semente, and that Semente has no Efeito Alternativo — there is
     * nothing for L30's other half to point at. The trace runs the one direction it can.
     */
    DOMINIO_PRIMORDIAL_PRINCIPAL(MagicTree.DOMINIO_PRIMORDIAL, BranchRole.PRINCIPAL),

    /**
     * Égide Primordial — a static barrier, the first thing in this tree that is not a Primordial
     * hit at all. See the sibling constant: it is the Alternativo branch by elimination rather than
     * by tracing an Efeito Alternativo, because none exists to trace.
     */
    DOMINIO_PRIMORDIAL_ALTERNATIVO(MagicTree.DOMINIO_PRIMORDIAL, BranchRole.ALTERNATIVO),

    /**
     * Aura Chocante → Nova Chocante — the damage line, deepening what every rung before the
     * divergence already does: electrical damage to whoever is near or whoever attacks you.
     * <b>Traced from the principal effect only</b>, as with Corpo Rochoso: no Magia in this tree
     * carries an Efeito Alternativo at all, so L30's other half has nothing to point at.
     */
    FURIA_DE_TESLA_PRINCIPAL(MagicTree.FURIA_DE_TESLA, BranchRole.PRINCIPAL),

    /**
     * Passo Relampejante → Ímpeto Trovejante — the speed line, which is the one thing Abraço de
     * Tesla's own effect does not already cover (extra Pontos de Ação, then closing Distância Longa
     * in a single action). See the sibling constant.
     */
    FURIA_DE_TESLA_ALTERNATIVO(MagicTree.FURIA_DE_TESLA, BranchRole.ALTERNATIVO),

    /**
     * Imbuir Fadiga → Onda da Exaustão → Ruína — the pure-maldição line, carrying no damage at all,
     * which is what Impor Arrepsia's own principal effect is. Ruína restates it almost word for word
     * ("não pode utilizar efeitos de Egos"), which is what settles it.
     * <b>This is the one tree where the document's listing order and the content trace disagree.</b>
     * Everywhere else the first-listed entry at a diverging rung is the one deepening the principal
     * effect; here it is the second. The order is never stated to mean anything, so the trace wins.
     */
    MORTE_PRINCIPAL(MagicTree.MORTE, BranchRole.PRINCIPAL),

    /**
     * Toque Antivida → Raio Antivida → Grito da Banshee — the life-drain line, which introduces
     * damage and the caster's own PV cost, neither of which the Semente does. See the sibling
     * constant: this tree's Semente carries no Efeito Alternativo, so this branch is identified by
     * what it adds rather than by what it evolves.
     */
    MORTE_ALTERNATIVO(MagicTree.MORTE, BranchRole.ALTERNATIVO),

    /**
     * Ludibriar os Olhos → Invisibilidade Verdadeira → Campo de Invisibilidade — the invisibility
     * line, which is Ocultar-se nas Sombras' own principal effect (being unseen) freed from its
     * "while in shadow" condition and then made true rather than perceptual.
     * <b>L30's rule does not hold cleanly here, and this constant is where it breaks.</b> Ocultar-se
     * nas Sombras' Efeito Alternativo is <i>Ocultação em Massa</i> — hiding others as well as
     * yourself — and the Magia that evolves it is Campo de Invisibilidade, which sits on <i>this</i>
     * branch, not the other one. Traced by principal effect regardless, since the other branch
     * (shadows as a medium to travel and hide inside) evolves neither half.
     */
    OCULTACAO_PRINCIPAL(MagicTree.OCULTACAO, BranchRole.PRINCIPAL),

    /**
     * Dobrar Sombras → Rastejar nas Sombras → Aspecto Sombrio — the shadow line, treating shadows as
     * a substance to shape, enter and finally become, rather than as cover to hide under. See the
     * sibling constant.
     */
    OCULTACAO_ALTERNATIVO(MagicTree.OCULTACAO, BranchRole.ALTERNATIVO),

    /**
     * Fogo Fátuo → Fogueira Boros → Bolo de Fogo Boros → Valquíria Boros — the Boros line: light,
     * warmth and healing. It is the Principal branch because Fogo Fátuo names Luz de Vela outright
     * ("como se este fosse afetado pela magia Luz de Vela"), which is the Semente's own principal
     * effect, and no Magia here evolves an Efeito Alternativo — the Semente has none.
     * <b>The one branch in the catalog with a name of its own</b>, carried by every Magia on the
     * path. Note this is the second tree (with Morte) where the document's listing order and the
     * content trace disagree: Golpe de Fogo is printed first at Broto.
     */
    PIROMANCIA_PRINCIPAL(MagicTree.PIROMANCIA, BranchRole.PRINCIPAL, "Boros"),

    /**
     * Hálito de Eldur → Bola de Fogo Elduriana → Olhar de Eldur, entered at Broto through Golpe de
     * Fogo — the Eldur line: fire as a weapon, from burning fists to a piercing beam. It develops
     * the ignition half of Luz de Vela rather than its illumination. See the sibling constant.
     */
    PIROMANCIA_ALTERNATIVO(MagicTree.PIROMANCIA, BranchRole.ALTERNATIVO, "Eldur"),

    /**
     * Murcha-Corpo → Serra-Pernas → Toque de Nanicolina → Enfadecer — the shrinking line, taking
     * Rearranjo Corporal's Desvantagem half and driving Força, Destreza and Categoria de Tamanho
     * downward.
     * <b>The weakest trace in the catalog, and honestly close to a coin flip.</b> Rearranjo Corporal
     * does both things symmetrically ("concedendo assim Vantagem ou Desvantagem"), and its Efeito
     * Alternativo (Rearranjo Estendido, which only changes the activation cost) is evolved by
     * neither branch. Decided on the document's own ordering — Murcha-Corpo is printed first at
     * Broto, and shrinking an enemy's arm is the Semente's first worked example.
     */
    POLIMORFISMO_PRINCIPAL(MagicTree.POLIMORFISMO, BranchRole.PRINCIPAL),

    /**
     * Infla-Músculos → Ogrificar → Titânecer → Dracônecer — the growing line, taking Rearranjo
     * Corporal's Vantagem half upward instead. See the sibling constant: the split between these two
     * is real and obvious, but which of them L30 calls "principal" is not.
     */
    POLIMORFISMO_ALTERNATIVO(MagicTree.POLIMORFISMO, BranchRole.ALTERNATIVO),

    /**
     * Drenar Vida → Toque do Ceifeiro → Sacrifico Profano — the line that keeps doing what Lacerar a
     * Alma does: strike one target, drain them, and take the vitality for yourself. Roubo de Vida is
     * carried by every rung of it, which is the Semente's own principal effect.
     * It also picks up the Semente's Efeito Alternativo <i>Arrancar a Alma</i> along the way
     * (Sacrifico Profano recovers from every target reduced to zero or fewer PV) — so on this tree
     * both halves of L30 point the same way, which is unusual and worth noting.
     */
    PROFANAR_PRINCIPAL(MagicTree.PROFANAR, BranchRole.PRINCIPAL),

    /**
     * Solo Profano → Face do Abismo → Coração do Abismo — the area-corruption line, which introduces
     * something Lacerar a Alma does not do at all: an effect that persists in a place rather than on
     * a target. Each rung enhances the one before it in the same footprint.
     */
    PROFANAR_ALTERNATIVO(MagicTree.PROFANAR, BranchRole.ALTERNATIVO),

    /**
     * Coração do Inverno → Muralha de Gelo — the warding line, deepening Corpo Gélido's own
     * principal effect: ice as armour, from a Defesas bonus to outright immunity to the cold, and
     * finally to a wall of it.
     */
    PROTECAO_INVERNAL_PRINCIPAL(MagicTree.PROTECAO_INVERNAL, BranchRole.PRINCIPAL),

    /**
     * Grilhões do Inverno → Prisão Invernal — the binding line, turning the same ice on an enemy to
     * hold them still. The two share a Corrente (<i>Queimaduras de Gelo</i>) that appears nowhere on
     * the other branch, which is what confirms the pairing across the Muda/Emergente rungs.
     */
    PROTECAO_INVERNAL_ALTERNATIVO(MagicTree.PROTECAO_INVERNAL, BranchRole.ALTERNATIVO),

    /**
     * Criar Carniçal → Invocar Abantesma → Festim dos Mortos — the creation line, which is Reanimar's
     * own principal effect (turn a corpse into an undead servant) at ever greater power: a smarter
     * one, then an incorporeal one, then a sarcophagus producing them every Rodada.
     * <b>The document's listing order disagrees</b>, as it does on Morte and Piromancia: Cura Mortis
     * is printed first at Muda. The trace wins, since the order is never stated to mean anything.
     */
    REANIMAR_PRINCIPAL(MagicTree.REANIMAR, BranchRole.PRINCIPAL),

    /**
     * Cura Mortis → Escudo de Cadáveres → Cadáver Instável — the servant line, which never raises a
     * new undead and instead heals, hides behind and finally detonates one you already control.
     * That develops Reanimar's Efeito Alternativo <i>Servo Cadavérico</i>, where the raised body is a
     * Subordinado to be used rather than an independent combatant.
     */
    REANIMAR_ALTERNATIVO(MagicTree.REANIMAR, BranchRole.ALTERNATIVO),

    /**
     * Ressurreição — healing carried to its conclusion, which is Lágrima de Undine's own principal
     * effect. <b>Traced by deity name, which is unusually firm here</b>: the trunk Magia is Lágrima
     * de <i>Undine</i> and this branch's own Efeito Alternativo is Marionete de <i>Undine</i>.
     */
    REGENERACAO_PRINCIPAL(MagicTree.REGENERACAO, BranchRole.PRINCIPAL),

    /**
     * Escarnio de Haloi — the inverted line, evolving Lágrima de Undine's Efeito Alternativo
     * <i>Vingança de <b>Haloi</b></i>, where the touch harms instead of heals and the caster takes
     * the recovery. Both halves of L30 point the same way on this tree, which is rare.
     */
    REGENERACAO_ALTERNATIVO(MagicTree.REGENERACAO, BranchRole.ALTERNATIVO),

    /**
     * Asas nos Pés → Acelerar — the hastening line, carrying forward Aumentar Passos' principal
     * effect (+2UD Movimento) as Pontos de Ação instead.
     */
    TEMPO_PRINCIPAL(MagicTree.TEMPO, BranchRole.PRINCIPAL),

    /**
     * Raízes nos Pés → Lentidão — the slowing line, carrying forward the same Semente's Efeito
     * Alternativo <i>Reduzir Passos</i> (-2UD Movimento), likewise as Pontos de Ação.
     * <b>The cleanest trace in the catalog.</b> L30's two halves are a single sentence apart in the
     * Semente's own descriptor block, and each branch takes exactly one of them. The two even cancel
     * each other by name ("anula os efeitos de Raízes nos Pés", and its mirror).
     */
    TEMPO_ALTERNATIVO(MagicTree.TEMPO, BranchRole.ALTERNATIVO),

    /**
     * Piscar → Teletransporte Verdadeiro → Portal → Portal Planar — the teleportation line, which is
     * Magica de Rua's own principal effect (send something elsewhere and have it arrive where you
     * chose) applied to the caster and then to arbitrary distance and finally to other planes.
     */
    TRANSPORTE_PRINCIPAL(MagicTree.TRANSPORTE, BranchRole.PRINCIPAL),

    /**
     * Montaria na Manga → Carruagem Oculta → Viajante do Aether → Perfuratriz de Sonhos — the vehicle
     * line, which never teleports anything: it summons something that carries you instead. Every rung
     * is a conjured conveyance with its own PV and Movimento Base.
     */
    TRANSPORTE_ALTERNATIVO(MagicTree.TRANSPORTE, BranchRole.ALTERNATIVO),

    /**
     * Revigorar → Revigorar Maior → Nova Rejuvenescedora → Benção da Luz — the healing line, which is
     * Aliviar a Dor's own principal effect (recover PV as though rested) at ever greater tiers of
     * Descanso, ending in a full restoration.
     */
    VIDA_PRINCIPAL(MagicTree.VIDA, BranchRole.PRINCIPAL),

    /**
     * Toque Curativo → Remover Maldição → Exorcizar → Corpo Fechado — the cleansing line, which
     * restores no PV at all and instead strips Malefícios: Doença and Veneno, then Maldição, then
     * Possessão, and finally immunity to every kind at once.
     */
    VIDA_ALTERNATIVO(MagicTree.VIDA, BranchRole.ALTERNATIVO),

    /**
     * Voo Livre → Voo em Massa — the granting line, deepening Levitar's own principal effect: from
     * vertical levitation to true flight, and then to flight for the whole party.
     * <b>The document's listing order disagrees</b> (Purgar Asas is printed first at Muda), as it does
     * on Morte, Piromancia and Reanimar. Four of eighteen trees disagree, which is itself the evidence
     * that the printed order carries no meaning.
     */
    VOO_PRINCIPAL(MagicTree.VOO, BranchRole.PRINCIPAL),

    /**
     * Purgar Asas → Furtar Asas — the denying line, which grants nothing and instead strips flight
     * from someone who has it, finally taking it for the caster. It reaches back past the divergence
     * to the Semente rather than the Broto, describing its victims as falling "como se estivesse sob
     * efeito de Queda Lenta".
     */
    VOO_ALTERNATIVO(MagicTree.VOO, BranchRole.ALTERNATIVO);

    private final MagicTree tree;
    private final BranchRole role;
    private final String authoredName;

    MagicBranch(final MagicTree tree, final BranchRole role) {
        this(tree, role, null);
    }

    MagicBranch(final MagicTree tree, final BranchRole role, final String authoredName) {
        this.tree = tree;
        this.role = role;
        this.authoredName = authoredName;
    }

    /**
     * This ramificação's own name where the document effectively gives it one, and its {@link
     * BranchRole}'s display name otherwise. <b>Only Piromancia's two are named</b>, and there only
     * because every Magia on each path carries a deity's name in its own title (Hálito de
     * <i>Eldur</i>, Fogueira <i>Boros</i>) — that is a name a character sheet can print, so it
     * would be a shame to discard it. No other tree offers anything comparable.
     */
    @Override
    public String getName() {
        return authoredName != null ? authoredName : role.getDisplayName();
    }

    @Override
    public MagicTree getTree() {
        return tree;
    }

    /** Whether this ramificação deepens the Efeito principal or evolves the Efeito Alternativo. */
    public BranchRole getRole() {
        return role;
    }

    /**
     * Both ramificações of tree, in {@link BranchRole} declaration order, or an empty list for a
     * tree that never diverges. Built per call rather than cached on {@link MagicTree}, since
     * that enum names this one right back and a field would make the two static initialisers
     * depend on each other's order — the same deferral {@code TestSpellTree} already uses.
     */
    static List<SpellBranch> of(final MagicTree tree) {
        return Arrays.stream(values())
                .filter(branch -> branch.tree == tree)
                .map(SpellBranch.class::cast)
                .toList();
    }
}
