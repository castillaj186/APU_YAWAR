# Faro visual del destino activo.
# #pulse se reinicia cada 20 ticks en rojo:run.
execute if score #pulse rj_k matches 0 as @e[type=armor_stand,tag=rojo_bunker_glow] at @s run particle minecraft:dust 1 0 0 1 ~ ~0.5 ~ 2.5 0.15 2.5 0 40 force
execute if score #pulse rj_k matches 0 as @e[type=armor_stand,tag=rojo_bunker_glow] at @s run particle minecraft:end_rod ~ ~1 ~ 1.2 3 1.2 0.02 28 force
execute if score #pulse rj_k matches 0 as @e[type=armor_stand,tag=rojo_dest_glow] at @s run particle minecraft:dust 1 0.15 0.05 1 ~ ~1 ~ 0.8 1.2 0.8 0 18 force
