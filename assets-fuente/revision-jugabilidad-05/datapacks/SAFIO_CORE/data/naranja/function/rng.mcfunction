summon marker ~ ~ ~ {Tags:["nr_rng"]}
execute store result score #rand naranja_rand run data get entity @e[type=marker,tag=nr_rng,limit=1] UUID[0]
kill @e[type=marker,tag=nr_rng]
