# APU YAWAR: El Juicio de los Dioses

**Proyecto independiente** creado para Minecraft Java 1.21.1 + Fabric + Java 21, editado desde Visual Studio Code.

## Estructura

- `apuyawar-systems`: motor técnico; ruleta HUD con código Java y texturas maestras transparentes.
- `apuyawar-world`: mod de contenido; comienza con un item de prueba; dimensión Cuy, cuys, jefes y minerales están PENDIENTES.
- `apuyawar-core`: datapack inicial; puente entre comandos y la ruleta del servidor.
- `assets-fuente`: script y notas de las **texturas provisionales**; no son la ilustración final aprobada.
- `docs`: plan de continuidad y normas para crear recursos originales.

No hay referencias ni dependencias de los mods antiguos. **No se incluyen 3,000 PNG ni mods JAR ya compilados**.

## Instalación en Windows / disco E

1. Descomprime ESTE ZIP en `E:\APU_YAWAR\` de modo que `E:\APU_YAWAR\settings.gradle` exista (no anides otra carpeta).
2. Abre `E:\APU_YAWAR\APU_YAWAR.code-workspace` con Visual Studio Code.
3. Instala **JDK 21**, **Minecraft Java 1.21.1 + Fabric Loader**, y extensiones Java/Gradle de VS Code.
4. Abre una terminal en la carpeta `E:\APU_YAWAR` y ejecuta: `gradlew.bat build`.
5. Los JAR estarán en:
   - `apuyawar-systems\build\libs\apuyawar-systems-0.1.0.jar`
   - `apuyawar-world\build\libs\apuyawar-world-0.1.0.jar`
   (Ignora `-sources.jar`, que no es el mod instalable.)
6. Para depurar primero, ejecuta: `gradlew.bat :apuyawar-systems:runClient`.
7. En el mundo de pruebas, con permisos de operador, prueba:
   - `/apuyawar_ruleta tirar`
   - `/apuyawar_ruleta forzar verde`
   - `/apuyawar_ruleta forzar amarillo`
   - `/give @s apuyawar_world:fragmento_apu` (si está cargado el mod World).
8. Para usar el datapack, COPIA la carpeta `apuyawar-core` dentro de `world/datapacks/` y ejecuta `/reload`. Prueba `/function apuyawar:ruleta/tirar`.

Si `gradlew.bat build` falla por Java 17/otro JDK, configura VS Code para JDK 21 y `JAVA_HOME`.
El wrapper puede descargar Gradle 8.12 y las dependencias de Minecraft/Fabric en la primera compilación; necesitas Internet.

## Importante sobre la ruleta

La V1 incluye: 8 sectores texturizados, entrada por piezas, destello inicial, aceleración, giro rápido, vibración, desaceleración matemática hasta el color correcto, rebote, destello final y salida por escala/transparencia. Las imágenes son **provisionales** y NO coinciden aún con la ilustración dorada aprobada. El Java no se genera 3,000 fotogramas.

**No implementado todavía:** ocho recompensas/eventos reales, Cuy Jardinero, Dimensión Cuy, jefes, sonidos sincronizados finales, tests Minecraft multijugador y reproducción artística idéntica a la referencia. `apuyawar-core` solo conecta pruebas de ruleta; no controla eventos peligrosos.

Se debe probar la compilación y el HUD en Minecraft 1.21.1 en Windows antes de usarlo en tu servidor.

## Primer ciclo de trabajo

1. Compilar la base.
2. Ejecutar la ruleta de prueba y revisar el resultado verde/amarillo.
3. Sustituir `textures/gui/roulette/*.png` por arte definitivo con **capas reales independientes**.
4. Ajustar animación, tamaño y sonido en `RouletteTimeline.java` y `RouletteRenderer.java`.
5. Crear por etapas los sistemas de servidor y contenido sin asumir que ya existen.
