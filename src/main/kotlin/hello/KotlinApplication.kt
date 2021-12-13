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


                val inRange = arenaUpdate.arena.state
                    .filter { it.key != myUrl }
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

                        state.x in (minXPos..maxXPos) &&
                                state.y in (minYPos..maxYPos)

                    }
                    .also {
                        println("My pos: $myData")
                        println("In range: $it")
                        println()
                    }

                if(myData.wasHit)
                    ServerResponse.ok().body(Mono.just(listOf("F", "R", "L").random()))
                if (inRange)
                    ServerResponse.ok().body(Mono.just("T"))
                else
                    ServerResponse.ok().body(Mono.just(listOf("F", "R", "L").random()))

                //ServerResponse.ok().body(Mono.just(listOf("F", "R", "L", "T").random()))
//                ServerResponse.ok().body(Mono.just("T"))
            }
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
