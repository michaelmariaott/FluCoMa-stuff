(
s.waitForBoot({
	Routine{
		var n_xdims = 6;
		~ds = FluidDataSet(s);

		s.sync;

		{
			var xstream = LFDNoise3.kr(2.dup(n_xdims));
			var trig = Impulse.kr(30);
			FluidDataSetWr.krstream(~ds,"",0,n_xdims,trig,xstream);

		}.play;

	}.play;
});
)

~ds.print