package com.teamup.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.teamup.app.data.repository.TeamRepository
import java.util.Calendar

@Composable
fun MainScreen(navController: NavController) {
    var teamName by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    val features = getFeatures()

    LaunchedEffect(Unit) {
        val team = TeamRepository.getUserTeam()
        teamName = team?.teamName ?: "Aucun groupe"

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val rawName = when {
                !user.displayName.isNullOrEmpty() -> user.displayName!!
                !user.email.isNullOrEmpty() -> user.email!!.split("@").firstOrNull() ?: "Utilisateur"
                else -> "Utilisateur"
            }
            userName = rawName.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
        } ?: run {
            userName = "Utilisateur"
        }
    }

    val bgColors = askedGradientColors()
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = bgColors,
            startY = 0f,
            endY = 1500f
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {

            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 24.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // SECTION 1 : Header avec Nom du groupe centré (AGRANDI)
                item {
                    HomeHeaderModern(userName = userName, teamName = teamName)
                }

                // SECTION 2 : Actions "À la une"
                item {
                    FeaturedActionsSection(navController = navController)
                }

                // SECTION 3 : Titre de la liste
                item {
                    Text(
                        text = "Fonctionnalités",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // SECTION 4 : Liste verticale des fonctionnalités
                items(features) { feature ->
                    FeatureListItem(
                        feature = feature,
                        onClick = { navController.navigate(feature.route) }
                    )
                }

                // SECTION 5 : Bouton Déconnexion
                item {
                    LogoutListItem(navController = navController)
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

// --- COMPOSANTS UI ---

@Composable
fun HomeHeaderModern(userName: String, teamName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        // --- 1. NOM DU GROUPE CENTRÉ ET AGRANDI ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), // Un peu plus opaque pour la lisibilité
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                // MODIF: Hauteur augmentée à 48dp pour loger le texte plus grand
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // MODIF: Padding horizontal augmenté à 24dp
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        // MODIF: Taille icône augmentée à 24dp
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = teamName,
                        // MODIF: Style passé à titleMedium (plus grand) et Bold
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. SALUTATIONS ET AVATAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = if (hour < 18) "Bonjour" else "Bonsoir"
                Text(
                    text = "$greeting, $userName",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Prêt à collaborer ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FeaturedActionsSection(navController: NavController) {
    val featuredColor = Color(0xFFFFD54F)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Grande carte principale
        Card(
            onClick = { navController.navigate("meetingPlanning") },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = featuredColor),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Planifier une\nRéunion",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        lineHeight = 28.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.BottomEnd).size(80.dp).offset(x = 10.dp, y = 10.dp)
                )

                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Commencer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Cartes secondaires
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryActionCard(
                title = "Agenda",
                icon = Icons.Default.CalendarMonth,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("agenda") }
            )
            SecondaryActionCard(
                title = "Tâches",
                icon = Icons.Default.TaskAlt,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("tasks") }
            )
        }
    }
}

@Composable
fun SecondaryActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FeatureListItem(
    feature: FeatureItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(feature.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = feature.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ouvrir",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LogoutListItem(navController: NavController) {
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        onClick = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(errorColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Déconnexion",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = errorColor
                    )
                    Text(
                        text = "Se déconnecter du compte",
                        style = MaterialTheme.typography.bodySmall,
                        color = errorColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// --- DATA & THEME ---

data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color,
    val route: String
)

@Composable
private fun getFeatures(): List<FeatureItem> {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    return remember {
        listOf(
            FeatureItem(Icons.Default.Groups, "Mon Groupe", "Gérer les membres et infos", primary, "groupInfo"),
            FeatureItem(Icons.Default.ChatBubbleOutline, "Discussions", "Chats d'équipe et privés", secondary, "chatList"),
            FeatureItem(Icons.Default.SnippetFolder, "Documents", "Fichiers partagés", tertiary, "documents")
        )
    }
}

@Composable
fun askedGradientColors(): List<Color> {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    return if (isDark) {
        listOf(Color(0xFF2D1A46), Color(0xFF1A1A2E))
    } else {
        listOf(
            Color(0xFFF3E5F5),
            Color(0xFFFFEBEE),
            Color(0xFFE3F2FD),
            MaterialTheme.colorScheme.background
        )
    }
}