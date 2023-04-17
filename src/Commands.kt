import kotlin.random.Random

interface Command {
    var receiver: Hero
    var cardName: String
    fun execute()
}
class Abandon(r: Hero) : Command {
    override var receiver = r
    override var cardName = "Acedia"
    override fun execute() {
        if (Singleton.deck.size == 0) Singleton.deck = BasicCardFactory.shuffle(Singleton.graveyard)
        println("Judgement for Acedia: ${Singleton.deck[0].name}, ${Singleton.deck[0].rank} of ${Singleton.deck[0].suit}.")
        if (Singleton.deck[0].suit != "Heart") {
            receiver.abandon = true
            println("Acedia. ${receiver.name}'s round got abandoned")
        } else {
            receiver.abandon = false
            println("Acedia voided.")
        }
        Singleton.graveyard.add(receiver.delayTatics[receiver.hasAcediaOnBoard()])
        receiver.delayTatics.removeAt(receiver.hasAcediaOnBoard())
        Singleton.graveyard.add(Singleton.deck[0])
        Singleton.deck.removeAt(0)
    }
}

class Lightning(r: Hero) : Command {
    override var receiver = r
    override var cardName = "Lightning Bolt"
    override fun execute() {
        if (Singleton.deck.size == 0) Singleton.deck = BasicCardFactory.shuffle(Singleton.graveyard)
        println("Judgement for Lightning Bolt: ${Singleton.deck[0].name}, ${Singleton.deck[0].rank} of ${Singleton.deck[0].suit}.")
        if (Singleton.deck[0].suit == "Spade" && Singleton.deck[0].rankNum >= 2 && Singleton.deck[0].rankNum <= 9) {
            receiver.hp = receiver.hp - 3
            println("Lightning Bolt. ${receiver.name}'s hp - 3, current hp is ${receiver.hp}.")
            receiver.healthCheck()
            Singleton.graveyard.add(receiver.delayTatics[receiver.hasLightningBoltOnBoard()])
            receiver.delayTatics.removeAt(receiver.hasLightningBoltOnBoard())
        } else {
            var nextReceiverIndex: Int = -1
            for ((index, player) in Singleton.heroes.withIndex()) {
                if (player == receiver) {
                    if (index == Singleton.heroes.size-1)
                        nextReceiverIndex = 0
                    else nextReceiverIndex = index + 1
                    while (!Singleton.heroes[nextReceiverIndex].alive) {
                        nextReceiverIndex++
                        if (index == Singleton.heroes.size)
                            nextReceiverIndex = 0
                    }
                    var nextReceiver = Singleton.heroes[nextReceiverIndex]
                    receiver.giveCard(receiver.hasLightningBoltOnBoard(),nextReceiver.delayTatics,receiver.delayTatics,nextReceiver)
                    println("Lightning Bolt voided. Pass to ${nextReceiver.name}.")
                    (nextReceiver.delayTatics[nextReceiver.hasLightningBoltOnBoard()] as LightningBolt).activate()
                    break
                }
            }
        }
        Singleton.graveyard.add(Singleton.deck[0])
        Singleton.deck.removeAt(0)
    }
}

class Dueling(r: Hero): Command {
    lateinit var giver: Hero
    override var receiver = r
    override var cardName = "Duel"
    override fun execute() {
        receiver.beingDuel(giver)
    }
}

class Attacking(s: Hero, r: Hero): Command {
    override var receiver = r
    var attacker = s
    override var cardName = "Attacking"
    override fun execute() {
        receiver.beingAttacked(attacker)
    }
}

//Frost Blade Effect
class BladeDiscard(r: Hero): Command {
    override var receiver = r
    override var cardName = "BladeDiscard"
    override fun execute() {
        receiver.beingDiscard() // --> heros.kt
    }
}

//Twin Swords Effect
class TwinSword(s: Hero, r: Hero): Command {
    override var receiver = r
    override var cardName = "TwinSword"
    var attacker = s
    override fun execute() {
        receiver.beingTwinSword(attacker) // --> heros.kt
    }
}

//Kirin Bowl Effect
class KirinBowDiscard(r: Hero): Command {
    override var receiver = r
    override var cardName = "KirinBowDiscard"
    override fun execute() {
        receiver.beingKirinDiscard() // --> heros.kt
    }
}

interface Subscriber {
    fun update(dodged: Boolean, hp: Int, numOfCards: Int)
}

interface Publisher {
    fun subscribe(s: Subscriber)
    fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int)
    fun removeSubscriber(s: Subscriber)
}

class Announcement() {
    var singleton = Singleton
    var inited = false
    var subs = mutableListOf<Hero>()

    fun initial() {
        if (!inited) {
            for (hero in singleton.heroes) subs.add(hero)
        }
        inited = true
    }
    fun brinkOfDeath(h: Hero) {
        var revived = false
        for (i in subs) {
            revived = i.revive(h)
            if (revived) return
        }
    }

    fun tacticEffect(h: Hero, c: Card, r: Hero): Boolean {
        println("${h.name} used ${c.name} on ${r.name}!")
        var cancelled = false
        for (i in subs) {
            cancelled = i.cancel(h, c, r)
            if (cancelled) return false
        }
        return true
    }
    fun removeSubscriber(h: Hero) {
        subs.remove(h)
    }
}

interface AnnouncementSubs {
    fun revive(h: Hero): Boolean
    fun cancel(h: Hero, c: Card, r: Hero): Boolean
}