package me.tatarka.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import kotlin.time.Duration

class SimplePagingSource<T : Any>(private val items: List<T>) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val start = params.key ?: 0
        return LoadResult.Page(
            data = items.subList(
                start,
                (start + params.loadSize).coerceAtMost(items.size)
            ).toList(),
            prevKey = if (start == 0) null else (start - params.loadSize).coerceAtLeast(0),
            nextKey = (start + params.loadSize).let { if (it >= items.size) null else it }
        )
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int {
        return ((state.anchorPosition ?: 0) - state.config.initialLoadSize / 2)
            .coerceAtLeast(0)
    }
}