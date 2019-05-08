package net.astail

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import slack.rtm.SlackRtmClient

object Main {
  def main(args: Array[String]): Unit = {
    val logger: Logger = LoggerFactory.getLogger(this.getClass)

    logger.info("start app")

    val token = ConfigFactory.load.getString("slack_token")
    val botChannel = ConfigFactory.load.getString("slack_channel")
    implicit val system = ActorSystem("slack")
    implicit val ec = system.dispatcher

    val client = SlackRtmClient(token)

    client.onMessage { message =>
      val channel: String = client.state.getChannelIdForName(botChannel).getOrElse("")
      val userId = client.state.getUserById(message.user).map(_.id).getOrElse("")
      val receiveMessage = message.text

      val sendMessageOption = messageCheck(receiveMessage, userId)

      sendMessageOption match {
        case Some(sendMessage: String) => {
          logger.info("================================================================")
          logger.info(s"receiveMessage: $receiveMessage")
          logger.info(s"sendMessage: $sendMessage")
          logger.info("================================================================")
          client.sendMessage(channel, sendMessage)
        }
        case None =>
      }
    }
  }


  def messageCheck(message: String, userId: String): Option[String] = {
    message match {
      case "pubg" => Some(s"<@$userId> fps")
      case "紹介料率" => Some("https://affiliate.amazon.co.jp/welcome/compensation/")
      case _ => url2affi.check(message, userId)
    }
  }
}
