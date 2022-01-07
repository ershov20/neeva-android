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

    fun getFlow(domainName: String): Flow<Domain?> {
        return domainAccessor.getFlow(domainName).distinctUntilChanged()
    }

    suspend fun queryNavSuggestions(query: String): List<NavSuggestion> {
        return domainAccessor.matchesTo(query).map { domain -> domain.toNavSuggestion() }
    }

    suspend fun insert(domain: Domain) = domainAccessor.upsert(domain)

    suspend fun updateFaviconFor(domainName: String, favicon: Favicon) {
        domainAccessor.updateFavicon(domainName, favicon)
    }
}
