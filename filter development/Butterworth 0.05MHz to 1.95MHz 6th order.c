/**************************************************************
WinFilter version 0.8
http://www.winfilter.20m.com
akundert@hotmail.com

Filter type: Band Pass
Filter model: Butterworth
Filter order: 6
Sampling Frequency: 66 MHz
Fc1 and Fc2 Frequencies: 0.050000 MHz and 1.950000 MHz
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
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
z = 1.#QNAN0 + j 1.#QNAN0
***************************************************************/
#define Ntap 31

#define DCgain 1

__int16 fir(__int16 NewSample) {
    __int16 FIRCoef[Ntap] = { 
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
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
