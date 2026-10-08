package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HeyPink

enum class HeyTab {
    FEED,
    EXPLORE,
    CREATE,
    MESSAGES,
    PROFILE
}

@Composable
fun HeyBottomBar(
    selectedTab: HeyTab,
    onTabSelected: (HeyTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == HeyTab.FEED,
            onClick = { onTabSelected(HeyTab.FEED) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HeyTab.FEED) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Feed",
                    modifier = Modifier.size(26.dp)
                )
            },
            modifier = Modifier.testTag("tab_feed"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HeyPink,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )

        NavigationBarItem(
            selected = selectedTab == HeyTab.EXPLORE,
            onClick = { onTabSelected(HeyTab.EXPLORE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HeyTab.EXPLORE) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Explore",
                    modifier = Modifier.size(26.dp)
                )
            },
            modifier = Modifier.testTag("tab_explore"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HeyPink,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )

        NavigationBarItem(
            selected = selectedTab == HeyTab.CREATE,
            onClick = { onTabSelected(HeyTab.CREATE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HeyTab.CREATE) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                    contentDescription = "New Post",
                    modifier = Modifier.size(28.dp)
                )
            },
            modifier = Modifier.testTag("tab_create"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HeyPink,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )

        NavigationBarItem(
            selected = selectedTab == HeyTab.MESSAGES,
            onClick = { onTabSelected(HeyTab.MESSAGES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HeyTab.MESSAGES) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Messages",
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.testTag("tab_messages"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HeyPink,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )

        NavigationBarItem(
            selected = selectedTab == HeyTab.PROFILE,
            onClick = { onTabSelected(HeyTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HeyTab.PROFILE) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                    contentDescription = "Profile",
                    modifier = Modifier.size(26.dp)
                )
            },
            modifier = Modifier.testTag("tab_profile"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HeyPink,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
