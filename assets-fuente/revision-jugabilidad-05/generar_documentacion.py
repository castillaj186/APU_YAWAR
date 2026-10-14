from pathlib import Path
import json
root=Path(__file__).resolve().parents[2]
data=root/'apuyawar-systems/src/main/resources/data/apuyawar_systems/catalogs'
docs=root/'docs'
red=json.loads((data/'challenges.json').read_text(encoding='utf-8'))
orange=json.loads((data/'difficulties.json').read_text(encoding='utf-8'))
def cell(value):return str(value).replace('|','/').replace('\n',' ')
lines=['# Catálogo rojo — 128 entradas','', '12 retos Java y 116 variantes del datapack. Consulte 04_JUICIOS_ROJO_NARANJA.md para las adaptaciones y restricciones.', '', '| ID | Nombre | Objetivo | Segundos | Alcance | Momento de evaluación |','|---|---|---|---:|---|---|']
for c in red:lines.append('| '+' | '.join(map(cell,[c['id'],c['name'],c['description'],c['duration']/20,c['scope'],'al finalizar' if c['atDeadline'] else 'continuo; todos deben cumplir']))+' |')
(docs/'05_CATALOGO_ROJO.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
lines=['# Catálogo naranja — 135 entradas','','35 dificultades Java y 100 variantes/adaptaciones del datapack. Todas duran difficultySeconds; predeterminado 1200 segundos. Una sola dificultad activa.','', '| ID | Nombre | Efecto ejecutado | Origen |','|---|---|---|---|']
for d in orange:lines.append('| '+' | '.join(map(cell,[d['id'],d['name'],d['description'],'Java' if d['legacy']<=35 else 'datapack / adaptación']))+' |')
(docs/'06_CATALOGO_NARANJA.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
modified=['apuyawar-systems/build.gradle','apuyawar-systems/src/main/java/pe/apuyawar/systems/ApuYawarSystems.java','apuyawar-systems/src/main/java/pe/apuyawar/systems/network/RouletteStartPayload.java','apuyawar-systems/src/main/java/pe/apuyawar/systems/client/ApuYawarClient.java','apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteClientState.java']
new=[]
base=root/'apuyawar-systems/src/main/java/pe/apuyawar/systems'
for pkg in ['events','config','challenges','difficulty','persistence']:new += [str(p.relative_to(root)).replace('\\','/') for p in (base/pkg).glob('*.java')]
new += ['apuyawar-systems/src/main/java/pe/apuyawar/systems/network/RouletteFinishedPayload.java','apuyawar-systems/src/test/java/pe/apuyawar/systems/EventVerification.java','apuyawar-systems/src/gametest/java/pe/apuyawar/systems/JudgmentGameTests.java','apuyawar-systems/src/gametest/resources/fabric.mod.json','apuyawar-systems/src/main/resources/data/apuyawar_systems/catalogs/challenges.json','apuyawar-systems/src/main/resources/data/apuyawar_systems/catalogs/difficulties.json']
new += ['docs/'+name for name in ['04_JUICIOS_ROJO_NARANJA.md','05_CATALOGO_ROJO.md','06_CATALOGO_NARANJA.md','07_ARCHIVOS_JUICIOS.md']]
stage=Path(__file__).resolve().parent
technical=[str(p.relative_to(root)).replace('\\','/') for p in stage.rglob('*') if p.is_file()]
lines=['# Archivos de implementación','','## Modificados','']+['- '+p for p in modified]+['','## Nuevos de producción, pruebas y documentación','']+['- '+p for p in sorted(new)]+['','## Análisis técnico nuevo (copias históricas; no se integran como datapacks)','']+['- '+p for p in sorted(technical)]+['','Los archivos generados por Gradle/GameTest permanecen bajo build, .gradle o configuraciones de ejecución. No se distribuyen ni instalan. Las clases de tests y sus metadatos quedan fuera del JAR de producción.']
(docs/'07_ARCHIVOS_JUICIOS.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('Documentación y manifiesto exacto de archivos generados.')
