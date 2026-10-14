scoreboard objectives add naranja dummy
scoreboard objectives add naranja_meta dummy
scoreboard objectives add naranja_rand dummy
scoreboard players set #minus_one naranja_rand -1
scoreboard players add #init naranja_meta 0
scoreboard players add #handled naranja 0
scoreboard players add #tick_count naranja 0
scoreboard players add #aggro_tick naranja 0
scoreboard players add #tmp naranja 0
scoreboard players add #age naranja 0
scoreboard players add #c1 naranja 0
execute if score #init naranja_meta matches 0 run function naranja:reset_silent
execute if score #init naranja_meta matches 0 run scoreboard players set #init naranja_meta 1
schedule function naranja:loop 1t replace
