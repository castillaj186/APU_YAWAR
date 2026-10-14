# Recalcula quién está dentro del destino activo.
tag @a[tag=rj_player] remove rj_in
tag @a[tag=rj_player] remove rj_ok
execute if score #dest rj_k matches 1 as @a[tag=rj_player,gamemode=!spectator,x=11551,y=-64,z=1863,dx=129,dy=400,dz=146] at @s if dimension minecraft:overworld run tag @s add rj_in
execute if score #dest rj_k matches 2 as @a[tag=rj_player,gamemode=!spectator,x=14026,y=72,z=107,dx=37,dy=8,dz=31] at @s if dimension minecraft:overworld run tag @s add rj_in
execute if score #dest rj_k matches 3 as @a[tag=rj_player,gamemode=!spectator,x=14184,y=47,z=-640,dx=27,dy=8,dz=31] at @s if dimension minecraft:overworld run tag @s add rj_in
execute if score #dest rj_k matches 4 as @a[tag=rj_player,gamemode=!spectator,x=7394,y=63,z=4784,dx=35,dy=8,dz=28] at @s if dimension minecraft:overworld run tag @s add rj_in
execute if score #dest rj_k matches 5 as @a[tag=rj_player,gamemode=!spectator,x=1510,y=126,z=2529,dx=43,dy=8,dz=47] at @s if dimension minecraft:overworld run tag @s add rj_in
tag @a[tag=rj_player,tag=rj_in] add rj_ok
