package me.tatarka.feed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey

@OptIn(ExperimentalPagingApi::class)
class MainViewModel : ViewModel() {
    private val localItems = LocalItems()
    private val remoteItems = RemoteItems()

    val flow = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = true),
        remoteMediator = MyRemoteMediator(localItems, remoteItems)
    ) {
        localItems.dataSource()
    }.flow.cachedIn(viewModelScope)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val viewModel by viewModels<MainViewModel>()
                val items = viewModel.flow.collectAsLazyPagingItems()
                val listState = rememberLazyListState()
                Screen(
                    items = items,
                    listState = listState,
                    onRefresh = {
                        items.refresh()
                    }
                )
            }
        }
    }
}

@Composable
fun Screen(
    items: LazyPagingItems<Item>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            items(items.itemCount, key = items.itemKey { it.id }) {
                val item = items[it]
                if (item != null) {
                    ListItem(headlineContent = { Text(item.text) })
                } else {
                    ListItem(headlineContent = { Text("Loading...") })
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Button(onClick = onRefresh, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text("Refresh")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        Row {
            ButtonWithPressColor("One")
            ButtonWithPressColor("Two")
        }
    }
}

@Composable
fun ButtonWithPressColor(text: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Button(
        interactionSource = interactionSource,
        onClick = {}, colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) Color.Red else Color.Blue
        )
    ) { Text(text) }
}