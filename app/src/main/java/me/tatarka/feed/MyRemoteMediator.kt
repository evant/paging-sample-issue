package me.tatarka.feed

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

class Item(val id: Long, val text: String) {
    override fun toString(): String {
        return "Item{id=${id}}"
    }
}

class LocalItems {
    private val items = mutableListOf<Item>()
    private var dataSource: SimplePagingSource<Item>? = null

    fun dataSource(): SimplePagingSource<Item> {
        dataSource = SimplePagingSource(items = items)
        return dataSource!!
    }

    fun insert(items: List<Item>, replace: Boolean = false) {
        if (replace) {
            this.items.clear()
        }
        this.items.addAll(items)
        dataSource?.invalidate()
    }
}

class RemoteItems {
    private val remoteItems = List(100) { index -> Item(index + 1L, "Item $index") }

    fun requestItems(loadKey: Long?, count: Int): List<Item> {
        Log.d("RemoteMediator", "request loadKey:${loadKey}, count:${count}")
        val startId = loadKey ?: 0
        val startIndex = remoteItems.indexOfLast { it.id == startId }
        return remoteItems.subList(
            startIndex + 1,
            (startIndex + 1 + count).coerceAtMost(remoteItems.size)
        ).toList()
    }
}

@OptIn(ExperimentalPagingApi::class)
class MyRemoteMediator(
    private val localItems: LocalItems,
    private val remoteItems: RemoteItems,
) : RemoteMediator<Int, Item>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Item>): MediatorResult {
        Log.d(
            "RemoteMediator",
            "loadType:${loadType}, anchorPosition:${state.anchorPosition}, firstItem:${state.firstItemOrNull()}, lastItem:${state.lastItemOrNull()}"
        )
        // The network load method takes an optional String
        // parameter. For every page after the first, pass the String
        // token returned from the previous page to let it continue
        // from where it left off. For REFRESH, pass null to load the
        // first page.
        val loadKey = when (loadType) {
            LoadType.REFRESH -> null
            // In this example, you never need to prepend, since REFRESH
            // will always load the first page in the list. Immediately
            // return, reporting end of pagination.
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            // Get the last User object id for the next RemoteKey.
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                // You must explicitly check if the last item is null when
                // appending, since passing null to networkService is only
                // valid for initial load. If lastItem is null it means no
                // items were loaded after the initial REFRESH and there are
                // no more items to load.
                if (lastItem == null) {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                lastItem.id
            }
        }
        val pageSize =
            if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize
        // Suspending network load via Retrofit. This doesn't need to
        // be wrapped in a withContext(Dispatcher.IO) { ... } block
        // since Retrofit's Coroutine CallAdapter dispatches on a
        // worker thread.
        val items = remoteItems.requestItems(loadKey, pageSize)
        // Insert new users into database, which invalidates the
        // current PagingData, allowing Paging to present the updates
        // in the DB.
        localItems.insert(items, replace = loadType == LoadType.REFRESH)
        // End of pagination has been reached if no users are returned from the
        // service
        return MediatorResult.Success(endOfPaginationReached = items.isEmpty())
    }

    override suspend fun initialize(): InitializeAction {
        // Need to refresh cached data from network; returning
        // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
        // APPEND and PREPEND from running until REFRESH succeeds.
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }
}