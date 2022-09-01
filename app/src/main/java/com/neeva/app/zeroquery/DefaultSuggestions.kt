// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Site

/** Default placeholder suggestions for the zero query surface */
object DefaultSuggestions {
    val DEFAULT_SITE_SUGGESTIONS = listOf(
        Site(
            siteURL = "https://wikipedia.org",
            title = "Wikipedia",
            largestFavicon = Favicon(
                faviconURL = "https://www.wikipedia.org/static/apple-touch/wikipedia.png",
                width = 160,
                height = 160
            )
        ),
        Site(
            siteURL = "https://reddit.com",
            title = "Reddit",
            largestFavicon = Favicon(
                faviconURL =
                "https://www.redditstatic.com/desktop2x/img/favicon/apple-icon-180x180.png",
                width = 180,
                height = 180
            )
        ),
        Site(
            siteURL = "https://news.ycombinator.com",
            title = "Hacker News",
            largestFavicon = Favicon(
                faviconURL = "https://news.ycombinator.com/favicon.ico",
                width = 180,
                height = 180
            )
        ),
        Site(
            siteURL = "https://twitter.com",
            title = "Twitter",
            largestFavicon = Favicon(
                faviconURL =
                "https://abs.twimg.com/responsive-web/client-web/icon-ios.b1fc7276.png",
                width = 192,
                height = 192
            )
        ),
        Site(
            siteURL = "https://pinterest.com",
            title = "Pinterest",
            largestFavicon = Favicon(
                faviconURL = "https://s.pinimg.com/webapp/logo_trans_144x144-5e37c0c6.png",
                width = 144,
                height = 144
            )
        ),
        Site(
            siteURL = "https://youtube.com",
            title = "Youtube",
            largestFavicon = Favicon(
                faviconURL = "https://www.youtube.com/s/desktop/5ab5196f/img/favicon_144x144.png",
                width = 144,
                height = 144
            )
        ),
        Site(
            siteURL = "https://linkedin.com",
            title = "Linkedin",
            largestFavicon = Favicon(
                faviconURL =
                "https://static.licdn.com/scds/common/u/images/logos/favicons/v1/favicon.ico",
                width = 180,
                height = 180
            )
        )
    )

    val DEFAULT_SEARCH_SUGGESTIONS = listOf(
        "Best Headphones", "Lemon Bar Recipe", "React Hooks"
    )
}
