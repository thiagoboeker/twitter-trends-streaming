package com.thiagoboeker.twitter_trends.api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.thiagoboeker.twitter_trends.tweets.TweetsDomain._

import scala.concurrent.ExecutionContext
object API {
   
   def apiHandler(apiKey: String, apiSecret: String)(implicit sys: ActorSystem, mat: ActorMaterializer, ex: ExecutionContext) = {
      val source: Source[Message, NotUsed] = Tweets(apiKey, apiSecret).map(r => TextMessage(r.compactPrint))
      Route {
         path("data") { r =>
            r.request.header[UpgradeToWebSocket] match {
               case Some(upgrade) => r.complete(upgrade.handleMessagesWithSinkSource(Sink.ignore, source))
               case None => r.complete(HttpResponse(400, entity = "Websocket denied."))
            }
         }
      }
   }
}
