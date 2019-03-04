package querymodule.cache

import javax.inject.Inject
import play.api.Configuration
import play.api.cache.Cached
import play.api.mvc._

/** Wrapper for cached HTTP requests
 *
 *  @param cached        Play caching API class instance
 *  @param configuration Play configuration
 */
class HttpCache @Inject() (implicit cached: Cached, configuration: Configuration) {
  private[this] val configPath: String = "play.cache"
  private[this] val ttlConfigPath: String = s"$configPath.ttl"

  /** Cache and retrieve successful responses
   *
   *  @param action     action performed if response is not cached
   *  @param configPath custom path to ttl configuration
   *  @return action parameter or cached response
   */
  def getSuccessful(action: Action[AnyContent], configPath: String = "invalid-path"): EssentialAction = {
    cached
      .empty(request => key(request))
      .includeStatus(200, ttl(configPath)) {
        action
      }
  }

  /** Retrieve TTL from config file
   *
   *  @param key custom config key (e.g. URI path, etc.)
   *  @return TTL in seconds
   */
  def ttl(key: String): Int = {
    import querymodule.config.ConfigCommon._
    def defaultTtl: Int = getConfigInt("default", ttlConfigPath, Some(30))

    getConfigInt(key, ttlConfigPath, Some(defaultTtl))
  }

  /** Build cache ID
   *
   *  @param request HTTP request
   *  @return path + hash code of QueryRequest wrapper
   */
  def key(request: RequestHeader): String = {
    request.queryString.hashCode() +
      sessionTokens(request)
      .flatMap(_._2)
      .mkString(".") +
      request.path
  }

  /** Retrieve required tokens from the Play session
   *
   *  @param request HTTP request
   *  @return list of session token values for a request
   */
  def sessionTokens(request: RequestHeader): Seq[(String, Option[String])] = {
    import querymodule.config.ConfigCommon._

    val tokens = getConfigListStr("tokens", configPath)

    tokens.map { token =>
      token -> request.session.get(token)
    }
  }
}
