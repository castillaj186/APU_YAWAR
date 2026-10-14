execute store result score #c rj_k if entity @e[type=minecraft:ghast,tag=rj_mob,distance=..60]
execute if score #c rj_k matches 2.. run return 0
summon minecraft:ghast ~ ~15 ~ {Tags:["rj_mob","reto"],PersistenceRequired:1b,DeathLootTable:"minecraft:empty"}
