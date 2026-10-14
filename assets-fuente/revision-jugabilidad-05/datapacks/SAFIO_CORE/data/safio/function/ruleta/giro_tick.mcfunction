# Giro con tres sensaciones: rapido, medio, lento. El frenado final hace el aterrizaje.
scoreboard players add #ticks_anim safio.anim 1
execute if score #restantes safio.anim matches 321.. if score #ticks_anim safio.anim matches 1.. run function safio:ruleta/salto_4
execute if score #restantes safio.anim matches 201..320 if score #ticks_anim safio.anim matches 1.. run function safio:ruleta/salto_3
execute if score #restantes safio.anim matches 121..200 if score #ticks_anim safio.anim matches 1.. run function safio:ruleta/salto_2
execute if score #restantes safio.anim matches 61..120 if score #ticks_anim safio.anim matches 1.. run function safio:ruleta/salto_1
execute if score #restantes safio.anim matches 31..60 if score #ticks_anim safio.anim matches 2.. run function safio:ruleta/salto_1
execute if score #restantes safio.anim matches 13..30 if score #ticks_anim safio.anim matches 3.. run function safio:ruleta/salto_1
execute if score #restantes safio.anim matches 5..12 if score #ticks_anim safio.anim matches 4.. run function safio:ruleta/salto_1
execute if score #restantes safio.anim matches 1..4 if score #ticks_anim safio.anim matches 5.. run function safio:ruleta/salto_1
