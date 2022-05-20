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
    var itemIndex: Int = -1,

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
    spaceEntity?.content?.typeSpecific?.onNewsItem?.newsItem
private fun GetSpacesDataQuery.Entity.richEntity() =
    spaceEntity?.content?.typeSpecific?.onRichEntity?.richEntity
private fun GetSpacesDataQuery.Entity.recipe() =
    spaceEntity?.content?.typeSpecific?.onWeb?.web?.recipes?.first()
private fun GetSpacesDataQuery.Entity.product() =
    spaceEntity?.content?.typeSpecific?.onWeb?.web?.retailerProduct

fun GetSpacesDataQuery.Entity.entityType() = when {
    news() != null -> SpaceEntityType.NEWS

    richEntity() != null -> SpaceEntityType.RICH_ENTITY

    recipe() != null -> SpaceEntityType.RECIPE

    product() != null && product()?.url != null -> SpaceEntityType.PRODUCT

    isSupportedImage() -> SpaceEntityType.IMAGE

    else -> SpaceEntityType.WEB
}

private fun GetSpacesDataQuery.Entity.isSupportedImage() =
    spaceEntity?.url?.lowercase()?.endsWith(".jpg") == true ||
        spaceEntity?.url?.lowercase()?.endsWith(".jpeg") == true ||
        spaceEntity?.url?.lowercase()?.endsWith(".png") == true

fun GetSpacesDataQuery.Entity.spaceItem(spaceID: String, thumbnailUri: Uri? = null): SpaceItem? {
    val id = metadata?.docID ?: return null
    return when (entityType()) {
        SpaceEntityType.NEWS -> SpaceItem(
            id = id,
            spaceID = spaceID,
            url = news()?.url.let { Uri.parse(it) },
            title = news()?.title,
            snippet = news()?.snippet,
            thumbnail = news()?.thumbnailImage?.url?.let { Uri.parse(it) },
            provider = news()?.providerName,
            faviconURL = news()?.favIconURL?.let { Uri.parse(it) },
            datePublished = news()?.datePublished,
            entityType = SpaceEntityType.NEWS
        )
        SpaceEntityType.RICH_ENTITY -> SpaceItem(
            id = id,
            spaceID = spaceID,
            url = spaceEntity?.url?.let { Uri.parse(it) },
            title = richEntity()?.title,
            snippet = richEntity()?.subTitle,
            thumbnail = richEntity()?.images?.first()?.thumbnailURL.let { Uri.parse(it) },
            entityType = SpaceEntityType.RICH_ENTITY
        )
        SpaceEntityType.PRODUCT -> SpaceItem(
            id = id,
            spaceID = spaceID,
            url = product()?.url?.let { Uri.parse(it) },
            title = product()?.name,
            snippet = spaceEntity?.snippet ?: product()?.description?.first(),
            thumbnail = thumbnailUri,
            stars = product()?.reviews?.ratingSummary?.rating?.productStars,
            numReviews = product()?.reviews?.ratingSummary?.numReviews,
            price = product()?.priceHistory?.currentPrice,
            entityType = SpaceEntityType.PRODUCT
        )
        SpaceEntityType.RECIPE -> SpaceItem(
            id = id,
            spaceID = spaceID,
            url = spaceEntity?.url?.let { Uri.parse(it) },
            title = recipe()?.title,
            snippet = spaceEntity?.snippet,
            thumbnail = recipe()?.imageURL?.let { Uri.parse(it) },
            stars = recipe()?.recipeRating?.recipeStars,
            numReviews = recipe()?.recipeRating?.numReviews,
            totalTime = recipe()?.totalTime,
            entityType = SpaceEntityType.RECIPE
        )
        else -> SpaceItem(
            id = id,
            spaceID = spaceID,
            url = spaceEntity?.url?.let { Uri.parse(it) },
            title = spaceEntity?.title,
            snippet = spaceEntity?.snippet,
            thumbnail = thumbnailUri,
            entityType = entityType()
        )
    }
}
