# AI Expense Voice

Android-only Kotlin app for a voice-first AI expense tracker, built with Jetpack Compose, MVVM, Retrofit, OkHttp, Coroutines, Flow, DataStore, and Material 3.

## Included

- Splash, login, register, chat-first shell, dashboard, and profile/settings screens
- JWT persistence with DataStore
- Bearer auth injection through OkHttp
- Runtime-configurable backend base URL
- Voice input flow using Android speech recognition
- Conversational chat UI with mixed-response rendering
- Dashboard built from multiple agent prompts
- Custom Compose charts for category share and spending trend

## Backend assumptions

- `POST /api/v1/auth/register` accepts JSON with `email`, `password`
- `POST /api/v1/auth/login` accepts form fields `username` and `password`
- `POST /api/v1/expenses/agent` accepts JSON like `{ "query": "show my expenses" }`

The register response is treated as a success when the request returns HTTP 200.

## Base URL

Set `AI_EXPENSE_BASE_URL=https://ai-expense-tracker-xdw6.onrender.com/` in Gradle properties for a build-time default, or change the URL at runtime from the Profile screen.

## Run

1. Open [AIExpenseVoice](/D:/Android Projects/AIExpenseVoice) in Android Studio.
2. Let Gradle sync.
3. Set the backend URL if needed.
4. Run on an Android device or emulator with Google voice recognition available.
