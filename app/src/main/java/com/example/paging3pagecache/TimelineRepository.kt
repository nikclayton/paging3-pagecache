package com.example.paging3pagecache

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.paging3pagecache.mastodon.MastodonService
import com.example.paging3pagecache.mastodon.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

// Things that make this more difficult than it should be:
//
// - Mastodon API doesn't support "Fetch page that contains item X", you have to rely on having
//   the page that contains item X, and the previous or next page, so you can use the prev/next
//   link values from the next or previous page to step forwards or backwards to the page you
//   actually want.
//
// - Not all Mastodon APIs that paginate support a "Fetch me just the item X". E.g., getting a
//   list of bookmarks (https://docs.joinmastodon.org/methods/bookmarks/#get) paginates, but does
//   not support a "Get a single bookmark" call. Ditto for favourites. So even though some API
//   methods do support that they can't be used here, because this has to work for all paging APIs.
//
// - Values of next/prev in the Link header do not have to match any of the item keys (or be taken
//   from the same namespace).
//
// - Two pages that are consecutive in the result set may not have next/prev values that point
//   back to each other. I.e., this is a valid set of two pages from an API call:
//
//   .--- page index
//   |     .-- ID of last item (key in `pageCache`)
//   v     V
//   0: k: 109934818460629189, prevKey: 995916, nextKey: 941865
//   1: k: 110033940961955385, prevKey: 1073324, nextKey: 997376
//
//   They are consecutive in the result set, but pageCache[0].prevKey != pageCache[1].nextKey. So
//   there's no benefit to using the nextKey/prevKey tokens as the keys in PageCache.
//
// - Bugs in the Paging library mean that on initial load (especially of rapidly changing timelines
//   like Federated) the user's initial position can jump around a lot. See:
//   - https://issuetracker.google.com/issues/235319241
//   - https://issuetracker.google.com/issues/289824257

/** Timeline repository where the timeline information is backed by an in-memory cache. */
class TimelineRepository constructor(
    private val mastodonService: MastodonService,
) {
    private val pageCache = PageCache()

    private var factory: InvalidatingPagingSourceFactory<String, Status>? = null

    /** @return flow of Mastodon [Status], loaded in [pageSize] increments */
    @OptIn(ExperimentalPagingApi::class)
    fun getStatusStream(
        viewModelScope: CoroutineScope,
        pageSize: Int = PAGE_SIZE,
        initialKey: String? = null
    ): Flow<PagingData<Status>> {
        Log.d(TAG, "getStatusStream(): key: $initialKey")

        factory = InvalidatingPagingSourceFactory {
            TimelinePagingSource(pageCache)
        }

        return Pager(
            config = PagingConfig(pageSize = pageSize, initialLoadSize = pageSize),
            remoteMediator = TimelineRemoteMediator(
                viewModelScope,
                mastodonService,
                factory!!,
                pageCache,
            ),
            pagingSourceFactory = factory!!
        ).flow
    }

    companion object {
        private const val TAG = "TimelineRepository"
        private const val PAGE_SIZE = 30
    }
}
