package com.neeva.app.storage

import com.neeva.app.storage.daos.DomainDao
import com.neeva.app.storage.entities.Domain
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DomainRepository(private val domainDao: DomainDao) {
    val allDomains: Flow<List<NavSuggestion>> = domainDao.getAll()
        .distinctUntilChanged()
        .map { domainList ->
            domainList.map { it.toNavSuggestion() }
        }

    suspend fun get(domainName: String): Domain? = domainDao.get(domainName)

    suspend fun queryNavSuggestions(query: String, limit: Int): List<NavSuggestion> {
        return domainDao.matchesTo(query, limit).map { domain -> domain.toNavSuggestion() }
    }

    suspend fun insert(domain: Domain) = domainDao.upsert(domain)

    suspend fun updateFaviconFor(domainName: String, favicon: Favicon) {
        domainDao.updateFavicon(domainName, favicon)
    }
}
