+ FluidDataSetWr {

	*krstream {
		arg dataset, labelPrefix = "", labelOffset = 0, streamSize, trig, krstream;
		var localbuf = LocalBuf(streamSize);

		streamSize.do({
			arg i;
			BufWr.kr(krstream[i],localbuf,i);
		});

		//BufWr.kr(krstream,localbuf,0.0);

		^this.kr(dataset,labelPrefix,labelOffset,localbuf,trig);
	}
}