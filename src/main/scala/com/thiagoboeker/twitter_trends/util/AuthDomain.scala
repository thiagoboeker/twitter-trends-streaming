package com.thiagoboeker.twitter_trends.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._
import akka.stream.scaladsl.Flow
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._

object AuthDomain extends DefaultJsonProtocol with SprayJsonSupport{
   
   val url = "https://api.twitter.com/oauth2/token"
   
   case class Token(token_type: String, access_token: String)
   
   implicit val tokenJsonFormat = jsonFormat2(Token)
   
   def auth(apiKey: String, apiSecret: String) = Authorization(BasicHttpCredentials(apiKey, apiSecret))
   
   def request(auth: HttpHeader) = HttpRequest(HttpMethods.POST,
      Uri(url).withQuery(Query(("grant_type", "client_credentials"))))
     .addHeader(auth)
   
   def Authentication(implicit sys: ActorSystem, mat: ActorMaterializer) = {
         Flow[(String, String)].mapAsync(1)(keys => Http().singleRequest(request(auth(keys._1, keys._2))))
           .mapAsync(1)(resp => Unmarshal(resp).to[Token])
           .throttle(1, 10 seconds)
   }
   
}
