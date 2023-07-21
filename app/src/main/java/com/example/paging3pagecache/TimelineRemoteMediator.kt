package com.example.paging3pagecache

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.paging3pagecache.mastodon.MastodonService
import com.example.paging3pagecache.mastodon.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Remote mediator for accessing timelines that are not backed by the database. */
@OptIn(ExperimentalPagingApi::class)
class TimelineRemoteMediator(
    private val viewModelScope: CoroutineScope,
    private val api: MastodonService,
    private val factory: InvalidatingPagingSourceFactory<String, Status>,
    private val pageCache: PageCache,
) : RemoteMediator<String, Status>() {

    override suspend fun load(loadType: LoadType, state: PagingState<String, Status>): MediatorResult {
        return try {
            val key = when (loadType) {
                LoadType.REFRESH -> {
                    // Find the closest page to the current position
                    val itemKey = state.anchorPosition?.let { state.closestItemToPosition(it) }?.id
                    itemKey?.let { ik ->
                        val pageContainingItem = pageCache.floorEntry(ik)
                            ?: throw java.lang.IllegalStateException("$itemKey not found in the pageCache page")

                        // Double check the item appears in the page
                        if (BuildConfig.DEBUG) {
                            pageContainingItem.value.data.find { it.id == itemKey }
                                ?: throw java.lang.IllegalStateException("$itemKey not found in returned page")
                        }

                        // The desired key is the prevKey of the page immediately before this one
                        pageCache.lowerEntry(pageContainingItem.value.data.last().id)?.value?.prevKey
                    }
                }
                LoadType.APPEND -> {
                    pageCache.firstEntry()?.value?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.PREPEND -> {
                    pageCache.lastEntry()?.value?.prevKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            Log.d(TAG, "- load(), type = $loadType, key = $key")

            val response = fetchStatusPageByKind(loadType, key, state.config.initialLoadSize)
            var page = Page.tryFrom(response).getOrElse { return MediatorResult.Error(it) }

            // If doing a refresh with a known key Paging3 wants you to load "around" the requested
            // key, so that it can show the item with the key in the view as well as context before
            // and after it.
            //
            // To ensure that the first page loaded after a refresh is big enough load the page
            // immediately before and the page immediately after as well, and merge the three of
            // them in to one large page.
            if (loadType == LoadType.REFRESH && key != null) {
                Log.d(TAG, "  Refresh with non-null key, creating huge page")
                val prevPageJob = viewModelScope.async {
                    page.prevKey?.let { key ->
                        fetchStatusPageByKind(LoadType.PREPEND, key, state.config.initialLoadSize)
                    }
                }
                val nextPageJob = viewModelScope.async {
                    page.nextKey?.let { key ->
                        fetchStatusPageByKind(LoadType.APPEND, key, state.config.initialLoadSize)
                    }
                }
                val prevPage = prevPageJob.await()
                    ?.let { Page.tryFrom(it).getOrElse { return MediatorResult.Error(it) } }
                val nextPage = nextPageJob.await()
                    ?.let { Page.tryFrom(it).getOrElse { return MediatorResult.Error(it) } }
                Log.d(TAG, "    prevPage: $prevPage")
                Log.d(TAG, "     midPage: $page")
                Log.d(TAG, "    nextPage: $nextPage")
                page = page.merge(prevPage, nextPage)

                if (BuildConfig.DEBUG) {
                    // Verify page contains the expected key
                    state.anchorPosition?.let { state.closestItemToPosition(it) }?.id?.let { itemId ->
                        page.data.find { it.id == itemId }
                            ?: throw IllegalStateException("Fetched page with $key, it does not contain $itemId")
                    }
                }
            }

            val endOfPaginationReached = page.data.isEmpty()
            if (!endOfPaginationReached) {
                synchronized(pageCache) {
                    if (loadType == LoadType.REFRESH) {
                        pageCache.clear()
                    }

                    pageCache.upsert(page)
                    Log.d(
                        TAG,
                        "  Page $loadType complete, now got ${pageCache.size} pages"
                    )
                    pageCache.debug()
                }
                Log.d(TAG, "  Invalidating paging source")
                factory.invalidate()
            }

            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    @Throws(IOException::class, HttpException::class)
    private suspend fun fetchStatusPageByKind(loadType: LoadType, key: String?, loadSize: Int): Response<List<Status>> {
        val (maxId, minId) = when (loadType) {
            // When refreshing fetch a page of statuses that are immediately *newer* than the key
            // This is so that the user's reading position is not lost.
            LoadType.REFRESH -> Pair(null, key)
            // When appending fetch a page of statuses that are immediately *older* than the key
            LoadType.APPEND -> Pair(key, null)
            // When prepending fetch a page of statuses that are immediately *newer* than the key
            LoadType.PREPEND -> Pair(null, key)
        }

        return api.publicTimeline(local = false, maxId = maxId, minId = minId, limit = loadSize)
    }

    companion object {
        private const val TAG = "TimelineRemoteMediator"
    }
}
