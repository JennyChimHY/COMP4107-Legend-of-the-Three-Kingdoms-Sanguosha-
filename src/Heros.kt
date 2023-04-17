import java.lang.Math.*
import kotlin.random.*

abstract class Hero(var r: Role) : Role by r, AnnouncementSubs, Subscriber {
    abstract var name: String
    abstract var maxHP: Int
    abstract var hp: Int
    abstract var gender: Boolean //T: Male; F: Female
    var abandon: Boolean = false
    var alive: Boolean = true
    var canAttackAgain: Boolean = true
    var hasHealCard: Boolean = true
    lateinit var str: Strategy
    var hand = mutableListOf<Card>()
    var board = mutableListOf<Card>() //cards placed on the board
    var delayTatics = mutableListOf<Card>()
    var commands = mutableListOf<Command>() //Delay tactics
    var canET: Boolean = true

    fun heal() {
        if (hasHeal() >= 0) {
            singleton.graveyard.add(hand[hasHeal()])
            hand.removeAt(hasHeal())
        }
        if (hp < maxHP) this.hp++
        if (hasHeal() == -1) hasHealCard = false
        println("$name healed themselves, their health is now $hp")

        if (this.hp >= 3) {
            (this.str as BasicStrategy).changeState(HealthyState.factory(this.str, this))
        }
    }

    open fun hasHeal(): Int {
        for ((place, card) in hand.withIndex()) {
            if (card.name == "Peach") return place
        }
        return -1
    }


    fun setStrategy(s: Strategy) {
        this.str = s
    }

    open fun drawCards() {
        for (i in 0..1) {
            if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
            singleton.deck[0].belongsTo = this
            hand.add(singleton.deck[0])
            singleton.deck.removeAt(0)
        }
        println("Drawing 2 cards. ${this.name} now has ${this.hand.size} cards.")
        if (hasHeal() >= 0) hasHealCard = true
    }

    open fun discardCards() {
        canAttackAgain = true
        var diff: Int = this.hand.size - this.hp
        while (diff > 0) {
            println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
            this.str.selectCardToDiscard()
            singleton.graveyard.add(hand[0])
            hand.removeAt(0)
            diff--
        }
        println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
        if (hasHeal() == -1) hasHealCard = false
    }

    open fun attack(t: Hero?) {     //some weapon not modify to check if target is real target to perform the effects
        //for Kirin Bow Effect
        var kirindiscard: Command? = null

        //for Serpent Spear Effect
        var sS: Boolean = false
        if (hasSerpentSpear() != -1 && hasAttack() == -1 && this.hand.size >= 2)
            sS = true

        // For Azure & Duress
        var target: Hero? = null
        var trackHp = -1

        if (t == null) {
            for (hero in Singleton.heroes) {
                var a = getTarget(this, hero, true)
                if (a == hero) {
                    trackHp = hero.hp
                    target = hero
                    println("${this.name} is a ${this.roleTitle}, attack ${this.getEnemy()}.")
                    break
                }
            }
        } else {
            target = t
        }

        if ((hasAttack() >= 0 || sS) && target != null) {

            if (sS) { //discard 2 cards from hand instead
                println("${this.name} can discard 2 cards as Attack by Serpent Spear Effect without using Attack card.")
                for(i in 0..1) {
                    var random = Random.nextInt(0, this.hand.size)
                    println("His hand has: $hand and Discared: ${hand[random]}.")
                    singleton.graveyard.add(hand[random])
                    hand.removeAt(random)
                }
            } else {
                singleton.graveyard.add(hand[hasAttack()])
                hand.removeAt(hasAttack())
            }

            //with no weapon
            var attack: Command = Attacking(this, target!!)
            println("$name attacks ${target.name}")
            this.canAttackAgain = false
            if (this is LuMeng) attacked = true

            if (this.board.size > 0) { //with weapon
                for (equip in this.board) {
                    when (equip) {
                        is FrostBlade -> if (target.hand.size >= 2) attack = BladeDiscard(target) //FrostBlade effect --> command
                        is TwinSwords -> if ((!this.gender) == target.gender) attack = TwinSword(this, target) //TwinSword effect --> command
                        is HeavenHalberd -> if (this.hand.size == 0) { //can target 2 more heroes
                            println("${this.name} can attack up to two more heroes with Heaven Halberd.")
                            var attackHH: Command? = null //if target not found then null --> no need to execute additional commands
                            var attackHH2: Command? = null
                            var heroHHattacked: Hero? = null
                            for (heroHH in Singleton.heroes) {
                                if (Singleton.heroes.size > 2) { //1st target, enough enemy
                                    if (heroHH != target && heroHH == this.getTarget( this, heroHH,true)) {
                                        heroHHattacked = heroHH
                                        attackHH = Attacking(this, heroHH)
                                        println("$name attacked ${heroHH.name}")
                                        break
                                    }
                                }
                            }

                            if (Singleton.heroes.size > 3 && heroHHattacked != null) { //2nd target, enough enemy
                                for (heroHH2 in Singleton.heroes) {
                                    if (heroHH2 != target && heroHH2 != heroHHattacked && heroHH2 == this.getTarget(this, heroHH2,true)) {
                                        attackHH2 = Attacking(this, heroHH2)
                                        println("$name attacked ${heroHH2.name}")
                                        break
                                    }
                                }
                            }
                            attackHH?.execute()
                            attackHH2?.execute()
                        }
                        is KirinBow -> {
                            println("${this.name} may discard one of ${target.name}'s equipped mounts by Kirin Bow Attack")
                            kirindiscard = KirinBowDiscard(target)
                        }

                    }
                }
            }

            attack.execute()
            if (kirindiscard != null) kirindiscard.execute()

            for (hero in Singleton.heroes) {
                if (target != null && getTarget(this, hero, true) == target && trackHp == target.hp) {
                    for (equip in board) {
                        if (equip is ZhugeCrossbow) {
                            println("${equip.name} allows me to attack again")
                            this.canAttackAgain = true
                        }
                        // Azure Dragon Crescent Blade effect
                        else if (equip is AzureDrangonCrescentBlade) {
                            if (target != null && trackHp == target.hp) {
                                canAttackAgain = true
                                if (hasAttack() != -1) {
                                    println("Against Azure Dragon Crescent Blade, dodging is meaningless.")
                                    println("I $name can immediately attack again.")
                                }
                            }
                        }
                        // Rock Cleaving Axe effect
                        else if (equip is RockCleavingAxe) {
                            if (target != null && trackHp == target.hp) {
                                if (hand.size + board.size >= 2) {
                                    println("But it was useless... ")
                                    print("$name discarded 2 cards to ignore the dodge")

                                    if (hand.size >= 2) {
                                        for (i in 0..1) {
                                            var random = Random.nextInt(0, this.hand.size)
                                            singleton.graveyard.add(hand[random])
                                            hand.removeAt(random)
                                        }
                                    } else {
                                        singleton.graveyard.add(hand[0])
                                        hand.removeAt(0)
                                        singleton.graveyard.add(board[0])
                                        board.removeAt(0)
                                    }

                                    println(", they now have ${hand.size} cards.")

                                    target.hp--
                                    println("${target.name} got hit by Rock Cleaving Axe, current hp is ${target.hp}")
                                    target.healthCheck()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //attack -->  true:  attackRange;   false:  normalRange
    //calculate the attack range according to board and find the possible target within the range
    open fun calculateRange(self: Hero, t: Hero, attack: Boolean): Boolean {
        if (t is ZhugeLiang && t.hand.size == 0) {
            if (!t.printed) {
                println("${t.name} has an empty fort, cannot be attacked.")
                t.printed = true
            }
            return false
        }

        if (self is GuanYuAdapter) {
            if (self.lastAtk == "Diamond") {
                if (!self.printedR) {
                    println("My Diamond attacks are not bound by range.")
                    self.printedR = true
                }
                return true
            }
        }

        if (self is ZhangFei && self.attacked) {
            if (!self.printed) {
                println("I attacked, all attacks next are not bound by range.")
                self.printed = true
            }
            return true
        }

        var selfRange: Int = 0
        var tRange: Int = 0

//        println("----Enter calculateRange----")
        for ((index, card) in Singleton.heroes.withIndex()) { //find the position of self and the target
            when (card) {
                self -> selfRange = index
                t -> tRange = index
            }
        }

        var d1 = (selfRange - tRange + Singleton.heroes.size) % Singleton.heroes.size
        var d2 = (tRange - selfRange + Singleton.heroes.size) % Singleton.heroes.size
        var distance = min(d1, d2) //basic range, without equipping weapons/mounts

        var sWeapon: Int = 0
        var sMount: Int = 0
        var tMount: Int = 0

        for (card in self.board) {
            if (card is WeaponCard) sWeapon =
                card.attackRange  //find the weapons of self for calculating the attack range
            else if (card is MountCard && card.mountRange < 0) sMount += abs(card.mountRange) //reduce the distance of self to target
        }

        for (card in t.board) { //find the equipment of target for calculating the latest range
            if (card is MountCard && card.mountRange > 0) tMount += card.mountRange //add the distance of target to self
        }

//        var distance = abs(selfRange - tRange) //mod % Singleton.heroes.size-1?
//        println("s: $self sWeapon+Mount: ${sWeapon + sMount} t: $t distance + Tmount: ${distance + tMount}" )
        if (attack)
            return ((sWeapon + sMount) >= (distance + tMount))
        else
            return ((distance + tMount - sMount) <= 1)
    }

    open fun hasAttack(): Int {
        for ((place, card) in hand.withIndex()) if (card.name == "Attack") return place
        return -1
    }

    open fun hasNumOfAttack(): Int {
        var num = 0
        for (card in hand)
            if (card.name == "Attack")
                num++
        return num
    }

    open fun templateMethod() {
        println(this.name + "'s turn:")
        executeCommand()
        if (this.alive) {
            drawCards()
            if (!abandon)
                playCards()
            else abandon = false
            discardCards()
        }
    }

    open fun playCards() {
        var play = true
        while (this.hand.size != 0 && play) {
            play = this.str.playNextCard()
        }
    }

    open fun dodgeAttack(): Boolean { //for Caocao Handler
        if (hasDodge() >= 0) {
            singleton.graveyard.add(hand[hasDodge()])
            hand.removeAt(hasDodge())
            return true
        }
        return false
    }

    open fun hasDodge(): Int {
        for ((place, card) in hand.withIndex()) if (card.name == "Dodge") return place
        return -1
    }

    open fun beingDuel(giver: Hero) {
        if (this.hasAttack() != -1) {
            println("${this.name} attacks ${giver.name}")
            singleton.graveyard.add(hand[hasAttack()])
            hand.removeAt(hasAttack())
            giver.beingDuel(this)
        } else {
            this.hp--
            println("${this.name} is unable to attack ${giver.name}, hp -1, current hp is ${this.hp}")
        }
    }

    open fun beingAttacked(s: Hero) { //s: Hero -- for Xiahou Dun override
        var dodged = false
        var byET = false
        if (canET) {
            for (equip in board) {
                if (equip is EightTrigrams) {
                    if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
                    println("Judgement for Eight Trigrams: ${singleton.deck[0].name}, ${singleton.deck[0].rank} of ${singleton.deck[0].suit}.")
                    if (singleton.deck[0].color == "Red") {
                        println("Success, dodged attack.")
                        dodged = true
                        byET = true
                    } else println("Failed, need a dodge card.")
                    singleton.graveyard.add(singleton.deck[0])
                    singleton.deck.removeAt(0)
                    break
                }
            }
        }
        if (!dodged) dodged = dodgeAttack()

        if (dodged) {
            if (byET) println("${this.name} used Eight Trigtams to dodged attack, current hp is ${this.hp}")
            else println("${this.name} used a dodge card to dodged attack, current hp is ${this.hp}")
        } else {
            this.hp--
            println("${this.name} is unable to dodge attack, current hp is ${this.hp}")
            if (this is XiahouDun) this.attackedXia = true
        }
        if (this.roleTitle.equals("Monarch")) {
            // (this as MonarchHero).notifySubscribers(dodged, this.hp, this.hand.size)
        }

        healthCheck()

        if (this.hp <= 2) {
            (this.str as BasicStrategy).changeState(UnhealthyState.factory(this.str, this))
        }
    }

    fun healthCheck() {
        if (this.hp <= 0 && alive) {
            singleton.announcement.brinkOfDeath(this)

            if (hp <= 0) {
                this.alive = false
                println("${this.name} has died.")
                if (this is WeiHero) {
                    (MonarchFactory.monarch as MonarchHero).removeSubscriber(this as Subscriber)
                }
                singleton.announcement.removeSubscriber(this)
                for (i in 0 until hand.size) {
                    singleton.graveyard.add(hand[0])
                    hand.removeAt(0)
                }
            }
        }
    }

    open fun beingDiscard() { //Frost Blade, this = 'victim', receiver
        var dodged = dodgeAttack()
        var count = 1
        if (dodged) {
            println("${this.name} used a dodge card to dodged Frost Blade attack.")
        } else if (this.roleTitle.equals("Monarch")) {
            (this as MonarchHero).notifySubscribers(dodged, this.hp, this.hand.size)
        } else {
            //board --> remove weapons/mounts *2
            //hand --> randomly discard 2 cards
            println("${this.name} is unable to dodge Frost Blade attack.")
            for (equip in this.board) {
                if (equip is WeaponCard || equip is MountCard) {
                    Singleton.graveyard.add(equip)
                    this.board.remove(equip)
                    println("${equip.name} is discarded.")
                    count++
                    if (count == 2)
                        break
                }
            }

            while (count <= 2) {
                var index = Random.nextInt(0, this.hand.size)
                Singleton.graveyard.add(this.hand[index])
                this.hand.removeAt(index)
                println("A card from hand is discarded.")
                count++
            }
            println("${this.name} has ${this.hand.size} card(s) now.")
        }
    }

    open fun beingTwinSword(attacker: Hero) { //Twin Swords, this = 'victim', receiver
        var dodged = dodgeAttack()
        if (dodged) {
            println("${this.name} used a dodge card to dodged Twin Swords attack.")
        } else if (this.roleTitle.equals("Monarch")) {
            (this as MonarchHero).notifySubscribers(dodged, this.hp, this.hand.size)
        } else {
            //choice 1: Discard a card from my hand
            //choice 2: Attacker draw a card
            var ran = 0
            if (this.hand.size <= 0) ran = 1 //cannot discard a card from hand
            else ran = Random.nextInt(0, 1)

            when (ran) {
                0 -> {
                    println("${this.name} has to discard a card from hand by Twin Swords Attack. Currently has ${this.hand.size} cards.")
                    var discardindex = Random.nextInt(0, this.hand.size)
                    singleton.graveyard.add(this.hand[discardindex])
                    this.hand.removeAt(discardindex)
                }
                1 -> {
                    println("${attacker.name} can draw one more card by Twin Swords Attack. Currently has ${attacker.hand.size} cards.")
                    attacker.hand.add(Singleton.deck.elementAt(0))
                    attacker.hand[attacker.hand.size - 1].belongsTo = attacker
                    Singleton.deck.removeAt(0)
                }
            }
        }
    }

    open fun beingKirinDiscard() {
        for (i in 0 until this.board.size) {
            var cardname = this.board[i].name
            if (this.board[i] is MountCard) {
                Singleton.graveyard.add(this.board[i])
                this.board.removeAt(i)
                println("${this.name}'s $cardname is discarded.")
            }
            break
        }
    }

    fun setCommand(c: Command) {
        c.receiver.commands.add(c)
        println("${c.receiver.name} being placed the ${c.cardName} card.")
    }

    fun executeCommand() {
        while (!commands.isEmpty() && this.alive) {
            commands[0].execute()
            commands.remove(commands[0])
        }
    }

    override fun update(dodged: Boolean, hp: Int, numOfCards: Int) {
        TODO("Not yet implemented")
    }

    override fun revive(h: Hero): Boolean {
        if (askRevive(h) && hasHeal() != -1) {
            println("$name saved ${h.name} from the brink of death!")
            singleton.graveyard.add(hand[hasHeal()])
            hand.removeAt(hasHeal())
            h.hp++
            return true
        } else println("$name can't save ${h.name}")
        return false
    }

    override fun cancel(h: Hero, c: Card, r: Hero): Boolean {
        var cancel = cancelEffect(h)

        if (cancel && hasImpeccable() != -1) {
            var activate = (hand[hasImpeccable()] as ImpeccablePlan)

            singleton.graveyard.add(hand[hasImpeccable()])
            hand.removeAt(hasImpeccable())

            activate.recieve = h
            activate.activate()
            if (activate.isFinal) {
                return true
            }
        }
        return false
    }

    fun hasImpeccable(): Int {
        for ((place, card) in hand.withIndex()) if (card is ImpeccablePlan) return place
        return -1
    }

    fun hasSleight(): Int {
        for ((place, card) in hand.withIndex()) if (card is SleightOfHand) return place
        return -1
    }

    fun hasWeaponsMounts(): Int {  //check whether the player has weapons or mounts in hand
        for ((place, card) in hand.withIndex()) if (card is WeaponCard || card is MountCard || card is ArmorCard) return place
        return -1
    }

    fun hasSerpentSpear() : Int {
        for ((place, card) in board.withIndex()) if (card is SerpentSpear) return place
        return -1
    }

    fun replace(listToReplace: MutableList<Card>, oldIndex: Int, newFrom: MutableList<Card>, newIndex: Int) {
        Singleton.graveyard.add(listToReplace[oldIndex])
        println("${newFrom[newIndex].name} is equipped by replacing ${listToReplace[oldIndex].name}.")
        listToReplace.removeAt(oldIndex)
        listToReplace.add(newFrom[newIndex])
        newFrom.removeAt(newIndex)
    }

    fun hasPeachGarden(): Int {
        for ((place, card) in hand.withIndex()) if (card is OathOfPeachGarden) return place
        return -1
    }

    fun hasHarvest(): Int {
        for ((place, card) in hand.withIndex()) if (card is Harvest) return place
        return -1
    }

    fun hasBarbariansAssault(): Int {
        for ((place, card) in hand.withIndex()) if (card is BarbariansAssault) return place
        return -1
    }

    fun hasHailOfArrows(): Int {
        for ((place, card) in hand.withIndex()) if (card is HailOfArrows) return place
        return -1
    }

    fun hasDuel(): Int {
        for ((place, card) in hand.withIndex()) if (card is Duel) return place
        return -1
    }

    fun hasAcedia(): Int {
        for ((place, card) in hand.withIndex()) if (card is Acedia) return place
        return -1
    }

    fun hasAcediaOnBoard(): Int {
        for ((place, card) in delayTatics.withIndex()) if (card is Acedia) return place
        return -1
    }

    fun hasLightningBolt(): Int {
        for ((place, card) in hand.withIndex()) if (card is LightningBolt) return place
        return -1
    }

    fun hasLightningBoltOnBoard(): Int {
        for ((place, card) in delayTatics.withIndex()) if (card is LightningBolt) return place
        return -1
    }

    fun hasDuress(): Int {
        for ((place, card) in hand.withIndex()) if (card is Duress) return place
        return -1
    }

    fun hasPlifer(): Int {
        for ((place, card) in hand.withIndex()) if (card is Plifer) return place
        return -1
    }

    open fun hasBurnBridges(): Int { //open for Gan Ning
        for ((place, card) in hand.withIndex()) if (card is BurnBridges) return place
        return -1
    }

    fun giveCard(cardPlace: Int, to: MutableList<Card>, from: MutableList<Card>, newOwner: Hero?): String {
        var place: Int
        if (cardPlace == -1) {
            place = Random.nextInt(0, from.size)
        } else {
            place = cardPlace
        }
        if (newOwner != null) {
            from[place].belongsTo = newOwner
        }
        to.add(from[place])
        var cardName = from[place].name
        from.removeAt(place)
        return cardName
    }

}

abstract class MonarchHero(r: Role) : Hero(r), Publisher by r as Publisher {
    override var maxHP: Int = 5
    override var hp: Int = 5
}

abstract class WarriorHero(r: Role) : Hero(r) {
    override var maxHP: Int = 4
    override var hp: Int = 4
}

abstract class AdvisorHero(r: Role) : Hero(r) {
    override var maxHP: Int = 3
    override var hp: Int = 3
}

interface Handler {
    fun setNext(h: Handler)
    fun handle(): Boolean
}

abstract class WeiHero(r: Role) : Hero(r), Handler { //dodge
    var n: Handler? = null
    override fun setNext(h: Handler) {
        this.n = h
    }

    override fun handle(): Boolean {
        if (this.alive) {
            if (hasDodge() != -1 && this.roleTitle != "Rebel") {
                singleton.graveyard.add(hand[hasDodge()])
                hand.removeAt(hasDodge())
                println("${this.name} spent 1 card to help his/her lord to dodge.")
                return true
            } else if (n != null) {
                println("${this.name} doesn't want to help.")
                return n!!.handle()
            } else {
                println("${this.name} doesn't want to help.")
                return false
            }
        } else if (n != null) {
            return n!!.handle()
        } else {
            return false
        }
    }
}

abstract class ShuHero(r: Role) : Hero(r), Handler { //attack
    var n: Handler? = null
    override fun setNext(h: Handler) {
        this.n = h
    }

    override fun handle(): Boolean {
//        if (this.alive) {
//            if (this.numOfCards > 0 && this.roleTitle != "Rebel") {
//                this.numOfCards -= 1
//                println("${this.name} spent 1 card to help his/her lord to attack.")
//                return true
//            } else if (n != null) {
//                println("${this.name} doesn't want to help.")
//                return n!!.handle()
//            } else {
//                println("${this.name} doesn't want to help.")
//                return false
//            }
//        } else if (n != null) {
//            return n!!.handle()
//        } else {
        return false
//        }
//    }
    }
}

abstract class WuHero(r: Role) : Hero(r) {

}

class LiuBei(r: Role) : MonarchHero(r) { //Shu
    override var name: String = "Liu Bei"
    override var gender: Boolean = true
    var hasAbility = true

    var n: Handler? = null
//    override fun attack() { //Handler helps to attack
//        if (n != null) {
//            var handle = n!!.handle()
//            println(handle)
//            if (!handle) {
//                println("No one can help lord to attack.")
//            }
//        }
//    }

    fun kindness() {
        if (hand.size >= 2 && hasAbility) {
            print("$name used Kindness. ")
            hasAbility = false
            var giveSucess = false

            for (hero in Singleton.heroes) {
                if (hero.roleTitle == "Minister" && hero.alive) {
                    println("Giving 2 cards to ${hero.name}.")
                    for (i in 0..1) {
                        print("Giving ${hand[0].name}. ")
                        hand[0].belongsTo = hero
                        hero.hand.add(hand[0])
                        hand.removeAt(0)
                    }
                    println()
                    giveSucess = true
                    break
                }
            }

            if (giveSucess) {
                if (hp < maxHP) {
                    var gen = Peach()
                    gen.belongsTo = this
                    hand.add(0, gen)
                    println("$name played peach through Kindness.")
                    heal()
                    Singleton.graveyard.remove(gen)
                } else {
                    for (hero in Singleton.heroes) {
                        if (getTarget(this, hero, true) == hero) {
                            var gen = BasicAttack()
                            gen.belongsTo = this
                            hand.add(0, gen)
                            println("$name played attack through Kindness.")
                            attack(null)
                            Singleton.graveyard.remove(gen)
                            break
                        }
                    }
                }
            }
            else println()
        }
    }

    override fun templateMethod() {
        hasAbility = true
        super.templateMethod()
    }

    fun hasAbility(): Boolean {
        return hasAbility
    }
}

class CaoCao(r: Role) : MonarchHero(r) { //Wei
    override var name: String = "Cao Cao"
    override var gender: Boolean = true
    var n: Handler? = null
    override fun dodgeAttack(): Boolean {
        if (n != null) {
            var handle = n!!.handle()
            if (!handle) {
                println("No one can help lord to dodge.")
            }
            return handle
        } else {
            if (hasDodge() >= 0) {
                singleton.graveyard.add(hand[hasDodge()])
                hand.removeAt(hasDodge())
                return true
            }
            return false
        }
    }
}

class SunQuan(r: Role) : MonarchHero(r) { //Wu
    override var name: String = "Sun Quan"
    override var gender: Boolean = true
}

class ZhangFei(r: Role) : ShuHero(r) { //Shu
    override var name: String = "Zhang Fei"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    var attacked = false
    var printed = false

    //    override fun playCards() {
//        while(numOfCards > 0){
//            this.numOfCards -= 1
//            println("${this.name} is a ${this.roleTitle}, spent 1 card to attack ${this.getEnemy()}.")
//        }
//    }
    override fun attack(t: Hero?) {
        while (hasAttack() != -1) {
            super.attack(t)
            canAttackAgain = true
            attacked = true
        }
    }
    override fun templateMethod() {
        attacked = false
        printed = false
        super.templateMethod()
    }
}


class XuChu(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Xu Chu"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
}

class SimaYi(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Sima Yi"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = true
}

class ZhouYu(r: Role) : WuHero(r) { //Wu
    override var name: String = "Zhou Yu"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = true
    override fun drawCards() {
        for (i in 0..2) {
            if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
            singleton.deck[0].belongsTo = this
            hand.add(singleton.deck[0])
            singleton.deck.removeAt(0)
        }
        println("I'm handsome, so I can draw one additional card.")
        println("${this.name} now has ${this.hand.size} cards.")
    }

    override fun playCards() {

        if (hand.size != 0) {
            var choice = Random.nextInt(0, 1) //reveal card or no action
            lateinit var gCard : Card
            when (choice) {
                0 -> {
                    //reveal and give card to another hero
                    print("Sow Discord: I can reveal one card, ")
                    var index = Random.nextInt(0, hand.size)
                    var notGiven = true
                    var index2 = 0
                    lateinit var randomHero : Hero
                    while(notGiven) {
                        index2 = Random.nextInt(0, Singleton.heroes.size)
                        randomHero = Singleton.heroes[index2]
                        if (randomHero != this && randomHero.alive) {
                            gCard = hand[index]
                            giveCard(index, randomHero.hand, hand,randomHero)
                            notGiven = false
                        }
                    }
                    println("given to ${randomHero.name}.")

                    //action of the given hero
                    println("${randomHero.name} has to do either one action: 1. Reveal all cards in his hand and discard all cards in his hand and equipment zone with the same color. 2. Lose 1 health.")

                    var choice2 = Random.nextInt(0, 2)
                    when(choice2) {
                        0 -> {
                            //Reveal all hand cards and discard same colour hand and board card
                            println("Action 1")
                            var discard = mutableListOf<Card>()
                            print("${randomHero.name}'s hand has: ")
                            for (hands in randomHero.hand) {
                                print("${hands.name} ")
                                if (hands.color == gCard.color) discard.add(hands)
                            }
                            println()
                            for (equip in randomHero.board) {
                                if (equip.color == gCard.color) discard.add(equip)
                            }
                            print("Discard: ")
                            for (cards in discard) {
                                print("${cards.name} ")
                                singleton.graveyard.add(cards)
                                randomHero.hand.remove(cards)
                            }
                            println()
                        }
                        1 -> { //Lost 1 health
                            println("Action 2")
                            var attack = Attacking(this, randomHero)
                            attack.execute()
                        }
                    }
                }
            }
            super.playCards()
        }
    }

    override fun discardCards() {
        canAttackAgain = true
        var diff: Int = this.hand.size - this.maxHP
        while (diff > 0) {
            println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
            this.str.selectCardToDiscard()
            singleton.graveyard.add(hand[0])
            hand.removeAt(0)
            diff--
        }
        println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
        if (hasHeal() == -1) hasHealCard = false
    }
}

class DiaoChan(r: Role) : AdvisorHero(r) { //Kindomless
    override var name: String = "Diao Chan"
    override var gender: Boolean = false
    override fun discardCards() {
        this.canAttackAgain = true
        var diff: Int = this.hand.size - this.hp
        while (diff > 0) {
            println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
            this.str.selectCardToDiscard()
            singleton.graveyard.add(hand[0])
            hand.removeAt(0)
            diff--
        }
        println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
        if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
        singleton.deck[0].belongsTo = this
        hand.add(singleton.deck[0])
        singleton.deck.removeAt(0)
        println("I can draw one more card, now I have ${this.hand.size} cards.")
    }
}

class GuanYu { //Shu
    val name = "Guan Yu"
    fun getAttackString() = "Power 💪 !!"
}

class GuanYuAdapter(r: Role) : ShuHero(r) { //Shu
    val guanyu = GuanYu()
    override var name: String = guanyu.name
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    var printed = false
    var printedR = false
    var lastAtk = "Null"
    override fun attack(t: Hero?) {
        println(guanyu.getAttackString())
        super.attack(t)
    }

    override fun hasAttack(): Int {
        for ((place, card) in hand.withIndex()) {
            if (card.name == "Attack" && card.suit == "Diamond") {
                lastAtk = card.suit
                return place
            } else if (card.color == "Red" && card.suit == "Diamond") {
                if (!printed) {
                    println("$name converted red ${card.name} to attack.")
                    printed = true
                }
                lastAtk = card.suit
                return place
            }
        }

        for ((place, card) in hand.withIndex()) {
            if (card.name == "Attack") {
                lastAtk = card.suit
                return place
            } else if (card.color == "Red") {
                if (!printed) {
                    println("$name converted red ${card.name} to attack.")
                    printed = true
                }
                lastAtk = card.suit
                return place
            }
        }

        for (card in board) {
            if (card is WeaponCard && card.color == "Red") {
                if (!printed) {
                    println("$name converted red ${card.name} to attack.")
                    printed = true
                }
                hand.add(0, card)
                board.remove(card)
                lastAtk = card.suit
                return 0
            }
        }

        return -1
    }

    override fun templateMethod() {
        printed = false
        printedR = false
        super.templateMethod()
    }

}

class DaQiao(r: Role) : WuHero(r) { //Wu
    override var name: String = "Da Qiao"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = false
    fun abandonMonarch() {
        this.setCommand(Abandon(MonarchFactory.monarch!!))
    }
}

class ZhugeLiang(r: Role) : ShuHero(r) { //Shu
    override var name: String = "Zhuge Liang"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = true
    var printed = false
    var printedD = false

    override fun templateMethod() {
        starGazing()
        printed = false
        printedD = false
        super.templateMethod()
    }

    fun starGazing() {
        var stars = mutableListOf<Card>()
        var position = 0

        println("Zhege Liang used Stargazing.")
        if (Singleton.heroes.size > 3) {
            for (i in 0 until 5) {
                if (Singleton.deck.size <= 0) Singleton.deck = BasicCardFactory.shuffle(Singleton.graveyard)
                println("[$i]: ${Singleton.deck[0].name}")
                stars.add(Singleton.deck[0])
                Singleton.deck.removeAt(0)
            }
        }
        else {
            for (i in 0 until 3) {
                if (Singleton.deck.size <= 0) Singleton.deck = BasicCardFactory.shuffle(Singleton.graveyard)
                println("[$i]: ${Singleton.deck[0].name}")
                stars.add(Singleton.deck[0])
                Singleton.deck.removeAt(0)
            }
        }

        var removeAtEnd = mutableListOf<Card>()
            for (card in stars) {
                if ((str as BasicStrategy).state is HealthyState) {
                    if (card is Peach || card is Dodge || card is OathOfPeachGarden) {
                        Singleton.deck.add(position, card)
                        removeAtEnd.add(card)
                        position++
                    }
                }
                else if ((str as BasicStrategy).state is UnhealthyState) {
                    if (card is Attack || card is HailOfArrows || card is BarbariansAssault || card is Duel) {
                        Singleton.deck.add(position, card)
                        removeAtEnd.add(card)
                        position++
                    }
                }
            }

            for (i in removeAtEnd) stars.remove(i)

            for (card in stars) {
                Singleton.deck.add(position, card)
                position++
            }

        println("Rearranged cards are:")
        if (Singleton.heroes.size > 3) for (i in 0 until 5) println("[$i]: ${Singleton.deck[i].name}")
        else for (i in 0 until 3) println("[$i]: ${Singleton.deck[i].name}")

    }
}

class ZhangYun(r: Role) : ShuHero(r) { //Shu
    override var name: String = "Zhang Yun"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    var printed = false
    var printedD = false

    override fun hasAttack(): Int {
        if (super.hasAttack() == -1) {
            for ((i, card) in hand.withIndex()) {
                if (card is Dodge) {
                    if (!printed) {
                        println("I converted ${card.name}, ${card.rank} of ${card.suit} into Attack.")
                        printed = true
                    }
                    return i
                }
            }
        }
        else return super.hasAttack()
        return -1
    }

    override fun hasDodge(): Int {
        if (super.hasDodge() == -1) {
            for ((i, card) in hand.withIndex()) {
                if (card is Attack) {
                    if (!printedD) {
                        println("I converted ${card.name}, ${card.rank} of ${card.suit} into Dodge.")
                        printedD = true
                    }
                    return i
                }
            }
        }
        else return super.hasDodge()
        return -1
    }

    override fun templateMethod() {
        printed = false
        printedD = false
        super.templateMethod()
    }
}

class MaChao(r: Role) : ShuHero(r) { //Shu
    override var name: String = "Ma Chao"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
}

class HuangYueYing(r: Role) : ShuHero(r) { //Shu
    override var name: String = "Huang Yue Ying"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = false
}

class GanNing(r: Role) : WuHero(r) { //Wu
    override var name: String = "Gan Ning"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    override fun hasBurnBridges(): Int {
        for ((place, card) in hand.withIndex()) if (card is BurnBridges || card.color == "Black") return place
        for ((place, card) in board.withIndex()) if (card is BurnBridges || card.color == "Black") return place
        return -1
    }
}

class LuMeng(r: Role) : WuHero(r) { //Wu
    override var name: String = "Lu Meng"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    var attacked = false

    override fun discardCards() {
        if (!attacked) {
            if (hand.size > hp) println("I did not attack, discard phase is ignored.")
            println("Current HP is ${this.hp}, now have ${this.hand.size} cards.")
        }
        else {
            attacked = false
            super.discardCards()
        }
    }
}

class HuangGai(r: Role) : WuHero(r) { //Wu
    override var name: String = "Huang Gai"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true

    override fun playCards() {
        //Sacrificial Injury
        if ((this.str as BasicStrategy).state is HealthyState) {
            var choice = mutableListOf<String>()
            if (hand.size != 0) choice.add("Hand")
            if (board.size != 0) choice.add("Board")
            while (choice.size < 3) { choice.add(" ") } //for no action

            var choiceint = Random.nextInt(0, 3) //0: discard hand, 1: discard board, 2,3: no action
            when (choice[choiceint]) {
                "Hand" -> {
                    var indexHand = Random.nextInt(0, hand.size)
                    singleton.graveyard.add(hand[indexHand])
                    hand.remove(hand[indexHand])
                    hp--
                    println("Ability of $name : Sacrificial Injury \nA hand card is discarded and lose 1 health. Current hp = $hp.")
                }
                "Board" -> {
                    var indexBoard = Random.nextInt(0, board.size)
                    singleton.graveyard.add(hand[indexBoard])
                    hand.remove(hand[indexBoard])
                    hp--
                    println("Ability of $name : Sacrificial Injury \nA card on board is discarded and lose 1 health. Current hp = $hp.")
                }
            }
        }

        super.playCards()
    }
}

class LuXun(r: Role) : WuHero(r) { //Wu
    override var name: String = "Lu Xun"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = true
}

class SunShangXiang(r: Role) : WuHero(r) { //Wu
    override var name: String = "Sun Shang Xiang"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = false
}

class XiahouDun(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Xiahou Dun"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true
    var attackedXia: Boolean = false

    override fun beingAttacked(s: Hero) {
        super.beingAttacked(s)
        if (attackedXia) {
            println("Staunch: for each damage you have taken, I may perform a judgment.")
            print("Judgement: ")
            if (singleton.deck[0].color == "Red") {
                println("Red, deal 1 damage to the source of damage.")
                var attack = Attacking(this, s)
                attack.execute()
            } else {
                println("Black, discard a card from the source of damage's hand or equipment zone.")
                if (s.hand.size != 0) {
                    var rand = Random.nextInt(0, s.hand.size)
                    singleton.graveyard.add(s.hand[rand])
                    s.hand.removeAt(rand)
                } else if (s.board.size != 0) {
                    var rand = Random.nextInt(0, s.board.size)
                    singleton.graveyard.add(s.board[rand])
                    s.board.removeAt(rand)
                }
            }
            singleton.graveyard.add(singleton.deck[0])
            singleton.deck.removeAt(0)

            attackedXia = false
        }
    }
}

class ZhangLiao(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Zhang Liao"
    override var maxHP: Int = 4
    override var hp: Int = 4
    override var gender: Boolean = true

    override fun drawCards() {
        println("Ambush: I may draw or take cards from at most 2 targeted hero.")
        var drawNo = Random.nextInt(0, 2)
        var takeNo = 2 - drawNo
        var count = 0
        println("Drawing $drawNo card(s) and taking $takeNo card(s).")

        if (drawNo > 0) {
            for (i in 1..drawNo) { //Drawing Cards
                if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
                singleton.deck[0].belongsTo = this
                hand.add(singleton.deck[0])
                singleton.deck.removeAt(0)
            }
        }
        //Taking X cards from X hero(es)
        if (takeNo > 0) {
            var hero1: Hero? = null
            for (hero1 in Singleton.heroes) {
                if (count < takeNo - 1 && hero1 != this && hero1.alive && hero1.hand.size > 0) {
                    var rand = Random.nextInt(0, hero1.hand.size)
                    giveCard(rand, this.hand, hero1.hand, this)
                    print("Took a card from ${hero1.name}")
                    count++
                }
            }
            if (hero1 != null && count < takeNo) {
                for (hero2 in Singleton.heroes) {
                    if (hero2 != this && hero2 != hero1 && hero2.alive && hero2.hand.size > 0) { //each hero taken once only
                        var rand = Random.nextInt(0, hero2.hand.size)
                        giveCard(rand, this.hand, hero2.hand, this)
                        print(" and ${hero2.name}")
                        count++
                    }
                }
            } else if (hero1 == null) print("\nNo more available hero, draw card(s) instead")
            //no more available hero alive / having enough hand cards --> draw
            while (count < takeNo) {
                if (singleton.deck.size == 0) singleton.deck = BasicCardFactory.shuffle(singleton.graveyard)
                singleton.deck[0].belongsTo = this
                hand.add(singleton.deck[0])
                singleton.deck.removeAt(0)
                count++
            }

            println(".\n${this.name} now has ${this.hand.size} cards.")
            if (hasHeal() >= 0) hasHealCard = true
        }
    }
}

class GuoJia(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Guo Jia"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = true
}

class ZhenJi(r: Role) : WeiHero(r) { //Wei
    override var name: String = "Zhen Ji"
    override var maxHP: Int = 3
    override var hp: Int = 3
    override var gender: Boolean = false
    var printed = false

    override fun hasDodge(): Int {
        if (super.hasDodge() == -1) {
            for ((i, card) in hand.withIndex()) {
                if (card.color == "Black") {
                    if (!printed) {
                        println("I converted ${card.name} to Dodge because it is ${card.color}.")
                        printed = true
                    }
                    return i
                }
            }
        }
        return super.hasDodge()
    }

    override fun templateMethod() {
        printed = false
        var cardsRemoved = mutableListOf<Card>()

        println("Zhen Ji's ability: I am the Goddess of Luo")
        for (card in Singleton.deck) {
            if (card.color == "Black") {
                println("${card.name} is Black, adding to my hand.")
                card.belongsTo = this
                hand.add(0, card)
            }
            else  {
                println("${card.name} is not Black, I cannot take any more cards.")
                break
            }
            cardsRemoved.add(card)
        }

        for (i in cardsRemoved) Singleton.deck.remove(i)

        super.templateMethod()
    }
}

class HuaTuo(r: Role) : AdvisorHero(r) { //Kindomless
    override var name: String = "Hua Tuo"
    override var gender: Boolean = true

    override fun revive(h: Hero): Boolean {
        if (askRevive(h) && hasHeal() != -1) {
            println("$name saved ${h.name} from the brink of death!")
            singleton.graveyard.add(hand[hasHeal()])
            hand.removeAt(hasHeal())
            h.hp++
            return true
        } else if (askRevive(h) && hasRed() != -1) {
            println("$name converted a Red card (${hand[hasRed()].name}) into a first aid kit.")
            singleton.graveyard.add(hand[hasRed()])
            hand.removeAt(hasRed())
            h.hp++
            return true
        }
        else println("$name can't save ${h.name}")
        return false
    }

    private fun hasRed(): Int {
        for ((place, card) in hand.withIndex()) {
            if (card.color == "Red") return place
        }
        return -1
    }

    private fun selfishBookOfGreenVesicle() {
        if (hand.size > 0 && hp < maxHP) {
            println("Hua Tuo has the Book of Green Vesicle, discarded ${hand[0].name}")
            singleton.graveyard.add(hand[0])
            hand.removeAt(0)
            hp++
            println("Healed themselves one. Hp now is $hp")
        }
    }

    override fun templateMethod() {
        println(this.name + "'s turn:")
        executeCommand()
        drawCards()
        if (!abandon) {
            selfishBookOfGreenVesicle()
            playCards()
        }
        else abandon = false
        discardCards()
    }
}

class LuBu(r: Role) : WarriorHero(r) { //Kindomless
    override var name: String = "Lu Bu"
    override var hp: Int = 5
    override var maxHP: Int = 5
    override var gender: Boolean = true
}

interface GameObjectFactory {
    fun getRandomRole(): Role
    fun createRandomHero(): Hero
}

object MonarchFactory : GameObjectFactory {
//    private val emperors = listOf(LiuBei(getRandomRole()), CaoCao(Monarch()), SunQuan(Monarch()))
//    val monarch = createRandomHero()

    var monarch: Hero? = null

    override fun getRandomRole(): Role {
        return Monarch()
    }

    override fun createRandomHero(): Hero {

        if (monarch != null) return monarch as Hero

        var randomNumber = Random.nextInt(0, 3)
//        monarch = emperors[randomNumber]

        monarch = when (randomNumber) {
            0 -> LiuBei(getRandomRole())
            1 -> CaoCao(getRandomRole())
            else -> SunQuan(Monarch())
        }

        if (monarch is LiuBei) monarch!!.setStrategy(LiuBeiStrategy(monarch!!))
        else monarch!!.setStrategy(BasicStrategy(monarch!!))
        (monarch!!.str as BasicStrategy).changeState(HealthyState.factory(monarch!!.str, monarch!!))
        return monarch as Hero
    }
}

object NonMonarchFactory : GameObjectFactory {
    private val roles = listOf(Minister(), Rebel(), Traitor())
    private val regulars = listOf(
        ZhangFei(getRandomRole()),
        ZhouYu(getRandomRole()),
        DiaoChan(getRandomRole()),
        GuanYuAdapter(getRandomRole()),
        XuChu(getRandomRole()),
        SimaYi(getRandomRole()),
        DaQiao(getRandomRole()),
        ZhugeLiang(getRandomRole()),
        ZhangYun(getRandomRole()),
        MaChao(getRandomRole()),
        HuangYueYing(getRandomRole()),
        GanNing(getRandomRole()),
        LuMeng(getRandomRole()),
        HuangGai(getRandomRole()),
        LuXun(getRandomRole()),
        SunShangXiang(getRandomRole()),
        XiahouDun(getRandomRole()),
        ZhangLiao(getRandomRole()),
        GuoJia(getRandomRole()),
        ZhenJi(getRandomRole()),
        HuaTuo(getRandomRole()),
        LuBu(getRandomRole())
    )

    //    private val weis = listOf(regulars[4],regulars[5]) //--> use inherited abstract class to check
    override fun getRandomRole(): Role {
        var randomNumber = Random.nextInt(0, roles.size)
        return roles[randomNumber]
    }

    override fun createRandomHero(): Hero {
        var randomNumber = Random.nextInt(0, regulars.size)
        while (regulars[randomNumber] in Singleton.heroes) {
            randomNumber = Random.nextInt(0, regulars.size)
        }

        //Help CaoCao dodge
        if (MonarchFactory.monarch is CaoCao && regulars[randomNumber] is WeiHero) {
            var caocao = MonarchFactory.monarch as CaoCao
            if (caocao.n == null) {
                caocao.n = regulars[randomNumber] as Handler
            } else {
                var handler = caocao.n
                while ((handler as WeiHero).n != null) {
                    handler = (handler).n
                }
                handler.n = regulars[randomNumber] as Handler
            }
        }

        if (regulars[randomNumber].roleTitle.equals("Minister") || regulars[randomNumber].roleTitle.equals("Traitor")) {
            if (regulars[randomNumber].r is Subscriber) {
                (MonarchFactory.monarch as MonarchHero).subscribe(regulars[randomNumber].r as Subscriber)
            }
        }

        if (regulars[randomNumber] is GuanYuAdapter) {
            regulars[randomNumber].setStrategy(GuanYuStrategy(regulars[randomNumber]))
        } else if (regulars[randomNumber] is DaQiao) {
            regulars[randomNumber].setStrategy(DaQiaoStrategy(regulars[randomNumber]))
        } else if (regulars[randomNumber] is GanNing) {
            regulars[randomNumber].setStrategy(GanNingStrategy(regulars[randomNumber]))
        } else {
            regulars[randomNumber].setStrategy(BasicStrategy(regulars[randomNumber]))
        }

        (regulars[randomNumber].str as BasicStrategy).changeState(
            HealthyState.factory(
                regulars[randomNumber].str,
                regulars[randomNumber]
            )
        )

        return regulars[randomNumber]
    }
}