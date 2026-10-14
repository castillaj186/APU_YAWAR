# Panel K — mobs hostiles, objetos crafteables, recetas interactivas y búsqueda unificada

Fecha: 14 de octubre de 2026. Rama: `feat/panel-k-catalogos-recetas`, basada en
`feat/panel-k-mobs-3d` (mobs 3D + nitidez ya confirmada). No se mezcla con la
PR de nitidez ni con la de mobs 3D; no se hizo merge a `main`.

**Estado: compila y construye limpio (`gradlew clean build`), `verifyJudgments`
en verde (128 objetivos, 135 catálogos, sin cambios). Sin confirmación visual
en Minecraft todavía — no hay cliente gráfico en este entorno.** No se tocó la
ruleta, Inti, Staff, Sol PE, Misiones (`MissionsScreen`, `MissionCatalog`,
`MissionState`), comandos administrativos, permisos, persistencia,
`panel.png`/`.mcmeta`, `ViewportFix`, `PanelScale`, ni `PanelLayout.CONTENT_AREA`
(solo se ajustaron márgenes internos en código Java, no las fracciones medidas
del panel). El diseño de los 4 botones no cambió.

## 1. Mobs hostiles únicamente

`MobCatalog` ya tenía las 79 criaturas curadas de la fase anterior, categorizadas
`PASSIVE`/`NEUTRAL`/`HOSTILE`. Ese trabajo se conserva íntegro en
`MobCatalog.ALL_VANILLA` (nuevo nombre del campo que antes era `ENTRIES`); lo
que cambió es que `MobCatalog.ENTRIES` — lo que de verdad consume el catálogo —
ahora es el filtro `ALL_VANILLA.stream().filter(category == HOSTILE)`. Las 34
criaturas que quedan son exactamente las que atacan por su cuenta; Enderman,
Lobo, Piglin, Golem de Hierro, etc. ya estaban categorizados `NEUTRAL` desde la
fase anterior (se defienden si se las provoca, no atacan de por sí) y por tanto
quedan fuera automáticamente, sin necesitar ninguna lista de exclusión nueva.
`ENDER_DRAGON`/`WITHER` siguen excluidos (nunca estuvieron en la lista). El
renderizador 3D (`MobCellRenderer`) no se tocó ni una línea.

## 2. Objetos: solo lo fabricable, desde el `RecipeManager` real

`ItemCatalog` dejó de recorrer `Registries.ITEM` (mostraba cualquier ítem
registrado). Ahora se construye desde `CraftingRecipeIndex`, que consulta
`recipeManager.listAllOfType(RecipeType.CRAFTING)` del mundo/servidor ACTUAL —
nunca una lista estática al cargar la clase. Filtra a solo `ShapedRecipe`
(con forma) y `ShapelessRecipe` (sin forma): `RecipeType.CRAFTING` también
incluye recetas "especiales" (teñido de armadura/estandartes, clonado de
libros/mapas, estrellas de fuegos artificiales) que son técnicamente
`CraftingRecipe` pero no tienen una cuadrícula fija de ingredientes que
mostrar honestamente — se excluyen a propósito. Fundición, cocción, ahumado,
fogata, corte de piedra, herrería, pociones y comercio ni siquiera son
`RecipeType.CRAFTING`, así que ya quedan fuera sin necesitar un filtro extra.

Las recetas se agrupan por objeto resultante (`Map<Item, List<CraftingRecipe>>`):
un objeto con varias formas de fabricarse aparece una sola vez en la
cuadrícula, con todas sus recetas accesibles al abrir el detalle.

### Por qué `ItemCatalog` dejó de ser estático

Antes era `static final List<CatalogEntry> ENTRIES` (se construía una sola vez,
al cargar la clase Java, para toda la vida del cliente). Las recetas dependen
del `RecipeManager` sincronizado del mundo/servidor actual, que puede variar
entre servidores o tras recargar un datapack — una lista estática quedaría
desactualizada. Ahora `ItemCatalog` es una instancia normal, creada en el
constructor de `SurvivalPanelScreen` (una vez por cada apertura del Panel K,
con `K`), igual que ya hacía la caché de entidades de `MobCellRenderer` desde
la fase anterior: vive y muere con la pantalla, sin necesitar invalidación
explícita ni un sistema de "recarga en caliente" (abrir el panel de nuevo ya
refleja el estado actual).

## 3. Visualizador de recetas reutilizable (`CraftingRecipeView`)

Clase sin estado (todo el estado — qué objeto, qué receta alternativa — vive
en `SurvivalPanelScreen`) usada **idéntica** desde Objetos y desde Búsqueda,
tal como se pidió. Dibuja:

- La cuadrícula REAL de ingredientes: para `ShapedRecipe`, su `getWidth()` ×
  `getHeight()` reales (ej. 2×2 para la mesa de trabajo, no forzado a 3×3) con
  los espacios vacíos de la receta respetados (`Ingredient.isEmpty()`, no se
  dibuja nada ahí); para `ShapelessRecipe` (sin posición real), se empaquetan
  en filas de hasta 3 — nunca se inventa una forma que la receta no tiene.
- Variantes de ingrediente: si `ingredient.getMatchingStacks().length > 1`
  (ej. "cualquier tabla de madera"), se dibuja el primer ítem válido más una
  marca `+N` — nunca se representa como si solo aceptara un ítem.
- Una flecha y el resultado real (`recipe.getResult(registries)`), con su
  cantidad si es mayor a 1.
- El nombre del objeto, y, si hay más de una receta alternativa, un contador
  "Receta i/N" con botones "<"/">" para recorrerlas.
- Un botón "« Volver".

Es puramente una consulta visual: en ningún momento se llama a
`recipe.craft(...)` ni se toca ningún inventario real — no abre la interfaz
vanilla de la mesa de trabajo, no consume materiales.

## 4. Búsqueda unificada

Se eliminó `SearchMode` (`RECIPES`/`HOSTILE_MOBS`) y los botones
"[Recetas] [Mobs hostiles]". Ahora hay una sola barra: al escribir, se buscan
a la vez en `MobCatalog.ENTRIES` (hostiles) y `itemCatalog.entries()`
(fabricables), normalizando ambos lados de la comparación con
`Normalizer.normalize(..., NFD)` + eliminación de marcas combinantes — minúsculas
y sin distinguir tildes, sin inventar un algoritmo de coincidencia difusa.
Nunca se buscan los espacios "?" de contenido bloqueado (no existen como
entradas reales en ninguna de las dos listas fuente).

Los resultados (`sealed interface SearchResult { Mob, Item }`) se muestran en
una cuadrícula (`CatalogGrid<SearchResult>`, el mismo componente que usan Mobs
y Objetos — mismo scroll/hover/selección) cuyo renderizador de celda
(`SearchResultCellRenderer`) **no dibuja nada nuevo**: delega en las mismas
instancias de `ItemCellRenderer`/`MobCellRenderer` que ya usan las otras dos
pestañas (comparten la misma caché de entidades de vista previa). Seleccionar
un resultado abre el mismo visualizador de receta (si es un objeto) o una
ficha básica de mob (si es un mob) — ver sección 6.

## 5. Navegación y persistencia de contexto

Se añadieron 6 campos de estado a `SurvivalPanelScreen` (no una máquina de
estados separada: dado que el panel nunca cierra la pantalla para mostrar un
detalle, bastaba con banderas simples):

- `itemsDetailItem`/`itemsDetailName`/`itemsDetailIndex` — detalle de receta
  abierto desde Objetos.
- `searchDetailItem`/`searchDetailName`/`searchDetailIndex` — detalle de
  receta abierto desde Búsqueda (independiente del anterior).
- `searchDetailMob` — ficha de mob abierta desde Búsqueda.

Volver desde un detalle solo pone a `null` el campo correspondiente; la
pestaña (`tab`) nunca cambia, así que "Objetos → receta → Volver → Objetos" y
"Búsqueda → receta → Volver → Búsqueda" regresan al contexto correcto por
construcción. El campo de texto de búsqueda (`searchField`, un
`TextFieldWidget` real, no se reconstruye nunca) y el `scroll`/`selected`
internos de `searchGrid` nunca se tocan mientras se ve un detalle — por eso el
texto escrito y la posición de scroll se conservan automáticamente al volver,
sin necesitar guardarlos y restaurarlos a mano.

## 6. Ficha básica de mob (desde Búsqueda)

Solo nombre real y categoría ("Categoría: Enemigo hostil") — no se inventan
drops, estadísticas ni habilidades que no estén implementadas, tal como se
pidió explícitamente. Reutiliza `MobCellRenderer.render(...)` (la misma
instancia, mismo caché) con una caja más grande que una celda de cuadrícula;
el tamaño automático por dimensiones reales de `MobCellRenderer` (de la fase
anterior) ya se encarga de que la criatura no se recorte ni quede diminuta
también a este tamaño mayor, sin ningún código nuevo de escalado.

## 7. Corrección de títulos y espaciado

Antes: el título se dibujaba exactamente en `fy` (la esquina superior del área
de contenido, sin ningún margen) y el cuerpo empezaba en `fy+14` — en
Búsqueda, el campo de texto se dibujaba también en `fy`, literalmente debajo
del título. Ahora `SurvivalPanelScreen.layout(box)` calcula explícitamente:

```
titleY        = fy + 6                                  (margen superior)
searchFieldY  = titleY + 10 + 6                          (tras el título)
bodyY         = tab == SEARCH ? searchFieldY + 16 + 6    (tras la barra, solo en Búsqueda)
                               : searchFieldY
```

Es un único método (`ContentLayout layout(int[] box)`) usado por `init()`
(posiciona el campo de texto), `render()` (título/cuerpo) y `mouseClicked()`
(hit-testing) — una sola fuente de verdad, no coordenadas repetidas con
números mágicos distintos en cada sitio. No se cambiaron las fracciones de
`PanelLayout.CONTENT_AREA` (medidas contra los píxeles reales de `panel.png`,
ver `12_PANEL_K_CORRECCION_VISUAL.md`) — toda la corrección es de márgenes
internos en código Java, como se pidió explícitamente.

## 8. Archivos

**Nuevos:**
- `panel/client/CraftingRecipeIndex.java` — índice de recetas real, agrupado por resultado.
- `panel/client/CraftingRecipeView.java` — visualizador de receta reutilizable.
- `panel/client/SearchResult.java` — tipo selladode resultado de búsqueda (Mob/Item).
- `panel/client/SearchResultCellRenderer.java` — delega en los renderizadores existentes.

**Modificados:**
- `panel/client/MobCatalog.java` — `ENTRIES` filtrado a solo `HOSTILE`; el roster completo se conserva en `ALL_VANILLA`.
- `panel/client/ItemCatalog.java` — reescrito: instancia por apertura de panel, construida desde `CraftingRecipeIndex` en vez de `Registries.ITEM`.
- `panel/client/CatalogGrid.java` — `entries` deja de ser `final` + nuevos `selectedEntry()`/`setEntries(List<T>)`, para que la búsqueda pueda reemplazar su lista de resultados sin perder la abstracción genérica ya existente.
- `panel/client/SurvivalPanelScreen.java` — ver secciones 4, 5 y 7. El bloque de nitidez (`renderBackground()` no-op, el `fill()` oscuro plano, `ViewportFix.restoreAfterBackground`, la sobrecarga de 10 argumentos de `drawTexture`, `PanelScale.snap`) se mantiene línea por línea, solo con comentarios "NO TOCAR" añadidos — confirmado por `git diff` contra `feat/panel-k-mobs-3d`.

**Sin cambios:** `CatalogEntry.java`, `ItemCellRenderer.java`, `MobCatalogEntry.java`,
`MobCellRenderer.java`, `PanelLayout.java`, `PanelTextures.java`, `PanelScale.java`,
`ViewportFix.java`, `MissionsScreen.java`, `AdminPanelScreen.java`, y todo lo
de `roulette/`, `judgment/`, `client/judgment/`, `missions/`, `staff/`,
`items/`, `events/`, `difficulty/`, `challenges/`, `network/`.

## 9. Resultado de compilación y pruebas realizadas

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat clean build                      # BUILD SUCCESSFUL
```

`verifyJudgments`: **PASS — 128 objetivos, 135 catálogos**, sin cambios.

Verificado además:
- `git diff` contra `feat/panel-k-mobs-3d` confirma que solo cambiaron los 4
  archivos listados arriba como "Modificados", y que el bloque de nitidez de
  `SurvivalPanelScreen.render()`/`renderBackground()` es idéntico salvo
  comentarios.
- API real de 1.21.1 confirmada vía `javap` antes de escribir código:
  `RecipeManager.listAllOfType`, `RecipeType.CRAFTING`, `ShapedRecipe`/
  `ShapelessRecipe` (`getWidth`/`getHeight`/`getIngredients`), `Ingredient`
  (`isEmpty`/`getMatchingStacks`), `TextRenderer.getWidth` — no se adivinó
  ninguna firma.

## 10. Limitaciones conocidas

- La ficha de mob es intencionalmente mínima (nombre + categoría): no hay
  todavía datos reales de daño/vida/drops que mostrar sin inventarlos.
- El visualizador de recetas usa un tamaño de celda fijo (18px) independiente
  de la escala del panel — se verificó que cabe de sobra en el ancho/alto
  típico del área de contenido, pero no se probó en una GUI extremadamente
  pequeña.
- `CraftingRecipeIndex` no distingue recetas "descubiertas"/"no descubiertas"
  del libro de recetas del jugador (muestra TODAS las recetas sincronizadas,
  igual que ya hacía el buscador de recetas de la fase anterior) — no se pidió
  respetar el progreso del libro de recetas, y añadirlo ahora habría sido
  inventar un requisito no solicitado.
- No existe todavía un punto de registro dinámico para que un mod externo
  añada una categoría de contenido distinta a "mob hostil" u "objeto
  fabricable" (ej. una máquina propia con su propio tipo de receta); los
  puntos de extensión actuales son: añadir una línea a `MobCatalog.build()`
  para una criatura nueva, y cualquier receta `Shaped`/`ShapelessRecipe`
  sincronizada por el servidor aparece sola en Objetos/Búsqueda sin tocar
  código. Una "máquina especial" necesitaría su propio visualizador nuevo —
  no se inventó uno sin que exista la máquina.

## 11. Pendiente de validación dentro de Minecraft

Nada de lo siguiente se ha podido confirmar visualmente — no hay cliente
gráfico en este entorno:

1. Que Mobs muestre solo las 34 hostiles (sin Vaca, Oveja, Enderman, Lobo, etc.).
2. Que Objetos muestre solo fabricables, y que ningún ítem exclusivo de
   Creative sin receta aparezca.
3. Que seleccionar un objeto abra su receta con los ingredientes correctos,
   incluidos los espacios vacíos (ej. la mesa de trabajo, 2×2 real, no 3×3).
4. Que las recetas alternativas se puedan recorrer con "<"/">".
5. Que la búsqueda ("Zombie", "Mesa de trabajo", "Espada") encuentre ambos
   tipos de resultado a la vez, sin botones de categoría.
6. Que "« Volver" regrese al contexto correcto (Objetos o Búsqueda) y que el
   texto/scroll de Búsqueda se conserven.
7. Que los títulos ya no choquen con el marco ni con el buscador, en varias
   escalas de GUI.
8. Que la nitidez siga intacta (fondo, marco, íconos) — no se tocó ese código,
   pero solo una prueba visual real lo confirma de verdad.
9. Que los 4 botones, Misiones y el Panel Administrativo sigan funcionando
   exactamente igual.
10. Rendimiento al escribir en la búsqueda y hacer scroll por muchos
    resultados, y al abrir/cerrar el panel repetidamente.

No se declara nada de esto validado hasta que el usuario lo confirme en el
juego real.
