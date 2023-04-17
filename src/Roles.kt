interface Role {
    var roleTitle: String
    var singleton: Singleton
    fun getEnemy(): String
    fun getTarget(self: Hero, target: Hero, attack: Boolean?): Hero?
    fun getAlly(self: Hero, target: Hero, attack: Boolean?): Hero?
    fun askRevive(h: Hero): Boolean
    fun cancelEffect(h: Hero): Boolean
}

class Monarch() : Role, Publisher {
    var subscribers = mutableListOf<Subscriber>()
    override var singleton = Singleton

    override var roleTitle: String = "Monarch"
    override fun getEnemy(): String = "Rebel, then Traitors"
    override fun getTarget(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Rebel" || hero.roleTitle == "Traitor") && hero.alive) {
//            println("Monarch check")
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun getAlly(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Minister") && hero.alive) {
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun subscribe(s: Subscriber) {
        subscribers.add(s)
    }

    override fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int) {
        for (i in subscribers) {
            i.update(dodged, hp, numOfCards)
        }
    }

    override fun removeSubscriber(s: Subscriber) {
        subscribers.remove(s)
    }

    override fun askRevive(h: Hero): Boolean {
        if (h.roleTitle == "Minister" || h.roleTitle == "Monarch") return true
        return false
    }

    override fun cancelEffect(h: Hero): Boolean {
        if (h.roleTitle == "Rebel" || h.roleTitle == "Traitor") return true
        return false
    }
}

class Minister() : Role, Subscriber {
    var monarchDL: Int = 0
    override var roleTitle: String = "Minister"
    override var singleton = Singleton
    override fun getEnemy(): String = "Rebel, then Traitors"
    override fun getTarget(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Rebel" || hero.roleTitle == "Traitor") && hero.alive) {
//            println("Minister check")
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun getAlly(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Monarch") && hero.alive) {
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun update(dodged: Boolean, hp: Int, numOfCards: Int) {
        if (!dodged) {
            if (hp == (MonarchFactory.monarch!!.maxHP - 1)) {
                if (numOfCards > 3) {
                    monarchDL = 1
                } else {
                    monarchDL = 2
                }
            } else if (hp <= 2) {
                if (numOfCards > 3) {
                    monarchDL = 4
                } else {
                    monarchDL = 5
                }
            } else {
                if (numOfCards > 3) {
                    monarchDL = 2
                } else {
                    monarchDL = 3
                }
            }
        } else if (hp <= 2 && numOfCards <= 3) {
            monarchDL = 5
        }
        println("${this.roleTitle} estimates the danger Level as ${this.monarchDL}")
    }

    override fun askRevive(h: Hero): Boolean {
        if (h.roleTitle == "Monarch" || h.roleTitle == "Minister") return true
        return false
    }

    override fun cancelEffect(h: Hero): Boolean {
        if (h.roleTitle == "Rebel" || h.roleTitle == "Traitor") return true
        return false
    }
}

class Rebel() : Role {
    override var roleTitle: String = "Rebel"
    override var singleton = Singleton
    override fun getEnemy(): String = "Monarch"
    override fun getTarget(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if (hero.roleTitle == "Monarch" && hero.alive) {
//            println("Rebel check")
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun getAlly(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if (hero.roleTitle == "Rebel" && hero.alive) {
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun askRevive(h: Hero): Boolean {
        if (h.roleTitle == "Rebel") return true
        return false
    }

    override fun cancelEffect(h: Hero): Boolean {
        if (h.roleTitle != "Rebel") return true
        return false
    }
}

class Traitor() : Role, Subscriber {
    var monarchDL: Int = 0
    override var roleTitle: String = "Traitor"
    override var singleton = Singleton
    override fun getEnemy(): String = "Minister and Rebel, then Monarch"
    override fun getTarget(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Rebel" || hero.roleTitle == "Minister") && hero.alive) {
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }

        var monarchLeft = true

        for (player in singleton.heroes)
            if (player.alive && player.roleTitle != roleTitle) monarchLeft = false

        if (hero.roleTitle == "Monarch" && hero.alive && monarchLeft) {
//            println("Traitor check")
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }
        return null
    }

    override fun getAlly(self: Hero, hero: Hero, attack: Boolean?): Hero? {
        if ((hero.roleTitle == "Monarch") && hero.alive && Singleton.heroes.size != 2) {
            if (attack == null) {
                return hero
            } else if (hero.calculateRange(self, hero, attack)) {
                return hero
            }
        }

        var monarchLeft = true

        for (player in singleton.heroes)
            if (player.alive && player.roleTitle != roleTitle) monarchLeft = false

        if (hero.roleTitle == "Monarch" && hero.alive && monarchLeft) {
            return null
        }
        return null
    }

    override fun update(dodged: Boolean, hp: Int, numOfCards: Int) {
        if (!dodged) {
            if (hp == (MonarchFactory.monarch!!.maxHP - 1)) {
                if (numOfCards >= 3) {
                    monarchDL = 1
                } else {
                    monarchDL = 2
                }
            } else if (hp <= 2) {
                if (numOfCards >= 3) {
                    monarchDL = 4
                } else {
                    monarchDL = 5
                }
            } else {
                if (numOfCards >= 3) {
                    monarchDL = 2
                } else {
                    monarchDL = 3
                }
            }
        } else if (hp <= 2 && numOfCards < 3) {
            monarchDL = 4
        }
        println("${this.roleTitle} estimates the danger Level as ${this.monarchDL}")
    }

    override fun askRevive(h: Hero): Boolean {
        var monarchLeft = true

        for (player in singleton.heroes)
            if (player.alive && player.roleTitle != roleTitle) monarchLeft = false

        if (monarchLeft) return false
        else if (!monarchLeft && h is MonarchHero) return true
        else if (h.roleTitle == roleTitle) return true

        return false
    }

    override fun cancelEffect(h: Hero): Boolean {
        var noTra = true

        for (player in singleton.heroes)
            if (player.alive && player.roleTitle == "Traitor") noTra = false

        return if (noTra && h.roleTitle != "Traitor") true
        else (h.roleTitle == "Rebel" && h.roleTitle != "Traitor")
    }
}