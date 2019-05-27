package com.thiagoboeker.twitter_trends.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import API._
import scala.io.StdIn

object Entry extends App {
   
   implicit val system = ActorSystem("MyAPP")
   implicit val mat = ActorMaterializer()
   import system.dispatcher
   
   val apiKey = "XXXXXXXXXXXXXXXXXXXXX"
   val apiSecret = "XXXXXXXXXXXXXXXXXXXX"
   
   val bind = Http().bindAndHandle(apiHandler(apiKey, apiSecret), "localhost", 9090)
   println("Serving on port: 9090")
   StdIn.readLine()
   bind.flatMap(_.unbind()).map(_ => system.terminate())
}
