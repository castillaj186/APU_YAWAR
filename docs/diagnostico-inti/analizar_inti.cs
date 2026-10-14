using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public class AnalizarInti {
  static string Bounds(string path) {
    using (var bitmap = new Bitmap(path)) {
      int minX=bitmap.Width,minY=bitmap.Height,maxX=-1,maxY=-1; long sumX=0,sumY=0,sumA=0;
      var rect=new Rectangle(0,0,bitmap.Width,bitmap.Height);
      var data=bitmap.LockBits(rect,ImageLockMode.ReadOnly,PixelFormat.Format32bppArgb);
      try {
        byte[] bytes=new byte[Math.Abs(data.Stride)*bitmap.Height];
        Marshal.Copy(data.Scan0,bytes,0,bytes.Length);
        for(int y=0;y<bitmap.Height;y++) for(int x=0;x<bitmap.Width;x++) {
          byte a=bytes[y*data.Stride+x*4+3];
          if(a<=8) continue;
          if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;
          sumX+=(long)x*a;sumY+=(long)y*a;sumA+=a;
        }
      } finally { bitmap.UnlockBits(data); }
      if(maxX<0) return bitmap.Width+"x"+bitmap.Height+",EMPTY";
      return bitmap.Width+"x"+bitmap.Height+","+minX+","+minY+","+maxX+","+maxY+","+
        Math.Round((double)sumX/sumA,2)+","+Math.Round((double)sumY/sumA,2);
    }
  }
  public static string[] Analyze(string dir) {
    var files=Directory.GetFiles(dir,"frame_*.png");
    Array.Sort(files, StringComparer.Ordinal);
    var result=new string[files.Length+1];
    result[0]="frame,width_height,min_x,min_y,max_x,max_y,alpha_centroid_x,alpha_centroid_y";
    for(int i=0;i<files.Length;i++) result[i+1]=Path.GetFileName(files[i])+","+Bounds(files[i]);
    return result;
  }
  public static void Main(string[] a) {
    foreach(var line in Analyze(a[0])) Console.WriteLine(line);
  }
}
