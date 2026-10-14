# Estado del puente externo para SAFIO 1.6.9. /reload no reinicia cambios.
scoreboard objectives add safio.addon dummy
scoreboard players set #ready safio.addon 1
scoreboard players add #instalado safio.addon 1
# Inicializar objetivos del catalogo de REVIIL sin iniciar ningun evento.
scoreboard objectives add rj_k dummy
scoreboard players add #activo rj_k 0
scoreboard players add #cuenta_on rj_k 0
