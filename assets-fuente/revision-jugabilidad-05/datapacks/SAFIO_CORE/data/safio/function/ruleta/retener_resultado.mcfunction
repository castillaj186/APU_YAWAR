# El fotograma 375 existe en los ocho colores y confirma el resultado final.
# Protege de que el ultimo fotograma blanco parezca otro color.
scoreboard players set #hold safio.anim 1
scoreboard players set #hold_ticks safio.anim 0
scoreboard players set #media_frame safio.anim 375
tellraw @a [{"text":"[SAFIO] RESULTADO: ","color":"gold"},{"score":{"name":"#resultado","objective":"safio.ruleta"},"color":"yellow"}]
