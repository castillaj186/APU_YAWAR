# Puente SAFIO 1.6.9: mantiene el motor de los 100 cambios del original.
# La activacion ahora la realiza el servidor Java al finalizar el giro naranja;
# rv_k de otro servidor no existe y causaba errores de scoreboard.
scoreboard players add #tick_count naranja 1
function naranja:aplicar
schedule function naranja:loop 1t replace
