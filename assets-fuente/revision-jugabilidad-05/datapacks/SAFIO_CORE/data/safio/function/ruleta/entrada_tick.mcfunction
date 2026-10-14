scoreboard players add #entrada safio.anim 1
function safio:ruleta/render/entrada_pro
execute if score #entrada safio.anim matches 3 as @a at @s run playsound minecraft:block.amethyst_block.chime master @s ~ ~ ~ 0.4 1.2
execute if score #entrada safio.anim matches 8.. run function safio:ruleta/empezar_giro
