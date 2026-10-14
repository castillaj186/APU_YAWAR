# NARANJA aplicar - cada tick
scoreboard players add #aggro_tick naranja 1

# ========== DAÑOS DE BLOQUE (siempre primero, no fallan por atributos) ==========
# c1 Magma mortal: 100 dmg tipo generic (ignora fire resistance; totem SI salva)
execute if score #c1 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~-0.2 ~ magma_block run damage @s 100 minecraft:generic
execute if score #c1 naranja matches 1 as @a[gamemode=adventure] at @s if block ~ ~-0.2 ~ magma_block run damage @s 100 minecraft:generic
execute if score #c1 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ magma_block run damage @s 100 minecraft:generic
execute if score #c1 naranja matches 1 as @a[gamemode=adventure] at @s if block ~ ~ ~ magma_block run damage @s 100 minecraft:generic

# c71 cactus extra
execute if score #c71 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ cactus run damage @s 4 minecraft:cactus
execute if score #c71 naranja matches 1 as @a[gamemode=adventure] at @s if block ~ ~ ~ cactus run damage @s 4 minecraft:cactus

# c72 magma cruel (extra suave si no esta c1)
execute if score #c72 naranja matches 1 unless score #c1 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~-0.2 ~ magma_block run damage @s 4 minecraft:hot_floor

# c73 bayas
execute if score #c73 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ sweet_berry_bush run damage @s 3 minecraft:sweet_berry_bush

# c74 campfire
execute if score #c74 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ campfire run damage @s 3 minecraft:in_fire
execute if score #c78 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ soul_campfire run damage @s 4 minecraft:in_fire

# c75 dripstone
execute if score #c75 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~-0.2 ~ pointed_dripstone run damage @s 3 minecraft:stalagmite

# c76 wither rose
execute if score #c76 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ wither_rose run damage @s 3 minecraft:wither

# c77 powder snow
execute if score #c77 naranja matches 1 as @a[gamemode=survival] at @s if block ~ ~ ~ powder_snow run damage @s 2 minecraft:freeze

# ========== GAMERULES ==========
execute if score #c40 naranja matches 1 run difficulty hard
execute if score #c61 naranja matches 1 run gamerule randomTickSpeed 2
execute if score #c62 naranja matches 1 run gamerule playersSleepingPercentage 101
execute if score #c63 naranja matches 1 run gamerule doInsomnia true
execute if score #c82 naranja matches 1 run gamerule doFireTick true
execute if score #c83 naranja matches 1 run gamerule mobExplosionDropDecay true
execute if score #c84 naranja matches 1 run gamerule maxEntityCramming 8
execute if score #c85 naranja matches 1 run gamerule spectatorsGenerateChunks false
execute if score #c86 naranja matches 1 run gamerule reducedDebugInfo true
execute if score #c87 naranja matches 1 run gamerule forgiveDeadPlayers false

# ========== LECHE / VACAS ==========
execute if score #c92 naranja matches 1 as @a[nbt={Inventory:[{id:"minecraft:milk_bucket"}]}] run clear @s milk_bucket 1
execute if score #c92 naranja matches 1 as @a[nbt={Inventory:[{id:"minecraft:milk_bucket"}]}] run give @s bucket 1
execute if score #c89 naranja matches 1 as @a[nbt={Inventory:[{id:"minecraft:milk_bucket"}]}] run clear @s milk_bucket
execute if score #c89 naranja matches 1 as @a[nbt={Inventory:[{id:"minecraft:milk_bucket"}]}] run give @s bucket 1
execute if score #c88 naranja matches 1 as @a[nbt={Inventory:[{id:"minecraft:mushroom_stew"}]}] run clear @s mushroom_stew 1

# ========== ANTORCHAS EN AGUA ==========
execute if score #c81 naranja matches 1 as @a at @s if block ~ ~ ~ water run fill ~-2 ~-1 ~-2 ~2 ~2 ~2 air replace torch
execute if score #c81 naranja matches 1 as @a at @s if block ~ ~ ~ water run fill ~-2 ~-1 ~-2 ~2 ~2 ~2 air replace wall_torch

# ========== SOLO CADA 20 TICKS (mobs) ==========
execute unless score #aggro_tick naranja matches 20.. run return 0
scoreboard players set #aggro_tick naranja 0

# Creepers - no longer c1; if we want charged use another - skip

# Health buffs
execute if score #c3 naranja matches 1 as @e[type=zombie,tag=!nr_a3] run attribute @s minecraft:generic.max_health base set 40
execute if score #c3 naranja matches 1 as @e[type=zombie,tag=!nr_a3] run data modify entity @s Health set value 40.0f
execute if score #c3 naranja matches 1 as @e[type=zombie,tag=!nr_a3] run tag @s add nr_a3
execute if score #c5 naranja matches 1 as @e[type=skeleton,tag=!nr_a5] run attribute @s minecraft:generic.max_health base set 30
execute if score #c5 naranja matches 1 as @e[type=skeleton,tag=!nr_a5] run data modify entity @s Health set value 30.0f
execute if score #c5 naranja matches 1 as @e[type=skeleton,tag=!nr_a5] run tag @s add nr_a5
execute if score #c7 naranja matches 1 as @e[type=spider,tag=!nr_a7] run attribute @s minecraft:generic.max_health base set 24
execute if score #c7 naranja matches 1 as @e[type=spider,tag=!nr_a7] run data modify entity @s Health set value 24.0f
execute if score #c7 naranja matches 1 as @e[type=spider,tag=!nr_a7] run tag @s add nr_a7
execute if score #c11 naranja matches 1 as @e[type=phantom,tag=!nr_a11] run attribute @s minecraft:generic.max_health base set 30
execute if score #c11 naranja matches 1 as @e[type=phantom,tag=!nr_a11] run data modify entity @s Health set value 30.0f
execute if score #c11 naranja matches 1 as @e[type=phantom,tag=!nr_a11] run tag @s add nr_a11
execute if score #c13 naranja matches 1 as @e[type=enderman,tag=!nr_a13] run attribute @s minecraft:generic.max_health base set 60
execute if score #c13 naranja matches 1 as @e[type=enderman,tag=!nr_a13] run data modify entity @s Health set value 60.0f
execute if score #c13 naranja matches 1 as @e[type=enderman,tag=!nr_a13] run tag @s add nr_a13
execute if score #c14 naranja matches 1 as @e[type=blaze,tag=!nr_a14] run attribute @s minecraft:generic.max_health base set 40
execute if score #c14 naranja matches 1 as @e[type=blaze,tag=!nr_a14] run data modify entity @s Health set value 40.0f
execute if score #c14 naranja matches 1 as @e[type=blaze,tag=!nr_a14] run tag @s add nr_a14
execute if score #c16 naranja matches 1 as @e[type=ghast,tag=!nr_a16] run attribute @s minecraft:generic.max_health base set 30
execute if score #c16 naranja matches 1 as @e[type=ghast,tag=!nr_a16] run data modify entity @s Health set value 30.0f
execute if score #c16 naranja matches 1 as @e[type=ghast,tag=!nr_a16] run tag @s add nr_a16
execute if score #c17 naranja matches 1 as @e[type=piglin,tag=!nr_a17] run attribute @s minecraft:generic.max_health base set 30
execute if score #c17 naranja matches 1 as @e[type=piglin,tag=!nr_a17] run data modify entity @s Health set value 30.0f
execute if score #c17 naranja matches 1 as @e[type=piglin,tag=!nr_a17] run tag @s add nr_a17
execute if score #c18 naranja matches 1 as @e[type=hoglin,tag=!nr_a18] run attribute @s minecraft:generic.max_health base set 60
execute if score #c18 naranja matches 1 as @e[type=hoglin,tag=!nr_a18] run data modify entity @s Health set value 60.0f
execute if score #c18 naranja matches 1 as @e[type=hoglin,tag=!nr_a18] run tag @s add nr_a18
execute if score #c19 naranja matches 1 as @e[type=piglin_brute,tag=!nr_a19] run attribute @s minecraft:generic.max_health base set 70
execute if score #c19 naranja matches 1 as @e[type=piglin_brute,tag=!nr_a19] run data modify entity @s Health set value 70.0f
execute if score #c19 naranja matches 1 as @e[type=piglin_brute,tag=!nr_a19] run tag @s add nr_a19
execute if score #c20 naranja matches 1 as @e[type=wither_skeleton,tag=!nr_a20] run attribute @s minecraft:generic.max_health base set 40
execute if score #c20 naranja matches 1 as @e[type=wither_skeleton,tag=!nr_a20] run data modify entity @s Health set value 40.0f
execute if score #c20 naranja matches 1 as @e[type=wither_skeleton,tag=!nr_a20] run tag @s add nr_a20
execute if score #c21 naranja matches 1 as @e[type=magma_cube,tag=!nr_a21] run attribute @s minecraft:generic.max_health base set 24
execute if score #c21 naranja matches 1 as @e[type=magma_cube,tag=!nr_a21] run data modify entity @s Health set value 24.0f
execute if score #c21 naranja matches 1 as @e[type=magma_cube,tag=!nr_a21] run tag @s add nr_a21
execute if score #c22 naranja matches 1 as @e[type=slime,tag=!nr_a22] run attribute @s minecraft:generic.max_health base set 24
execute if score #c22 naranja matches 1 as @e[type=slime,tag=!nr_a22] run data modify entity @s Health set value 24.0f
execute if score #c22 naranja matches 1 as @e[type=slime,tag=!nr_a22] run tag @s add nr_a22
execute if score #c23 naranja matches 1 as @e[type=drowned,tag=!nr_a23] run attribute @s minecraft:generic.max_health base set 35
execute if score #c23 naranja matches 1 as @e[type=drowned,tag=!nr_a23] run data modify entity @s Health set value 35.0f
execute if score #c23 naranja matches 1 as @e[type=drowned,tag=!nr_a23] run tag @s add nr_a23
execute if score #c24 naranja matches 1 as @e[type=husk,tag=!nr_a24] run attribute @s minecraft:generic.max_health base set 35
execute if score #c24 naranja matches 1 as @e[type=husk,tag=!nr_a24] run data modify entity @s Health set value 35.0f
execute if score #c24 naranja matches 1 as @e[type=husk,tag=!nr_a24] run tag @s add nr_a24
execute if score #c25 naranja matches 1 as @e[type=stray,tag=!nr_a25] run attribute @s minecraft:generic.max_health base set 30
execute if score #c25 naranja matches 1 as @e[type=stray,tag=!nr_a25] run data modify entity @s Health set value 30.0f
execute if score #c25 naranja matches 1 as @e[type=stray,tag=!nr_a25] run tag @s add nr_a25
execute if score #c26 naranja matches 1 as @e[type=witch,tag=!nr_a26] run attribute @s minecraft:generic.max_health base set 40
execute if score #c26 naranja matches 1 as @e[type=witch,tag=!nr_a26] run data modify entity @s Health set value 40.0f
execute if score #c26 naranja matches 1 as @e[type=witch,tag=!nr_a26] run tag @s add nr_a26
execute if score #c27 naranja matches 1 as @e[type=evoker,tag=!nr_a27] run attribute @s minecraft:generic.max_health base set 40
execute if score #c27 naranja matches 1 as @e[type=evoker,tag=!nr_a27] run data modify entity @s Health set value 40.0f
execute if score #c27 naranja matches 1 as @e[type=evoker,tag=!nr_a27] run tag @s add nr_a27
execute if score #c28 naranja matches 1 as @e[type=vindicator,tag=!nr_a28] run attribute @s minecraft:generic.max_health base set 40
execute if score #c28 naranja matches 1 as @e[type=vindicator,tag=!nr_a28] run data modify entity @s Health set value 40.0f
execute if score #c28 naranja matches 1 as @e[type=vindicator,tag=!nr_a28] run tag @s add nr_a28
execute if score #c29 naranja matches 1 as @e[type=ravager,tag=!nr_a29] run attribute @s minecraft:generic.max_health base set 150
execute if score #c29 naranja matches 1 as @e[type=ravager,tag=!nr_a29] run data modify entity @s Health set value 150.0f
execute if score #c29 naranja matches 1 as @e[type=ravager,tag=!nr_a29] run tag @s add nr_a29
execute if score #c30 naranja matches 1 as @e[type=guardian,tag=!nr_a30] run attribute @s minecraft:generic.max_health base set 40
execute if score #c30 naranja matches 1 as @e[type=guardian,tag=!nr_a30] run data modify entity @s Health set value 40.0f
execute if score #c30 naranja matches 1 as @e[type=guardian,tag=!nr_a30] run tag @s add nr_a30
execute if score #c31 naranja matches 1 as @e[type=elder_guardian,tag=!nr_a31] run attribute @s minecraft:generic.max_health base set 120
execute if score #c31 naranja matches 1 as @e[type=elder_guardian,tag=!nr_a31] run data modify entity @s Health set value 120.0f
execute if score #c31 naranja matches 1 as @e[type=elder_guardian,tag=!nr_a31] run tag @s add nr_a31
execute if score #c32 naranja matches 1 as @e[type=shulker,tag=!nr_a32] run attribute @s minecraft:generic.max_health base set 40
execute if score #c32 naranja matches 1 as @e[type=shulker,tag=!nr_a32] run data modify entity @s Health set value 40.0f
execute if score #c32 naranja matches 1 as @e[type=shulker,tag=!nr_a32] run tag @s add nr_a32
execute if score #c33 naranja matches 1 as @e[type=silverfish,tag=!nr_a33] run attribute @s minecraft:generic.max_health base set 12
execute if score #c33 naranja matches 1 as @e[type=silverfish,tag=!nr_a33] run data modify entity @s Health set value 12.0f
execute if score #c33 naranja matches 1 as @e[type=silverfish,tag=!nr_a33] run tag @s add nr_a33
execute if score #c34 naranja matches 1 as @e[type=endermite,tag=!nr_a34] run attribute @s minecraft:generic.max_health base set 12
execute if score #c34 naranja matches 1 as @e[type=endermite,tag=!nr_a34] run data modify entity @s Health set value 12.0f
execute if score #c34 naranja matches 1 as @e[type=endermite,tag=!nr_a34] run tag @s add nr_a34
execute if score #c35 naranja matches 1 as @e[type=zoglin,tag=!nr_a35] run attribute @s minecraft:generic.max_health base set 60
execute if score #c35 naranja matches 1 as @e[type=zoglin,tag=!nr_a35] run data modify entity @s Health set value 60.0f
execute if score #c35 naranja matches 1 as @e[type=zoglin,tag=!nr_a35] run tag @s add nr_a35
execute if score #c55 naranja matches 1 as @e[type=creeper,tag=!nr_a55] run attribute @s minecraft:generic.max_health base set 30
execute if score #c55 naranja matches 1 as @e[type=creeper,tag=!nr_a55] run data modify entity @s Health set value 30.0f
execute if score #c55 naranja matches 1 as @e[type=creeper,tag=!nr_a55] run tag @s add nr_a55
execute if score #c91 naranja matches 1 as @e[type=zombie,tag=!nr_a91] run attribute @s minecraft:generic.max_health base set 50
execute if score #c91 naranja matches 1 as @e[type=zombie,tag=!nr_a91] run data modify entity @s Health set value 50.0f
execute if score #c91 naranja matches 1 as @e[type=zombie,tag=!nr_a91] run tag @s add nr_a91
execute if score #c99 naranja matches 1 as @e[type=slime,tag=!nr_a99] run attribute @s minecraft:generic.max_health base set 32
execute if score #c99 naranja matches 1 as @e[type=slime,tag=!nr_a99] run data modify entity @s Health set value 32.0f
execute if score #c99 naranja matches 1 as @e[type=slime,tag=!nr_a99] run tag @s add nr_a99
execute if score #c2 naranja matches 1 as @e[type=creeper,tag=!nr_s2] run attribute @s minecraft:generic.movement_speed base set 0.34
execute if score #c2 naranja matches 1 as @e[type=creeper,tag=!nr_s2] run tag @s add nr_s2
execute if score #c6 naranja matches 1 as @e[type=skeleton,tag=!nr_s6] run attribute @s minecraft:generic.movement_speed base set 0.32
execute if score #c6 naranja matches 1 as @e[type=skeleton,tag=!nr_s6] run tag @s add nr_s6
execute if score #c8 naranja matches 1 as @e[type=spider,tag=!nr_s8] run attribute @s minecraft:generic.movement_speed base set 0.35
execute if score #c8 naranja matches 1 as @e[type=spider,tag=!nr_s8] run tag @s add nr_s8
execute if score #c15 naranja matches 1 as @e[type=blaze,tag=!nr_s15] run attribute @s minecraft:generic.movement_speed base set 0.28
execute if score #c15 naranja matches 1 as @e[type=blaze,tag=!nr_s15] run tag @s add nr_s15
execute if score #c18 naranja matches 1 as @e[type=hoglin,tag=!nr_s18] run attribute @s minecraft:generic.movement_speed base set 0.35
execute if score #c18 naranja matches 1 as @e[type=hoglin,tag=!nr_s18] run tag @s add nr_s18
execute if score #c33 naranja matches 1 as @e[type=silverfish,tag=!nr_s33] run attribute @s minecraft:generic.movement_speed base set 0.35
execute if score #c33 naranja matches 1 as @e[type=silverfish,tag=!nr_s33] run tag @s add nr_s33
execute if score #c95 naranja matches 1 as @e[type=guardian,tag=!nr_s95] run attribute @s minecraft:generic.movement_speed base set 0.4
execute if score #c95 naranja matches 1 as @e[type=guardian,tag=!nr_s95] run tag @s add nr_s95
execute if score #c100 naranja matches 1 as @e[type=enderman,tag=!nr_s100] run attribute @s minecraft:generic.movement_speed base set 0.4
execute if score #c100 naranja matches 1 as @e[type=enderman,tag=!nr_s100] run tag @s add nr_s100
execute if score #c39 naranja matches 1 as @e[type=#naranja:hostiles,tag=!nr_fr] run attribute @s minecraft:generic.follow_range base set 48
execute if score #c39 naranja matches 1 as @e[type=#naranja:hostiles,tag=!nr_fr] run tag @s add nr_fr

# effects
execute if score #c4 naranja matches 1 as @e[type=zombie] run effect give @s strength 6 0 true
execute if score #c36 naranja matches 1 as @e[type=#naranja:hostiles] run effect give @s strength 6 0 true
execute if score #c37 naranja matches 1 as @e[type=#naranja:hostiles] run effect give @s speed 6 0 true
execute if score #c38 naranja matches 1 as @e[type=#naranja:hostiles] run effect give @s resistance 6 0 true
execute if score #c10 naranja matches 1 as @e[type=cave_spider] run effect give @s strength 6 0 true
execute if score #c12 naranja matches 1 as @a[gamemode=survival] at @s if entity @e[type=phantom,distance=..4] run effect give @s poison 4 0 true
execute if score #c12 naranja matches 1 as @a[gamemode=adventure] at @s if entity @e[type=phantom,distance=..4] run effect give @s poison 4 0 true
execute if score #c94 naranja matches 1 as @e[type=witch] run effect give @s regeneration 6 0 true
execute if score #c97 naranja matches 1 as @e[type=hoglin] at @s if entity @a[distance=..12] run effect give @s strength 6 0 true
execute if score #c98 naranja matches 1 as @e[type=phantom] at @s if entity @e[type=phantom,distance=0.1..8] run effect give @s speed 6 0 true
execute if score #c93 naranja matches 1 as @e[type=spider] at @s if entity @a[distance=..8] run effect give @s invisibility 3 0 true
execute if score #c100 naranja matches 1 as @e[type=enderman] at @s if entity @a[distance=..6] run effect give @s speed 4 1 true
execute if score #c53 naranja matches 1 as @e[type=blaze] run effect give @s regeneration 6 0 true
execute if score #c54 naranja matches 1 as @e[type=enderman] run effect give @s resistance 6 0 true
execute if score #c56 naranja matches 1 as @e[type=skeleton] run effect give @s regeneration 6 0 true
execute if score #c57 naranja matches 1 as @e[type=spider] run effect give @s jump_boost 6 1 true
execute if score #c43 naranja matches 1 as @e[type=zombie] at @s if block ~ ~-1 ~ iron_ore run effect give @s speed 6 0 true
execute if score #c46 naranja matches 1 as @e[type=drowned] at @s if block ~ ~ ~ water run effect give @s speed 6 0 true
execute if score #c47 naranja matches 1 as @e[type=witch] at @s if entity @a[distance=..10] run effect give @s resistance 6 0 true
execute if score #c48 naranja matches 1 as @e[type=creeper] at @s if block ~ ~-1 ~ redstone_ore run data merge entity @s {powered:1b}
execute if score #c49 naranja matches 1 as @e[type=pillager] run effect give @s speed 6 0 true
execute if score #c49 naranja matches 1 as @e[type=vindicator] run effect give @s speed 6 0 true
execute if score #c50 naranja matches 1 as @e[type=blaze] run effect give @s speed 6 0 true
execute if score #c58 naranja matches 1 as @e[type=zombie] at @s if block ~ ~-1 ~ farmland run effect give @s strength 6 0 true
execute if score #c59 naranja matches 1 as @e[type=drowned] at @s if entity @a[distance=..8] run effect give @s strength 6 0 true
execute if score #c60 naranja matches 1 as @e[type=piglin_brute] at @s if entity @a[distance=..12] run effect give @s strength 6 0 true
execute if score #c45 naranja matches 1 as @a[gamemode=survival,nbt={SelectedItem:{id:"minecraft:diamond"}}] at @s if entity @e[type=#naranja:hostiles,distance=..16] run effect give @s glowing 3 0 true

# spider webs
execute if score #c9 naranja matches 1 as @e[type=spider] at @s if entity @a[distance=..3,gamemode=survival] run fill ~ ~ ~ ~ ~ ~ cobweb replace air
execute if score #c79 naranja matches 1 as @a[gamemode=survival] at @s if entity @e[type=spider,distance=..4] run fill ~ ~ ~ ~ ~ ~ cobweb replace air

# animal caps
execute if score #c65 naranja matches 1 as @a at @s store result score #tmp naranja run execute if entity @e[type=cow,distance=..48]
execute if score #c65 naranja matches 1 if score #tmp naranja matches 20.. as @a at @s run kill @e[type=cow,distance=..48,sort=random,limit=2]
execute if score #c66 naranja matches 1 as @a at @s store result score #tmp naranja run execute if entity @e[type=pig,distance=..48]
execute if score #c66 naranja matches 1 if score #tmp naranja matches 20.. as @a at @s run kill @e[type=pig,distance=..48,sort=random,limit=2]
execute if score #c67 naranja matches 1 as @a at @s store result score #tmp naranja run execute if entity @e[type=sheep,distance=..48]
execute if score #c67 naranja matches 1 if score #tmp naranja matches 20.. as @a at @s run kill @e[type=sheep,distance=..48,sort=random,limit=2]
execute if score #c68 naranja matches 1 as @a at @s store result score #tmp naranja run execute if entity @e[type=chicken,distance=..48]
execute if score #c68 naranja matches 1 if score #tmp naranja matches 25.. as @a at @s run kill @e[type=chicken,distance=..48,sort=random,limit=3]

# baby villagers
execute if score #c64 naranja matches 1 as @e[type=villager,nbt={Age:-24000}] run kill @s
execute if score #c64 naranja matches 1 as @e[type=villager] store result score #age naranja run data get entity @s Age
execute if score #c64 naranja matches 1 as @e[type=villager] if score #age naranja matches ..-1 run kill @s

# iron golem unnamed
execute if score #c70 naranja matches 1 as @e[type=iron_golem] unless data entity @s CustomName run damage @s 2 minecraft:generic

# skeletons arrows
execute if score #c90 naranja matches 1 as @e[type=skeleton] run data modify entity @s HandItems[1] set value {id:"minecraft:arrow",Count:64b}
