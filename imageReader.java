import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;	

public class imageReader {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage imageBeforeChange;
	BufferedImage imageAfterChange;

	//enum constants to define Y,U,V
	public enum Sample{
		Y_s, Y_u, Y_v;		
	}
	
	//inner class to represent RGB
	class RGB {
		int r, g, b;
		public RGB(int r, int g, int b){
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	//inner class to represent YUV
	class YUV {
		double y, u, v;
		public YUV(double y,double u, double v) {
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}
	

	public void showIms(String[] args){
		int width = 352;  	
		int height = 288; 	

		//Get luminance value
		int Y = Integer.parseInt(args[1]);
		//Get chrominance values
		int U = Integer.parseInt(args[2]);
		int V = Integer.parseInt(args[3]);
		//get Quantization value
		int Q = Integer.parseInt(args[4]);

		imageBeforeChange = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imageAfterChange = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		try {
			File file = new File(args[0]);	
			InputStream is = new FileInputStream(file);

			long len = file.length();
			byte[] bytes = new byte[(int)len];

			int offset = 0;
			int numRead = 0;
			//Read input stream and store in bytes array
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length - offset)) >= 0) {	
				offset += numRead;
			}

			int index = 0;
			RGB[][] RGBBeforeChange = new RGB[height][width];
			RGB[][] RGBAfterChange = new RGB[height][width];
			YUV[][] YUV_r = new YUV[height][width];

			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {

					int R = bytes[index];				    
					int G = bytes[index + height*width];	
					int B = bytes[index + height*width*2];  
					
					int pixel = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);				
					
					imageBeforeChange.setRGB(x,y,pixel);

					//convert to int
					R = R & 0xFF;
					G = G & 0xFF;
					B = B & 0xFF;

					
					RGB rgb = new RGB(R, G, B);
					RGBBeforeChange[y][x] = rgb;
					
					//RGB to YUV conversion
					double[] arrYUV = convertRBGtoYUV(R, G, B);	//get converted YUV for each pixel

					//Subsample YUV
					YUV objYUV = new YUV(arrYUV[0], arrYUV[1], arrYUV[2]);
					YUV_r[y][x] = objYUV;

					index++;
				}
			}

			//Up Sampling
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {					
					if(Y !=0 && U != 0 && V != 0){
						YUV_r = upSample(YUV_r, Y, width, i, j, Sample.Y_s);
						YUV_r = upSample(YUV_r, U, width, i, j, Sample.Y_u);
						YUV_r = upSample(YUV_r, V, width, i, j, Sample.Y_v);
					}
				}
			}

			boolean doQuantization = true;
			Integer[] b_s = null;
			if(Q <= 256){
				double slotSize = 256/ (double) Q;
				b_s = getb_sForQuantization(slotSize);
			}else{
				doQuantization = false;
			}

			//Image is displayed
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					YUV yuv = YUV_r[i][j];
					
					int[] arrRGB = convertYUVtoRGB(yuv.y, yuv.u, yuv.v);
					int R = arrRGB[0];
					int G = arrRGB[1];
					int B = arrRGB[2];	

					if(doQuantization) {
						int[] quantizedRGB = quantize(R, G, B, b_s);
						R = quantizedRGB[0];
						G = quantizedRGB[1];
						B = quantizedRGB[2];
					}					
					int processedPixel = 0xff000000 | ((R) << 16) | ((G) << 8) | (B);
					imageAfterChange.setRGB(j, i, processedPixel);
					
					RGBAfterChange[i][j] = new RGB(R, G, B);
					
				}
			}
		

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(imageBeforeChange));
		lbIm2 = new JLabel(new ImageIcon(imageAfterChange));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);


	}
	
	//method to convert RGB to YUV
	private double[] convertRBGtoYUV(int R, int G, int B) {
		double[] YUV = new double[3];

		YUV[0] = (0.299 * R + 0.587 * G + 0.114 * B);
		YUV[1] = (0.596 * R + (-0.274 * G) + (-0.322 * B));
		YUV[2] = (0.211 * R + (-0.523 * G) + (0.312 * B));

		return YUV;
	}

	//method to convert YUV to RGB
	private int[] convertYUVtoRGB(double Y, double U, double V) {
		int[] RGB = new int[3];

		RGB[0] = (int) (1.000 * Y + 0.956 * U + 0.621 * V);
		RGB[1] = (int) (1.000 * Y + (-0.272 * U) + (-0.647 * V));
		RGB[2] = (int) (1.000 * Y + (-1.106 * U) + (1.703 * V));

		return RGB;
	}
       
	
	private int[] quantize(int R, int G, int B, Integer[] b_s) {

		for(int i=0; i < b_s.length-1; i++) {
			if(R >= b_s[i] && R <= b_s[i+1]){				
				int mean = (int) Math.round((b_s[i] + b_s[i+1])/(double)2);
				if(R < mean){
					R = b_s[i];
				}else{
					R = b_s[i+1];
				}
				break;
			}
		}
		if(R > 255){
			R = 255;
		}else if(R < 0){
			R = 0;
		}

		for(int i=0; i < b_s.length-1; i++){
			if(G >= b_s[i] && G <= b_s[i+1]){				
				int mean = (int) Math.round((b_s[i] + b_s[i+1])/(double)2);
				if(G < mean){
					G = b_s[i];
				}else{
					G = b_s[i+1];
				}
				break;
			}
		}
		if(G > 255){
			G = 255;
		}else if(G < 0){
			G = 0;
		}
		
		for(int i=0; i < b_s.length-1; i++){
			if(B >= b_s[i] && B <= b_s[i+1]){				
				int mean = (int) Math.round((b_s[i] + b_s[i+1])/(double)2);
				if(B < mean){
					B = b_s[i];
				}else{
					B = b_s[i+1];
				}
				break;
			}
		}
		if(B > 255){
			B = 255;
		}else if(B < 0){
			B = 0;
		}

		return new int[]{R, G, B};
	}


	private Integer[] getb_sForQuantization(double quantValue) {
		LinkedList<Integer> list = new LinkedList<Integer>();
		double trueValue = 0;
		int value = 0;

		list.add(value);
		while(true){
			trueValue = trueValue + quantValue;
			value = (int) Math.round(trueValue);

			if(value > 255){
				break;
			}
			list.add(value);
		}

		Integer[] b_s = new Integer[list.size()];
		b_s = list.toArray(b_s);

		return b_s;
	}
	
	//upsampling
		private YUV[][] upSample(YUV[][] YUV_r, int space, int width, int i, int j, Sample sample) {

			int k = j % space;

			if(k != 0) {
				int prev = j-k;
				int next = j+space-k; 

				if(next < width) {
					YUV previousValue = YUV_r[i][prev];
					YUV currentValue = YUV_r[i][j];
					YUV nextValue = YUV_r[i][next];
					
					if(sample == Sample.Y_s) {
						currentValue.y = ((space - k)* previousValue.y + (k * nextValue.y))/space;
					}else if(sample == Sample.Y_u) {
						currentValue.u = ((space - k)* previousValue.u + (k * nextValue.u))/space;
					}else if(sample == Sample.Y_v) {
						currentValue.v = ((space - k)* previousValue.v + (k * nextValue.v))/space;
					}				
				} else {
					YUV previousValue = YUV_r[i][prev];

					for(int m = prev+1; m < width; m++) {
						YUV currentValue = YUV_r[i][m];
						if(sample == Sample.Y_s) {
							currentValue.y = previousValue.y;
						}else if(sample == Sample.Y_u) {
							currentValue.u = previousValue.u;
						}else if(sample == Sample.Y_v) {
							currentValue.v = previousValue.v;
						}
					}
				}

			}

			return YUV_r;

		}
		

	public static void main(String[] args) {
		imageReader imr = new imageReader();
		imr.showIms(args);
	}
	
}