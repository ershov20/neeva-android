package com.neeva.app.storage

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import androidx.room.*
import com.neeva.app.browsing.baseDomain
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    fun matchesTo(query: String): Flow<List<Domain>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(vararg domains: Domain)

    @Update
    suspend fun update(vararg domains: Domain)

    @Delete
    suspend fun delete(domain: Domain)
}


class DomainRepository(private val domainAccessor: DomainAccessor) {
    val allDomains: Flow<List<NavSuggestion>> = domainAccessor.getAll()
        .distinctUntilChanged().map { domainList ->
        domainList.map { it.toNavSuggest() }
    }

    @WorkerThread
    fun listen(domainName: String): Flow<Domain> {
        return domainAccessor.listen(domainName).filterNotNull().distinctUntilChanged()
    }

    @WorkerThread
    fun matchesTo(query: String): Flow<List<NavSuggestion>> {
        return domainAccessor.matchesTo(query).distinctUntilChanged().map { domainList ->
            domainList.map { it.toNavSuggest() }
        }.flowOn(Dispatchers.Default).conflate()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(domain: Domain) {
        if (domainAccessor.find(domain.domainName) != null) {
            domainAccessor.update(domain)
        } else {
            domainAccessor.add(domain)
        }
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateFaviconFor(url: String, favicon: Favicon) {
        val domainName = Uri.parse(url).baseDomain() ?: return
        val domain = domainAccessor.find(domainName)
        if (domain == null) {
            val newDomain = Domain(
                domainName = domainName, providerName = null, largestFavicon = favicon
            )
            domainAccessor.add(newDomain)
        } else if (favicon.width > domain.largestFavicon?.width ?: 0) {
            val newDomain = Domain(
                domainUID = domain.domainUID, domainName = domainName,
                providerName = domain.providerName, largestFavicon = favicon
            )
            domainAccessor.update(newDomain)
        }
    }
}

class DomainViewModel(private val repository: DomainRepository) : ViewModel() {
    val allDomains: LiveData<List<NavSuggestion>> = repository.allDomains.asLiveData()

    val textFlow = MutableStateFlow("")
    var domainsSuggestions: LiveData<List<NavSuggestion>> = textFlow.flatMapLatest {
        repository.matchesTo(it)
    }.asLiveData()

    var autocompletedSuggestion: LiveData<NavSuggestion?> = domainsSuggestions.map {
        if (it.isEmpty()) return@map null

        it.first()
    }

    fun getFaviconFor(uri: Uri?): LiveData<Bitmap?> {
        val domainName = uri?.baseDomain() ?: return MutableLiveData()
        return repository.listen(domainName)
            .mapNotNull { it.largestFavicon?.toBitmap()}.asLiveData()
    }

    fun insert(domain: Domain) = viewModelScope.launch {
        repository.insert(domain)
    }

    fun insert(url:String, title: String? = null) = viewModelScope.launch {
        val domainName = Uri.parse(url).baseDomain() ?: return@launch
        repository.insert(Domain(domainName = domainName, providerName = title, largestFavicon = null))
    }

    fun updateFaviconFor(url: String, favicon: Favicon) = viewModelScope.launch {
        repository.updateFaviconFor(url, favicon)
    }
}

class DomainViewModelFactory(private val repository: DomainRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DomainViewModel(repository) as T
    }
}

// TODO: Find a more elegant way to handle this through Uri
fun Domain.url() : Uri = Uri.parse("https://www.${this.domainName}")

fun Domain.toNavSuggest() : NavSuggestion  = NavSuggestion(
    url = this.url(),
    label = this.providerName ?: this.domainName,
    secondaryLabel = domainName
)