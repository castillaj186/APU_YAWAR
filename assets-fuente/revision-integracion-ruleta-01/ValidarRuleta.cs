using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;

public static class ValidarRuleta {
    static readonly string[] Colors={"rojo","rosa","morado","azul","turquesa","verde","amarillo","naranja"};
    public static string Run(string dir) {
        var names=new string[13];for(int i=0;i<8;i++)names[i]="sector_"+Colors[i];
        names[8]="ring";names[9]="medallion";names[10]="pointer";names[11]="glow";names[12]="flash";
        foreach(var name in names)using(var b=new Bitmap(Path.Combine(dir,name+".png"))) {
            if(b.Width!=512||b.Height!=512||b.PixelFormat!=PixelFormat.Format32bppArgb)throw new Exception("Formato incorrecto: "+name);
            if(b.GetPixel(0,0).A!=0)throw new Exception("Fondo no transparente: "+name);
        }
        var sectors=new Bitmap[8];int violations=0,gaps=0;
        try {
            for(int i=0;i<8;i++)sectors[i]=new Bitmap(Path.Combine(dir,names[i]+".png"));
            for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
                double dx=x+.5-256,dy=y+.5-256,r=Math.Sqrt(dx*dx+dy*dy);
                double a=(Math.Atan2(dy,dx)*180/Math.PI+90+720)%360;
                int sum=0;
                for(int i=0;i<8;i++) {
                    int alpha=sectors[i].GetPixel(x,y).A;sum+=alpha;
                    if(alpha>0) {
                        double relative=(a-i*45+360)%360;
                        // 1 px de tolerancia radial y angular para el antialias subpíxel.
                        double angleTolerance=180/Math.PI/Math.Max(1,r);
                        if(r<21||r>170||(relative>45+angleTolerance && relative<360-angleTolerance))violations++;
                    }
                }
                if(r>=23&&r<=168&&sum==0)gaps++;
            }
            for(int i=0;i<8;i++) {
                double sectorAngle=(-67.5+i*45)*Math.PI/180;
                int x=(int)(256+120*Math.Cos(sectorAngle)),y=(int)(256+120*Math.Sin(sectorAngle));
                if(sectors[i].GetPixel(x,y).A<250)throw new Exception("Sector sin cobertura en el centro: "+Colors[i]);
                double rotation=((180-(-67.5+i*45))%360+360)%360;
                double final=(((-67.5+i*45)+rotation)%360+360)%360;
                if(Math.Abs(final-180)>.001)throw new Exception("Parada incorrecta: "+Colors[i]);
            }
        } finally {foreach(var b in sectors)if(b!=null)b.Dispose();}
        foreach(var name in new[]{"glow","flash"})using(var b=new Bitmap(Path.Combine(dir,name+".png"))) {
            for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
                Color p=b.GetPixel(x,y);
                if(p.A>0 && (p.R!=255||p.G!=255||p.B!=255))throw new Exception("Máscara no blanca: "+name);
                double r=Math.Sqrt((x+.5-256)*(x+.5-256)+(y+.5-256)*(y+.5-256));
                if(r<168&&p.A>0)throw new Exception("Efecto cubre símbolos: "+name);
            }
        }
        using(var b=new Bitmap(Path.Combine(dir,"ring.png")))if(b.GetPixel(256,256).A!=0)throw new Exception("Centro del aro opaco");
        using(var b=new Bitmap(Path.Combine(dir,"pointer.png"))) {
            for(int n=0;n<512;n++)if(b.GetPixel(0,n).A>0||b.GetPixel(511,n).A>0||b.GetPixel(n,0).A>0||b.GetPixel(n,511).A>0)throw new Exception("Puntero cortado por el lienzo");
            if(b.GetPixel(99,256).A<100)throw new Exception("Punta del puntero ausente");
        }
        if(violations!=0||gaps!=0)throw new Exception("Geometría inválida: fuera="+violations+", huecos="+gaps);
        return "OK: 13 PNG RGBA 512x512; 8 sectores sin huecos interiores ni geometría fuera de tolerancia; 8 paradas alineadas a las 9:00; efectos blancos sin cobertura de símbolos; aro transparente; puntero sin recorte del lienzo y punta presente.";
    }
}
