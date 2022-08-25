package com.neeva.app.neevascope

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.StarHalf
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neeva.app.R
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import kotlin.math.floor
import kotlin.math.round

fun LazyListScope.RecipeList(
    recipe: NeevaScopeRecipe,
    faviconCache: FaviconCache?,
    currentUrl: Uri?,
    showFullRecipe: MutableState<Boolean>
) {
    item {
        RecipeHeader(recipe = recipe, faviconCache = faviconCache, currentUrl = currentUrl)
    }

    item {
        NeevaScopeDivider()
    }

    item {
        RecipeInfoSection(recipe.totalTime, recipe.prepTime, recipe.yield)
    }

    if (!showFullRecipe.value) {
        item {
            ShowMoreButton(text = R.string.neevascope_recipe_show_full, showAll = showFullRecipe)
        }
    } else {
        recipe.ingredients?.let {
            item {
                NeevaScopeDivider()
            }

            item {
                RecipeIngredientSection(it)
            }
        }

        recipe.instructions?.let {
            item {
                NeevaScopeDivider()
            }

            item {
                RecipeInstructionSection(it)
            }
        }

        item {
            ShowMoreButton(text = R.string.neevascope_recipe_hide_full, showAll = showFullRecipe)
        }
    }

    item {
        NeevaScopeDivider()
    }
}

@Composable
fun RecipeHeader(
    recipe: NeevaScopeRecipe,
    faviconCache: FaviconCache?,
    currentUrl: Uri?
) {
    val resources = LocalContext.current.resources
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)
        ) {
            NeevaScopeSectionHeader(R.string.neevascope_recipe, recipe.title)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recipe.recipeRating?.let {
                    RatingStar(maxStars = it.maxStars, recipeStars = it.recipeStars)
                }

                recipe.recipeRating?.numReviews
                    .takeIf { it != null && it > 0 }
                    ?.let {
                        Text(
                            text = resources.getQuantityString(
                                R.plurals.neevascope_recipe_reviews,
                                it,
                                it
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
            }

            currentUrl?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    faviconCache?.let {
                        val faviconBitmap: Bitmap? by it.getFaviconAsync(currentUrl)
                        faviconBitmap?.let { bitmap ->
                            Icon(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
                            )
                        }
                    }

                    Text(
                        text = currentUrl.host.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Column {
            AsyncImage(
                model = recipe.imageURL,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.clip(RoundedCornerShape(Dimensions.RADIUS_MEDIUM))
            )
        }
    }
}

@Composable
fun RatingStar(
    maxStars: Double,
    recipeStars: Double
) {
    val normalizedRating = normalizeRating(recipeStars, maxStars)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (recipeStars > 0 && floor(normalizedRating).toInt() >= 1) {
            for (i in 1..floor(normalizedRating).toInt()) {
                Star(Icons.Outlined.Star)
            }
            if (round(normalizedRating) > floor(normalizedRating)) {
                Star(Icons.Outlined.StarHalf)
            } else {
                for (i in round(normalizedRating).toInt() until 5) {
                    Star(Icons.Outlined.StarBorder)
                }
            }
        }
    }
}

@Composable
fun Star(
    starType: ImageVector
) {
    Icon(
        starType,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
    )
}

fun normalizeRating(stars: Double, maxStars: Double): Double {
    val standardStars = 5.0
    return if (maxStars <= standardStars) {
        stars
    } else {
        stars / maxStars * standardStars
    }
}

@Composable
fun RecipeInfoSection(
    totalTime: String?,
    prepTime: String?,
    yieldString: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)) {
        totalTime?.let { totaltime ->
            prepTime?.let { preptime ->
                RecipeInfoRow(
                    text = stringResource(
                        R.string.neevascope_recipe_cookingtime,
                        totaltime,
                        preptime
                    ),
                    icon = Icons.Outlined.Timer
                )
            }
        }

        yieldString?.let {
            RecipeInfoRow(
                text = it,
                icon = Icons.Outlined.People,
                iconDescription = stringResource(id = R.string.neevascope_recipe_yield)
            )
        }
    }
}

@Composable
fun RecipeInfoRow(
    text: String,
    icon: ImageVector,
    iconDescription: String? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
        verticalAlignment = Alignment.Top
    ) {
        val lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.value
        Column(
            modifier = Modifier.height(lineHeight.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
            )
        }

        Column(verticalArrangement = Arrangement.Center) {
            Text(text = text)
        }
    }
}

@Composable
fun RecipeIngredientSection(
    ingredients: List<String>
) {
    RecipeSectionHeader(header = R.string.neevascope_recipe_ingredients)
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)) {
        ingredients.forEach { ingredient ->
            Text(text = ingredient)
        }
    }
}

@Composable
fun RecipeInstructionSection(
    instructions: List<String>
) {
    RecipeSectionHeader(header = R.string.neevascope_recipe_instructions)
    instructions.forEachIndexed { index, instruction ->
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)) {
            Text(text = stringResource(R.string.neevascope_recipe_instruction_index, index + 1))

            Text(text = instruction)
        }
    }
}

@Composable
fun RecipeSectionHeader(
    @StringRes header: Int
) {
    Row(modifier = Modifier.padding(bottom = Dimensions.PADDING_SMALL)) {
        Text(
            text = stringResource(id = header),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@PortraitPreviews
@Composable
fun RecipeHeader_Preview() {
    LightDarkPreviewContainer {
        RecipeHeader(
            recipe = NeevaScopeRecipe(
                title = "Lemon Bars",
                imageURL = "",
                totalTime = "3 hours, 50 minutes",
                prepTime = "10 minutes",
                yield = "24",
                ingredients = listOf(
                    "1 cup (230g; 2 sticks) unsalted butter, melted",
                    "1/2 cup (100g) granulated sugar"
                ),
                instructions = listOf(
                    "Preheat the oven to 325°F (163°C)",
                    "Mix the melted butter, sugar, vanilla extract, and salt together in a bowl."
                ),
                recipeRating = RecipeRating(5.0, 4.7, 877),
                reviews = listOf(),
                preference = null
            ),
            faviconCache = null,
            currentUrl = Uri.parse("https://sallysbakingaddiction.com/lemon-bars-recipe/")
        )
    }
}
