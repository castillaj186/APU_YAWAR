# CIELO ROJO: se activa cuando comienza el evento ROJO.
# Mantiene el atardecer existente y aplica colores mediante Colorful Skies.
# La advertencia del borde crea una vineta roja muy suave en los bordes de pantalla;
# no cambia el tamaño ni el centro del borde del mundo.
execute unless score #sky_on rj_k matches 1 run function rojo:cielo/guardar
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sky FF0000
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sun FF0000
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a moon FF0000
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a cloud FF0000
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sunrise FF0000
worldborder warning distance 30000000
execute as @a at @s run playsound minecraft:ambient.nether_wastes.mood master @s ~ ~ ~ 0.7 0.6
