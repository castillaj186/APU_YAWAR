# Cancela solo el giro/aterrizaje, NO modifica eventos activos ni dias.
scoreboard players set #animando safio.anim 0
scoreboard players set #fase_anim safio.anim 0
scoreboard players set #restantes safio.anim 0
scoreboard players set #ticks_anim safio.anim 0
title @a clear
tellraw @a {"text":"[SAFIO] Giro cancelado por administracion.","color":"yellow"}

scoreboard players set #color_bloqueado safio.ruleta 0
scoreboard players set #hold safio.anim 0
scoreboard players set #hold_ticks safio.anim 0
