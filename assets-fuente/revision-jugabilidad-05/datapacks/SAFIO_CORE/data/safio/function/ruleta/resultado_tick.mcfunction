scoreboard players add #ticks_anim safio.anim 1
execute if score #ticks_anim safio.anim matches 2.. run scoreboard players set #ticks_anim safio.anim 0
execute if score #ticks_anim safio.anim matches 0 run scoreboard players add #resultado_anim safio.anim 1
execute if score #resultado_anim safio.anim matches 1..8 run function safio:ruleta/render/resultado_pro
execute if score #resultado_anim safio.anim matches 1..8 if score #ticks_anim safio.anim matches 0 as @a at @s run playsound minecraft:block.amethyst_cluster.hit master @s ~ ~ ~ 0.25 1.2
execute if score #resultado_anim safio.anim matches 8.. run function safio:ruleta/iniciar_salida
