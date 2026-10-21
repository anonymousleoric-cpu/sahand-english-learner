package com.sahandsoft.englishlearner.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sahandsoft.englishlearner.ui.screens.games.GamesHubScreen
import com.sahandsoft.englishlearner.ui.screens.games.GuessGameScreen
import com.sahandsoft.englishlearner.ui.screens.games.MatchGameScreen
import com.sahandsoft.englishlearner.ui.screens.games.SpeedGameScreen
import com.sahandsoft.englishlearner.ui.screens.home.HomeScreen
import com.sahandsoft.englishlearner.ui.screens.library.BookReaderScreen
import com.sahandsoft.englishlearner.ui.screens.library.DialogueScreen
import com.sahandsoft.englishlearner.ui.screens.library.LibraryScreen
import com.sahandsoft.englishlearner.ui.screens.library.LessonScreen
import com.sahandsoft.englishlearner.ui.screens.library.StoryReaderScreen
import com.sahandsoft.englishlearner.ui.screens.tools.AboutScreen
import com.sahandsoft.englishlearner.ui.screens.tools.DictionaryScreen
import com.sahandsoft.englishlearner.ui.screens.tools.ProfileScreen
import com.sahandsoft.englishlearner.ui.screens.tools.QuizScreen
import com.sahandsoft.englishlearner.ui.screens.tools.ReviewScreen
import com.sahandsoft.englishlearner.ui.screens.tools.SettingsScreen
import com.sahandsoft.englishlearner.ui.screens.tools.ToolsScreen
import com.sahandsoft.englishlearner.ui.screens.words.FlashcardsScreen
import com.sahandsoft.englishlearner.ui.screens.words.WordsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    TabItem(Routes.HOME, "خانه", Icons.Rounded.Home),
    TabItem(Routes.WORDS, "واژگان", Icons.Rounded.MenuBook),
    TabItem(Routes.LIBRARY, "کتابخانه", Icons.Rounded.LocalLibrary),
    TabItem(Routes.GAMES, "بازی‌ها", Icons.Rounded.SportsEsports),
    TabItem(Routes.TOOLS, "ابزارها", Icons.Rounded.Widgets)
)

@Composable
fun SahandRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in Routes.bottomTabs) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.WORDS) { WordsScreen(nav) }
            composable(
                route = Routes.FLASHCARDS,
                arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "all" })
            ) { FlashcardsScreen(nav) }
            composable(Routes.LIBRARY) { LibraryScreen(nav) }
            composable(Routes.GAMES) { GamesHubScreen(nav) }
            composable(Routes.TOOLS) { ToolsScreen(nav) }
            composable(
                route = Routes.STORY,
                arguments = listOf(navArgument("storyId") { type = NavType.StringType })
            ) { StoryReaderScreen(nav) }
            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { BookReaderScreen(nav) }
            composable(
                route = Routes.LESSON,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
            ) { LessonScreen(nav) }
            composable(
                route = Routes.DIALOGUE,
                arguments = listOf(navArgument("dialogueId") { type = NavType.StringType })
            ) { DialogueScreen(nav) }
            composable(Routes.QUIZ) { QuizScreen(nav) }
            composable(Routes.REVIEW) { ReviewScreen(nav) }
            composable(Routes.DICTIONARY) { DictionaryScreen(nav) }
            composable(Routes.PROFILE) { ProfileScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.ABOUT) { AboutScreen(nav) }
            composable(Routes.GAME_MATCH) { MatchGameScreen(nav) }
            composable(Routes.GAME_GUESS) { GuessGameScreen(nav) }
            composable(Routes.GAME_SPEED) { SpeedGameScreen(nav) }
        }
    }
}
