package com.example.ryozo;

import java.util.Arrays;
import java.util.ArrayList;

public class FFTUtilFloat {
  public static final float THRESHOLD_FACTOR = 0.7f;
  public static int MAX_I = 100; // この値は適当

  float[] wImage;
  float[] wReal;
  float[] tmpReal;
  float[] tmpImage;
  float[] noiseSpectrum;
  float[] window;
  int[] indices;
  int fftSize;
  int halfSize;
  int nLevel;
  float ifftDenom;
  boolean filterNoise;
  boolean windowing;

  public FFTUtilFloat(int length){
    length = getSmallest2Power(length);
    fftSize = length;
    halfSize = length / 2;
    ifftDenom = 1.0f / (fftSize);
    wReal = new float[halfSize];
    wImage = new float[halfSize];
    indices = new int[length];
    for (int i = 0; i < length; ++i) {
      indices[i] = i;
    }
    makeIndices(indices, 0, length);
    
    window = new float[length];
    windowing = false;
    noiseSpectrum = new float[length];
    filterNoise = false;
    tmpReal = new float[length];
    tmpImage = new float[length];
    double factor = -2.0 * Math.PI / ((float)length);
    double base = (double)(1 << 16); // 2 ^ 16
    for (int i=0; i < halfSize; i++) {
      double angle = factor * (double) i;
      double realPart = Math.cos(angle);
      double imagePart = Math.sin(angle);
      wReal[i] = (float) realPart;
      wImage[i] = (float) imagePart;
    }
  }
  
  public float hanning(int n, int N) {
	  double angle = 2.0 * Math.PI * ((double) n / (double) (N-1));
	  return ((float) (0.5 * (1.0 - Math.cos(angle))));
  }

  public float hamming(int n, int N) {
	  double angle = 2.0 * Math.PI * ((double) n / (double) (N-1));
	  return ((float) (0.54 - 0.46 * Math.cos(angle)));
  }
  
  public float flattop(int n, int N) {
	  double angle = 2.0 * Math.PI * ((double) n / (double) (N-1));
	  return ((float) (0.21557895 - 0.41663158 * Math.cos(angle) + 0.277263158 * Math.cos(2.0 * angle) - 0.083578947 * Math.cos(6.0 * angle) + 0.006947368 * Math.cos(8.0 * angle)));
  }
  
  public float blackman(int n, int N) {
	  double angle = 2.0 * Math.PI * ((double) n / (double) (N-1));
	  return ((float) (0.42 - 0.5 * Math.cos(angle) + 0.08 * Math.cos(2.0 * angle)));
  }
  
  public void setWindow() {
	  for (int i=0; i<fftSize; ++i) {
		  window[i] = flattop(i, fftSize);
	  }
	  
	  windowing = true;
  }

  // 初期インデックス作成
  // 偶数と奇数に分ける
  public void makeIndices(int[] indices, int offset, int length) {
    if (length - offset < 2) return;
    int[] origin = Arrays.copyOfRange(indices, offset, length);
    int halfsize = (length - offset) / 2;
    for (int i=0; i < halfsize; ++i) {
      indices[offset + i] = origin[i * 2];
      indices[offset + i + halfsize] = origin[i * 2 + 1];
    }
    // 分割統治法で再起する
    makeIndices(indices, offset, offset + halfsize);
    makeIndices(indices, offset + halfsize, length);
  }

  public void fft(short[] input) {
    for (int i=0; i < fftSize; ++i) {
      tmpReal[i] = (float) input[indices[i]];
      if (windowing) {
    	  tmpReal[i] *= window[i];
      }
    }
    Arrays.fill(tmpImage, 0.0f);

    int stepSize = 2;
    int halfSize = 1;
    int wStep = fftSize >> 1;
    for (int i=0; i<nLevel; ++i) {
      for (int base=0; base < fftSize; base += stepSize) {
        int wIndex = 0;
        for (int j=0; j < halfSize; ++j) {
          int aIndex = j + base;
          int bIndex = aIndex + halfSize;

          float aReal = tmpReal[aIndex];
          float aImage = tmpImage[aIndex];
          float bReal = tmpReal[bIndex];
          float bImage = tmpImage[bIndex];

          float wR = wReal[wIndex];
          float wI = wImage[wIndex];

          float wfbReal = wR * bReal - wI * bImage;
          float wfbImage = wR * bImage + wI * bReal;

          tmpReal[aIndex] = aReal + wfbReal;
          tmpImage[aIndex] = aImage + wfbImage;
          tmpReal[bIndex] = aReal - wfbReal;
          tmpImage[bIndex] = aImage - wfbImage;

          wIndex += wStep;
        }
      }

      stepSize = stepSize << 1;
      halfSize = halfSize << 1;
      wStep = wStep >> 1;
    }
  }

  // fftのWを複素共役にしただけ。一緒の関数にするより乗算nlog(n) 回分計算が少ない
  public void ifft() {
    // PowerSpectolが入力なので、Realのみ取り扱う
    for (int i=0; i < fftSize; ++i) {
      tmpReal[i] = (float) tmpImage[indices[i]];
    }
    Arrays.fill(tmpImage, 0.0f);

    int stepSize = 2;
    int halfSize = 1;
    int wStep = fftSize >> 1;
    for (int i=0; i<nLevel; ++i) {
      for (int base=0; base < fftSize; base += stepSize) {
        int wIndex = 0;
        for (int j=0; j < halfSize; ++j) {
          int aIndex = j + base;
          int bIndex = aIndex + halfSize;

          float aReal = tmpReal[aIndex];
          float aImage = tmpImage[aIndex];
          float bReal = tmpReal[bIndex];
          float bImage = tmpImage[bIndex];

          float wR = wReal[wIndex];
          float wI = wImage[wIndex];

          float wfbReal = wR * bReal + wI * bImage;
          float wfbImage = wR * bImage - wI * bReal;

          tmpReal[aIndex] = aReal + wfbReal;
          tmpImage[aIndex] = aImage + wfbImage;
          tmpReal[bIndex] = aReal - wfbReal;
          tmpImage[bIndex] = aImage - wfbImage;

          wIndex += wStep;
        }
      }

      stepSize = stepSize << 1;
      halfSize = halfSize << 1;
      wStep = wStep >> 1;
    }

    for (int i=0; i < fftSize; ++i) {
      tmpReal[i] *= ifftDenom;
      tmpImage[i] *= ifftDenom;
    }
  }

  // パワースペクトルを求める。スペルミスしてる。
  // 後で使いやすいように全部Imageの方に突っ込む(本当はReal)
  public void powerSpectrum(short[] input) {
    fft(input);
    for (int i=0; i<fftSize; ++i) {
      tmpImage[i] = tmpReal[i] * tmpReal[i] + tmpImage[i] * tmpImage[i];
    }
  }
  // 自己相関関数を求める。
  public void acf(short[] input) {
    powerSpectrum(input);
    if (filterNoise) { // filterNoiseがオンならフィルタリング処理
    	for (int i=0; i<fftSize; ++i) {
    		tmpReal[i] -= noiseSpectrum[i];
    		if (tmpReal[i] < 0.0f) {
    			tmpReal[i] = 0.0f;
    		}
    	}
    }
    ifft();
  }

  // acfからピッチ検出する
  public int getPitchIndex(short[] input) {
    ArrayList<Pair> list = new ArrayList<Pair>();
    acf(input);
    boolean flag = false;
    float max = 0.0f;
    int index = 0;

    for(int i=0; i<fftSize; ++i) {
      float v = tmpReal[i];
      if (v < 0.0f) {
        if (index != 0) {
          list.add(new Pair(index, max));
          index = 0;
        }
        flag = true;
      } else if ((v > max) && flag) {
        index = i;
        max = v;
      }
    }
    if (index != 0) {
      list.add(new Pair(index, max));
      index = 0;
    }
    float th = max * THRESHOLD_FACTOR;
    int ret = -1;
    for (Pair p : list) {
      if (p.val >= th) {
    	ret = p.ind;
        break;
      }
    }
    return ret;
  }

  public void calibrate(short[] input, int numWindow) {
	  float denom = 1.0f / (float)numWindow;
	  powerSpectrum(input);
	  for (int i=0; i<fftSize; ++i) {
		  noiseSpectrum[i] += tmpReal[i] * denom;
	  }
	  filterNoise = true;
  }
  
  public float getHealtz(short[] input, float samplingRate) {
    int index = getPitchIndex(input);
    return samplingRate / ((float)index);
  }


  public int getSmallest2Power(int length) {
    int i = 1;
    while(i < MAX_I) {
      int power = (int)Math.pow(2.0, (double)i);
      if (length <= power) {
        nLevel = i;
        return power;
      }
      ++i;
    }
    nLevel = -1;
    return -1;
  }

  public class Pair {
    int ind;
    float val;

    public Pair(int i, float v) {
      ind = i;
      val = v;
    }
  }
}