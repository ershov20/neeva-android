package com.neeva.app.storage

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.*
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Entity(indices = [Index(value = ["domainName"], unique = true)])
data class Domain(
    @PrimaryKey (autoGenerate = true) val domainUID: Int = 0,
    val domainName: String,
    val providerName: String?,
    @Embedded val largestFavicon: Favicon?,
)

@Dao
interface DomainAccessor {
    @Query("SELECT * FROM domain")
    fun getAll(): Flow<List<Domain>>

    @Query("SELECT * FROM domain WHERE domainName LIKE :domainUrl")
    suspend fun find(domainUrl: String): Domain?

    @Query("SELECT * FROM domain WHERE domainName LIKE :domainUrl")
    fun listen(domainUrl: String): Flow<Domain?>

    // Returns list of all domains that has a domainName containing the query
    @Query("SELECT * FROM domain WHERE domainName LIKE :query||'%'")
    fun matchesTo(query: String): List<Domain>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(vararg domains: Domain)

    @Update
    suspend fun update(vararg domains: Domain)

    @Delete
    suspend fun delete(domain: Domain)

    @Transaction
    suspend fun updateFavicon(domainName: String, favicon: Favicon) {
        val domain = find(domainName)
        if (domain == null) {
            add(
                Domain(
                    domainName = domainName,
                    providerName = null,
                    largestFavicon = favicon
                )
            )
        } else {
            update(
                domain.copy(
                    domainName = domainName,
                    largestFavicon = Favicon.bestFavicon(favicon, domain.largestFavicon)
                )
            )
        }
    }

    @Transaction
    suspend fun upsert(domain: Domain) {
        if (find(domain.domainName) == null) {
            add(domain)
        } else {
            update(domain)
        }
    }
}

class DomainRepository(private val domainAccessor: DomainAccessor) {
    val allDomains: Flow<List<NavSuggestion>> = domainAccessor.getAll()
        .distinctUntilChanged()
        .map { domainList ->
            domainList.map { it.toNavSuggestion() }
        }

    @WorkerThread
    fun listen(domainName: String): Flow<Domain> {
        return domainAccessor.listen(domainName).filterNotNull().distinctUntilChanged()
    }

    @WorkerThread
    fun queryNavSuggestions(query: String): List<NavSuggestion> {
        return domainAccessor.matchesTo(query).map { domain -> domain.toNavSuggestion() }
    }

    suspend fun insert(domain: Domain) = domainAccessor.upsert(domain)

    suspend fun updateFaviconFor(domainName: String, favicon: Favicon) {
        domainAccessor.updateFavicon(domainName, favicon)
    }
}

// TODO: Find a more elegant way to handle this through Uri
fun Domain.url() : Uri = Uri.parse("https://www.${this.domainName}")

fun Domain.toNavSuggestion() : NavSuggestion  = NavSuggestion(
    url = this.url(),
    label = this.providerName ?: this.domainName,
    secondaryLabel = domainName
)
