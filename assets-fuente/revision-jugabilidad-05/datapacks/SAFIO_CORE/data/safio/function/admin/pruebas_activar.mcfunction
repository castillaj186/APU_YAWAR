# SOLO ADMIN: permite ganar recompensas fuera de 16:00-23:00 mientras haces pruebas.
scoreboard players set #modo_prueba safio.estado 1
tellraw @a {"text":"[SAFIO] PRUEBAS ACTIVADAS: se permiten premios fuera del horario. Desactivarlas despues.","color":"yellow"}
