import kotlin.random.Random

object Singleton {
    var heroes = mutableListOf<Hero>()
    var deck = mutableListOf<Card>()
    var graveyard = mutableListOf<Card>()
    var announcement = Announcement()

    init {
        // Initialization logic goes here

        // Create heroes, i is the amount of non-monarch players
        heroes.add(MonarchFactory.createRandomHero())
        for (i in 1..4) heroes.add(NonMonarchFactory.createRandomHero())

        // Create the deck
        deck = BasicCardFactory.standardDeck()

        // Draw 4 cards for each hero at start
        for (hero in heroes){
            for (i in 1..4) {
                deck[0].belongsTo = hero
                hero.hand.add(deck[0])
                deck.removeAt(0)
            }
            println("${hero.name} is a ${hero.roleTitle}")
        }
        println()

        announcement.initial()

    }
}

fun main() {
    var monarch = Singleton.heroes[0]
    var s = Singleton
    while (MonarchFactory.monarch!!.alive) {
        for (player in s.heroes) {
            if (player.alive) {
                player.templateMethod()
                println()
            }

            if (!monarch.alive) break
            var noTorR = true
            for (player in s.heroes) {
                if ((player.roleTitle == "Traitor" || player.roleTitle == "Rebel") && player.alive) noTorR = false
            }
            if (noTorR) {
                println("Monarch and Ministers win!")
                return
            }
        }
    }

    var noSurvivors = true
    for (player in s.heroes) {
        if (player.alive && player.roleTitle != "Traitor") {
            noSurvivors = false
            break
        }
    }
    if (noSurvivors) println("Traitor win!")
    else println("Rebels win!")

}