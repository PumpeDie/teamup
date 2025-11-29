# TeamUp

Application Android de gestion de travail en groupe pour faciliter la collaboration et l'organisation d'équipe.

## Description

TeamUp est une application mobile permettant de coordonner efficacement le travail en groupe grâce à la synchronisation des emplois du temps et la gestion collaborative des tâches.

## Fonctionnalités prévues

- [ ] **Emploi du temps personnel** - Saisie et gestion automatique avec jours fériés et vacances
- [x] **Groupes de travail** - Création et gestion de groupes avec centralisation des emplois du temps
- [ ] **Rendez-vous** - Planification de rendez-vous avec mise à jour automatique des emplois du temps
- [ ] **Sessions de travail** - Configuration de sessions avec paramètres personnalisables (durée, fréquence, etc.)
- [ ] **Liste de tâches** - Gestion des tâches par groupe avec assignation individuelle
- [ ] **Deadlines** - Définition d'échéances affichées sur l'emploi du temps
- [ ] **Rapports de progression** - Suivi de l'avancement des travaux
- [ ] **Partage de documents** - Système de partage de fichiers par groupe
- [x] **Chat intégré** - Messagerie instantanée par groupe
- [ ] **Notifications** - Rappels pour rendez-vous et tâches

## Technologies

- **Langage** : Kotlin
- **UI** : Android Jetpack Compose
- **Backend** : Firebase (Realtime Database + Authentication)

### Structure de la base de données

```
/users/{uid}
  ├─ username: String
  └─ email: String

/teams/{teamId}
  ├─ teamName: String
  ├─ creatorId: String
  ├─ adminIds: [String]
  ├─ memberIds: [String]
  ├─ chatRooms/{chatRoomId}
  │   ├─ chatName: String
  │   ├─ lastMessage: String
  │   └─ lastMessageTime: Long
  ├─ messages/{chatRoomId}/{messageId}
  ├─ tasks/{taskId}
  └─ agenda/{eventId}
```

## Installation

```bash
git clone https://github.com/PumpeDie/teamup.git
cd teamup
```

Ouvrir le projet dans Android Studio et synchroniser Gradle.

## Limites et améliorations possibles

### Sécurité
- Les règles Firebase Database sont basiques
- Validation des permissions seulement côté client
- IDs de groupe permanents et non expirables

### Invitations
- Système actuel : partage d'ID manuel
- Améliorations possibles : codes temporaires (6-8 caractères), QR codes scannables

### Statistiques
- Nombre de tâches/messages par membre
- Graphiques de progression du groupe
- Exports de données

## Équipe

Projet collaboratif - UQAC