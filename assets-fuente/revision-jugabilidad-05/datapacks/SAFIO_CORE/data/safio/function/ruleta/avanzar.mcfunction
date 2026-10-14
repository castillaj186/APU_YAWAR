# Avanza el disco segun el salto actual.
scoreboard players operation #salto safio.anim < #restantes safio.anim
scoreboard players operation #frame safio.anim += #salto safio.anim
execute if score #frame safio.anim matches 129.. run scoreboard players remove #frame safio.anim 128
scoreboard players operation #paso safio.anim += #salto safio.anim
scoreboard players operation #restantes safio.anim -= #salto safio.anim
scoreboard players set #ticks_anim safio.anim 0
function safio:ruleta/mostrar_frame
execute if score #restantes safio.anim matches 1..48 as @a at @s run playsound minecraft:block.note_block.hat master @s ~ ~ ~ 0.14 1.45
execute if score #restantes safio.anim matches 0 run function safio:ruleta/iniciar_resultado
