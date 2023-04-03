package gay.pizza.crtsnoop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

fun createRunningQueriesStatement(db: Connection): PreparedStatement {
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
  return db.prepareStatement(query)
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
  val statement = createRunningQueriesStatement(db)

  while (true) {
    val queries = RunningQuery.extractAll(statement.executeQuery())
    for (query in queries) {
      println(Json.encodeToString(RunningQuery.serializer(), query))
    }
    Thread.sleep(50)
  }
}
