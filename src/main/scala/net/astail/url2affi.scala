package net.astail

import java.net.{HttpURLConnection, URL}

import com.typesafe.config.ConfigFactory

import scala.util.Random

object url2affi {
  val userList = ConfigFactory.load.getString("affi_user_list")

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

  def shortUrl(url: String) = {
    // 短縮urlを展開
    HttpURLConnection setFollowRedirects false
    val openUrl = new URL(url) openConnection() getHeaderField "Location"

    normalUrl(openUrl)
  }

  def normalUrl(url: String) = {
    // もともとついてあるtag移行の文字を消す
    url match {
      case s if s contains "&tag=" => s replaceAll("&tag=.*", "")
      case _ => url
    }
  }

  def createTag(userId: String): String = {
    userMap.get(userId) match {
      case Some(v) => v
      case _ => userMap.toList(r)._2
    }
  }

  def check(message: String, userId: String): Option[String] = {
    val user = "<@" + userId + "> "
    val ref = "/ref=as_li_ss_tl?ie=UTF8&linkCode=sl1&tag="

    val splitMessage = message split " "

    // slackのurlは< >に囲まれているので先頭と行末を消す
    val url = splitMessage.head drop 1 dropRight 1

    val optionUser: Option[String] = {
      if (splitMessage.length >= 2)
        splitMessage.reverse.headOption
      else
        None
    }

    val tag: String = optionUser match {
      case Some("t") => setUserMap.toList(0)._2
      case Some("a") => setUserMap.toList(1)._2
      case Some(s) => setUserMap getOrElse(s, createTag(userId))
      case None => createTag(userId)
    }

    url match {
      case s if s.startsWith("https://amzn.to") => Some(user + shortUrl(s) + ref + tag)
      case s if s.startsWith("https://www.amazon.co.jp") => Some(user + normalUrl(s) + ref + tag)
      case _ => None
    }
  }
}
