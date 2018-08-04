

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.System.exit;

public class DCT
{
    static BufferedImage inputImage;
    static JFrame inputFrame, outputFrame;
    static JLabel inputLabel, outputLabel;
    static double[][] cos = new double[8][8];
    
    public static void main(String[] args) 
    {
        
        String strFileName = args[0];
        int  quantLevel = Integer.parseInt(args[1]);
        int delMode = Integer.parseInt(args[2]);
        int latency = Integer.parseInt(args[3]);
        BufferedImage[] imageOutput = new BufferedImage[64];
        int[][][] iaDCT;
        
        if(delMode > 7)
        {
            System.out.println("Incorrect Quantization level");
            exit(0);
        }                       
        
        InitWinParams();
        
        inputImage = ReadImage(strFileName, 352, 288);
        
        DisplayImage(inputImage, 0);
        
        for(int i = 0;i<8;i++)
        {
            for(int j = 0;j<8;j++)
            {
                cos[i][j] = cos((2*i+1)*j*3.14159/16.00);
            }
        }
        
        iaDCT = DCT_Quantized(inputImage, quantLevel);        
        
        if(delMode == 1)
            DCT_Sequential(iaDCT, quantLevel, latency);
        else if(delMode == 2)
            DCT_Progressive(iaDCT, quantLevel, latency);
        else if(delMode == 3)
            DCT_SuccesiveBitApprox(iaDCT, quantLevel, latency);                                               
    }
    
    public static void InitWinParams()
    {
        inputFrame = new JFrame();
        outputFrame = new JFrame();
        
        inputFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        outputFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);                
        
        inputLabel = new JLabel();
        outputLabel = new JLabel();
    }
    
  
    public static BufferedImage ReadImage(String strImageName, int iWeight, int iHeight) 
    {
        BufferedImage imgNew = new BufferedImage(iWeight, iHeight, BufferedImage.TYPE_INT_RGB);

        try 
        {
            File file = new File(strImageName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int) len];

            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) 
            {
                offset += numRead;
            }

            int index = 0;

            for (int y = 0; y < iHeight; y++) 
            {
                for (int x = 0; x < iWeight; x++) 
                {
                    byte r = bytes[index];
                    byte g = bytes[index + iHeight * iWeight];
                    byte b = bytes[index + iHeight * iWeight * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    
                    imgNew.setRGB(x, y, pix);
                    index++;
                }
            }
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return imgNew;
    }
    
    public static void DisplayImage(BufferedImage imageOutput, int iType)
    {
        if(iType == 0)
        {            
            inputLabel.setIcon(new ImageIcon(imageOutput));            
            inputFrame.getContentPane().add(inputLabel,BorderLayout.CENTER);
            inputFrame.setLocation(20, 20);
            inputFrame.pack();
            inputFrame.setVisible(true);
        }
        else if(iType == 1)
        {
            
            outputLabel.setIcon(new ImageIcon(imageOutput));
            outputFrame.getContentPane().removeAll();
            outputFrame.getContentPane().add(outputLabel,BorderLayout.CENTER);
            outputFrame.setLocation(inputFrame.getWidth() + 20, 20);
            outputFrame.pack();
            outputFrame.setVisible(true);
        }
    }
    
   
    
    public static BufferedImage DCT(int[][][] iaDCT, int quantLevel, int Row, int Col)
    {        
        int[][][] iaDCTImage = new int[352][288][3];
        int iHeight = inputImage.getHeight();
        int iWeight = inputImage.getWidth();
        
        BufferedImage imageOutput = new BufferedImage(352, 288, inputImage.getType());
        
        for(int i = 0;i<iWeight;i+=8)
        {
            for(int j = 0;j<iHeight;j+=8)
            {                                
                for(int x = 0;x<8;x++)
                {
                    for(int y = 0;y<8;y++)
                    {                                                
                        float finalR = 0.00f, finalG = 0.00f, finalB = 0.00f;                                                    
                        
                        for(int u = 0;u<8;u++)
                        {
                            for(int v = 0;v<8;v++)
                            {
                                float finalCu = 1.0f, finalCv = 1.0f;                                
                                if(u == 0)
                                    finalCu =  0.707f;
                                if(v == 0)
                                    finalCv = 0.707f;
                                
                                double iR, iG, iB;                                                                
                                if(u < Col && v < Row)
                                {
                                    iR = iaDCT[i + u][j + v][0] * Math.pow(2, quantLevel);
                                    iG = iaDCT[i + u][j + v][1] * Math.pow(2, quantLevel);
                                    iB = iaDCT[i + u][j + v][2] * Math.pow(2, quantLevel);
                                }
                                else
                                {
                                    iG = 0;
                                    iB = 0;
                                    iR =  0;                                 
                                }
                                
                                        
                                finalR += finalCu * finalCv * iR*cos[x][u]*cos[y][v];
                                finalG += finalCu * finalCv * iG*cos[x][u]*cos[y][v];
                                finalB += finalCu * finalCv * iB*cos[x][u]*cos[y][v];
                            }
                        }
                        
                        finalR *= 0.25;
                        finalG *= 0.25;
                        finalB *= 0.25;                        
                        
                        if(finalR <= 0)
                            finalR = 0;
                        else if(finalR >= 255)
                            finalR = 255;
                            
                        if(finalG <= 0)
                            finalG = 0;
                        else if(finalG >= 255)
                            finalG = 255;
                            
                        if(finalB <= 0)
                            finalB = 0;
                        else if(finalB >= 255)
                            finalB = 255; 
                        
                        iaDCTImage[i + x][j + y][0]  = (int)finalR;
                        iaDCTImage[i + x][j + y][1]  = (int)finalG;
                        iaDCTImage[i + x][j + y][2]  = (int)finalB;
                    }
                }                                               
            }
        }
        
        for(int i = 0;i<iWeight;i++)
        {
            for(int j = 0;j<iHeight;j++)
            {                
                int inputColor = 0xff000000 | ((iaDCTImage[i][j][0] & 0xff) << 16) | ((iaDCTImage[i][j][1] & 0xff) << 8) | (iaDCTImage[i][j][2] & 0xff);
                
                imageOutput.setRGB(i, j, inputColor);
            }
        }
        
        return imageOutput;
    }
    
    public static int[][][] DCT_Quantized(BufferedImage inputImage, int quantLevel)
    {
        BufferedImage imageOutput = new BufferedImage(352,288,inputImage.getType());
        int[][][] iaDCTImage = new int[352][288][3];
        int iHeight = inputImage.getHeight();
        int iWeight = inputImage.getWidth();               
        
        for(int i = 0;i<iWeight;i+=8)
        {
            for(int j = 0;j<iHeight;j+=8)
            {                
                for(int u = 0;u<8;u++)
                {
                    for(int v = 0;v<8;v++)
                    {                        
                        float finalCu = 1.0f, finalCv = 1.0f;
                        float finalR = 0.00f, finalG = 0.00f, finalB = 0.00f;
                        
                        if(u == 0)
                            finalCu =  0.707f;
                        if(v == 0)
                            finalCv = 0.707f;
                                                     
                        for(int x = 0;x<8;x++)
                        {
                            for(int y = 0;y<8;y++)
                            {                                
                                int iR, iG, iB;                                
                                
                                iR = (inputImage.getRGB(i+x,j+y)>>16) & 0xFF;
                                iG = (inputImage.getRGB(i+x,j+y)>>8) & 0xFF;
                                iB = inputImage.getRGB(i+x,j+y) & 0xFF;
                                
                               
                                finalR += iR*cos[x][u]*cos[y][v];
                                finalG += iG*cos[x][u]*cos[y][v];
                                finalB += iB*cos[x][u]*cos[y][v];
                                
                            }
                        }                        
                        iaDCTImage[i+u][j+v][0] = (int) Math.round(finalR * 0.25*finalCu*finalCv/Math.pow(2, quantLevel));
                        iaDCTImage[i+u][j+v][1] = (int) Math.round(finalG * 0.25*finalCu*finalCv/Math.pow(2, quantLevel));
                        iaDCTImage[i+u][j+v][2] = (int)Math.round(finalB * 0.25*finalCu*finalCv/Math.pow(2, quantLevel));
                    }
                }
            }
        }                
                
        return iaDCTImage;
    }
    
   
    public static void DCT_Progressive(int[][][] iaDCT, int quantLevel, int latency)
    {        
        int[][][] iaDCTImage = new int[352][288][3];
        int iHeight = inputImage.getHeight();
        int iWeight = inputImage.getWidth();
        int iFreqCount = 0;
        BufferedImage imageOutput = new BufferedImage(352, 288, inputImage.getType());
        
        long iTime = System.currentTimeMillis();
        while(iFreqCount < 64)
        {
            for(int i = 0;i<iHeight;i+=8)
            {
                for(int j = 0;j<iWeight;j+=8)
                {                                    
                    for(int x = 0;x<8;x++)
                    {
                        for(int y = 0;y<8;y++)
                        {                                                
                            float finalR = 0.00f, finalG = 0.00f, finalB = 0.00f;                                                    
                        
                            for(int u = 0;u<8;u++)
                            {
                                for(int v = 0;v<8;v++)
                                {
                                    float finalCu = 1.0f, finalCv = 1.0f;                                
                                    if(u == 0)
                                        finalCu =  0.707f;
                                    if(v == 0)
                                        finalCv = 0.707f;
                                
                                    double iR, iG, iB;                                                                                                
                                    if((u*8 + v) <= (iFreqCount))
                                    {
                                        iR = iaDCT[j + u][i + v][0] * Math.pow(2, quantLevel);
                                        iG = iaDCT[j + u][i + v][1] * Math.pow(2, quantLevel);
                                        iB = iaDCT[j + u][i + v][2] * Math.pow(2, quantLevel);                                    
                                    }
                                    else
                                    {
                                        iR = 0;
                                        iG = 0;
                                        iB = 0;
                                    }
                                                               
                                    finalR += finalCu * finalCv * iR * cos[x][u]*cos[y][v];
                                    finalG += finalCu * finalCv * iG * cos[x][u]*cos[y][v];
                                    finalB += finalCu * finalCv * iB * cos[x][u]*cos[y][v];
                                }
                            }
                        
                            finalR *= 0.25;
                            finalG *= 0.25;
                            finalB *= 0.25;
                        
                            if(finalR <= 0)
                                finalR = 0;
                            else if(finalR >= 255)
                                finalR = 255;
                            
                            if(finalG <= 0)
                                finalG = 0;
                            else if(finalG >= 255)
                                finalG = 255;
                            
                            if(finalB <= 0)
                                finalB = 0;
                            else if(finalB >= 255)
                                finalB = 255; 
                        
                            iaDCTImage[j + x][i + y][0]  = (int)finalR;
                            iaDCTImage[j + x][i + y][1]  = (int)finalG;
                            iaDCTImage[j + x][i + y][2]  = (int)finalB;
                        
                            int inputColor = 0xff000000 | ((iaDCTImage[j+x][i+y][0] & 0xff) << 16) | ((iaDCTImage[j+x][i+y][1] & 0xff) << 8) | (iaDCTImage[j+x][i+y][2] & 0xff);
                            imageOutput.setRGB(j+x, i+y, inputColor);
                        }
                    }
                }
            }
        
            DisplayImage(imageOutput, 1);
            try 
            {
                Thread.sleep((int) latency);
            } 
            catch (InterruptedException ex) 
            {
                Thread.currentThread().interrupt();
            }
            iFreqCount++;
           
        }            
    }
    
    public static void DCT_SuccesiveBitApprox(int[][][] iaDCT, int quantLevel, int latency)
    {
        int[][][] iaDCTImage = new int[352][288][3];
        int iHeight = inputImage.getHeight();
        int iWeight = inputImage.getWidth();
        Integer bMask = 0x07FF;
        Integer bMask_1;
        
        BufferedImage imageOutput = new BufferedImage(352, 288, inputImage.getType());
        int b = 0;
        
        while(b < 12)
        {
            for(int i = 0;i<iHeight;i+=8)
            {
                for(int j = 0;j<iWeight;j+=8)
                {                                    
                    for(int x = 0;x<8;x++)
                    {
                        for(int y = 0;y<8;y++)
                        {                                                
                            float finalR = 0.00f, finalG = 0.00f, finalB = 0.00f;                                                    
                        
                            for(int u = 0;u<8;u++)
                            {
                                for(int v = 0;v<8;v++)
                                {
                                    float finalCu = 1.0f, finalCv = 1.0f;                                
                                    if(u == 0)
                                        finalCu =  0.707f;
                                    if(v == 0)
                                        finalCv = 0.707f;
                                
                                    int iR, iG, iB;                                                                                                
                                    double dR, dG, dB;
                                    
                                    iR = (int) (iaDCT[j + u][i + v][0] * Math.pow(2, quantLevel));
                                    iG = (int) (iaDCT[j + u][i + v][1] * Math.pow(2, quantLevel));
                                    iB = (int) (iaDCT[j + u][i + v][2] * Math.pow(2, quantLevel));
                                
                                    bMask_1 = ~bMask;
                                   
                                    if(iR < 0)
                                    {
                                        iR *= -1;                                        
                                        iR = iR & (bMask_1);
                                        iR *= -1;
                                    }
                                    else
                                    {
                                        iR = iR & (bMask_1);
                                    }
                                    if(iG < 0)
                                    {
                                        iG *= -1;
                                        iG = iG & (bMask_1);
                                        iG *= -1;
                                    }
                                    else
                                    {
                                        iG = iG & (bMask_1);
                                    }
                                    if(iB < 0)
                                    {
                                        iB *= -1;
                                        iB = iB & (bMask_1);
                                        iB *= -1;
                                    }
                                    else
                                    {
                                        iB = iB & (bMask_1);
                                    }
                                
                                    dR = iR;
                                    dG = iG;
                                    dB = iB;
                                
                                                                  
                                    finalR += finalCu * finalCv * dR * cos[x][u]*cos[y][v];
                                    finalG += finalCu * finalCv * dG * cos[x][u]*cos[y][v];
                                    finalB += finalCu * finalCv * dB * cos[x][u]*cos[y][v];
                                }
                            }
                        
                            finalR *= 0.25;
                            finalG *= 0.25;
                            finalB *= 0.25;                        
                        
                            if(finalR <= 0)
                                finalR = 0;
                            else if(finalR >= 255)
                                finalR = 255;
                            
                            if(finalG <= 0)
                                finalG = 0;
                            else if(finalG >= 255)
                                finalG = 255;
                            
                            if(finalB <= 0)
                                finalB = 0;
                            else if(finalB >= 255)
                                finalB = 255; 
                        
                            iaDCTImage[j + x][i + y][0]  = (int)finalR;
                            iaDCTImage[j + x][i + y][1]  = (int)finalG;
                            iaDCTImage[j + x][i + y][2]  = (int)finalB;    
                            
                            int inputColor = 0xff000000 | ((iaDCTImage[j+x][i+y][0] & 0xff) << 16) | ((iaDCTImage[j+x][i+y][1] & 0xff) << 8) | (iaDCTImage[j+x][i+y][2] & 0xff);
                            imageOutput.setRGB(j+x, i+y, inputColor);
                        }
                    }                                                                                                           
                }                        
            }             
        
            DisplayImage(imageOutput, 1);
            try 
            {
                Thread.sleep((int) latency);
            } 
            catch (InterruptedException ex) 
            {
                Thread.currentThread().interrupt();
            }
            
            bMask = bMask >> 1;
            b++;
            
        }  
    }
    
    public static void DCT_Sequential(int[][][] iaDCT, int quantLevel, int latency)
    {
        int[][][] iaDCTImage = new int[352][288][3];
        int iHeight = inputImage.getHeight();
        int iWeight = inputImage.getWidth();
        
        BufferedImage imageOutput = new BufferedImage(352, 288, inputImage.getType());
        
        long iTime = System.currentTimeMillis();
        for(int i = 0;i<iHeight;i+=8)
        {
            for(int j = 0;j<iWeight;j+=8)
            {                                    
                for(int x = 0;x<8;x++)
                {
                    for(int y = 0;y<8;y++)
                    {                                                
                        float finalR = 0.00f, finalG = 0.00f, finalB = 0.00f;                                                    
                        
                        for(int u = 0;u<8;u++)
                        {
                            for(int v = 0;v<8;v++)
                            {
                                float finalCu = 1.0f, finalCv = 1.0f;                                
                                if(u == 0)
                                    finalCu =  0.707f;
                                if(v == 0)
                                    finalCv = 0.707f;
                                
                                double iR, iG, iB;                                                                                                
                                    
                                iR = iaDCT[j + u][i + v][0] * Math.pow(2, quantLevel);
                                iG = iaDCT[j + u][i + v][1] * Math.pow(2, quantLevel);
                                iB = iaDCT[j + u][i + v][2] * Math.pow(2, quantLevel);                                    
                                                            
                                finalR += finalCu * finalCv * iR * cos[x][u]*cos[y][v];
                                finalG += finalCu * finalCv * iG * cos[x][u]*cos[y][v];
                                finalB += finalCu * finalCv * iB * cos[x][u]*cos[y][v];
                            }
                        }
                        
                        finalR *= 0.25;
                        finalG *= 0.25;
                        finalB *= 0.25;                        
                        
                        if(finalR <= 0)
                            finalR = 0;
                        else if(finalR >= 255)
                            finalR = 255;
                            
                        if(finalG <= 0)
                            finalG = 0;
                        else if(finalG >= 255)
                            finalG = 255;
                            
                        if(finalB <= 0)
                            finalB = 0;
                        else if(finalB >= 255)
                            finalB = 255; 
                        
                        iaDCTImage[j + x][i + y][0]  = (int)finalR;
                        iaDCTImage[j + x][i + y][1]  = (int)finalG;
                        iaDCTImage[j + x][i + y][2]  = (int)finalB;
                        
                        int inputColor = 0xff000000 | ((iaDCTImage[j+x][i+y][0] & 0xff) << 16) | ((iaDCTImage[j+x][i+y][1] & 0xff) << 8) | (iaDCTImage[j+x][i+y][2] & 0xff);
                        imageOutput.setRGB(j+x, i+y, inputColor);
                    }
                }                                                              
                
                DisplayImage(imageOutput, 1);
                try
                {
                    Thread.sleep((int) latency);
                } 
                catch (InterruptedException ex) 
                {
                    Thread.currentThread().interrupt();
                }                
            }                        
        }                                               
    }
    
}