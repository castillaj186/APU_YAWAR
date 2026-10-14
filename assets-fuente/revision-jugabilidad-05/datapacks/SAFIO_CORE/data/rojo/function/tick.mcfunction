# ROJO usa un tick programado propio para no chocar con bestiario_v12.
execute if score #activo rj_k matches 1 run function rojo:run
execute unless score #activo rj_k matches 1 as @a[tag=rj_uc] run function rojo:uc_quitar
execute if score #activo rj_k matches 1 run schedule function rojo:tick 1t replace
