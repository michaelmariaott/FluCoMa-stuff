(
s.waitForBoot({
	Routine{
		var n_xdims = 6;
		~ds = FluidDataSet(s);

		s.sync;

		{
			var xstream = LFDNoise3.kr(2.dup(n_xdims));
			var trig = Impulse.kr(30);
			var counter = Stepper.kr(trig,0,0,59);
			var buf = LocalBuf(n_xdims);
			n_xdims.do({
				arg i;
				BufWr.kr(xstream[i],buf,i);
			});
			FluidDataSetWr.kr(~ds,"",counter,buf,trig);
		}.play;
	}.play;
});
)

~ds.print