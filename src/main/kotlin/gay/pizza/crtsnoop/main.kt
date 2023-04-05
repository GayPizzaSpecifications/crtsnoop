package gay.pizza.crtsnoop

import com.google.common.cache.CacheBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.sql.*

fun createRunningQueriesStatement(db: Connection): Pair<Statement, String> {
  val query = """
    SELECT
        query_start AS query_start,
        query AS query,
        client_addr AS client_ip,
        client_hostname AS client_hostname
    FROM pg_stat_activity
    WHERE query != '<IDLE>' AND query != '<insufficient privilege>' AND query NOT ILIKE '%pg_stat_activity%'
    ORDER BY query_start DESC
  """.trimIndent()
  val statement = db.createStatement()
  return statement to query
}

@Serializable
data class RunningQuery(
  val query: String,
  val startTime: String? = null,
  val clientIp: String? = null,
  val clientHost: String? = null
) { companion object }

fun RunningQuery.Companion.extractAll(resultSet: ResultSet): List<RunningQuery> {
  return generateSequence { if (resultSet.next()) resultSet else null }.map {
    RunningQuery(
      startTime = it.getTimestamp("query_start")?.toInstant()?.toString(),
      query = it.getString("query"),
      clientIp = it.getString("client_ip"),
      clientHost = it.getString("client_hostname")
    )
  }.toList()
}

fun main() {
  val db = DriverManager.getConnection("jdbc:postgresql://crt.sh/certwatch", "guest", "")
  val cache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .build<String, String>()

  while (true) {
    try {
      val (statement, query) = createRunningQueriesStatement(db)
      statement.use { stmt ->
        val queries = RunningQuery.extractAll(stmt.executeQuery(query))
        for (item in queries) {
          val identity = Json.encodeToString(RunningQuery.serializer(), item)
          if (cache.getIfPresent(identity) == null) {
            println(identity)
          }
          cache.put(identity, identity)
        }
      }
    } catch (e: Exception) {
      System.err.println("ERROR: $e")
      e.printStackTrace()
    }
  }
}
