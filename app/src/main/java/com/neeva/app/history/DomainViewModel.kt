package com.neeva.app.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.browsing.baseDomain
import com.neeva.app.storage.Domain
import com.neeva.app.storage.DomainRepository
import com.neeva.app.storage.Favicon
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DomainViewModel(private val repository: DomainRepository) : ViewModel() {
    private val _domainSuggestions = MutableStateFlow<List<NavSuggestion>>(emptyList())
    val domainSuggestions: StateFlow<List<NavSuggestion>> = _domainSuggestions

    /** Updates the query that is being used to fetch domain name suggestions. */
    fun updateSuggestionQuery(currentInput: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _domainSuggestions.value = repository.queryNavSuggestions(currentInput)
        }
    }

    /** Returns a Flow that emits the best Favicon for the given Uri. */
    fun getFaviconFlow(uri: Uri?): Flow<Favicon?> {
        val domainName = uri?.baseDomain() ?: return flowOf(null)
        return repository
            .listen(domainName)
            .map { it.largestFavicon }
            .distinctUntilChanged()
    }

    fun insert(domain: Domain) = viewModelScope.launch {
        repository.insert(domain)
    }

    fun insert(url:String, title: String? = null) = viewModelScope.launch {
        val domainName = Uri.parse(url).baseDomain() ?: return@launch
        repository.insert(
            Domain(
                domainName = domainName,
                providerName = title,
                largestFavicon = null
            )
        )
    }

    fun updateFaviconFor(url: String, favicon: Favicon) = viewModelScope.launch {
        repository.updateFaviconFor(url, favicon)
    }

    companion object {
        class DomainViewModelFactory(private val repository: DomainRepository) :
            ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DomainViewModel(repository) as T
            }
        }
    }
}
