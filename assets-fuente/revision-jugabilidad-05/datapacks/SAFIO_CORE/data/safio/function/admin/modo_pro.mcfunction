execute if score #animando safio.anim matches 1 run tellraw @a {"text":"[SAFIO] Espera a que el giro termine para cambiar la interfaz.","color":"red"}
execute unless score #animando safio.anim matches 1 run scoreboard players set #visual safio.config 1
execute unless score #animando safio.anim matches 1 run tellraw @a {"text":"[SAFIO] Modo PRO activado.","color":"yellow"}
