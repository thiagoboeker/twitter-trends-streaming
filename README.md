# Twitter-Trends-Streaming
This mini project is just a little bit of fun with Akka Streams.
I've been passionate about learning Akka/Stream/Reactive technologies, although right now it's not a so desireable skill in my current
location...I believe this tech is gold and I'm looking forward to become a master of Reactive Programming.That's my main goal.

### The Goal
The goal of the project was to build a service that could extract information about the trending topics of Tweeter through their API.
Some usefull things like volume of tweets about that trend and some samples of tweets to see what people might be talking about.

To do this there is some topics to tackle:

* **Authentication**
* **WebSocket** 
* **The data flow**

### Authentication
Its really simple to authenticate with Twitter, they provide various types of contexts. In this case I only used the Application-only
context so I could easily authenticate using my API Key and API Secret:

```scala
  def Authentication(implicit sys: ActorSystem, mat: ActorMaterializer) = {
         Flow[(String, String)].mapAsync(1)(keys => Http().singleRequest(request(auth(keys._1, keys._2))))
           .mapAsync(1)(resp => Unmarshal(resp).to[Token])
           .throttle(1, 10 seconds)
   }
```

The main parts of this bit are the mapAsync's which here are used to propagate the resolved responses of the futures, and the request function.
The first argument of mapAsync is the parallelism level defines how many parallel executions are permitted. Another thing is the throttling 
which is very useful to limitate the rate of source operators, in this case due to Rate limits from my license of the API or just for convenience. 
The way that it was thought is one token for request so throttling here seems fine.The request and auth functions are like the following:

```scala
  def auth(apiKey: String, apiSecret: String) = Authorization(BasicHttpCredentials(apiKey, apiSecret))
  def request(auth: HttpHeader) = HttpRequest(HttpMethods.POST,
      Uri(url).withQuery(Query(("grant_type", "client_credentials"))))
     .addHeader(auth)
```

### Websocket
Handling Websocket connections with Akka is pretty straightforward

```scala
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
```

Here I builded a Source that maps the Tweets responses to TextMessages which is basically a wrapper of text that can be passed
to Akka sockets. Then on the Router with request context and through pattern matching against the *UpgradeToWebSocket* typed header I've
extracted the upgrade request.If a upgrade is present the request is completed with a function *handleMessagesWithSinkSource* that builds 
a bidi flow exposing a *Sink* to receive messages from the socket and the source to push messages in the socket, in this case its the Source of Tweets above.
So there it is, now the socket will be pushed with Tweeter information. As I dont care about receiving messages *Sink.ignore* just ignores
any inputs.

### How the data Flow
The tweeter data flows in a custom source operator builded as so:
```scala
  def Tweets(apiKey: String, apiSecret: String)(implicit sys: ActorSystem, mat: ActorMaterializer, ex: ExecutionContext) = {
      // request is a function that creates a HttpRequest object for the Tweeter samples endpoint
      // TweetsList is a case class for JSON formatting
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
         
         // Extract the trend and broadcast it
         source ~> trends ~> bcast
         // Feeds the trend a attach a token for the samples request
         bcast ~> zip.in0
         authSource ~> zip.in1
         
         // Request the samples for the trend and wrap the trend with its tweets samples
         zip.out ~> tweets ~> tweetZip.in1
         bcast.out(1) ~> tweetZip.in0
         tweetZip.out ~> wrapper
         
         SourceShape(wrapper.out)
      })
      
   }
```

### Final
And with all that we have a nice JSON output to be consumed in the frontend which looks like this:
```JSON
  {
    "trend":{
      "name":"#MemorialDay",
      "query":"%23MemorialDay",
      "tweet_volume":427612,
      "url":"http://twitter.com/search?q=%23MemorialDay"
    },
    "tweets":{
        "statuses":[
          {
            "created_at":"Mon May 27 17:58:44 +0000 2019",
            "id_str":"1133070045125500928",
            "text":"RT @thiagoboeker: Happy #MemorialDay",
            "user":{
              ...
            }
          }]
    }
  }
```
There some case classes that I defined to make use of SprayJson easy JSON formatting, and with this everything
is in place.



