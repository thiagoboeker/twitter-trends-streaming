package com.thiagoboeker.twitter_trends.tweets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, Zip}
import akka.stream.{ActorMaterializer, SourceShape}
import com.thiagoboeker.twitter_trends.places.TrendsDomain._
import com.thiagoboeker.twitter_trends.util.AuthDomain._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext

object TweetsDomain extends DefaultJsonProtocol with SprayJsonSupport{
   
   implicit val userJsonFormat = jsonFormat6(User)
   implicit val tweetJsonFormat = jsonFormat4(Tweet)
   implicit val tweetListJsonFormat = jsonFormat1(TweetsList)
   implicit val tweetObjectJsonFormat = jsonFormat2(TweetObject)
   val url = "https://api.twitter.com/1.1/search/tweets.json"
   
   case class User(id: Long, name: Option[String], screen_name: Option[String], location: Option[String], url: Option[String], description: Option[String])
   case class Tweet(created_at: Option[String], id_str: Option[String], text: Option[String], user: User)
   case class TweetsList(statuses: List[Tweet])
   case class TweetObject(trend: Trend, tweets: TweetsList)
   
   def request(trends: (Trend, Token)) = HttpRequest(HttpMethods.GET,
      Uri(url).withQuery(Query(("q", trends._1.name))))
     .addHeader(Authorization(OAuth2BearerToken(trends._2.access_token)))
   
   def Wrapper = Flow[(Trend, TweetsList)].map(l => TweetObject(l._1, l._2).toJson)
   
   def Tweets(apiKey: String, apiSecret: String)(implicit sys: ActorSystem, mat: ActorMaterializer, ex: ExecutionContext) = {
      
      val twtFlow = Flow[(Trend, Token)].mapAsync(1)(trends =>
         Http().singleRequest(request(trends)).flatMap(resp => {
            Unmarshal(resp.entity).to[TweetsList]
         })
      )
      
      val creds = (apiKey, apiSecret)
      
      Source.fromGraph(GraphDSL.create() { implicit builder =>
         
         import GraphDSL.Implicits._
         
         val source = builder.add(Source.single(creds).via(Authentication))
         val authSource = builder.add(Source.repeat(creds).via(Authentication))
         val bcast = builder.add(Broadcast[Trend](2))
         val tweets = builder.add(twtFlow)
         val trends = builder.add(GlobalTrends.flatMapConcat(l => Source(l(0).trends)))
         val zip = builder.add(Zip[Trend, Token])
         val tweetZip = builder.add(Zip[Trend, TweetsList])
         val wrapper = builder.add(Wrapper)
         
         source ~> trends ~> bcast
         
         bcast ~> zip.in0
         authSource ~> zip.in1
         
         zip.out ~> tweets ~> tweetZip.in1
         bcast.out(1) ~> tweetZip.in0
         tweetZip.out ~> wrapper
         
         SourceShape(wrapper.out)
      })
      
   }
}
