import struct
import numpy as np
import os
from testInterp import calcSpline as updateData

try:
    fileName='tempData.bin'

    f=open(fileName,'rb')
    flen=os.path.getsize(fileName)
    flen/=8

    vals=[]
    for i in range(int(flen)):
        val=struct.unpack('>d',f.read(8))
        vals.append(list(val)[0])

    f.close()

    length=int(len(vals)/2)
    X=np.zeros(length)
    Y=np.zeros(length)
    for i in range(length):
        X[i]=vals[i*2]
        Y[i]=vals[i*2+1]
        
    #print(X)
    #print(Y)

    data=updateData(X,Y)
    X=data[0]
    Y=data[1]

    f=open(fileName,'wb')
    for i in range(len(X)):
        f.write(struct.pack('>d',X[i]))
        f.write(struct.pack('>d',Y[i]))

    f.close()

    print(1)
except:
    print(0)
