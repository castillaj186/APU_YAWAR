# Un solo ciclo de reloj y de animacion por tick del servidor.
# Solicitudes del mod: iniciarlas EN ESTE TICK para no perder el resultado forzado.
execute if score #solicitud safio.anim matches 1 run function safio:ruleta/procesar_solicitud
scoreboard players add #ticks safio.timer 1
execute if score #ticks safio.timer matches 20.. run function safio:segundo
execute if score #ticks safio.timer matches 20.. run scoreboard players set #ticks safio.timer 0
execute if score #animando safio.anim matches 1 run function safio:ruleta/tick
