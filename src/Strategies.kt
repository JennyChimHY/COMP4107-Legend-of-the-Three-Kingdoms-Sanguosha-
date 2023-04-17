interface Strategy {
    fun playNextCard(): Boolean
    fun selectCardToDiscard()
}

open class BasicStrategy(h: Hero) : Strategy {
    var hero = h
    lateinit var state: State
    fun changeState(s: State) {
        state = s
    }

    override fun playNextCard(): Boolean {
        hero.healthCheck()

        if (hero.hasDuel() != -1 && hero.hand[hero.hasDuel()] is Duel) {
            for (target in Singleton.heroes) {
                if (target == hero.getTarget(hero, target, null) && (hero.hasNumOfAttack() >= target.hand.size/2)) {
                    if (target is ZhugeLiang && target.hand.size == 0) {
                        if (!target.printedD) {
                            println("${target.name} has an empty fort, cannot be dueled.")
                            target.printedD = true
                        }
                        continue
                    }
                    (hero.hand[hero.hasDuel()] as Duel).target = target
                    (hero.hand[hero.hasDuel()] as Duel).activate()
                    Singleton.graveyard.add(hero.hand[hero.hasDuel()])
                    hero.hand.remove(hero.hand[hero.hasDuel()])
                    return true
                }
            }
        }
        if (hero.hasDuress() != -1 && hero.hand[hero.hasDuress()] is Duress) {
            for (targetA in Singleton.heroes) {
                for ((index, equip) in targetA.board.withIndex()) {
                    if (targetA == hero.getTarget(hero, targetA, null) && equip is WeaponCard) {
                        (hero.hand[hero.hasDuress()] as Duress).targetAttack = targetA
                        (hero.hand[hero.hasDuress()] as Duress).weaponIndex = index
                        for (targetD in Singleton.heroes) {
                            if (targetD != targetA && targetD == hero.getTarget(hero, targetD, null)) {
                                (hero.hand[hero.hasDuress()] as Duress).targetDodge = targetD
                                (hero.hand[hero.hasDuress()] as Duress).activate()
                                Singleton.graveyard.add(hero.hand[hero.hasDuress()])
                                hero.hand.remove(hero.hand[hero.hasDuress()])
                                return true
                            }
                        }
                    }
                }

            }
        }
        if (hero.canAttackAgain && (hero.hasAttack() != -1 || (hero.hasSerpentSpear() != -1 && hero.hand.size >= 2))) {
            for (target in Singleton.heroes) {
                if (hero.getTarget(hero, target, true) == target && target.alive) {
                    hero.attack(target)
                    return true
                }
            }
        }
        if (hero.hasAcedia() != -1 && hero.hand[hero.hasAcedia()] is Acedia) {
            for (target in Singleton.heroes) {
                if (target == hero.getTarget(hero, target, null)) {
                    (hero.hand[hero.hasAcedia()] as Acedia).target = target
                    (hero.hand[hero.hasAcedia()] as Acedia).activate()
                    target.delayTatics.add(hero.hand[hero.hasAcedia()])
                    hero.hand.remove(hero.hand[hero.hasAcedia()])
                    return true
                }
            }
        }
        if (hero.hasLightningBolt() != -1 && hero.hand[hero.hasLightningBolt()] is LightningBolt) {
            (hero.hand[hero.hasLightningBolt()] as LightningBolt).activate()
            hero.delayTatics.add(hero.hand[hero.hasLightningBolt()])
            hero.hand.remove(hero.hand[hero.hasLightningBolt()])
            return true
        }
        if (hero.hasSleight() != -1 && hero.hand[hero.hasSleight()] is SleightOfHand) {
            (hero.hand[hero.hasSleight()] as SleightOfHand).activate()
            Singleton.graveyard.add(hero.hand[hero.hasSleight()])
            hero.hand.remove(hero.hand[hero.hasSleight()])
            return true
        }
        else if (hero.hasPlifer() != -1 && (hero.hand[hero.hasPlifer()] is Plifer) && (hero.hand[hero.hasPlifer()] as Plifer).activate()) {
            Singleton.graveyard.add(hero.hand[hero.hasPlifer()])
            hero.hand.remove(hero.hand[hero.hasPlifer()])
            return true
        }
        else if (hero.hasBurnBridges() != -1 && hero.hand[hero.hasBurnBridges()] is BurnBridges && (hero.hand[hero.hasBurnBridges()] as BurnBridges).activate()) {
            Singleton.graveyard.add(hero.hand[hero.hasBurnBridges()])
            hero.hand.remove(hero.hand[hero.hasBurnBridges()])
            return true
        }
        else if (hero.hasBarbariansAssault() != -1 && hero.hand[hero.hasBarbariansAssault()] is BarbariansAssault) {
            (hero.hand[hero.hasBarbariansAssault()] as BarbariansAssault).activate()
            Singleton.graveyard.add(hero.hand[hero.hasBarbariansAssault()])
            hero.hand.remove(hero.hand[hero.hasBarbariansAssault()])
            return true
        }
        else if (hero.hasPeachGarden() != -1 && hero.hand[hero.hasPeachGarden()] is OathOfPeachGarden) {
            (hero.hand[hero.hasPeachGarden()] as OathOfPeachGarden).activate()
            Singleton.graveyard.add(hero.hand[hero.hasPeachGarden()])
            hero.hand.remove(hero.hand[hero.hasPeachGarden()])
            return true
        }
        else if (hero.hasHarvest() != -1 && hero.hand[hero.hasHarvest()] is Harvest) {
            (hero.hand[hero.hasHarvest()] as Harvest).activate()
            Singleton.graveyard.add(hero.hand[hero.hasHarvest()])
            hero.hand.remove(hero.hand[hero.hasHarvest()])
            return true
        }
        else if (hero.hasHailOfArrows() != -1 && hero.hand[hero.hasHailOfArrows()] is HailOfArrows) {
            (hero.hand[hero.hasHailOfArrows()] as HailOfArrows).activate()
            Singleton.graveyard.add(hero.hand[hero.hasHailOfArrows()])
            hero.hand.remove(hero.hand[hero.hasHailOfArrows()])
            return true
        }
        else if (hero.hasWeaponsMounts() != -1) { //place the weapon or mount onto board
            var index = hero.hasWeaponsMounts()
            for ((place, card) in hero.board.withIndex()) {
                if ((hero.hand[index] is WeaponCard && card is WeaponCard) || (hero.hand[index] is ArmorCard && card is ArmorCard) || (hero.hand[index] is MountCard && card is MountCard && (hero.hand[index] as MountCard).mountRange == card.mountRange)) {
                    hero.replace(hero.board,place,hero.hand,index)
                    return true
                }
            }
            println("${hero.hand[index].name} is equipped")
            hero.board.add(hero.hand[index])
            hero.hand.removeAt(index)
            return true
        }else if (state is UnhealthyState) {
            return state.playHealCard()
        }
        else return false
    }

    override fun selectCardToDiscard() {
        println("Selecting a card to discard...")
        state.recommendCardToDiscard()
    }
}

class GuanYuStrategy(h: Hero) : BasicStrategy(h) {
    override fun selectCardToDiscard() {
        println("I prefer red cards. ")
        println("Selecting a card to discard...")
        state.recommendCardToDiscard()
    }
}

class DaQiaoStrategy(h: Hero) : BasicStrategy(h) {
    var canAbandon = true
    override fun playNextCard(): Boolean {
        if (canAbandon && hero.roleTitle.equals("Rebel")) {
            (hero as DaQiao).abandonMonarch()
            Singleton.graveyard.add(hero.hand[0])
            hero.hand.removeAt(0)
            canAbandon = false
            return true
        }

        return super.playNextCard()
    }

    override fun selectCardToDiscard() {
        canAbandon = true
        println("Selecting a card to discard...")
        state.recommendCardToDiscard()
    }
}

class LiuBeiStrategy(h: Hero): BasicStrategy(h) {

    override fun playNextCard(): Boolean {
        if ((hero as LiuBei).hasAbility()) {
            (hero as LiuBei).kindness()
            return true
        }

        return super.playNextCard()
    }
}

class GanNingStrategy(h: Hero): BasicStrategy(h) {
    var gan = h
    override fun playNextCard(): Boolean {
//        if(gan.hand.size > 0) {
//            for (cards in gan.hand) {
//                if (cards.color == "Black") {
//                    (cards as BurnBridges).activate()
//                }
//            }
//        } else if (gan.board.size > 0) {
//            for (cards in gan.board) {
//                if (cards.color == "Black") {
//                    (cards as BurnBridges).activate()
//                }
//            }
//        }

        return super.playNextCard()
    }
}

