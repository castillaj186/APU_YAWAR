summon minecraft:creeper ~ ~ ~ {Ignited:1b,Fuse:70,ExplosionRadius:14b,Tags:["rj_mob","reto"],DeathLootTable:"minecraft:empty",powered:1b,Glowing:1b,CustomName:'{"text":"Creeper Nuclear","color":"dark_red","bold":true}'}
execute if score #cnn rj_k matches 2.. run summon minecraft:creeper ~ ~ ~ {Ignited:1b,Fuse:90,ExplosionRadius:14b,Tags:["rj_mob","reto"],DeathLootTable:"minecraft:empty",powered:1b,Glowing:1b,CustomName:'{"text":"Creeper Nuclear","color":"dark_red","bold":true}'}
tellraw @s ["",{"text":"☢ ","color":"dark_red","bold":true},{"text":"¡CREEPER NUCLEAR! Corre lo más lejos que puedas.","color":"red"}]
playsound minecraft:entity.warden.sonic_charge master @s ~ ~ ~ 1 0.6
