scoreboard players set #fase_anim safio.anim 2
scoreboard players set #ticks_anim safio.anim 0
function safio:ruleta/mostrar_frame
execute as @a at @s run playsound minecraft:block.note_block.hat master @s ~ ~ ~ 0.45 1.35
