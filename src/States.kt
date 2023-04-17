interface State {
    fun playHealCard(): Boolean
    fun recommendCardToDiscard()
}

class HealthyState(s: Strategy, var h: Hero) : State {
    var str = s
    fun setStrategy(newStr: Strategy) {
        str = newStr
    }

    companion object {
        fun factory(s: Strategy, h: Hero): HealthyState {
            return HealthyState(s, h)
        }
    }

    override fun playHealCard(): Boolean {
        if (h.hp < h.maxHP && h.hasHealCard) {
            h.heal()
            return true
        } else {
            return false
        }
    }

    override fun recommendCardToDiscard() {
        println("Healthy, keep attack card instead of dodge card.")
    }
}

class UnhealthyState(s: Strategy, var h: Hero) : State {
    var str = s
    fun setStrategy(newStr: Strategy) {
        str = newStr
    }

    companion object {
        fun factory(s: Strategy, h: Hero): UnhealthyState {
            return UnhealthyState(s, h)
        }
    }

    override fun playHealCard(): Boolean {
        if (h.hp < h.maxHP && h.hasHealCard) {
            h.heal()
            return true
        } else {
            return false
        }
    }

    override fun recommendCardToDiscard() {
        println("Not Healthy, keep dodge card instead of attack card.")
    }
}