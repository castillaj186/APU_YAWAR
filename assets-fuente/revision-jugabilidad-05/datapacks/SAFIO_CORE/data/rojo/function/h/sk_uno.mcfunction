execute store result score #c rj_k if entity @e[tag=rj_mob,distance=..10]
execute if score #c rj_k matches 14.. run return 0
summon minecraft:skeleton ~ ~ ~ {Tags:["rj_mob","reto"],PersistenceRequired:1b,DeathLootTable:"minecraft:empty",ActiveEffects:[{Id:12,Amplifier:0b,Duration:99999,ShowParticles:0b}],HandItems:[{id:"minecraft:bow",Count:1b},{}],HandDropChances:[0.0f,0.0f]}
execute if score #mc rj_k matches 2.. run summon minecraft:skeleton ~ ~ ~ {Tags:["rj_mob","reto"],PersistenceRequired:1b,DeathLootTable:"minecraft:empty",ActiveEffects:[{Id:12,Amplifier:0b,Duration:99999,ShowParticles:0b}],HandItems:[{id:"minecraft:bow",Count:1b},{}],HandDropChances:[0.0f,0.0f]}
execute if score #mc rj_k matches 3.. run summon minecraft:skeleton ~ ~ ~ {Tags:["rj_mob","reto"],PersistenceRequired:1b,DeathLootTable:"minecraft:empty",ActiveEffects:[{Id:12,Amplifier:0b,Duration:99999,ShowParticles:0b}],HandItems:[{id:"minecraft:bow",Count:1b},{}],HandDropChances:[0.0f,0.0f]}
