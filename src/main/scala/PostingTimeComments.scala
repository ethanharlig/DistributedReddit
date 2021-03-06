import java.time.Instant

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object PostingTimeComments {
  def main(args: Array[String]): Unit = {
    val before = Instant.now()
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val conf = new SparkConf().setAppName("Driver").setMaster("local[1]")

    val rdd = RedditUtil.getRedditRDD(conf)

        allSubs(rdd)

//    perSub(rdd)
    println(Instant.now().toEpochMilli - before.toEpochMilli)
  }

  def allSubs(rdd: RDD[RedditPost]): Unit = {
    rdd
      .map { redditPost: RedditPost =>
        val hr = Instant.ofEpochSecond(redditPost.timeCreated.toLong).toString.split("T")(1).split(":")(0).toInt

        (hr, (redditPost.numComments, 1))
      }.reduceByKey{ case
      ((x1, x2), (y1, y2)) => (x1 + y1, x2 + y2)
    }.map { case (hr, (a, b)) =>
      (hr, a / b.toFloat)
    }.sortByKey().collect().foreach(println)
  }

  def perSub(rdd: RDD[RedditPost]): Unit = {
    rdd
      .map { redditPost: RedditPost =>
        val hr = Instant.ofEpochSecond(redditPost.timeCreated.toLong).toString.split("T")(1).split(":")(0).toInt

        (redditPost.subreddit + "," + hr.toString, (redditPost.numComments, 1))
      }.reduceByKey{ case
      ((x1, x2), (y1, y2)) => (x1 + y1, x2 + y2)
    }.map { case (k, (comments, posts)) =>
      (k, comments / posts.toFloat)
    }.sortBy(a => (a._1.split(",")(0), a._2)).collect().foreach { case (k, a) =>
      val spl = k.split(",")
      println(spl(0), spl(1), a)
    }
  }
}
