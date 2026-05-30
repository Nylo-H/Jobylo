# Guide Frontend — Filtres sur l'écran d'accueil

## Endpoint mis à jour

```
GET /api/jobs/available?categoryId=&location=&q=&minPrice=&maxPrice=&sort=
```

Tous les paramètres sont **optionnels** et **combinables**.

| Paramètre | Exemple | Rôle |
|-----------|---------|------|
| `categoryId` | `uuid` | Filtrer par catégorie (depuis l'arbre) |
| `location` | `Paris` | Recherche textuelle dans le lieu |
| `q` | `ménage` | Recherche dans le titre |
| `minPrice` | `10` | Prix minimum |
| `maxPrice` | `100` | Prix maximum |
| `sort` | `date_desc` | Tri |

---

## Modèle existant (inchangé)

```dart
@freezed
class JobResponse with _$JobResponse {
  factory JobResponse({
    required String id,
    required String title,
    String? description,
    String? location,
    double? price,
    required String creatorUsername,
    required String status,
    String? categoryId,
    String? categoryName,
    List<String>? images,
    DateTime? createdAt,
  }) = _JobResponse;

  factory JobResponse.fromJson(Map<String, dynamic> json) =>
      _$JobResponseFromJson(json);
}
```

---

## Écran d'accueil avec filtres

### Maquette

```
┌─────────────────────────────────────┐
│  🔍 Jobylo                🔔        │
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────────────────────┐  │
│  │ 🔍 Rechercher un service...  │  │  ← q (debounce 300ms)
│  └──────────────────────────────┘  │
│                                     │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ Paris  │ │ Ménage │ │  Tri ▼ │  │  ← chips cliquables
│  └────────┘ └────────┘ └────────┘  │
│                                     │
│  ── Catégories ─────────────────    │
│  [ Ménage ] [ Jardin ] [ Cours ]   │  ← GET /categories/tree
│  [ Dépannage ] [ Garde ] [ Autre ] │     tap → ?categoryId=
│                                     │
│  ── Annonces ────────────────────   │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ [IMG] Femme de ménage 3h     │  │
│  │       35 000 XAF             │  │
│  │       📍 Paris 11e           │  │  ← location du job
│  │       👤 Alice  ★★★★☆       │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │ [IMG] Jardinage              │  │
│  │       25 000 XAF             │  │
│  │       📍 Lyon 3e             │  │
│  │       👤 Paul  ★★★☆☆        │  │
│  └──────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

---

## Implémentation des filtres

### 1. Barre de recherche (q)

```dart
TextField(
  onChanged: (value) {
    debouncer.run(() {
      setState(() => query = value);
      fetchJobs();
    });
  },
  decoration: InputDecoration(
    hintText: 'Rechercher un service...',
    prefixIcon: Icon(Icons.search),
  ),
)
```

### 2. Filtre par lieu (location)

```dart
// Chip "Paris" cliquable → BottomSheet ou TextField
showModalBottomSheet(
  context: context,
  builder: (_) => Column(
    children: [
      TextField(
        decoration: InputDecoration(labelText: 'Ville ou quartier'),
        onSubmitted: (value) {
          setState(() => location = value);
          fetchJobs();
        },
      ),
      // Suggestions rapides
      ListTile(title: 'Paris', onTap: () => selectLocation('Paris')),
      ListTile(title: 'Lyon', onTap: () => selectLocation('Lyon')),
      ListTile(title: 'Marseille', onTap: () => selectLocation('Marseille')),
    ],
  ),
);
```

### 3. Filtre par catégorie (categoryId)

```dart
// Grille de catégories
GridView.builder(
  itemCount: categories.length,
  itemBuilder: (_, i) => CategoryChip(
    label: categories[i].name,
    selected: selectedCategoryId == categories[i].id,
    onTap: () {
      setState(() => selectedCategoryId = categories[i].id);
      fetchJobs();
    },
  ),
);
```

### 4. Filtre de prix (minPrice / maxPrice)

```dart
RangeSlider(
  values: RangeValues(minPrice, maxPrice),
  min: 0,
  max: 500000,
  onChanged: (values) {
    setState(() {
      minPrice = values.start;
      maxPrice = values.end;
    });
    fetchJobs();
  },
);
```

### 5. Tri (sort)

```dart
PopupMenuButton<String>(
  icon: Icon(Icons.sort),
  onSelected: (value) {
    setState(() => sort = value);
    fetchJobs();
  },
  itemBuilder: (_) => [
    PopupMenuItem(value: 'date_desc', child: Text('Plus récents')),
    PopupMenuItem(value: 'date_asc', child: Text('Plus anciens')),
    PopupMenuItem(value: 'price_asc', child: Text('Moins cher')),
    PopupMenuItem(value: 'price_desc', child: Text('Plus cher')),
  ],
);
```

### 6. Appel API final

```dart
Future<void> fetchJobs() async {
  final response = await dio.get('/jobs/available', queryParameters: {
    if (selectedCategoryId != null) 'categoryId': selectedCategoryId,
    if (location != null && location.isNotEmpty) 'location': location,
    if (query != null && query.isNotEmpty) 'q': query,
    if (minPrice != null) 'minPrice': minPrice,
    if (maxPrice != null) 'maxPrice': maxPrice,
    'sort': sort ?? 'date_desc',
  });
  setState(() => jobs = response.data.map((j) => JobResponse.fromJson(j)).toList());
}
```

**Important :** Utilise `removeWhere` sur les params null — Dio ignore les `null`. Avec le `if` dans le map literal, les clés vides ne sont pas envoyées.

---

## Gestion de l'état (Provider / Riverpod)

```dart
@freezed
class FilterState with _$FilterState {
  factory FilterState({
    String? categoryId,
    String? location,
    String? query,
    double? minPrice,
    double? maxPrice,
    @Default('date_desc') String sort,
    @Default([]) List<JobResponse> jobs,
    @Default(false) bool isLoading,
  }) = _FilterState;
}
```

```dart
class JobsNotifier extends StateNotifier<FilterState> {
  JobsNotifier(this.dio) : super(FilterState());

  Future<void> applyFilters() async {
    state = state.copyWith(isLoading: true);
    final response = await dio.get('/jobs/available', queryParameters: {
      if (state.categoryId != null) 'categoryId': state.categoryId,
      if (state.location != null) 'location': state.location,
      if (state.query != null) 'query': state.query,
      if (state.minPrice != null) 'minPrice': state.minPrice,
      if (state.maxPrice != null) 'maxPrice': state.maxPrice,
      'sort': state.sort,
    });
    state = state.copyWith(
      jobs: (response.data as List).map((j) => JobResponse.fromJson(j)).toList(),
      isLoading: false,
    );
  }
}
```

---

## Navigation avec les catégories

```
Accueil (liste avec filtres)
  │
  ├── Tap catégorie "Ménage"
  │     → GET /jobs/available?categoryId=uuid
  │     → Affiche "Ménage" en header + résultats filtrés
  │
  └── Tap annonce
        → GET /jobs/{id}
        → Écran détail (titre, prix, photos, lieu, créateur)
        → Bouton "Postuler" ou "Contacter"
```

---

## Résumé

| Filtre | Widget | Paramètre API | Debounce |
|--------|--------|---------------|----------|
| Recherche texte | TextField | `q` | 300ms |
| Lieu | TextField + suggestions | `location` | Non |
| Catégorie | Grille de chips | `categoryId` | Non |
| Prix | RangeSlider | `minPrice`, `maxPrice` | Non |
| Tri | PopupMenuButton | `sort` | Non |

Tous les filtres sont **indépendants** et **combinables**. Chaque changement déclenche un nouvel appel API.
