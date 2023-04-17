import kotlin.random.Random

abstract class Card {
    abstract val name: String
    val suit: String = generateSuit()
    val color: String = markColor()
    val rankNum: Int = Random.nextInt(1, 14)
    val rank: String = generateRank()
    val singleton = Singleton
    lateinit var belongsTo: Hero

    fun generateSuit(): String {
        val suits = listOf("Diamond", "Club", "Heart", "Spade")
        return suits[Random.nextInt(suits.size)]
    }

    fun generateRank(): String = when (rankNum) {
        1 -> "Ace"
        11 -> "Jack"
        12 -> "Queen"
        13 -> "King"
        else -> {
            rankNum.toString()
        }
    }

    fun markColor(): String {
        return if (suit == "Diamond" || suit == "Heart") "Red"
        else "Black"
    }
}

abstract class BasicCard : Card() {

}

abstract class Attack() : BasicCard() {
    override var name: String = "Attack"
    abstract var type: String
}

class BasicAttack : Attack() {
    override var type: String = "Basic"
}

class ThunderAttack : Attack() {
    override var type: String = "Thunder"
}

class FireAttack : Attack() {
    override var type: String = "Fire"
}

class Dodge : BasicCard() {
    override var name: String = "Dodge"
}

class Peach : BasicCard() {
    override var name: String = "Peach"
}

class Wine : BasicCard() {
    override var name: String = "Wine"
}

// Tactics
abstract class TacticsCard : Card() {
    abstract fun activate(): Boolean
}

class SleightOfHand : TacticsCard() {
    override var name = "Sleight of Hand"

    override fun activate(): Boolean {
        if (singleton.announcement.tacticEffect(belongsTo, this, belongsTo)) {
            for (i in 0..1) {
                if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
                singleton.deck[0].belongsTo = belongsTo
                belongsTo.hand.add(singleton.deck[0])
                singleton.deck.removeAt(0)
            }
            println("Drawing 2 cards. ${belongsTo.name} now has ${belongsTo.hand.size} cards.")
            if (belongsTo.hasHeal() >= 0) belongsTo.hasHealCard = true
        }
        return true
    }

}

class ImpeccablePlan : TacticsCard() {
    override val name = "Impeccable Plan"
    var isFinal = true
    lateinit var recieve: Hero
    override fun activate(): Boolean {
        isFinal = singleton.announcement.tacticEffect(belongsTo, this, recieve)
        return true
    }
}

class Duel : TacticsCard() {
    lateinit var target: Hero
    override val name = "Duel"
    override fun activate(): Boolean {
        var duel: Command = Dueling(target)
        (duel as Dueling).giver = belongsTo
        println("${belongsTo.name} asks ${target.name} to fight a duel.")
        duel.execute()
        return true
    }
}

class Duress : TacticsCard() {
    lateinit var targetAttack: Hero
    lateinit var targetDodge: Hero
    var weaponIndex: Int = -1
    override val name = "Duress"
    override fun activate(): Boolean {
        println("${belongsTo.name} force ${targetAttack.name} to attack ${targetDodge.name} under duress.")
        if (targetAttack.hasAttack() != -1) {
            targetAttack.attack(targetDodge)
        } else {
            println("${targetAttack.name} does not attack ${targetDodge.name}, the weapon ${targetAttack.board[weaponIndex].name} is given to ${belongsTo.name}.")
            targetAttack.giveCard(weaponIndex, belongsTo.hand, targetAttack.board, belongsTo)
        }
        return true
    }
}

class Plifer : TacticsCard() {
    override val name = "Plifer"
    override fun activate(): Boolean {
        for (hero in singleton.heroes) {
            if (hero == belongsTo.getTarget(belongsTo, hero, false) && (hero.hand.size != 0 || hero.board.size != 0)) {
                if (hero.hand.size != 0) {
                    hero.giveCard(-1, belongsTo.hand, hero.hand, belongsTo)
                    println("Plifer. ${belongsTo.name} picked a card from ${hero.name}. ")
                } else {
                    println(
                        "Plifer. ${belongsTo.name} picked ${
                            hero.giveCard(
                                -1,
                                belongsTo.hand,
                                hero.board, belongsTo
                            )
                        } from ${hero.name}. "
                    )
                }
                return true
            } else if (hero == belongsTo.getAlly(belongsTo, hero, false) && hero.delayTatics.size != 0) {
                var pickedCardName: String = hero.giveCard(-1, belongsTo.hand, hero.delayTatics, belongsTo)
                println("Plifer. ${belongsTo.name} picked $pickedCardName from ${hero.name}. ")
                for ((index, command) in hero.commands.withIndex()) {
                    if (command.cardName.equals(pickedCardName)) {
                        hero.commands.removeAt(index)
                        break
                    }
                }
                return true
            }
        }
        return false
    }
}

class BurnBridges : TacticsCard() {
    override val name = "Burn Bridges"
    override fun activate(): Boolean {
        for (hero in singleton.heroes) {
            if (hero == belongsTo.getTarget(belongsTo, hero, null) && (hero.hand.size != 0 || hero.board.size != 0)) {
                if (hero.hand.size != 0) {
                    hero.giveCard(-1, Singleton.graveyard, hero.hand, null)
                    println("Burn Bridges. ${belongsTo.name} discard a card from ${hero.name}. ")
                } else {
                    println(
                        "Burn Bridges. ${belongsTo.name} discard ${
                            hero.giveCard(
                                -1,
                                Singleton.graveyard,
                                hero.board, null
                            )
                        } from ${hero.name}. "
                    )
                }
                return true
            } else if (hero == belongsTo.getAlly(belongsTo, hero, false) && hero.delayTatics.size != 0) {
                var discardCardName: String = hero.giveCard(-1, Singleton.graveyard, hero.delayTatics, null)
                println("Burn Bridges. ${belongsTo.name} discard $discardCardName from ${hero.name}. ")
                for ((index, command) in hero.commands.withIndex()) {
                    if (command.cardName.equals(discardCardName)) {
                        hero.commands.removeAt(index)
                        break
                    }
                }
                return true
            }
        }
        return false
    }
}

class OathOfPeachGarden : TacticsCard() {
    override val name = "Oath of Peach Garden"

    override fun activate(): Boolean {
        //move the card from hand to graveyard
        println("${belongsTo.name} uses $name.")

        println("All heroes gain 1 Health.")
        for (hero in Singleton.heroes) {
            if (hero.alive && hero.hp < hero.maxHP) {
                if (singleton.announcement.tacticEffect(belongsTo, this, belongsTo)) { //search for any impeccable plan
                    hero.hp++
                    println("${hero.name}'s current hp is ${hero.hp}")
                }
            }
        }
        return true
    }
}

class Harvest : TacticsCard() {
    override val name = "Harvest"

    override fun activate(): Boolean { // : Boolean
        //move the card from hand to graveyard
        println("${belongsTo.name} uses $name.")

        //alive heroes RANDOMLY pick the cards revealed, starting from self
        var aliveHero = mutableListOf<Hero>()
        for (hero in Singleton.heroes) {
            if (hero.alive) {
                aliveHero.add(hero)
            }
        }

        //pick top x cards from deck, x = no. of alive heros and reveal to all
        var reveal = mutableListOf<Card>()
        for (i in 0 until aliveHero.size) {
            //if out of deck --> shuffle graveyard
            if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)

            //pick the card from deck to reveal
            reveal.add(Singleton.deck.elementAt(0))
            println("${i + 1}'s card to be revealed: ${reveal[i].name}")
            Singleton.deck.removeAt(0)
        }

            //find the position of self in heros list
            var pickerindex = 0
        for ((place, hero) in aliveHero.withIndex()) {
            if (hero == belongsTo) {
                pickerindex = place
                break
            }
        }

            var count = 1 //initialize
        while (count <= aliveHero.size) {
            //println(singleton.announcement.tacticEffect(belongsTo, this, belongsTo))
            //if (singleton.announcement.tacticEffect(belongsTo, this, belongsTo)) {
            var randompick = Random.nextInt(0, reveal.size)
            println("${Singleton.heroes[pickerindex].name} picks ${reveal[randompick].name}")
            reveal[randompick].belongsTo = Singleton.heroes[pickerindex] //assign belongsTo
            Singleton.heroes[pickerindex].hand.add(reveal[randompick]) //add into hand
            reveal.removeAt(randompick)

            if (pickerindex == Singleton.heroes.size - 1) {
                pickerindex = 0
            } else pickerindex++
            count++
            //}
        }

            //FOR Impeccable: if someone is banned, he/she cannot pick the card. The wasted card will put into graveyard.
            while (reveal.size > 0) { //cannot use (reveal != null)
                singleton.graveyard.add(reveal[0]) //avoid concurrent modification error
                reveal.removeAt(0)
            }
        //}
        return true
    }
}

class BarbariansAssault : TacticsCard() {
    override val name = "Barbarians Assault"

    override fun activate(): Boolean {
        //move the card from hand to graveyard
        println("${belongsTo.name} uses $name.")

        //        if (singleton.announcement.tacticEffect(belongsTo, this, belongsTo)) {
            //find the position of self in heros list
            var targetindex = 0
            for ((place, hero) in Singleton.heroes.withIndex()) {
                if (hero == belongsTo) {
                    if (place + 1 == Singleton.heroes.size) { //if the hero next to self out of bound
                        targetindex = 0
                    } else targetindex = place + 1 //starting from the hero next to self
                    break
                }
            }

            //alive heroes have to use attack() card to dodge, except self, otherwise hp-1
            var count = 1
            while (count < Singleton.heroes.size) { //except self
                var heroBA = Singleton.heroes[targetindex]
                var attackindex = heroBA.hasAttack()
                if (heroBA.alive && attackindex != -1) {
                    println("${heroBA.name} uses an 'Attack' to dodge.")
                    Singleton.graveyard.add(heroBA.hand[attackindex])
                    heroBA.hand.removeAt(attackindex)
                } else {
                    if(heroBA.alive) {
                        heroBA.hp--
                        println("${heroBA.name} do not have 'Attack'. 1 damage from ${belongsTo.name}, current hp is ${heroBA.hp}")
                    }

                    //copy from beingAttacked() to check hp
                    if (heroBA.hp <= 0 && heroBA.alive) {
                        singleton.announcement.brinkOfDeath(heroBA)

                        if (heroBA.hp <= 0) {
                            heroBA.alive = false
                            println("${heroBA.name} has died.")
                            if (heroBA is WeiHero) {
                                (MonarchFactory.monarch as MonarchHero).removeSubscriber(heroBA as Subscriber)
                            }
                            singleton.announcement.removeSubscriber(heroBA)
                            for (i in 0 until heroBA.hand.size) {
                                singleton.graveyard.add(heroBA.hand[0])
                                heroBA.hand.removeAt(0)
                            }
                        }
                    }
                    if (heroBA.hp <= 2) {
                        (heroBA.str as BasicStrategy).changeState(UnhealthyState.factory(heroBA.str, heroBA))
                    }
                }
                count++
                if (targetindex == Singleton.heroes.size - 1) {
                    targetindex = 0
                } else targetindex++
            }
//        }
        return true
    }
}

    class HailOfArrows : TacticsCard() {
        override val name = "Hail of Arrows"

        override fun activate(): Boolean {
            //move the card from hand to graveyard
            println("${belongsTo.name} uses $name.")

//            if (singleton.announcement.tacticEffect(belongsTo, this, belongsTo)) {

                //find the position of self in heros list
                var targetindex = 0
                for ((place, hero) in Singleton.heroes.withIndex()) {
                    if (hero == belongsTo) {
                        if (place + 1 == Singleton.heroes.size) { //if the hero next to self out of bound
                            targetindex = 0
                        } else targetindex = place + 1 //starting from the hero next to self
                        break
                    }
                }

                //alive heroes have to use dodge, except self, otherwise hp-1
                var count = 1
                while (count < Singleton.heroes.size) { //except self
                    if (Singleton.heroes[targetindex].alive) Singleton.heroes[targetindex].beingAttacked(belongsTo)
                    count++
                    if (targetindex == Singleton.heroes.size - 1) {
                        targetindex = 0
                    } else targetindex++
                }
//            }
            return true
        }
    }

class Acedia : TacticsCard() {
    lateinit var target: Hero
    override val name = "Acedia"
    override fun activate(): Boolean {
        belongsTo.setCommand(Abandon(target))
        return true
    }
}

class LightningBolt : TacticsCard() {
    override val name = "Lightning Bolt"
    override fun activate(): Boolean {
        belongsTo.setCommand(Lightning(belongsTo))
        return true
    }
}

    abstract class WeaponCard() : Card() {
        abstract var attackRange: Int
    }


    //Weapons
    class ZhugeCrossbow : WeaponCard() {
        override var name: String = "Zhuge Crossbow"
        override var attackRange: Int = 1

    }

    class SwordOfBlueSteel : WeaponCard() {
        override var name: String = "Sword Of Blue Steel"
        override var attackRange: Int = 2

    }

    class FrostBlade : WeaponCard() {
        override var name: String = "Frost Blade"
        override var attackRange: Int = 2

    }

    class TwinSwords : WeaponCard() {
        override var name: String = "Twin Swords"
        override var attackRange: Int = 2
    }

    class AzureDrangonCrescentBlade : WeaponCard() {
        override var name: String = "Azure Drangon Crescent Blade"
        override var attackRange: Int = 3
    }

    class SerpentSpear : WeaponCard() {
        override var name: String = "Serpent Spear"
        override var attackRange: Int = 3
    }

    class RockCleavingAxe : WeaponCard() {
        override var name: String = "Rock Cleaving Axe"
        override var attackRange: Int = 3
    }

    class HeavenHalberd : WeaponCard() {
        override var name: String = "Heaven Halberd"
        override var attackRange: Int = 4
    }

    class KirinBow : WeaponCard() {
        override var name: String = "Kirin Bow"
        override var attackRange: Int = 5
    }

    abstract class ArmorCard : Card() {

    }

    //Armor
    class EightTrigrams : ArmorCard() {
        override var name: String = "Eight Trigrams"
    }

    abstract class MountCard : Card() {
        abstract var mountRange: Int
    }

    //Mounts
    class RedHare : MountCard() {
        override var name: String = "Red Hare"
        override var mountRange: Int = -1
    }

    class DaYuan : MountCard() {
        override var name: String = "Da Yuan"
        override var mountRange: Int = -1
    }

    class HuaLiu : MountCard() {
        override var name: String = "HuaLiu"
        override var mountRange: Int = 1
    }

    class TheShadow : MountCard() {
        override var name: String = "The Shadow"
        override var mountRange: Int = 1
    }

    interface DeckFactory {
        open fun standardDeck(): MutableList<Card>
    }

    object BasicCardFactory : DeckFactory {

        private fun generateAttack(): Card {
            var gen = Random.nextInt(0, 3)

            return when (gen) {
                0 -> BasicAttack() as Card
                1 -> FireAttack() as Card
                2 -> ThunderAttack() as Card
                else -> {
                    BasicAttack() as Card
                }
            }
        }

        /* Create a standard deck with 104 cards and shuffle.
            30 attack, 15 dodge, 8 peaches, 30 instant tactics, 4 delayed tactics, 9 weapons, 2 armor, and 6 horses. */
        override fun standardDeck(): MutableList<Card> {
            var temp = mutableListOf<Card>()

            // Add basic 52 cards
            for (i in 1..30) temp.add(generateAttack())
            for (i in 1..15) temp.add(Dodge())
            for (i in 1..8) temp.add(Peach())

            /* For instant tactics: 6 Burn Bridges, 5 Plifer, 4 Sleight of Hand, 3 Impeccable Plan, Barbarians Assault,
        and Duel, 2 Duress and Harvest, 1 Hail of Arrows and Oath of Peach Garden. */
            for (i in 1..6) temp.add(BurnBridges())
            for (i in 1..5) temp.add(Plifer())
            for (i in 1..4) temp.add(SleightOfHand())
            for (i in 1..3) {
                temp.add(ImpeccablePlan())
                temp.add(BarbariansAssault())
                temp.add(Duel())
            }
            for (i in 1..2) {
                temp.add(Duress())
                temp.add(Harvest())
            }
            // add arrows and garden
            temp.add(HailOfArrows())
            temp.add(OathOfPeachGarden())

            /* For delayed tactics: 3 Acedia and 1 Lightning Bolt. */
            for (i in 1..3) temp.add(Acedia())
            temp.add(LightningBolt())

            /* For weapons: 2 Zhuge Crossbow and 1 for the rest. */
            val weapons = mutableListOf<Card>(
                SwordOfBlueSteel(), FrostBlade(), TwinSwords(), AzureDrangonCrescentBlade(),
                SerpentSpear(), RockCleavingAxe(), HeavenHalberd(), KirinBow()
            )
            for (i in 1..2) temp.add(ZhugeCrossbow())
            // add other weapons
            for (i in 1..weapons.size - 1) temp.add(weapons[i])

            /* For armor: 2 Eight Trigrams. */
            for (i in 1..2) temp.add(EightTrigrams())

            /* For mounts: 3 of each type (+1 and -1). */
            val mounts = mutableListOf<Int>(1, 2)
            for (a in 1 until mounts.size) {
                for (i in 1..2) {
                    when (a) {
                        1 -> temp.add(RedHare())
                        2 -> temp.add(DaYuan())
                    }
                }
            }
            temp.add(HuaLiu())
            temp.add(TheShadow())

            return shuffle(temp)
        }

        fun shuffle(deck: MutableList<Card>): MutableList<Card> {
            var newDeck = mutableListOf<Card>()

            println("Deck shuffled with list below ->")
            for (i in 0 until deck.size) {
                var add = Random.nextInt(0, deck.size)
                newDeck.add(deck[add])

                println("Card[$i]: ${deck[add].name}")
                deck.removeAt(add)
            }
            println()

            return newDeck
        }
    }