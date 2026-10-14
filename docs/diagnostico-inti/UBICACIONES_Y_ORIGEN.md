# Ubicaciones y origen de recursos de Inti

## Originales inalterados

`E:\APU_YAWAR\assets-fuente\inti-juicio\frames-originales\frame_001.png` a `frame_199.png`

## Atlas histórico (ya no es el recurso que usa el renderer activo)

`E:\APU_YAWAR\assets-fuente\inti-juicio\revision-int-01\inti_atlas.png`

Se generó con FFmpeg a partir de los originales, escalando cada PNG a 256×256 y aplicando una cuadrícula `tile=16x13`, con fondo transparente. No existía un archivo de script persistente para esa operación; la orden utilizada quedó documentada en `DIAGNOSTICO_COMPLETO_INTI.md`.

## Recursos activos actuales

`E:\APU_YAWAR\apuyawar-systems\src\main\resources\assets\apuyawar_systems\textures\gui\inti\frames\frame_001.png` a `frame_199.png`

Son copias reducidas a 512×512 con alfa RGBA. El renderer activo usa estos archivos individuales y no referencia el atlas histórico.
