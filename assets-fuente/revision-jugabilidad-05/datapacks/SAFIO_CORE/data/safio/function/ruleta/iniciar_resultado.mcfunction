# La ruleta ya cayo en el resultado. Empieza la transformacion por color.
scoreboard players set #fase_anim safio.anim 3
scoreboard players set #ticks_anim safio.anim 0
scoreboard players set #resultado_anim safio.anim 0
execute as @a at @s run playsound minecraft:block.note_block.bell master @s ~ ~ ~ 0.8 1.45
title @a subtitle {"text":"RESULTADO","color":"gold","bold":true}
