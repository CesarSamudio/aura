# Aura

**App de ahorros, gastos y deudas.** Android nativo, sin internet y sin un solo permiso.

Aura sirve para llevar las cuentas del día a día: cuánto ahorrado tenés, en qué se te va la
plata, y —lo que más pesa en la escuela— a quién se le tiene que cobrar.

![La app en los 7 temas](docs/montaje-app-temas.png)

## Descargar

Hay dos versiones de la misma app. La diferencia es **una sola lista de 24 nombres**:

| Versión | Para quién | Archivo |
|---|---|---|
| **Aura v1.15** | Cualquiera | [`apk/Aura-v1.15.apk`](apk/Aura-v1.15.apk) |
| **Aura 11G v1.15** | Los del 11G: trae la lista del salón ya escrita | [`apk/Aura-v1.15-11G.apk`](apk/Aura-v1.15-11G.apk) |

> **Instalá `Aura-v1.15.apk` si no sos del 11G.**
> La del 11G es EXACTAMENTE la misma app con la lista del salón escrita de antemano
> (para no tener que teclear los 24 nombres). Además instala con otro paquete, así que
> las dos conviven en el mismo teléfono sin pisarse.

Para instalar: copiá el `.apk` al teléfono y abrilo. Android va a pedir permiso para
instalar apps de fuentes desconocidas: es normal, es el aviso de siempre.

Si preferís bajarlo de GitHub, los mismos `.apk` están en la [release v1.15](https://github.com/CesarSamudio/aura/releases/tag/v1.15).

## Qué hace

- **Ahorros y gastos**: cuánto entró, cuánto salió, y el balance del día.
- **Deudas**: armás la deuda, decidís cuántos la pagan, y le vas marcando a cada
  quién cuánto puso. Con **monto especial** para cuando alguien paga otra cosa.
  Y si **no sabés cuántos van a pagar**, dejás el campo vacío y la cuota se calcula
  con la gente que vayas añadiendo.
- **Pruebas por persona**: adjuntás el screenshot de la transferencia (solo con YAPI)
  y queda guardado con la deuda.
- **Listas de gente**: creás tus propias listas (un curso, un equipo) y quedan
  guardadas para las próximas deudas: abrís, marcás lo que falta y listo.
- **7 temas**: negro y naranja, y además Osmo, Caldera, SafePal, Boost, Mode y Trunk
  (claros y oscuros). Se cambian en **Ajustes → Apariencia** y se aplican al instante.
- **Colores a tu gusto**: además de los temas, se puede cambiar **cada color por
  separado** (la paleta de la app y la de los textos) con un selector de color que
  te dice, en vivo, si ese color se lee o no.
- **Fondo con tu foto**: se elige una imagen del teléfono (la app la reescala para
  que se vea bien y no pese tanto) y se le ajusta la opacidad y el difuminado (de
  verdad, con kernel gaussiano: se ve como el de una lente, no a píxeles).
- **Fondo animado**: degradados que se mueven (aureola, aurora, barrido, cónico o
  malla), con su opacidad, velocidad e intensidad.
- **Animaciones a tu medida**: las gráficas, los números y la interfaz se pueden
  apagar, acelerar o frenar, y cada cosa tiene su estilo. Apretando y soltando
  una opción te explica qué hace.
- **Copia de seguridad**: genera un `.zip` con todo y lo guardás donde quieras
  (Drive, WhatsApp, correo). Sin nube y sin cuentas.

## Privacidad

- **Cero permisos.** Ni internet, ni almacenamiento, ni cámara, ni ubicación.
- Todo vive dentro de la app, en el teléfono. Nada sale de acá.
- El `.zip` de la copia lo compartís vos, cuando querés.

> Ojo: si desinstalás la app, Android borra su carpeta y se pierden los datos.
> Por eso está la copia de seguridad.

## Para compilar

Requisitos: JDK 21, Android SDK (compileSdk 35) y Gradle 8.11.

```bash
# Las dos versiones (la general y la del 11G)
./gradlew assembleGeneralRelease assembleG11Release

# Los tests
./gradlew testGeneralReleaseUnitTest testG11ReleaseUnitTest
```

Las dos versiones se hacen con **product flavors** (`general` y `g11`). Lo único que
cambia entre ellas está en:

```
app/src/general/java/com/cesar/ahorros/data/ListaSalon.kt   -> null
app/src/g11/java/com/cesar/ahorros/data/ListaSalon.kt       -> la lista del 11G
```

### La clave de firma

**No está en el repositorio** (a propósito: con esa clave cualquiera podría firmar una
versión de la app). Para compilar una versión firmada poné el `.jks` en
`app/keystore/ahorros.jks` y las contraseñas en `local.properties`:

```properties
aura.storePassword=...
aura.keyAlias=...
aura.keyPassword=...
```

Sin la clave, el build compila igual pero el APK sale **sin firmar** (no se puede instalar).

## Cómo está hecho

- **Kotlin + Jetpack Compose** (Material 3), sin librerías de terceros.
- **MVVM** con `StateFlow`; los datos se guardan en **JSON plano** dentro de la carpeta
  de la app (`movimientos.json`, `deudas.json`, `listas.json`, `ajustes.json`).
- **Sin base de datos**: para este volumen es instantáneo y no pesa nada en el APK.
- **228 tests** en verde (259 con las capturas), entre ellos uno que mide el
  **contraste de color de los 7 temas contra WCAG 2.2 AA** y otro que mide el
  contraste de los colores que personaliza el usuario.- Las capturas de pantalla de los tests se generan en el PC con **Robolectric**
  (gráficos nativos), sin emulador.

## Las capturas

| | |
|---|---|
| ![Ajustes: los temas](docs/ajustes-apariencia.png) | ![Ajustes: la paleta a tu gusto](docs/ajustes-colores.png) |
| **Ajustes → Apariencia**: los temas de fábrica | **Colores**: cada color por separado, con su hexadecimal |

## Licencia

MIT.
