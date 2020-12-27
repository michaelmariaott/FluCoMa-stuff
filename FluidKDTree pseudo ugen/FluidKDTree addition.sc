+ FluidKDTree {
	krstream {
		arg trig, inputBuffer, outputBuffer, numNeighbours = 1, lookupDataSet, inputStream;

		inputBuffer.numFrames.do({
			arg i;
			BufWr.kr(inputStream[i],inputBuffer,i);
		});

		this.kr(trig,inputBuffer,outputBuffer,numNeighbours,lookupDataSet);

		^outputBuffer.numFrames.collect({
			arg i;
			BufRd.kr(1,outputBuffer,i,1,1);
		});
	}

	krstreamlocal {
		arg trig, inputSize, outputSize, numNeighbours = 1, lookupDataSet, inputStream;
		var inputBuffer = LocalBuf(inputSize);
		var outputBuffer = LocalBuf(outputSize);

		^this.krstream(trig,inputBuffer,outputBuffer,numNeighbours,lookupDataSet,inputStream);
	}
}