package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neeva.app.suggestions.NavSuggestion

@Entity(
    tableName = "Domain",
    indices = [Index(value = ["domainName"], unique = true)]
)
data class Domain(
    @PrimaryKey(autoGenerate = true) val domainUID: Int = 0,
    val domainName: String,
    val providerName: String?,
    @Embedded val largestFavicon: Favicon?,
) {
    fun url(): Uri = Uri.Builder().scheme("https").authority(this.domainName).build()

    fun toNavSuggestion(): NavSuggestion = NavSuggestion(
        url = this.url(),
        label = this.providerName ?: this.domainName,
        secondaryLabel = domainName
    )
}
