"""Genera 13 capas provisionales únicamente con --generar.
Salida: assets-fuente/provisionales; nunca escribe en los recursos de los mods.
Requiere Pillow solo para generar. No sobrescribe archivos existentes.
"""
from pathlib import Path
import argparse
import math

S=512
C=(256,256)
OUT=Path(__file__).resolve().parent/'provisionales'

COLORS=['rojo','rosa','morado','azul','turquesa','verde','amarillo','naranja']
PALETTE=[(232,50,51),(240,82,175),(129,57,206),(39,128,228),(34,198,206),(55,183,82),(255,210,53),(239,140,39)]
GOLD=(255,216,117,255); DARK=(84,43,11,255)

def fresh():return Image.new('RGBA',(S,S),(0,0,0,0))
def polar(rad,ang):
    ar=math.radians(ang)
    return (C[0]+rad*math.cos(ar),C[1]+rad*math.sin(ar))
def poly_sector(rad0,rad1,a0,a1):
    return [polar(rad0,a0)]+[polar(rad1,a0+(a1-a0)*k/48) for k in range(49)]+[polar(rad0,a1)]
def badge(img,icon,x,y,s):
    """Dibujo estilizado y básico de cada símbolo, independiente del sector."""
    d=ImageDraw.Draw(img)
    line=(255,235,155,255); shade=(89,46,14,255)
    # medallón semitransparente de cada símbolo
    d.ellipse([x-s,y-s,x+s,y+s],fill=(85,51,33,118),outline=line,width=2)
    if icon==0: # montaña
        d.polygon([(x-s*.7,y+s*.45),(x-s*.2,y-s*.45),(x+s*.12,y+s*.16),(x+s*.32,y-s*.14),(x+s*.75,y+s*.45)],fill=line)
        d.line([(x-s*.7,y+s*.50),(x+s*.75,y+s*.50)],fill=shade,width=3)
    elif icon==1: # corazón
        d.ellipse([x-s*.55,y-s*.3,x,y+s*.23],fill=line)
        d.ellipse([x-s*.02,y-s*.3,x+s*.52,y+s*.23],fill=line)
        d.polygon([(x-s*.57,y),(x+s*.54,y),(x,y+s*.67)],fill=line)
    elif icon==2: # luna
        d.ellipse([x-s*.52,y-s*.55,x+s*.56,y+s*.56],fill=line)
        d.ellipse([x-s*.16,y-s*.77,x+s*.84,y+s*.27],fill=(77,37,100,255))
        d.ellipse([x+s*.36,y-s*.52,x+s*.49,y-s*.39],fill=line)
    elif icon==3: # gota
        d.polygon([(x,y-s*.75),(x-s*.5,y+s*.08),(x-s*.5,y+s*.37),(x-s*.2,y+s*.66),(x+s*.2,y+s*.66),(x+s*.5,y+s*.37),(x+s*.5,y+s*.08)],fill=line)
        d.ellipse([x-s*.5,y,x+s*.5,y+s*.7],fill=line)
    elif icon==4: # viento
        for off in [-.34,0,.34]:
            d.arc([x-s*.62,y+s*off-s*.18,x+s*.6,y+s*off+s*.4],12,190,fill=line,width=4)
    elif icon==5: # hoja
        d.ellipse([x-s*.52,y-s*.52,x+s*.52,y+s*.48],fill=line)
        d.polygon([(x-s*.48,y+s*.4),(x+s*.5,y-s*.6),(x+s*.23,y+s*.4)],fill=(59,132,52,255))
        d.line([(x-s*.5,y+s*.5),(x+s*.46,y-s*.51)],fill=line,width=3)
    elif icon==6: # sol
        d.ellipse([x-s*.30,y-s*.30,x+s*.30,y+s*.30],fill=line)
        for k in range(8):
            ar=k*math.pi/4
            d.line([(x+math.cos(ar)*s*.42,y+math.sin(ar)*s*.42),(x+math.cos(ar)*s*.68,y+math.sin(ar)*s*.68)],fill=line,width=4)
    else: # fuego
        d.polygon([(x-s*.54,y+s*.52),(x-s*.49,y-s*.08),(x-s*.17,y-s*.48),(x-s*.06,y-s*.02),(x+s*.24,y-s*.72),(x+s*.57,y+s*.35),(x+s*.29,y+s*.6)],fill=line)
        d.polygon([(x-s*.18,y+s*.52),(x+s*.04,y-s*.07),(x+s*.29,y+s*.52)],fill=(242,107,28,255))


def save_new(image, path):
    # Modo exclusivo: incluso ante una creación concurrente, no reemplaza archivos.
    with path.open('xb') as stream:
        image.save(stream, format='PNG', optimize=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generar', action='store_true',
                        help='Crear las 13 capas de prueba en assets-fuente/provisionales.')
    args = parser.parse_args()
    if not args.generar:
        parser.print_help()
        return
    names = [f'sector_{color}.png' for color in COLORS]
    names += ['ring.png', 'medallion.png', 'pointer.png', 'glow.png', 'flash.png']
    existing = [name for name in names if (OUT / name).exists()]
    if existing:
        parser.error('No se sobrescribe ningún archivo. Ya existen: ' + ', '.join(existing))
    global Image, ImageDraw
    from PIL import Image, ImageDraw
    OUT.mkdir(parents=True, exist_ok=True)
    # cada capa por segmento: centrados en la misma textura
    for i,(name,col) in enumerate(zip(COLORS,PALETTE)):
        img=fresh();d=ImageDraw.Draw(img)
        a0=-90+i*45;a1=a0+45
        d.polygon(poly_sector(22,169,a0,a1),fill=(*col,255))
        # bordes y ornamentos interiores (siguen la rotación)
        d.polygon(poly_sector(152,169,a0,a1),fill=(*tuple(max(20,int(v*.69)) for v in col),255))
        d.line([polar(26,a0),polar(173,a0)],fill=(255,226,146,255),width=4)
        d.line([polar(26,a1),polar(173,a1)],fill=(255,226,146,255),width=4)
        d.arc([C[0]-148,C[1]-148,C[0]+148,C[1]+148],a0,a1,fill=(255,245,180,180),width=3)
        d.arc([C[0]-160,C[1]-160,C[0]+160,C[1]+160],a0,a1,fill=(104,63,22,255),width=3)
        # filigrana fina
        for frac in (.23,.77):
            ar=a0+(a1-a0)*frac
            p=polar(161,ar)
            d.ellipse([p[0]-2,p[1]-2,p[0]+2,p[1]+2],fill=GOLD)
        x,y=polar(112,(a0+a1)/2)
        badge(img,i,int(x),int(y),22)
        save_new(img, OUT/f'sector_{name}.png')

    # Aro ornamental (sólo oro; sin colores de sectores ni puntero)
    ring=fresh();d=ImageDraw.Draw(ring)
    for radius,width,color in [(193,8,(94,48,16,230)),(187,8,(243,181,72,255)),(182,3,(255,239,151,255)),(176,7,(122,73,22,255)),(170,3,(247,202,100,255))]:
        d.ellipse([256-radius,256-radius,256+radius,256+radius],outline=color,width=width)
    for k in range(32):
        ang=k*360/32-90
        for r in (178,190):
            p=polar(r,ang);sz=4 if k%4==0 else 2
            d.ellipse([p[0]-sz,p[1]-sz,p[0]+sz,p[1]+sz],fill=(255,228,131,255))
    for k in range(8):
        ang=-90+k*45
        p=polar(182,ang)
        d.regular_polygon((p[0],p[1],10),4,rotation=ang+45,fill=(255,208,102,255),outline=(96,53,20,255))
    save_new(ring, OUT/'ring.png')

    # medallón solar con 8 rayos; toda su geometría depende del centro del disco
    mid=fresh(); d=ImageDraw.Draw(mid)
    for k in range(8):
        a=-90+45*k
        d.polygon([polar(29,a-11),polar(53,a),polar(29,a+11)],fill=(200,133,40,255),outline=(255,224,126,255))
    for r,w,c in [(42,7,(126,74,22,255)),(36,5,(255,212,109,255)),(27,5,(103,58,18,255)),(20,3,(255,237,148,255))]:
        d.ellipse([256-r,256-r,256+r,256+r],fill=None,outline=c,width=w)
    d.regular_polygon((256,256,17),8,rotation=22.5,fill=(243,181,69,255),outline=(255,240,168,255))
    d.regular_polygon((256,256,10),4,rotation=45,fill=(101,57,23,255),outline=(255,225,124,255))
    save_new(mid, OUT/'medallion.png')

    # Puntero NUEVO dibujado separadamente. No se recorta de la ruleta, no existe amarillo "pegado".
    p=fresh();d=ImageDraw.Draw(p)
    # sombras del cuerpo y alas de soporte
    d.polygon([(10,243),(18,233),(25,231),(30,218),(41,215),(52,224),(68,235),(92,247),(113,256),(92,265),(68,277),(52,288),(41,297),(30,294),(25,281),(18,279),(10,269)],fill=(89,47,16,255),outline=(250,202,98,255),width=3)
    d.polygon([(22,249),(41,229),(61,238),(78,249),(99,256),(78,263),(61,274),(41,283),(22,263)],fill=(198,126,37,255),outline=(255,228,135,255),width=3)
    # biseles que apuntan al extremo sin cortarlo
    d.line([(48,237),(75,250),(101,256)],fill=(255,240,167,255),width=3)
    d.line([(48,275),(75,262),(101,256)],fill=(255,227,123,255),width=3)
    # punta íntegra, fina y orgánicamente unida al cuerpo
    # Joya iridiscente redonda sobre el cuerpo
    for rad,color in [(34,(71,44,18,255)),(31,(250,211,108,255)),(27,(75,47,30,255))]:
        d.ellipse([47-rad,256-rad,47+rad,256+rad],fill=color)
    # cortes suaves de color de gema (pertenecen a la gema, no al segmento)
    for j,color in enumerate([(245,81,158,255),(131,69,239,255),(35,155,255,255),(73,232,206,255),(249,204,79,255)]):
        start=198+j*33
        d.pieslice([23,232,71,280],start,start+33,fill=color)
    d.ellipse([21,230,73,282],outline=(255,233,139,255),width=4)
    d.ellipse([30,236,45,250],fill=(255,255,248,170))
    save_new(p, OUT/'pointer.png')

    # sprite de halo radiante y máscara de destello
    for name,sigma,max_alpha in [('glow',180,112),('flash',127,240)]:
        im=fresh();pixels=im.load()
        for yy in range(S):
            for xx in range(S):
                rad=math.hypot(xx-256,yy-256)
                if name=='flash':
                    a=max_alpha*math.exp(-((rad-190)/16)**2) if 169 < rad < 249 else 0
                else:
                    a=max_alpha*math.exp(-((rad-164)/sigma)**2)*max(0,1-(rad/249)**4)
                pixels[xx,yy]=(255,255,255,int(max(0,min(255,a))))
        save_new(im, OUT/f'{name}.png')


    print('TEXTURAS PROVISIONALES CREADAS: 13 en', OUT)


if __name__ == '__main__':
    main()
