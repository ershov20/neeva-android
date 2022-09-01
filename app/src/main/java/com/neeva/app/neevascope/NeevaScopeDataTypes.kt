// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.net.Uri

data class NeevaScopeWebResult(
    val faviconURL: String,
    val displayURLHost: String,
    val displayURLPath: List<String>?,
    val actionURL: Uri,
    val title: String,
    val snippet: String? = null,
    val publicationDate: String? = null
)

data class DiscussionComment(
    val body: String,
    val url: Uri? = null,
    val upvotes: Int? = null
)

data class DiscussionContent(
    val body: String,
    val comments: List<DiscussionComment>
)

data class NeevaScopeDiscussion(
    // Required properties
    val title: String,
    val content: DiscussionContent,
    val url: Uri,
    val slash: String,

    // Optionally displayed properties
    val upvotes: Int? = null,
    val numComments: Int? = null,
    val interval: String? = null,
)

data class RecipeRating(
    val maxStars: Double,
    val recipeStars: Double,
    val numReviews: Int? = null
)

data class ReviewRating(
    val maxStars: Double? = null,
    val actualStars: Double? = null
)

data class RecipeReview(
    val reviewerName: String,
    val body: String,
    val rating: ReviewRating
)

data class NeevaScopeRecipe(
    val title: String,
    val imageURL: String,
    val totalTime: String? = null,
    val prepTime: String? = null,
    val yield: String? = null,
    val ingredients: List<String>?,
    val instructions: List<String>?,
    val recipeRating: RecipeRating? = null,
    val reviews: List<RecipeReview>?,
    val preference: String? = null
)
