package hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import kotlin.math.max
import kotlin.math.min

@SpringBootApplication
class KotlinApplication {

    @Bean
    fun routes() = router {
        GET {
            ServerResponse.ok().body(Mono.just("Let the battle begin!"))
        }

        POST("/**", accept(APPLICATION_JSON)) { request ->
            request.bodyToMono(ArenaUpdate::class.java).flatMap { arenaUpdate ->
                println(arenaUpdate)
                val myUrl = arenaUpdate._links.self.href
                val myData = arenaUpdate.arena.state[myUrl]!!

                val arenaState = arenaUpdate.arena.state
                    .filter { it.key != myUrl }

                val inRange = arenaState
                    .any { (player, state) ->
                        var dx = 0
                        var dy = 0
                        when (myData.direction) {
                            "N" -> dy = -1
                            "S" -> dy = 1
                            "W" -> dx = -1
                            "E" -> dx = 1
                            else -> println("unknown direction ${myData.direction}")
                        }

                        val minXPos = min(myData.x, myData.x + (3 * dx))
                        val maxXPos = max(myData.x, myData.x + (3 * dx))

                        val minYPos = min(myData.y, myData.y + (3 * dy))
                        val maxYPos = max(myData.y, myData.y + (3 * dy))

                        val inRange = state.x in (minXPos..maxXPos) &&
                                state.y in (minYPos..maxYPos)

                        if (inRange) {
                            println("My pos: $myData")
                            println("In range: $player -> $state")
                            println()
                        }

                        inRange
                    }

                if (myData.wasHit) {
                    val forwardPosition = forward(myData.x, myData.y, myData.direction)
                    if (!underFire(forwardPosition.first, forwardPosition.second, arenaState.values) &&
                        inBounds(forwardPosition.first, forwardPosition.second, arenaUpdate.arena.dims)
                    )
                        response("F")
                    else
                        response("R")
                } else if (inRange) {
                    response("T")
                } else {
                    response(listOf("F", "R", "L").random())
                }

            }
        }
    }

    private fun inBounds(x: Int, y: Int, dims: List<Int>): Boolean {
        val (dimX, dimY) = dims
        return x < dimX && y < dimY
    }

    fun response(res: String) = ServerResponse.ok().body(Mono.just(res))

    fun underFire(x: Int, y: Int, state: Collection<PlayerState>): Boolean {
        val toCheck = buildList {
            (1..3).forEach {
                add(CheckRange(x, y - it, "N"))
                add(CheckRange(x, y + it, "S"))
                add(CheckRange(x - it, y, "E"))
                add(CheckRange(x + it, y, "W"))
            }
        }

        // is there anyone targeting this field?
        return state.any { player ->
            toCheck.any { it.x == player.x && it.y == player.y && it.direction == player.direction }
        } ||
                // is this field empty?
                state.any { player -> x == player.x && y == player.y }
    }

    fun forward(x: Int, y: Int, direction: String): Pair<Int, Int> {
        return when (direction) {
            "N" -> x to y - 1
            "S" -> x to y + 1
            "W" -> x - 1 to y
            "E" -> x + 1 to y
            else -> throw RuntimeException("unknown direction $direction")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinApplication>(*args)
}

data class ArenaUpdate(val _links: Links, val arena: Arena)
data class PlayerState(val x: Int, val y: Int, val direction: String, val score: Int, val wasHit: Boolean)
data class Links(val self: Self)
data class Self(val href: String)
data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)


data class CheckRange(val x: Int, val y: Int, val direction: String)