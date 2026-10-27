# AgroCampo · AgroSierra (PARI)

Aplicación Android (Kotlin Multiplatform + Compose) para que los técnicos de campo de la cooperativa
AgroSierra consulten el padrón de lotes y registren labores (riego, fumigación, cosecha) **aun sin señal**.
Lo registrado sin red queda pendiente y se sincroniza con **AgroAPI** (`agrosierra-api-pari`) al recuperar la conexión.

- **Stack:** Kotlin 2.1 · Compose Multiplatform 1.7 · Ktor Client 3 · Koin 4 · SQLDelight 2 · kotlinx.serialization
- **Plataforma evaluada:** Android (minSdk 26, targetSdk 35)

## Ejecución

1. Levantar el servicio (en el repositorio `agrosierra-api-pari`):
   ```bash
   docker compose up --build
   ```
2. Abrir este proyecto en Android Studio y ejecutar el módulo **androidApp** en un emulador.
   - La dirección base está en `androidApp/build.gradle.kts` (`URL_BASE`): `http://10.0.2.2:8080/api/v1/`.
     `10.0.2.2` es la PC vista desde el emulador. En un dispositivo físico, cambiarla por la IP de la PC.
3. Ingresar con `tecnico1`, `jefe1` o `admin` (contraseña `Agro2026*`).

### Pruebas (commonTest)

```bash
./gradlew :shared:testDebugUnitTest
```

| Prueba | Qué garantiza |
|---|---|
| `ConversionDtoTest` | El DTO se convierte al dominio (con `@SerialName`), tolera claves desconocidas y los listados se leen del envoltorio de paginación. |
| `TraduccionErroresTest` | 400/401/403/404/409/500 y falta de red se traducen a un `ErrorApi` distinto; el 400 conserva el mensaje por campo. La cabecera `Authorization` se agrega en un solo lugar y un 401 cierra la sesión. |
| `RegistroSinConexionTest` | Sin red la labor queda guardada, marcada como pendiente y con id provisional negativo (-1, -2, …). |
| `SincronizacionTest` | La sincronización **sube antes de bajar**, reemplaza la fila provisional por la definitiva y no duplica ni pierde filas. Si no hay red al enviar, no descarga. |
| `CorreccionRechazadaTest` | Una corrección sin red rechazada por el servidor se descarta y se restaura la versión del servidor. |

Se usa `MockEngine` en lugar de la red y un `JdbcSqliteDriver` en memoria en lugar de la base del dispositivo.

## Estructura de paquetes

```
shared/src/commonMain/kotlin/pe/upeu/agrosierra/
├── domain/
│   ├── model/        Lote, Labor, TipoLabor, Sesion, Privilegios, ErrorApi, Resultado, ResumenSincronizacion
│   ├── repository/   SesionRepository, LoteRepository, LaborRepository, SincronizacionRepository, MonitorConexion
│   └── usecase/      IniciarSesion, RestaurarSesion, CerrarSesion, ObservarLotes, ObservarDetalleLote,
│                     GuardarLabor, EliminarLabor, CerrarCampania, SincronizarUseCase
├── data/
│   ├── remote/       ClienteHttp (HttpClient único + plugin SesionHttp), ApiConfig, MapeoErrores, AgroApi
│   │   └── dto/      DTO del contrato y funciones de conversión al dominio
│   ├── local/        AlmacenSeguro (interfaz), GestorSesion, BaseLocal (SQLDelight)
│   └── repository/   implementaciones offline-first
├── presentation/     sesion/, listado/, detalle/, perfil/, navigation/, theme/, comun/, sincronizacion/
└── di/               redModule, dataModule, domainModule, presentationModule
shared/src/commonMain/sqldelight/pe/upeu/agrosierra/AgroDb.sq

androidApp/src/main/kotlin/pe/upeu/agrosierra/
├── AlmacenSeguroAndroid.kt     EncryptedSharedPreferences + MasterKey del Android Keystore
├── BaseLocalAndroid.kt         AndroidSqliteDriver
├── MonitorConexionAndroid.kt   ConnectivityManager -> Flow<Boolean>
├── PlatformModule.android.kt   motor OkHttp, ApiConfig desde BuildConfig y dependencias de Android
├── AgroApplication.kt          startKoin(platformModule + modulosComunes)
└── MainActivity.kt
```

## Contrato del API consumido

| Verbo | Ruta | Privilegio | Uso en la app |
|---|---|---|---|
| POST | `auth/login` | Público | Inicio de sesión |
| GET | `usuarios/me` | Autenticado | Refrescar privilegios en Perfil |
| GET | `lotes?pagina&tamanio` | LOTE_LEER | Descarga del padrón al sincronizar |
| GET | `lotes/{id}/labores?pagina&tamanio` | LABOR_LEER | Descarga de labores al sincronizar |
| POST | `labores` | LABOR_CREAR | Registro (o envío de una pendiente) |
| PUT | `labores/{id}` | LABOR_EDITAR | Corrección |
| DELETE | `labores/{id}` | LABOR_ELIMINAR | Eliminación (204, no se convierte el cuerpo) |
| POST | `lotes/{id}/cerrar-campania` | CAMPANIA_CERRAR | Cierre de campaña (requiere conexión) |

## Roles y privilegios en la interfaz

La interfaz decide qué **ofrecer** con `Sesion.tiene(privilegio)`, nunca con el nombre del rol.
El servicio sigue verificando cada operación: si se fuerza, responde 403 y la app lo informa sin cerrar la sesión.

| Acción en la app | Privilegio | TECNICO | JEFE_CAMPO | ADMINISTRADOR |
|---|---|:-:|:-:|:-:|
| Ver padrón y detalle | LOTE_LEER / LABOR_LEER | ✔ | ✔ | ✔ |
| Botón «Registrar labor» | LABOR_CREAR | ✔ | ✔ | |
| Corregir labor | LABOR_EDITAR | ✔ (propias, 24 h: lo valida el servicio) | ✔ | |
| Eliminar labor | LABOR_ELIMINAR | | ✔ | |
| Botón «Cerrar campaña» | CAMPANIA_CERRAR | | ✔ | |

## Manejo de respuestas

| Respuesta | Conducta |
|---|---|
| 200 | La capa de datos convierte el cuerpo al dominio. |
| 201 | Se guarda la fila definitiva y se limpia la marca de pendiente. |
| 204 | No se convierte el cuerpo: solo se comprueba el código. |
| 400 | Cada mensaje de `validationErrors` se muestra bajo su campo. |
| 401 | El plugin `SesionHttp` borra el token y la app vuelve al inicio de sesión. |
| 403 | «Operación no autorizada» y la sesión se mantiene. |
| 404 | Se informa y se quita la fila local (el listado se refresca solo). |
| 409 | Se muestra el motivo devuelto por el servidor (RN-01, RN-03). |
| 500 | Mensaje breve con botón **Reintentar**. |
| Sin red / tiempo agotado | La labor queda pendiente y se envía al sincronizar. |

## Decisiones de arquitectura

1. **Offline-first:** todas las pantallas observan la base local (`Flow` de SQLDelight). La red nunca alimenta
   directamente la interfaz: solo actualiza la base al registrar o sincronizar.
2. **Id provisional negativo:** sin red no hay id del servidor, así que la fila toma `min(id) - 1` (siempre
   negativo, un rango que el servidor nunca emite). Al confirmarse, la fila provisional se sustituye por la
   definitiva **en la misma transacción**.
3. **Sincronización en orden:** `SincronizarUseCase` primero envía los pendientes (POST para los nuevos, PUT para
   las correcciones) y después descarga el padrón y las labores. Si se invirtiera, la descarga podría pisar
   cambios locales aún no enviados. Es manual (botón) y automática al volver la red o al iniciar sesión.
4. **Un único `HttpClient`** inyectado por Koin, con dirección base centralizada (`ApiConfig` desde `BuildConfig`),
   tiempos de espera y registro reducido en producción. La cabecera `Authorization` se añade en un solo plugin.
5. **Errores tipados:** `MapeoErrores.llamarApi` es el único lugar que conoce HTTP y traduce cada respuesta a
   `ErrorApi` (sellada); la interfaz tiene una conducta distinta para cada caso.
6. **Token seguro:** `EncryptedSharedPreferences` con clave maestra en el Android Keystore. Al reabrir, si el token
   sigue vigente (`expiraEn`) se entra directo; si venció, se vuelve al inicio de sesión sin mensajes técnicos.
7. **Capas:** `domain` no depende de nada (ni de serialización), `data` implementa los contratos del dominio y
   `presentation` usa `ViewModel` + `StateFlow<UiState>` con casos de uso inyectados por Koin.
