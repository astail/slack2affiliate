package net.astail

import java.net.{HttpURLConnection, URL}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import slack.rtm.SlackRtmClient

import scala.util.Random


object Main {
  def main(args: Array[String]): Unit = {
    val logger: Logger = LoggerFactory.getLogger(this.getClass)

    logger.info("start app")

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
      val text = message.text

      val sendMessageOption = logCheck(text, user)

      sendMessageOption match {
        case Some(s) => {
          channelId match {
            case channel => {
              logger.info("================================================================")
              logger.info(s"text: $text")
              logger.info(s"sendMessage: $s")
              logger.info("================================================================")
              client.sendMessage(channel, s.toString)
            }
            case _ =>
          }
        }
        case None =>
      }
    }
  }


  def logCheck(message: String, user: String): Option[String] = {
    message match {
      case s if s contains "pubg" => Some(s"@$user fps")
      case "還元率" => Some("https://affiliate.amazon.co.jp/welcome/compensation/")
      case _ => url2affi(message, user)
    }
  }


  val userList = ConfigFactory.load.getString("affi_user_list")

  // todo: tryで包む
  val setUserMap: Map[String, String] =
    userList
      .split(",")
      .map(_.replaceAll("\\(|\\)", "").trim)
      .toList
      .map(_.split("="))
      .map(xs => xs(0) -> xs(1))
      .toMap

  val userMap: Map[String, String] =
    Map(
      (setUserMap.toList(0)._1, setUserMap.toList(1)._2),
      (setUserMap.toList(1)._1, setUserMap.toList(0)._2)
    )


  def r = new Random().nextInt(userMap.size)

  def url2affi(message: String, user: String): Option[String] = {
    val tag = userMap.get(user) match {
      case Some(v) => v
      case _ => userMap.toList(r)._2
    }

    // slackのurlは< >に囲まれているので先頭と行末を消す
    val url = message drop 1 dropRight 1


    def shortUrl(url: String) = {
      // 短縮urlを展開
      HttpURLConnection setFollowRedirects false
      val openUrl = new URL(url) openConnection() getHeaderField ("Location")

      normalUrl(openUrl)
    }

    def normalUrl(url: String) = {
      // もともとついてあるtag移行の文字を消す
      val deleteTagUrl = url match {
        case s if s contains ("&tag=") => s replaceAll("&tag=.*", "")
        case _ => url
      }
      Some("@" + user + " " + deleteTagUrl + "/ref=as_li_ss_tl?ie=UTF8&linkCode=sl1&tag=" + tag)
    }

    url match {
      case s if s.startsWith("https://amzn.to") => shortUrl(s)
      case s if s.startsWith("https://www.amazon.co.jp") => normalUrl(s)
      case _ => None
    }
  }

}
