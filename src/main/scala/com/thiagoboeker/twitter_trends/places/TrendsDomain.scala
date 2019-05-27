package com.thiagoboeker.twitter_trends.places

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext

object TrendsDomain extends DefaultJsonProtocol{
   
   implicit val trendJsonFormat = jsonFormat4(Trend)
   implicit val trendsListJsonFormat = jsonFormat1(Trends)
   
   import com.thiagoboeker.twitter_trends.util.AuthDomain._
   
   case class Trend(name: String, url: String, query: String, tweet_volume: Option[Int])
   case class Trends(trends: List[Trend])
   
   val url = "https://api.twitter.com/1.1/trends/place.json"
   
   def request(token: Token) = {
      HttpRequest(HttpMethods.GET, Uri(url).withQuery(Query(("id", "1"))))
        .addHeader(Authorization(OAuth2BearerToken(token.access_token)))
   }
   
   def GlobalTrends(implicit sys: ActorSystem, mat: ActorMaterializer, ex: ExecutionContext) = {
      Flow[Token].mapAsync(1)(token => Http().singleRequest(request(token))
        .flatMap(r => {
           Unmarshal(r.entity).to[List[Trends]]
        }))
   }
   
}
