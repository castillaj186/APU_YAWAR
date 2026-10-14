using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using System.Globalization;
public static class VistaAnimacion {
    static readonly string[] Names={"rojo","rosa","morado","azul","turquesa","verde","amarillo","naranja"};
    static readonly int[] Palette={0xE83233,0xF052AF,0x8139CE,0x2780E4,0x22C6CE,0x37B752,0xFFD235,0xEF8C27};
    static void Tint(Graphics g,Bitmap b,int rgb,double alpha,double scale) {
        if(alpha<=0)return;
        using(var attr=new ImageAttributes()) {
            var m=new ColorMatrix();m.Matrix00=((rgb>>16)&255)/255f;m.Matrix11=((rgb>>8)&255)/255f;m.Matrix22=(rgb&255)/255f;m.Matrix33=(float)alpha;
            attr.SetColorMatrix(m);int size=(int)Math.Round(512*scale);
            g.DrawImage(b,new Rectangle(-size/2,-size/2,size,size),0,0,512,512,GraphicsUnit.Pixel,attr);
        }
    }
    static int Mix(int from,int to,double amount) {
        int rgb=0;for(int shift=0;shift<=16;shift+=8)rgb|=(int)Math.Round(((from>>shift)&255)*(1-amount)+((to>>shift)&255)*amount)<<shift;
        return rgb;
    }
    static Bitmap Frame(Bitmap[] textures,double[] row) {
        var image=new Bitmap(512,512,PixelFormat.Format32bppArgb);
        using(var g=Graphics.FromImage(image)) {
            g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            g.TranslateTransform(256,256);g.ScaleTransform((float)row[3],(float)row[3]);
            int winner=(int)row[1],color=Mix(0xF6BE59,Palette[winner],row[10]);
            double alpha=row[4];
            Tint(g,textures[11],color,row[8]*alpha,row[9]);
            for(int slot=0;slot<8;slot++) {
                double build=row[11+slot],fill=row[19+slot];
                var state=g.Save();g.RotateTransform((float)(row[2]+slot*45));
                if(fill<1||slot==winner)Tint(g,textures[slot],0xFFFFFF,alpha*build,1);
                if(fill>0&&slot!=winner)Tint(g,textures[winner],0xFFFFFF,alpha*build*fill,1);
                g.Restore(state);
            }
            var disk=g.Save();g.RotateTransform((float)row[2]);
            Tint(g,textures[8],0xFFFFFF,alpha*row[5],1);Tint(g,textures[9],0xFFFFFF,alpha*row[5],1);g.Restore(disk);
            Tint(g,textures[12],color,row[7]*.6*alpha,row[9]);
            Tint(g,textures[10],0xFFFFFF,alpha*row[6],1);
        }
        return image;
    }
    static void Save(Bitmap b,string root,string name) {
        // Solo reescribe las vistas técnicas generadas, nunca las texturas ni referencias.
        using(var stream=new FileStream(Path.Combine(root,name),FileMode.Create))b.Save(stream,ImageFormat.Png);
    }
    public static void Generate(string textures,string csv,string root) {
        var layers=new Bitmap[13];
        try {
            for(int i=0;i<8;i++)layers[i]=new Bitmap(Path.Combine(textures,"sector_"+Names[i]+".png"));
            string[] rest={"ring","medallion","pointer","glow","flash"};for(int i=0;i<5;i++)layers[8+i]=new Bitmap(Path.Combine(textures,rest[i]+".png"));
            string[] lines=File.ReadAllLines(csv);
            using(var sequence=new Bitmap(1600,1200))using(var g=Graphics.FromImage(sequence))
            using(var results=new Bitmap(1024,1024))using(var rg=Graphics.FromImage(results))
            using(var font=new Font("Segoe UI",14))using(var white=new SolidBrush(Color.White)) {
                g.Clear(Color.FromArgb(27,33,42));rg.Clear(Color.FromArgb(27,33,42));
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;rg.InterpolationMode=InterpolationMode.HighQualityBicubic;
                int tile=0,result=0;
                for(int n=1;n<lines.Length;n++) {
                    var tokens=lines[n].Split(',');var row=new double[tokens.Length];for(int j=0;j<tokens.Length;j++)row[j]=double.Parse(tokens[j],CultureInfo.InvariantCulture);
                    int tick=(int)row[0],winner=(int)row[1];
                    if(winner==0)using(var frame=Frame(layers,row)) {
                        int x=tile%4*400,y=tile/4*400;
                        g.DrawString("t="+tick+" / "+(tick/20.0).ToString("0.0",CultureInfo.InvariantCulture)+" s",font,white,x+15,y+10);
                        g.DrawImage(frame,new Rectangle(x+30,y+50,340,340));tile++;
                    }
                    if(tick==520)using(var frame=Frame(layers,row)) {
                        int x=result%2*512,y=result/2*512;
                        rg.DrawString("Resultado: "+Names[winner],font,white,x+15,y+8);
                        rg.DrawImage(frame,new Rectangle(x+20,y+35,472,472));result++;
                        if(winner==0)Save(frame,root,"resultado_rojo_512.png");
                    }
                }
                Save(sequence,root,"vista_secuencia.png");Save(results,root,"vista_resultados.png");
            }
        } finally {foreach(var b in layers)if(b!=null)b.Dispose();}
    }
}
