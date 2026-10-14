# Selecciona que familia de frames se renderiza en este tick.
execute if score #fase_anim safio.anim matches 1 run function safio:ruleta/render/entrada_pro
execute if score #fase_anim safio.anim matches 2 if score #restantes safio.anim matches 121.. run function safio:ruleta/render/giro_rapido_pro
execute if score #fase_anim safio.anim matches 2 unless score #restantes safio.anim matches 121.. run function safio:ruleta/render/giro_normal_pro
execute if score #fase_anim safio.anim matches 3 run function safio:ruleta/render/resultado_pro
execute if score #fase_anim safio.anim matches 4 run function safio:ruleta/render/salida_pro
