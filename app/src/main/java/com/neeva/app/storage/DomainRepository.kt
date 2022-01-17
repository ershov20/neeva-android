package com.neeva.app.storage

import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DomainRepository(private val domainAccessor: DomainAccessor) {
    val allDomains: Flow<List<NavSuggestion>> = domainAccessor.getAll()
        .distinctUntilChanged()
        .map { domainList ->
            domainList.map { it.toNavSuggestion() }
        }

    suspend fun get(domainName: String): Domain? = domainAccessor.get(domainName)

    suspend fun queryNavSuggestions(query: String, limit: Int): List<NavSuggestion> {
        return domainAccessor.matchesTo(query, limit).map { domain -> domain.toNavSuggestion() }
    }

    suspend fun insert(domain: Domain) = domainAccessor.upsert(domain)

    suspend fun updateFaviconFor(domainName: String, favicon: Favicon) {
        domainAccessor.updateFavicon(domainName, favicon)
    }
}
