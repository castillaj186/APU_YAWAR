scoreboard players set #n rj_k 8
function rojo:rand
execute if score #r rj_k matches 1 positioned ~5 ~ ~ run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 2 positioned ~-5 ~ ~ run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 3 positioned ~ ~ ~5 run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 4 positioned ~ ~ ~-5 run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 5 positioned ~4 ~ ~4 run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 6 positioned ~-4 ~ ~-4 run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 7 positioned ~ ~ ~ run summon minecraft:lightning_bolt ~ ~ ~
execute if score #r rj_k matches 8 positioned ~2 ~ ~ run summon minecraft:lightning_bolt ~ ~ ~
