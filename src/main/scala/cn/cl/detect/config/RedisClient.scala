package cn.cl.detect.config

import cn.cl.detect.constant.Constants
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

object RedisClient extends Serializable {

  val redisHost = ConfigurationManager.getProperty(Constants.REDIS_HOST);
  val redisPort = ConfigurationManager.getInteger(Constants.REDIS_PORT);
  val redisTimeout = ConfigurationManager.getInteger(Constants.REDIS_TIMEOUT)

  /**
    * JedisPool是连接池，保证线程安全和效率
    */
  lazy val pool = new JedisPool(new GenericObjectPoolConfig(),redisHost,redisPort,redisTimeout)

    def main(args: Array[String]): Unit = {
      val jedis = RedisClient.pool.getResource();
      jedis.select(1);
      val map = jedis.hgetAll("umbrella_table");
      println(map.size())
      jedis.close();
    }
}
