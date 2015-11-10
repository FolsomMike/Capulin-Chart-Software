/**************************************************************
WinFilter version 0.8
http://www.winfilter.20m.com
akundert@hotmail.com

Filter type: Band Pass
Filter model: Butterworth
Filter order: 6
Sampling Frequency: 66 MHz
Fc1 and Fc2 Frequencies: 4.050000 MHz and 5.950000 MHz
Coefficents Quantization: 16-bit

Z domain Zeros
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = -1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000
z = 1.000000 + j 0.000000

Z domain Poles
z = 0.833123 + j -0.389594
z = 0.833123 + j 0.389594
z = 0.807301 + j -0.424889
z = 0.807301 + j 0.424889
z = 0.869678 + j -0.371438
z = 0.869678 + j 0.371438
z = 0.801656 + j -0.472247
z = 0.801656 + j 0.472247
z = 0.908138 + j -0.370716
z = 0.908138 + j 0.370716
z = 0.823222 + j -0.518870
z = 0.823222 + j 0.518870
***************************************************************/
#define Ntap 31

#define DCgain 262144

__int16 fir(__int16 NewSample) {
    __int16 FIRCoef[Ntap] = { 
         7701,
        12028,
        14003,
        12797,
         8322,
         1342,
        -6642,
        -13727,
        -18080,
        -18427,
        -14436,
        -6856,
         2618,
        11748,
        18316,
        20704,
        18316,
        11748,
         2618,
        -6856,
        -14436,
        -18427,
        -18080,
        -13727,
        -6642,
         1342,
         8322,
        12797,
        14003,
        12028,
         7701
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
