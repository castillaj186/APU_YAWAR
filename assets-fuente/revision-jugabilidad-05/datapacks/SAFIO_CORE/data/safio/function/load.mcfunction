# SAFIO CORE SIG FINAL - Minecraft Java 1.21.1
scoreboard objectives add safio.dia dummy
scoreboard objectives add safio.fase dummy
scoreboard objectives add safio.ruleta dummy
scoreboard objectives add safio.estado dummy
scoreboard objectives add safio.timer dummy
scoreboard objectives add safio.anim dummy
scoreboard objectives add safio.config dummy
scoreboard objectives add safio.cal dummy
scoreboard objectives add safio.slots dummy
scoreboard objectives add safio.mods dummy

execute unless score #dia safio.dia matches 0.. run scoreboard players set #dia safio.dia 1
execute unless score #fase safio.fase matches 0.. run scoreboard players set #fase safio.fase 1
execute unless score #resultado safio.ruleta matches 0.. run scoreboard players set #resultado safio.ruleta 0
execute unless score #forzado safio.ruleta matches 0.. run scoreboard players set #forzado safio.ruleta 0
execute unless score #evento safio.estado matches 0.. run scoreboard players set #evento safio.estado 0
execute unless score #timer safio.timer matches 0.. run scoreboard players set #timer safio.timer 0
execute unless score #visual safio.config matches 0..1 run scoreboard players set #visual safio.config 1

# Datos de misiones persistentes: NO borrarlos con /reload.
execute unless score #submisiones safio.estado matches 0..1 run scoreboard players set #submisiones safio.estado 0
execute unless score #dia1_fecha safio.estado matches 0.. run scoreboard players set #dia1_fecha safio.estado 0
execute unless score #modo_prueba safio.estado matches 0..1 run scoreboard players set #modo_prueba safio.estado 0

scoreboard players set #animando safio.anim 0
scoreboard players set #fase_anim safio.anim 0
scoreboard players set #restantes safio.anim 0
scoreboard players set #ticks_anim safio.anim 0
scoreboard players set #entrada safio.anim 0
scoreboard players set #resultado_anim safio.anim 0
scoreboard players set #salida safio.anim 0
scoreboard players set #ticks safio.timer 0

execute unless score #frame safio.anim matches 1..128 run scoreboard players set #frame safio.anim 1
execute unless score #paso safio.anim matches 0.. run scoreboard players set #paso safio.anim 0
execute unless score #meta safio.anim matches 0.. run scoreboard players set #meta safio.anim 0
execute unless score #salto safio.anim matches 0.. run scoreboard players set #salto safio.anim 0

# Nunca resetear calendarios y dificultades al recargar el datapack.
execute unless score #roulette_done safio.cal matches 0.. run scoreboard players set #roulette_done safio.cal 0
# ID de secuencia persistente, utilizado para resincronizar clientes.
execute unless score #media_seq safio.anim matches 0.. run scoreboard players set #media_seq safio.anim 0
# Borra SOLO el bloqueo transitorio de un giro abortado al recargar.
scoreboard players set #color_bloqueado safio.ruleta 0
scoreboard players set #hold safio.anim 0
scoreboard players set #hold_ticks safio.anim 0
scoreboard players set #media_frame safio.anim 0
scoreboard players set #media_total safio.anim 0
scoreboard players set #solicitud safio.anim 0
function safio:actualizar_fase
tellraw @a {"text":"[SAFIO] CORE + MISIONES V2 listo. Ruleta PRO conservada.","color":"green"}

# SAFIO CORE 1.6.7: firma para evitar combinaciones de mod/datapack incompatibles.
scoreboard players set #core_build safio.config 168
# Un marcador ausente no indica un evento pendiente. No sobrescribir uno existente.
execute unless score #pending_slot safio.cal matches 0.. run scoreboard players set #pending_slot safio.cal 0
