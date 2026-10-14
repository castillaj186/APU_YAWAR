execute store result score #c rj_k if entity @e[type=minecraft:vex,tag=rj_mob,distance=..25]
execute if score #c rj_k matches 8.. run return 0
summon minecraft:vex ~ ~2 ~ {Tags:["rj_mob","reto"],LifeTicks:400,PersistenceRequired:1b,DeathLootTable:"minecraft:empty",HandItems:[{id:"minecraft:iron_sword",Count:1b},{}]}
summon minecraft:vex ~ ~2 ~ {Tags:["rj_mob","reto"],LifeTicks:400,PersistenceRequired:1b,DeathLootTable:"minecraft:empty",HandItems:[{id:"minecraft:iron_sword",Count:1b},{}]}
