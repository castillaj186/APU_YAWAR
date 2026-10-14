# Prueba inicial paso a paso

1. Compila con `gradlew.bat build` desde E:\APU_YAWAR.
2. Inicia Minecraft de desarrollo con `gradlew.bat :apuyawar-systems:runClient`.
3. Crea mundo de prueba con trucos activados. Prueba `/apuyawar_ruleta forzar verde`.
4. Repite `/apuyawar_ruleta forzar amarillo`, y luego `/apuyawar_ruleta tirar`.
5. Comprueba que el puntero no gira, el color bajo él cambia realmente, se detiene en el ganador, hay vibración/destello y termina encogiéndose.
6. Al sustituir texturas, inspecciona que no exista amarillo pegado al puntero y que la punta dorada no esté perforada.
7. Si instalas ambos JAR + Fabric API en un servidor de prueba, copia también `apuyawar-core` al mundo como datapack, recarga y prueba `/function apuyawar:ruleta/tirar`.

**Limites:** este ZIP no ha pasado aún una compilación real contra Minecraft ni pruebas de cliente/servidor de Windows. Primero se valida y luego se avanza a versión 0.2.0.
