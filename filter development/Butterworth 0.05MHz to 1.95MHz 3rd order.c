/**************************************************************
WinFilter version 0.8
http://www.winfilter.20m.com
akundert@hotmail.com

Filter type: Band Pass
Filter model: Butterworth
Filter order: 3
Sampling Frequency: 66 MHz
Fc1 and Fc2 Frequencies: 0.050000 MHz and 1.950000 MHz
Coefficents Quantization: 16-bit

Z domain Zeros
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000

Z domain Poles
z = 0.837906 + j -0.000000
z = 0.994437 + j 0.000165
z = 0.903986 + j -0.147089
z = 0.903986 + j 0.147089
z = 0.997894 + j -0.004467
z = 0.997894 + j 0.004467
***************************************************************/
#define Ntap 31

#define DCgain 262144

__int16 fir(__int16 NewSample) {
    __int16 FIRCoef[Ntap] = { 
         -565,
          182,
         1129,
         2290,
         3679,
         5303,
         7163,
         9251,
        11547,
        14018,
        16609,
        19243,
        21811,
        24157,
        25979,
        26878,
        25979,
        24157,
        21811,
        19243,
        16609,
        14018,
        11547,
         9251,
         7163,
         5303,
         3679,
         2290,
         1129,
          182,
         -565
    };

    static __int16 x[Ntap]; //input samples
    __int32 y=0;            //output sample
    int n;

    //shift the old samples
    for(n=Ntap-1; n>0; n--)
       x[n] = x[n-1];

    //Calculate the new output
    x[0] = NewSample;
    for(n=0; n<Ntap; n++)
        y += FIRCoef[n] * x[n];
    
    return y / DCgain;
}
