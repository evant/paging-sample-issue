# Shows paging issue with following official docs

https://developer.android.com/topic/libraries/architecture/paging/v3-network-db#implement-remotemediator

If you copy the RemoteMediator example it won't page past the first page.

This is because an append request gets enqueed after the initial refresh and sees no items and stops paging.

```
loadType:REFRESH, anchorPosition:null, firstItem:null, lastItem:null
request loadKey:null, count:90
loadType:PREPEND, anchorPosition:null, firstItem:null, lastItem:null
loadType:APPEND, anchorPosition:null, firstItem:null, lastItem:null
```

related issue: https://issuetracker.google.com/issues/381126154
