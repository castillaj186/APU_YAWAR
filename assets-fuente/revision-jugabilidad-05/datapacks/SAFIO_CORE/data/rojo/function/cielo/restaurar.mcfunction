# Vuelve el ciclo de dia y la hora aproximada (redondeada a la hora de 1000 ticks mas cercana).
scoreboard players set #sky_on rj_k 0
gamerule doDaylightCycle true
execute if score #dt0 rj_k matches 0..999 run time set 500
execute if score #dt0 rj_k matches 1000..1999 run time set 1500
execute if score #dt0 rj_k matches 2000..2999 run time set 2500
execute if score #dt0 rj_k matches 3000..3999 run time set 3500
execute if score #dt0 rj_k matches 4000..4999 run time set 4500
execute if score #dt0 rj_k matches 5000..5999 run time set 5500
execute if score #dt0 rj_k matches 6000..6999 run time set 6500
execute if score #dt0 rj_k matches 7000..7999 run time set 7500
execute if score #dt0 rj_k matches 8000..8999 run time set 8500
execute if score #dt0 rj_k matches 9000..9999 run time set 9500
execute if score #dt0 rj_k matches 10000..10999 run time set 10500
execute if score #dt0 rj_k matches 11000..11999 run time set 11500
execute if score #dt0 rj_k matches 12000..12999 run time set 12500
execute if score #dt0 rj_k matches 13000..13999 run time set 13500
execute if score #dt0 rj_k matches 14000..14999 run time set 14500
execute if score #dt0 rj_k matches 15000..15999 run time set 15500
execute if score #dt0 rj_k matches 16000..16999 run time set 16500
execute if score #dt0 rj_k matches 17000..17999 run time set 17500
execute if score #dt0 rj_k matches 18000..18999 run time set 18500
execute if score #dt0 rj_k matches 19000..19999 run time set 19500
execute if score #dt0 rj_k matches 20000..20999 run time set 20500
execute if score #dt0 rj_k matches 21000..21999 run time set 21500
execute if score #dt0 rj_k matches 22000..22999 run time set 22500
execute if score #dt0 rj_k matches 23000..23999 run time set 23500

# Restaurar colores predeterminados de Colorful Skies al terminar ROJO.
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sky clear
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sun clear
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a moon clear
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a cloud clear
# Requiere ColorfulSkies (no instalado en SAFIO Fabric 1.21.1): colorfulskies color @a sunrise clear
