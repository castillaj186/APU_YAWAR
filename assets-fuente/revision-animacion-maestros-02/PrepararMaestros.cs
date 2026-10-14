using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;


// Preparación determinista del lienzo y vistas. Usa únicamente el arte aprobado, sin generación artística.
public static class PrepararMaestros {
    const int Size = 512;
    static readonly string[] Names = {"rojo","rosa","morado","azul","turquesa","verde","amarillo","naranja"};
    static readonly int[] Colors = {0xE83233,0xF052AF,0x8139CE,0x2780E4,0x22C6CE,0x37B752,0xFFD235,0xEF8C27};
    static double Clamp(double v,double lo,double hi) { return Math.Max(lo,Math.Min(hi,v)); }
    static double Smooth(double t) { t=Clamp(t,0,1);return t*t*(3-2*t); }
    static Color Sample(Bitmap b,double x,double y) {
        if(x<0||x>b.Width-1||y<0||y>b.Height-1)return Color.Transparent;
        int ix=(int)x,iy=(int)y;double fx=x-ix,fy=y-iy;
        double aa=0,rr=0,gg=0,bb=0;
        for(int j=0;j<2;j++)for(int i=0;i<2;i++) {
            Color p=b.GetPixel(Math.Min(ix+i,b.Width-1),Math.Min(iy+j,b.Height-1));
            double w=(i==0?1-fx:fx)*(j==0?1-fy:fy)*p.A/255.0;
            aa+=w;rr+=w*p.R;gg+=w*p.G;bb+=w*p.B;
        }
        return aa==0?Color.Transparent:Color.FromArgb((int)Math.Round(aa*255),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa));
    }
    static void Save(Bitmap b,string root,string name) {
        using(var stream=new FileStream(Path.Combine(root,name),FileMode.CreateNew))b.Save(stream,ImageFormat.Png);
    }
    static Bitmap Canvas() {return new Bitmap(Size,Size,PixelFormat.Format32bppArgb);}
    static Bitmap Transform(string path,double sx,double sy,double scale,double dx,double dy,string effect) {
        using(var src=new Bitmap(path)) {
            var dst=Canvas();
            for(int y=0;y<Size;y++)for(int x=0;x<Size;x++) {
                double aa=0,rr=0,gg=0,bb=0;
                for(int j=0;j<4;j++)for(int i=0;i<4;i++) {
                    double px=x+(i+.5)/4,py=y+(j+.5)/4;
                    Color p=Sample(src,sx+(px-dx)/scale-.5,sy+(py-dy)/scale-.5);
                    double a=p.A/255.0,r=Math.Sqrt((px-256)*(px-256)+(py-256)*(py-256));
                    if(effect=="ring")a*=Smooth((r-145)/3);
                    if(effect=="glow"||effect=="flash") {
                        double luminance=(.2126*p.R+.7152*p.G+.0722*p.B)/255;
                        a*=Math.Sqrt(luminance)*Smooth((r-169)/13)*Smooth((256-r)/12);
                        a=Math.Min(a,effect=="flash"?.82:.70);
                        p=Color.FromArgb(255,255,255,255);
                    }
                    aa+=a;rr+=a*p.R;gg+=a*p.G;bb+=a*p.B;
                }
                if(aa>0)dst.SetPixel(x,y,Color.FromArgb((int)Math.Round(aa*255/16),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa)));
            }
            return dst;
        }
    }
    public static void Layers(string refs,string output) {
        using(var b=Transform(Path.Combine(refs,"ring_delgado.png"),627,606,.375,256,256,"ring"))Save(b,output,"ring.png");
        using(var b=Transform(Path.Combine(refs,"medallion_delgado.png"),627,606,.057,256,256,""))Save(b,output,"medallion.png");
        // Punta completa anclada a las 9:00; tamaño uniforme, sin recortar gema ni cuerpo.
        using(var b=Transform(Path.Combine(refs,"pointer.png"),1007,631,.09,101,256,""))Save(b,output,"pointer.png");
        foreach(var effect in new[]{"glow","flash"})
            using(var b=Transform(Path.Combine(refs,effect+".png"),627,617,.40,256,256,effect))Save(b,output,effect+".png");
    }
    // Cada color queda en la MISMA posición base. Las otras posiciones son rotaciones Java.
    public static void Masters(string refs,string borderPath,string output) {
        using(var shared=new Bitmap(borderPath)) {
            for(int k=0;k<8;k++) {
                using(var source=new Bitmap(Path.Combine(refs,"sector_maestro_"+Names[k]+".png")))using(var target=Canvas()) {
                    double cs=Math.Cos(-22.5*Math.PI/180),sn=Math.Sin(-22.5*Math.PI/180);
                    int baseRgb=Colors[k];
                    for(int y=0;y<Size;y++)for(int x=0;x<Size;x++) {
                        double aa=0,rr=0,gg=0,bb=0;
                        for(int j=0;j<4;j++)for(int i=0;i<4;i++) {
                            double dx=x+(i+.5)/4-256,dy=y+(j+.5)/4-256;
                            double r=Math.Sqrt(dx*dx+dy*dy);
                            double a=(Math.Atan2(dy,dx)*180/Math.PI+90+720)%360;
                            if(r<22||r>=169||a>=45)continue;
                            // Escala uniforme: conserva las proporciones de los nuevos símbolos.
                            double cx=dx*cs-dy*sn,cy=dx*sn+dy*cs;
                            Color p=Sample(source,627+cx/.1292,1440+cy/.1292);
                            Color border=Sample(shared,x+(i+.5)/4-.5,y+(j+.5)/4-.5);
                            double distance=Math.Min(r*Math.Sin(a*Math.PI/180),r*Math.Sin((45-a)*Math.PI/180));
                            double weight=Math.Max(1-Smooth((distance-4)/2),Math.Max(Smooth((r-158)/3),1-Smooth((r-36)/3)));
                            double pa=p.A/255.0;
                            double cr=p.R*pa+((baseRgb>>16)&255)*(1-pa),cg=p.G*pa+((baseRgb>>8)&255)*(1-pa),cb=p.B*pa+(baseRgb&255)*(1-pa);
                            if(border.A>100){cr=cr*(1-weight)+border.R*weight;cg=cg*(1-weight)+border.G*weight;cb=cb*(1-weight)+border.B*weight;}
                            aa++;rr+=cr;gg+=cg;bb+=cb;
                        }
                        if(aa>0)target.SetPixel(x,y,Color.FromArgb((int)Math.Round(aa*255/16),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa)));
                    }
                    Save(target,output,"sector_"+Names[k]+".png");
                }
            }
        }
    }
}
