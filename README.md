# StepWalker — расширенная версия

Добавлено:

- foreground service + постоянное уведомление;
- история прогулок через Room;
- Health Connect reader для шагов;
- DataStore для настроек;
- GPX exporter;
- маршрут до 1000 GPS-точек;
- проверка наличия шагового датчика;
- исправленный baseline;
- GPS-дистанция с fallback на оценку по шагам.

## Сборка

### Вариант 1 — Android Studio

Открой папку `StepWalker` в Android Studio, выполни Gradle Sync и:

`Build > Build APK(s)`

### Вариант 2 — GitHub Actions (без установки Android Studio)

В репозитории уже лежит `.github/workflows/build-apk.yml`. Он автоматически собирает debug‑APK на каждый `push` в любую ветку.

1. Запушь проект в свой репозиторий на GitHub (см. команды ниже).
2. Открой вкладку **Actions** в репозитории — запустится job **Build APK**.
3. Когда job позеленеет, зайди в этот run → раздел **Artifacts** → скачай `StepWalker-debug-apk.zip`. Внутри — обычный `.apk`, который можно установить на телефон.

```bash
cd StepWalker
git init
git add .
git commit -m "StepWalker"
git branch -M main
git remote add origin <ССЫЛКА_НА_ТВОЙ_РЕПОЗИТОРИЙ>
git push -u origin main
```

Если репозиторий уже подключён и код уже запушен — просто добавь файл `.github/workflows/build-apk.yml` из этого архива в свой репозиторий (commit + push), и сборка запустится сама.

## Важное

Foreground service добавлен для продолжения трекинга при уходе Activity в фон. Для production нужно дополнительно протестировать поведение на конкретных устройствах/OEM и корректно синхронизировать состояние Activity ↔ Service.

Health Connect использует `aggregate()` для StepsRecord, что Android рекомендует для агрегированных значений и снижения риска двойного счёта:
https://developer.android.com/health-and-fitness/health-connect/read-data

С июня 2026 встроенные шаги Health Connect получают device-specific Synthetic Package Name; приложение не должно хардкодить DataOrigin:
https://developer.android.com/health-and-fitness/health-connect/read-data

Foreground services:
https://developer.android.com/develop/background-work/services/fgs


## Карта маршрута и экспорт GPX

В проект добавлен полноценный экран карты на Google Maps Compose:
- маршрут рисуется `Polyline`;
- камера автоматически подстраивается под весь маршрут;
- отмечаются старт и последняя GPS-точка;
- есть кнопка «Экспорт GPX»;
- экспорт использует системный `CreateDocument`, поэтому пользователь сам выбирает папку и имя файла.

Для Google Maps нужен API key. Создайте файл `local.properties` в корне проекта (рядом с `settings.gradle.kts`) и добавьте:

`MAPS_API_KEY=ВАШ_GOOGLE_MAPS_API_KEY`

Ключ не должен попадать в Git.

Официальная документация:
- Google Maps Compose: https://developers.google.com/maps/documentation/android-sdk/maps-compose
- Android CreateDocument: https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument


## Максимальная точность GPS и шагов

В обновлении точность трекинга усилена на уровне приложения:

- `FusedLocationProviderClient` + `PRIORITY_HIGH_ACCURACY`;
- целевой интервал GPS 500 мс, минимум 250 мс;
- отключён location batching (`setMaxUpdateDelayMillis(0)`);
- `setMinUpdateDistanceMeters(0.5f)`;
- `setWaitForAccurateLocation(true)`;
- отбрасываются GPS-точки с `accuracy > 15 м`;
- отбрасываются физически невозможные скачки со скоростью выше 10 м/с;
- mock-location отбрасывается на Android 12+;
- маршрут хранит до 5000 принятых GPS-точек;
- foreground service владеет GPS и шагомером, поэтому блокировка экрана не останавливает трекинг;
- `TYPE_STEP_COUNTER` используется как основной аппаратный источник;
- baseline шагомера сохраняется, чтобы перезапуск сервиса не обнулял текущую сессию;
- если `TYPE_STEP_COUNTER` отсутствует, используется `TYPE_STEP_DETECTOR`;
- если GPS временно недоступен, расстояние временно оценивается по шагам и длине шага 0.75 м, а GPS-координаты не подделываются.

Важно: 500 мс — агрессивная частота запроса, но это не означает, что GNSS-чип физически выдаёт новую точную координату каждые 500 мс. Реальная частота и точность зависят от телефона, GNSS, условий приёма, энергосбережения и разрешений.

Для максимальной точности пользователь должен предоставить приложению `Precise location` (точное местоположение), а не только приблизительное. На Android 14+ foreground service также должен иметь соответствующий тип и разрешения; StepWalker использует типы `location|health`.

## Точность: патч от кента применён

Применены изменения из отдельного патча: старт только при `ACCESS_FINE_LOCATION`, запрос GPS с интервалом 500 мс / минимумом 250 мс, `setMinUpdateDistanceMeters(0.0f)`, `primeLastLocation()`, фильтрация точек `>20 м` и скачков `>12 м/с`, а также диагностика точности скорости/спутников.
