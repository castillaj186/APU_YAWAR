using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;

// Preparación determinista del lienzo y vistas. El arte se reconstruye con image_gen.
public static class PrepararRuleta {
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
        using(var b=Transform(Path.Combine(refs,"ring.png"),627,606,.375,256,256,"ring"))Save(b,output,"ring.png");
        using(var b=Transform(Path.Combine(refs,"medallion.png"),627,606,.077,256,256,""))Save(b,output,"medallion.png");
        // Punta completa anclada a las 9:00; tamaño uniforme, sin recortar gema ni cuerpo.
        using(var b=Transform(Path.Combine(refs,"pointer.png"),1007,631,.09,101,256,""))Save(b,output,"pointer.png");
        foreach(var effect in new[]{"glow","flash"})
            using(var b=Transform(Path.Combine(refs,effect+".png"),627,617,.40,256,256,effect))Save(b,output,effect+".png");
    }
    public static void Sectors(string art,string master,string output) {
        using(var shared=new Bitmap(master)) {
            for(int k=0;k<8;k++) {
                string input=Path.Combine(art,"sector_"+Names[k]+".png");
                using(var source=new Bitmap(input))using(var target=Canvas()) {
                    double rotation=(-22.5-k*45)*Math.PI/180,cs=Math.Cos(rotation),sn=Math.Sin(rotation);
                    double back=-k*45*Math.PI/180,bc=Math.Cos(back),bs=Math.Sin(back);
                    int baseRgb=Colors[k];
                    for(int y=0;y<Size;y++)for(int x=0;x<Size;x++) {
                        double aa=0,rr=0,gg=0,bb=0;
                        for(int j=0;j<4;j++)for(int i=0;i<4;i++) {
                            double dx=x+(i+.5)/4-256,dy=y+(j+.5)/4-256;
                            double r=Math.Sqrt(dx*dx+dy*dy);
                            double a=(Math.Atan2(dy,dx)*180/Math.PI+90-k*45+720)%360;
                            if(r<22||r>=169||a>=45)continue;
                            double mx=dx*bc-dy*bs,my=dx*bs+dy*bc;
                            Color border=Sample(shared,256+mx-.5,256+my-.5);
                            Color p=Sample(source,(256+dx*cs-dy*sn)*source.Width/Size-.5,(256+dx*sn+dy*cs)*source.Height/Size-.5);
                            // Todos comparten físicamente el mismo borde maestro, además de la máscara.
                            double distance=Math.Min(r*Math.Sin(a*Math.PI/180),r*Math.Sin((45-a)*Math.PI/180));
                            double borderWeight=Math.Max(1-Smooth((distance-4)/2),Math.Max(Smooth((r-158)/3),1-Smooth((r-36)/3)));
                            double pa=p.A/255.0;
                            double cr=p.R*pa+((baseRgb>>16)&255)*(1-pa),cg=p.G*pa+((baseRgb>>8)&255)*(1-pa),cb=p.B*pa+(baseRgb&255)*(1-pa);
                            if(border.A>100) {cr=cr*(1-borderWeight)+border.R*borderWeight;cg=cg*(1-borderWeight)+border.G*borderWeight;cb=cb*(1-borderWeight)+border.B*borderWeight;}
                            aa++;rr+=cr;gg+=cg;bb+=cb;
                        }
                        if(aa>0)target.SetPixel(x,y,Color.FromArgb((int)Math.Round(aa*255/16),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa)));
                    }
                    Save(target,output,"sector_"+Names[k]+".png");
                }
            }
        }
    }
    static void DrawTint(Graphics g,Bitmap b,int rgb,float alpha,Rectangle rect) {
        using(var attrs=new ImageAttributes()) {
            var matrix=new ColorMatrix();matrix.Matrix00=((rgb>>16)&255)/255f;matrix.Matrix11=((rgb>>8)&255)/255f;matrix.Matrix22=(rgb&255)/255f;matrix.Matrix33=alpha;
            attrs.SetColorMatrix(matrix);g.DrawImage(b,rect,0,0,512,512,GraphicsUnit.Pixel,attrs);
        }
    }
    static Bitmap Composite(string textures,double rotation,bool flash) {
        var target=Canvas();
        using(var g=Graphics.FromImage(target)) {
            g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            using(var halo=new Bitmap(Path.Combine(textures,"glow.png")))DrawTint(g,halo,0xF6BE59,.2f,new Rectangle(0,0,512,512));
            var state=g.Save();g.TranslateTransform(256,256);g.RotateTransform((float)rotation);
            foreach(var name in Names)using(var b=new Bitmap(Path.Combine(textures,"sector_"+name+".png")))g.DrawImage(b,-256,-256,512,512);
            foreach(var name in new[]{"ring","medallion"})using(var b=new Bitmap(Path.Combine(textures,name+".png")))g.DrawImage(b,-256,-256,512,512);
            g.Restore(state);
            if(flash)using(var b=new Bitmap(Path.Combine(textures,"flash.png")))DrawTint(g,b,Colors[0],.6175f,new Rectangle(0,0,512,512));
            using(var p=new Bitmap(Path.Combine(textures,"pointer.png")))g.DrawImage(p,0,0,512,512);
        }
        return target;
    }
    static void Checker(Graphics g,int w,int h) {
        g.Clear(Color.FromArgb(31,35,43));using(var brush=new SolidBrush(Color.FromArgb(43,48,57)))for(int y=0;y<h;y+=32)for(int x=0;x<w;x+=32)if((x/32+y/32)%2==0)g.FillRectangle(brush,x,y,32,32);
    }
    static void Detail(Bitmap b,string root,string name,Rectangle crop,int w,int h) {
        using(var result=new Bitmap(w,h,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(result)) {
            Checker(g,w,h);g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            g.DrawImage(b,new Rectangle(0,0,w,h),crop,GraphicsUnit.Pixel);Save(result,root,name);
        }
    }
    public static void Previews(string textures,string root) {
        using(var complete=Composite(textures,0,false)) {
            Save(complete,root,"ruleta_completa_512.png");
            Detail(complete,root,"vista_ruleta_completa.png",new Rectangle(0,0,512,512),1024,1024);
            Detail(complete,root,"vista_puntero_aro.png",new Rectangle(5,184,150,144),900,864);
            Detail(complete,root,"vista_uniones.png",new Rectangle(220,75,125,150),750,900);
        }
        using(var stopped=Composite(textures,247.5,false))using(var flash=Composite(textures,247.5,true)) {
            Detail(flash,root,"vista_destello_rojo.png",new Rectangle(0,0,512,512),1024,1024);
            using(var screen=new Bitmap(1600,900))using(var g=Graphics.FromImage(screen)) {
                g.Clear(Color.FromArgb(31,40,49));g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                int side=(int)Math.Round(900*.42*512/458);
                g.DrawImage(stopped,new Rectangle((1600-side)/2,(900-side)/2,side,side));
                using(var font=new Font("Segoe UI",20))using(var brush=new SolidBrush(Color.White)) {
                    g.DrawString("APU YAWAR — escala aprobada: aro ~42% de la altura",font,brush,30,25);
                    g.DrawString("Simulación técnica; texturas integrables, sin captura de Minecraft",font,brush,30,830);
                }
                Save(screen,root,"vista_tamano_aprobado.png");
            }
        }
    }
}
