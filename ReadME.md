# SocialNet

Android-приложение — социальная сеть для обмена текстовыми и графическими публикациями.
Курсовой проект, КубГУ.

<p align="left">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Android" src="https://img.shields.io/badge/Android-minSdk%2024-3DDC84?logo=android&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Firebase" src="https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black">
  <img alt="Room" src="https://img.shields.io/badge/Room-Offline--First-6DB33F">
  <img alt="Cloudinary" src="https://img.shields.io/badge/Cloudinary-Media-3448C5">
</p>

---

## 📖 О проекте

**SocialNet** — мобильное приложение для обмена постами (текст + изображения) с системой подписок,
эмодзи-реакций и двумя лентами: **общей** (все посты) и **подписками** (только авторы, на которых
подписан пользователь).

Приложение построено по принципу **Offline-First**: чтение всегда идёт из локального кэша (Room),
а сеть подтягивает свежие данные в фоне. Это позволяет пользоваться приложением без интернета
(просматривать ранее загруженные посты, профиль, подписки).

### Возможности

- 🔐 **Аутентификация** — регистрация по email/паролю с подтверждением почты, восстановление пароля
- 📰 **Две ленты** — общая и подписки с пагинацией и realtime-обновлением
- ✍️ **Публикация постов** — текст (до 5000 символов) + до 10 изображений (JPG/PNG/GIF, до 10 МБ)
- ✏️ **Редактирование и удаление** — своих постов через контекстное меню
- 👤 **Профили** — свой и чужие, с возможностью подписки и отписки
- ❤️ **Реакции** — 5 типов эмодзи (👍 ❤️ 🔥 😮 😢), счётчики и realtime-синхронизация
- 👥 **Подписки** — лента, отфильтрованная по авторам
- 🖼️ **Полноэкранный просмотр** изображений с pinch-to-zoom и свайпами
- 🌙 **Гостевой режим** — просмотр общей ленты без регистрации
- 📴 **Офлайн-доступ** — чтение из Room без сети, кэширование изображений через Coil
- 📧 **Верификация email** — доступ к записи только после подтверждения почты

---

## 🛠 Технологический стек

### Клиент (Android)

| Слой | Технология |
|---|---|
| Язык | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Архитектура | MVVM + Clean Architecture |
| DI | Hilt |
| Локальная БД | Room |
| Асинхронность | Kotlin Coroutines + Flow |
| Сеть | Retrofit (Cloudinary API), Firebase SDK |
| Изображения | Coil |
| Навигация | Navigation Compose |

### Backend (BaaS)

| Сервис | Назначение |
|---|---|
| **Firebase Auth** | Регистрация, вход, верификация email |
| **Cloud Firestore** | Хранение данных (профили, посты, подписки, реакции) |
| **Cloudinary** | Хранение и раздача изображений (посты, аватары) |
| **GitHub Actions** | Фоновые задачи очистки (см. ниже) |

---

## 🏛 Архитектура

Проект построен по принципам **Clean Architecture** с разделением на 4 слоя:

```
┌──────────────────────────────────────────────┐
│  presentation  (Compose, ViewModel, Route)   │
│  • экраны, ViewModel, UiState                │
└─────────────────────┬────────────────────────┘
                      │ зависит от ↓
┌─────────────────────▼────────────────────────┐
│  domain  (UseCases, контракты репозиториев)  │
│  • бизнес-логика                             │
│  • валидаторы, use case'ы                    │
└─────────────────────┬────────────────────────┘
                      │ зависит от ↓
┌─────────────────────▼────────────────────────┐
│  data  (реализации, DTO, mappers, Room)      │
│  • Firebase, Cloudinary, Room                │
└─────────────────────┬────────────────────────┘
                      │ зависит от ↓
┌─────────────────────▼────────────────────────┐
│  core  (модели, AppError, AppResult, utils)  │
│  • не зависит ни от чего                     │
└──────────────────────────────────────────────┘
```

### Offline-First

```
         ┌──────────────┐
         │   Compose    │
         └──────▲───────┘
                │ Flow<List<Post>>
         ┌──────┴───────┐
         │     Room     │  ← единственный источник истины
         └──────▲───────┘
                │ upsert
   ┌────────────┴──────────┐
   │   OfflineFirstRepo    │
   └────────────┬──────────┘
                │
      ┌─────────┴─────────┐
      │                   │
┌─────▼─────┐    ┌────────▼────────┐
│ Firestore │    │ Firestore       │
│  one-shot │    │  snapshot       │
│ (sync)    │    │  (realtime)     │
└───────────┘    └─────────────────┘
```

UI **всегда** читает из Room. Сеть (Firestore) обновляет Room — как по запросу (pull-to-refresh, пагинация),
так и через `addSnapshotListener` (realtime). Когда Room эмитит новое значение, Compose перерисовывается.

---

## 📁 Структура проекта

```
app/src/main/java/com/example/socialnet/
├── core/
│   ├── common/           # AppError, AppResult, ValidationRules
│   ├── model/            # Post, User, Reaction, FeedType, ImageRef
│   ├── ui/theme/         # Цвета, типографика
│   └── utils/            # AppDispatchers, CursorCodec, IdGenerator
│
├── domain/
│   ├── model/            # (используются core/model)
│   ├── provider/         # ImageMetadataProvider (порт для data)
│   ├── repository/
│   │   ├── common/       # OfflineFirst*Repository (контракты)
│   │   └── remote/       # Firebase*, Cloudinary* (контракты)
│   ├── usecase/          # Auth, Post, Feed, Profile, Reaction, Subscription
│   └── validation/       # AuthValidator, PostValidator, ImageValidator
│
├── data/
│   ├── di/               # Hilt-модули (RepositoryModule, DatabaseModule, ...)
│   ├── dto/              # Request/Response DTO для Firestore/Cloudinary
│   ├── local/
│   │   ├── dao/          # PostDao, UserDao, SubscriptionDao
│   │   ├── entity/       # PostEntity, UserEntity, SubscriptionEntity
│   │   ├── converter/    # TypeConverters (List, Map → JSON)
│   │   └── SocialNetDatabase.kt
│   ├── mapper/           # DTO ↔ Domain ↔ Entity
│   ├── provider/         # AndroidImageMetadataProvider
│   ├── remote/
│   │   ├── cloudinary/   # CloudinaryApi (Retrofit)
│   │   └── dto/          # CloudinaryUploadResult, CloudinaryDeleteResponse
│   └── repository/
│       ├── common/       # OfflineFirst*RepositoryImpl
│       └── remote/       # Firebase*Repository, CloudinaryMediaRepository
│
└── presentation/
    ├── MainActivity.kt
    ├── SocialNetApplication.kt   # @HiltAndroidApp, MediaManager.init
    ├── main/                     # AppViewModel, SocialNetApp, MainNavHost
    ├── auth/                     # Login, Register, Reset, EmailVerification
    ├── feed/                     # FeedScreen, FeedViewModel, PostCard
    ├── post/                     # SavePostScreen (create + edit)
    ├── profile/                  # OwnProfile, OtherProfile
    └── common/                   # UiState, ErrorMapper, UiStrings, UiEvent
```

---

## 🚀 Быстрый старт

### Требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK 34+
- Аккаунты: Firebase (бесплатный Spark), Cloudinary (бесплатный Free)

### 1. Клонирование

```bash
git clone https://github.com/your-username/socialnet.git
cd socialnet
```

### 2. Firebase

1. Создайте проект на [console.firebase.google.com](https://console.firebase.google.com/).
2. **Authentication** → Sign-in method → включите **Email/Password**.
3. **Firestore Database** → Create database → **Production mode** → регион (например, `europe-west3`).
4. **Project settings** → Android app → зарегистрируйте приложение с package
   `com.example.socialnet` → скачайте `google-services.json` → положите в `app/`.
5. **Firestore Rules** → скопируйте содержимое из [`firestore.rules`](firestore.rules) (см. ниже) → **Publish**.
6. **Firestore Indexes** → создайте составной индекс для `posts`:
    - `authorId` ASC, `createdAt` DESC (Query scope: Collection)
    - `createdAt` DESC (создаётся автоматически, проверить не помешает)
7. **Authentication → Settings → User actions** — включите **Email enumeration protection**
   (или отключите на время тестирования, если нужен точный ответ «email уже существует»).

### 3. Cloudinary

1. Создайте аккаунт на [cloudinary.com](https://cloudinary.com/).
2. **Dashboard** → скопируйте **Cloud name**.
3. **Settings → Upload → Upload presets** → **Add upload preset**:
    - Signing mode: **Unsigned**
    - Allowed formats: `jpg,png,gif`
    - Max file size: `10000000` (10 МБ)
    - Folder: `socialnet` (опционально)
    - **Return delete token** — **включить** (нужно для компенсации при сбое)
    - Сохраните имя preset (например, `socialnet_unsigned`)
4. **Settings → API Keys** → скопируйте **API key** и **API secret**
   (понадобятся для GitHub Actions, **не для клиента**).

### 4. `local.properties`

В корне проекта создайте/дополните `local.properties`:

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_UPLOAD_PRESET=socialnet_unsigned
```

`local.properties` **не коммитится** в Git (см. `.gitignore`).

### 5. Сборка и запуск

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Или в Android Studio: **Run ▶**.

---

## 🔥 Firestore Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function isVerified() {
      return request.auth != null && request.auth.token.email_verified == true;
    }

    // Посты
    match /posts/{postId} {
      allow read: if true;
      allow create: if isVerified() && request.resource.data.authorId == request.auth.uid;
      allow update: if 
        (isVerified() && resource.data.authorId == request.auth.uid) ||
        (isSignedIn() && !request.resource.data.diff(resource.data).affectedKeys()
            .hasAny(['authorId', 'text', 'imageUrls', 'imagePublicIds', 'createdAt', 'updatedAt']));
      allow delete: if isVerified() && resource.data.authorId == request.auth.uid;

      match /reactions/{userId} {
        allow read: if true;
        allow write: if isVerified() && request.auth.uid == userId;
      }
    }

    // Пользователи
    match /users/{userId} {
      allow read: if true;
      allow create: if isSignedIn() && request.auth.uid == userId;
      allow update: if isVerified() && request.auth.uid == userId;

      match /following/{authorId} {
        allow read: if isSignedIn() && request.auth.uid == userId;
        allow write: if isVerified() && request.auth.uid == userId;
      }
      match /followers/{followerId} {
        allow read: if isSignedIn() && request.auth.uid == userId;
        allow write: if isVerified() && request.auth.uid == followerId;
      }
    }
  }
}
```

---

## 📊 Схема данных Firestore

```
users/{uid}
  ├── email: string
  ├── name: string
  ├── avatarUrl: string?          # Cloudinary secure_url
  ├── avatarPublicId: string?     # для удаления в Cloudinary
  ├── bio: string?
  ├── createdAt: Timestamp
  │
  ├── following/{authorId}        # подколлекция
  │     └── createdAt: Timestamp
  └── followers/{followerId}
        └── createdAt: Timestamp

posts/{postId}
  ├── authorId: string
  ├── text: string
  ├── imageUrls: [string]         # secure_url в Cloudinary
  ├── imagePublicIds: [string]
  ├── createdAt: Timestamp        # ключ сортировки
  ├── updatedAt: Timestamp?
  ├── reactionsTotal: number
  ├── reactionsByType: { LIKE: n, LOVE: n, ... }
  │
  └── reactions/{userId}          # docId = uid → одна реакция на пост
        ├── type: string          # "LIKE", "LOVE", ...
        └── createdAt: Timestamp
```

---

## ⚙️ GitHub Actions — фоновая очистка

В проекте настроены два workflow, которые запускаются по cron:

| Workflow | Назначение | Расписание |
|---|---|---|
| `cleanup-unverified-users.yml` | Удаляет Firebase Auth аккаунты, не подтвердившие email за 7 дней | Каждый день в 03:00 UTC |
| `cleanup-orphaned-images.yml` | Удаляет «сиротские» изображения из Cloudinary (нет ссылок в Firestore) | Каждый день в 04:00 UTC |

**Настройка секретов** (в GitHub → Settings → Secrets and variables → Actions):

- `FIREBASE_SERVICE_ACCOUNT` — полное содержимое JSON-ключа сервисного аккаунта Firebase
  (Project settings → Service accounts → Generate new private key)
- `CLOUDINARY_URL` — строка `cloudinary://<api_key>:<api_secret>@<cloud_name>`
  (из Cloudinary Dashboard → API Keys)

**Зачем это нужно**:

1. **Reconciliation Job** — клиент не может удалять файлы из Cloudinary без `api_secret`.
   Когда пользователь удаляет пост или меняет аватар — старый файл остаётся в облаке «сиротой».
   Скрипт сверяет `public_id` из Firestore с реальными файлами в Cloudinary и удаляет те,
   на которые нет ссылок.
2. **Cleanup unverified** — аккаунты, которые не подтвердили email за неделю, удаляются.
   Это освобождает «занятые» email-адреса.

---

## 🧪 Тестирование

### Smoke-тест

В проекте есть **SmokeTestScreen** — экран для быстрой проверки всей цепочки:

- Регистрация → создание поста → лента → подписка → реакция → выход

Это упрощает проверку `data`-слоя без прохождения полного UI-флоу.

### Ручные сценарии

Проверьте:

| Сценарий | Ожидание |
|---|---|
| Гость смотрит ленту | Видит посты, таб «Подписки» серый, кнопки реакций disabled |
| Регистрация | Письмо на email → переход по ссылке → «Я подтвердил» → лента |
| Создание поста с 5 картинками | Загрузка в Cloudinary → пост появляется в ленте |
| Pull-to-refresh | Обновление из Firestore → Room → UI |
| Офлайн | Открытие ленты → видны ранее загруженные посты |
| Другой пользователь ставит реакцию | Через realtime — счётчик обновляется на всех устройствах |
| Удаление поста | Мгновенно в UI, файлы в Cloudinary — через сутки (Reconciliation Job) |

---

## 🚧 Ограничения MVP

Честно зафиксированы в пояснительной записке:

- **Пагинация ленты подписок при >30 подписок** — `whereIn` имеет лимит 30.
  Для MVP используется чанкование, realtime — только для первой чанки.
- **Удалённые на сервере посты** удаляются из Room не мгновенно, а при pull-to-refresh
  или после `trimToMostRecent`.
- **Realtime для `myReaction`** — своя реакция синхронизируется, но не между устройствами.
  На другом устройстве обновится после pull-to-refresh.
- **`enrichPosts` — N+1** при каждом snapshot. Для 20 постов это ~40 чтений на одно событие.
  В production — вынести авторов и реакции в отдельный batch-запрос с кэшем.
- **`Firebase Storage` не используется** — заменён на Cloudinary из-за требований тарифа Blaze.

---

## 🗺 Roadmap

- [ ] Комментарии к постам
- [ ] Личные сообщения
- [ ] Push-уведомления (FCM)
- [ ] Полнотекстовый поиск (Algolia / Typesense)
- [ ] Story / 24-часовые публикации
- [ ] Полная поддержка пагинации подписок через fan-out on write
- [ ] Оптимизация `enrichPosts` (кэш + `getAll`)

---

## 📄 Лицензия

Проект выполнен в учебных целях. Использование сторонними лицами — по договорённости с автором.

---

## 👤 Автор

**Холкин Фёдор Эдуардович**  
Студент МОАИС, ФКТиПМ, Кубанский государственный университет  

2026