# Marcador visual del destino. Los bunkers quedan con contorno brillante + partículas rojas.
kill @e[type=armor_stand,tag=rojo_dest_glow]
execute if score #dest rj_k matches 1 in minecraft:overworld run summon armor_stand 11616 69 1903 {Invisible:1b,Invulnerable:1b,NoGravity:1b,Glowing:1b,Small:1b,NoBasePlate:1b,Tags:["rojo_dest_glow"]}
execute if score #dest rj_k matches 2 in minecraft:overworld run summon armor_stand 14045 75 121 {Invisible:1b,Invulnerable:1b,NoGravity:1b,Glowing:1b,Small:1b,NoBasePlate:1b,Tags:["rojo_dest_glow","rojo_bunker_glow"]}
execute if score #dest rj_k matches 3 in minecraft:overworld run summon armor_stand 14198 50 -625 {Invisible:1b,Invulnerable:1b,NoGravity:1b,Glowing:1b,Small:1b,NoBasePlate:1b,Tags:["rojo_dest_glow","rojo_bunker_glow"]}
execute if score #dest rj_k matches 4 in minecraft:overworld run summon armor_stand 7412 66 4797 {Invisible:1b,Invulnerable:1b,NoGravity:1b,Glowing:1b,Small:1b,NoBasePlate:1b,Tags:["rojo_dest_glow","rojo_bunker_glow"]}
execute if score #dest rj_k matches 5 in minecraft:overworld run summon armor_stand 1532 129 2552 {Invisible:1b,Invulnerable:1b,NoGravity:1b,Glowing:1b,Small:1b,NoBasePlate:1b,Tags:["rojo_dest_glow","rojo_bunker_glow"]}
