package com.neeva.app.storage.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neeva.app.GetSpacesDataQuery

enum class SpaceEntityType {
    WEB, IMAGE, NEWS, PRODUCT, RECIPE, RICH_ENTITY
}

@Entity(tableName = "SpaceItem")
data class SpaceItem(
    @PrimaryKey val id: String,
    val spaceID: String,
    val url: Uri?,
    val title: String?,
    val snippet: String?,
    val thumbnail: Uri?,

    @ColumnInfo(defaultValue = "0")
    val entityType: SpaceEntityType = SpaceEntityType.WEB,

    /** [SpaceEntityType.RECIPE] and [SpaceEntityType.PRODUCT] specific */
    @ColumnInfo(defaultValue = "")
    val stars: Double? = null,

    @ColumnInfo(defaultValue = "")
    val numReviews: Int? = null,

    /** [SpaceEntityType.RECIPE] specific */
    @ColumnInfo(defaultValue = "")
    val totalTime: String? = null,

    /** [SpaceEntityType.PRODUCT] specific */
    @ColumnInfo(defaultValue = "")
    val price: Double? = null,

    /** [SpaceEntityType.NEWS] specific */
    @ColumnInfo(defaultValue = "")
    val provider: String? = null,

    @ColumnInfo(defaultValue = "")
    val faviconURL: Uri? = null,

    @ColumnInfo(defaultValue = "")
    val datePublished: String? = null
)

private fun GetSpacesDataQuery.Entity.news() =
    this.spaceEntity?.content?.typeSpecific?.onNewsItem?.newsItem
private fun GetSpacesDataQuery.Entity.richEntity() =
    this.spaceEntity?.content?.typeSpecific?.onRichEntity?.richEntity
private fun GetSpacesDataQuery.Entity.recipe() =
    this.spaceEntity?.content?.typeSpecific?.onWeb?.web?.recipes?.first()
private fun GetSpacesDataQuery.Entity.product() =
    this.spaceEntity?.content?.typeSpecific?.onWeb?.web?.retailerProduct

fun GetSpacesDataQuery.Entity.entityType() = when {
    this.news() != null -> SpaceEntityType.NEWS

    this.richEntity() != null -> SpaceEntityType.RICH_ENTITY

    this.recipe() != null -> SpaceEntityType.RECIPE

    this.product() != null && this.product()?.url != null -> SpaceEntityType.PRODUCT

    else -> SpaceEntityType.WEB
}

fun GetSpacesDataQuery.Entity.spaceItem(spaceID: String, thumbnailUri: Uri? = null) =
    when (this.entityType()) {
        SpaceEntityType.NEWS -> SpaceItem(
            id = this.metadata?.docID!!,
            spaceID = spaceID,
            url = this.news()?.url.let { Uri.parse(it) },
            title = this.news()?.title,
            snippet = this.news()?.snippet,
            thumbnail = this.news()?.thumbnailImage?.url?.let { Uri.parse(it) },
            provider = this.news()?.providerName,
            faviconURL = this.news()?.favIconURL?.let { Uri.parse(it) },
            datePublished = this.news()?.datePublished,
            entityType = SpaceEntityType.NEWS
        )
        SpaceEntityType.RICH_ENTITY -> SpaceItem(
            id = this.metadata?.docID!!,
            spaceID = spaceID,
            url = this.spaceEntity?.url?.let { Uri.parse(it) },
            title = this.richEntity()?.title,
            snippet = this.richEntity()?.subTitle,
            thumbnail = this.richEntity()?.images?.first()?.thumbnailURL.let { Uri.parse(it) },
            entityType = SpaceEntityType.RICH_ENTITY
        )
        SpaceEntityType.PRODUCT -> SpaceItem(
            id = this.metadata?.docID!!,
            spaceID = spaceID,
            url = this.product()?.url?.let { Uri.parse(it) },
            title = this.product()?.name,
            snippet = this.spaceEntity?.snippet ?: this.product()?.description?.first(),
            thumbnail = thumbnailUri,
            stars = this.product()?.reviews?.ratingSummary?.rating?.productStars,
            numReviews = this.product()?.reviews?.ratingSummary?.numReviews,
            price = this.product()?.priceHistory?.currentPrice,
            entityType = SpaceEntityType.PRODUCT
        )
        SpaceEntityType.RECIPE -> SpaceItem(
            id = this.metadata?.docID!!,
            spaceID = spaceID,
            url = this.spaceEntity?.url?.let { Uri.parse(it) },
            title = this.recipe()?.title,
            snippet = this.spaceEntity?.snippet,
            thumbnail = this.recipe()?.imageURL?.let { Uri.parse(it) },
            stars = this.recipe()?.recipeRating?.recipeStars,
            numReviews = this.recipe()?.recipeRating?.numReviews,
            totalTime = this.recipe()?.totalTime,
            entityType = SpaceEntityType.RECIPE
        )
        else -> SpaceItem(
            id = this.metadata?.docID!!,
            spaceID = spaceID,
            url = this.spaceEntity?.url?.let { Uri.parse(it) },
            title = this.spaceEntity?.title,
            snippet = this.spaceEntity?.snippet,
            thumbnail = thumbnailUri,
            entityType = SpaceEntityType.WEB
        )
    }
