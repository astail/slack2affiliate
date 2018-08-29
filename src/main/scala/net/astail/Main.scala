package net.astail

import java.net.{HttpURLConnection, URL}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import slack.rtm.SlackRtmClient
import scala.util.Random


object Main {
  def main(args: Array[String]): Unit = {

    val token = ConfigFactory.load.getString("slack_token")
    val botChannel = ConfigFactory.load.getString("slack_channel")
    implicit val system = ActorSystem("slack")
    implicit val ec = system.dispatcher

    val client = SlackRtmClient(token)
    val selfId = client.state.self.id

    client.onMessage { message =>

      val channelId: String = message.channel
      val channel = client.state.getChannelIdForName(botChannel).getOrElse("")
      val user = client.state.getUserById(message.user).map(_.name).getOrElse("")

      val sendMessage = logCheck(message.text, user)


      // todo: [warn]なのでどうにかする
      channelId match {
        case channel => client.sendMessage(channel, sendMessage)
        case _ =>
      }
    }
  }


  def logCheck(message: String, user: String): String = {
    message match {
      case s if s contains ("pubg") => s"@$user fps"
      case _ => url2affi(message, user)
    }
  }


  val userList = ConfigFactory.load.getString("affi_user_list")

  // todo: tryで包む
  val setUserMap: Map[String,String] =
    userList.split(",").map(_.replaceAll("\\(|\\)", "").trim).toList.map(_.split("=")).map(xs => xs(0) -> xs(1)).toMap

  val userMap: Map[String,String] =
    Map((setUserMap.toList(0)._1, setUserMap.toList(1)._2), (setUserMap.toList(1)._1, setUserMap.toList(0)._2))


  def r = new Random().nextInt(userMap.size)

  def url2affi(message: String, user: String): String = {
    val tag = userMap.get(user) match {
      case Some(v) => v
      case _ => userMap.toList(r)._2
    }


    // todo: url以外に反応しない

    // slackのurlは< >に囲まれているので先頭と行末を消す
    val url = message drop 1 dropRight 1

    // 短縮urlを展開
    HttpURLConnection setFollowRedirects false
    val openUrl = new URL(url) openConnection() getHeaderField ("Location")

    // もともとついてあるtag移行の文字を消す
    val deleteTagUrl = openUrl match {
      case s if s contains ("&tag=") => s replaceAll("&tag=.*", "")
      case _ => openUrl
    }

    "@" + user + " " + deleteTagUrl + "/ref=as_li_ss_tl?ie=UTF8&linkCode=sl1&tag=" + tag
  }
}
