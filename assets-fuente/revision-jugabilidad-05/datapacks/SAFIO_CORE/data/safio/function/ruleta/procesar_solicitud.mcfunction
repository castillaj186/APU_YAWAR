# SAFIO 1.6.8: puente determinista, evita la ejecucion diferida de Brigadier.
# No duplica giros: la bandera se consume UNA sola vez, antes de iniciar.
scoreboard players set #solicitud safio.anim 0
execute if score #animando safio.anim matches 0 if score #evento safio.estado matches 0 if score #type safio.live matches 0 if score #forzado safio.ruleta matches 1..8 unless score #activo rj_k matches 1 unless score #cuenta_on rj_k matches 1 run function safio:ruleta/iniciar
