scoreboard players add #ticks_anim safio.anim 1
execute if score #ticks_anim safio.anim matches 2.. run scoreboard players set #ticks_anim safio.anim 0
execute if score #ticks_anim safio.anim matches 0 run scoreboard players add #salida safio.anim 1
execute if score #salida safio.anim matches 1..8 run function safio:ruleta/render/salida_pro
execute if score #salida safio.anim matches 8.. run function safio:ruleta/detener
