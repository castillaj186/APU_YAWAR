using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;
public static class ValidarMaestros {
    static readonly string[] Colors={"rojo","rosa","morado","azul","turquesa","verde","amarillo","naranja"};
    public static string Run(string root) {
        if(Directory.GetFiles(root,"*.png").Length!=13)throw new Exception("Se requieren solo 13 maestros");
        var names=new string[13];for(int i=0;i<8;i++)names[i]="sector_"+Colors[i];
        names[8]="ring";names[9]="medallion";names[10]="pointer";names[11]="glow";names[12]="flash";
        using(var reference=new Bitmap(Path.Combine(root,"sector_rojo.png"))) {
            for(int k=0;k<13;k++)using(var b=new Bitmap(Path.Combine(root,names[k]+".png"))) {
                if(b.Width!=512||b.Height!=512||b.PixelFormat!=PixelFormat.Format32bppArgb)throw new Exception("Formato: "+names[k]);
                if(b.GetPixel(0,0).A!=0)throw new Exception("Fondo opaco");
                for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
                    Color p=b.GetPixel(x,y);double dx=x+.5-256,dy=y+.5-256,r=Math.Sqrt(dx*dx+dy*dy);
                    if(k<8) {
                        if(p.A!=reference.GetPixel(x,y).A)throw new Exception("Máscara distinta: "+names[k]);
                        double a=Math.Atan2(dy,dx)*180/Math.PI,tolerance=180/Math.PI/Math.Max(1,r);
                        if(p.A>0&&(r<21||r>170||a< -90-tolerance||a> -45+tolerance))throw new Exception("Sector fuera de geometría");
                        if(r>23&&r<168&&a> -89&&a< -46&&p.A<250)throw new Exception("Hueco interior");
                    }
                    if(k>=11) {
                        if(p.A>0&&(p.R!=255||p.G!=255||p.B!=255))throw new Exception("Efecto no neutro");
                        if(r<168&&p.A>0)throw new Exception("Efecto cubre símbolos");
                    }
                }
                if(k<8)for(int slot=0;slot<8;slot++)using(var rotated=new Bitmap(512,512))using(var g=Graphics.FromImage(rotated)) {
                    g.TranslateTransform(256,256);g.RotateTransform(slot*45);g.DrawImage(b,-256,-256,512,512);
                    double angle=(-67.5+slot*45)*Math.PI/180;
                    if(rotated.GetPixel((int)(256+120*Math.Cos(angle)),(int)(256+120*Math.Sin(angle))).A<240)throw new Exception("Posición incorrecta: "+names[k]+"/"+slot);
                }
                if(k==8&&b.GetPixel(256,256).A!=0)throw new Exception("Aro opaco en el centro");
                if(k==10&&b.GetPixel(99,256).A<100)throw new Exception("Puntero sin punta");
            }
        }
        return "OK: 13 maestros RGBA 512x512, máscara común exacta, 64 combinaciones color/posición por rotación, efectos neutros sin cubrir símbolos y punta presente.";
    }
}
