function naranja:count_remaining
execute if score #remaining naranja matches 0 run tellraw @a ["",{"text":"● NARANJA ","color":"gold","bold":true},{"text":"No quedan cambios por activar.","color":"#FFD588"}]
execute if score #remaining naranja matches 1.. run function naranja:rng
execute if score #remaining naranja matches 1.. run scoreboard players operation #pick naranja = #rand naranja_rand
execute if score #pick naranja matches ..-1 run scoreboard players operation #pick naranja *= #minus_one naranja_rand
execute if score #remaining naranja matches 1.. run scoreboard players operation #pick naranja %= #remaining naranja
execute if score #remaining naranja matches 1.. run scoreboard players add #pick naranja 1
execute unless score #c1 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_1
execute unless score #c1 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c2 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_2
execute unless score #c2 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c3 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_3
execute unless score #c3 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c4 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_4
execute unless score #c4 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c5 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_5
execute unless score #c5 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c6 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_6
execute unless score #c6 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c7 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_7
execute unless score #c7 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c8 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_8
execute unless score #c8 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c9 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_9
execute unless score #c9 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c10 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_10
execute unless score #c10 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c11 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_11
execute unless score #c11 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c12 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_12
execute unless score #c12 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c13 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_13
execute unless score #c13 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c14 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_14
execute unless score #c14 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c15 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_15
execute unless score #c15 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c16 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_16
execute unless score #c16 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c17 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_17
execute unless score #c17 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c18 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_18
execute unless score #c18 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c19 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_19
execute unless score #c19 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c20 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_20
execute unless score #c20 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c21 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_21
execute unless score #c21 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c22 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_22
execute unless score #c22 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c23 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_23
execute unless score #c23 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c24 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_24
execute unless score #c24 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c25 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_25
execute unless score #c25 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c26 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_26
execute unless score #c26 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c27 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_27
execute unless score #c27 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c28 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_28
execute unless score #c28 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c29 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_29
execute unless score #c29 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c30 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_30
execute unless score #c30 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c31 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_31
execute unless score #c31 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c32 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_32
execute unless score #c32 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c33 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_33
execute unless score #c33 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c34 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_34
execute unless score #c34 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c35 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_35
execute unless score #c35 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c36 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_36
execute unless score #c36 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c37 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_37
execute unless score #c37 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c38 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_38
execute unless score #c38 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c39 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_39
execute unless score #c39 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c40 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_40
execute unless score #c40 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c41 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_41
execute unless score #c41 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c42 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_42
execute unless score #c42 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c43 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_43
execute unless score #c43 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c44 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_44
execute unless score #c44 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c45 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_45
execute unless score #c45 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c46 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_46
execute unless score #c46 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c47 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_47
execute unless score #c47 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c48 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_48
execute unless score #c48 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c49 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_49
execute unless score #c49 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c50 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_50
execute unless score #c50 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c51 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_51
execute unless score #c51 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c52 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_52
execute unless score #c52 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c53 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_53
execute unless score #c53 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c54 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_54
execute unless score #c54 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c55 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_55
execute unless score #c55 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c56 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_56
execute unless score #c56 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c57 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_57
execute unless score #c57 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c58 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_58
execute unless score #c58 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c59 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_59
execute unless score #c59 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c60 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_60
execute unless score #c60 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c61 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_61
execute unless score #c61 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c62 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_62
execute unless score #c62 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c63 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_63
execute unless score #c63 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c64 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_64
execute unless score #c64 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c65 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_65
execute unless score #c65 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c66 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_66
execute unless score #c66 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c67 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_67
execute unless score #c67 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c68 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_68
execute unless score #c68 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c69 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_69
execute unless score #c69 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c70 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_70
execute unless score #c70 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c71 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_71
execute unless score #c71 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c72 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_72
execute unless score #c72 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c73 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_73
execute unless score #c73 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c74 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_74
execute unless score #c74 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c75 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_75
execute unless score #c75 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c76 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_76
execute unless score #c76 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c77 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_77
execute unless score #c77 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c78 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_78
execute unless score #c78 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c79 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_79
execute unless score #c79 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c80 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_80
execute unless score #c80 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c81 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_81
execute unless score #c81 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c82 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_82
execute unless score #c82 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c83 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_83
execute unless score #c83 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c84 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_84
execute unless score #c84 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c85 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_85
execute unless score #c85 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c86 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_86
execute unless score #c86 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c87 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_87
execute unless score #c87 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c88 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_88
execute unless score #c88 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c89 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_89
execute unless score #c89 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c90 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_90
execute unless score #c90 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c91 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_91
execute unless score #c91 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c92 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_92
execute unless score #c92 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c93 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_93
execute unless score #c93 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c94 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_94
execute unless score #c94 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c95 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_95
execute unless score #c95 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c96 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_96
execute unless score #c96 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c97 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_97
execute unless score #c97 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c98 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_98
execute unless score #c98 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c99 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_99
execute unless score #c99 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
execute unless score #c100 naranja matches 1 if score #pick naranja matches 1 run function naranja:unlock_100
execute unless score #c100 naranja matches 1 if score #pick naranja matches 2.. run scoreboard players remove #pick naranja 1
