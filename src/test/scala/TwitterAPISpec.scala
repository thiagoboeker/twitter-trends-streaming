import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.Timeout
import com.thiagoboeker.twitter_trends.api.API._
import com.thiagoboeker.twitter_trends.util.AuthDomain._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class TwitterAPISpec extends WordSpec with ScalatestRouteTest with Matchers with SprayJsonSupport{
   
   override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
   
   val apiKey = "XXXXXXXXXXXXXXX"
   val apiSecret = "XXXXXXXXXXXXXX"
   val accessToken = "XXXXXXXXXXXXXX"
   val tokenSecret = "XXXXXXXXXXXXXXXX"
   
   "API Spec" should {
      "Authenticate" in {
         val future = Source.single((apiKey, apiSecret)).via(Authentication).runWith(Sink.head)
         val token = Await.result(future, 10 seconds)
         assert(token.token_type == "bearer")
      }
      "WebSocket" in {
         implicit val timeout = Timeout(30 seconds)
         val ws = WSProbe()
         WS("/data", ws.flow) ~> apiHandler(apiKey, apiSecret) ~> check {
            isWebSocketUpgrade shouldEqual true
         }
      }
   }
}
