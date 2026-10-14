summon minecraft:creeper ~ ~ ~ {Ignited:1b,Fuse:30,ExplosionRadius:4b,Tags:["rj_mob","reto"],DeathLootTable:"minecraft:empty",powered:1b,Glowing:1b,CustomName:'{"text":"Creeper Eléctrico","color":"aqua"}'}
execute if score #cen rj_k matches 2.. run summon minecraft:creeper ~ ~ ~ {Ignited:1b,Fuse:40,ExplosionRadius:4b,Tags:["rj_mob","reto"],DeathLootTable:"minecraft:empty",powered:1b,Glowing:1b,CustomName:'{"text":"Creeper Eléctrico","color":"aqua"}'}
playsound minecraft:entity.lightning_bolt.thunder master @s ~ ~ ~ 0.5 1.5
