(
s.waitForBoot({
	~n_points = 100;
	~x_ds = FluidDataSet(s);
	~n_xdims = 6;
	~xdata = Dictionary.newFrom([
		"cols",~n_xdims,
		"data",Dictionary.newFrom(Array.fill(~n_points,{
			arg i;
			[i.asString,Array.fill(~n_xdims,{rrand(-1.0,1.0)})];
		}).flatten)
	]);
	~x_ds.load(~xdata);

	~y_ds = FluidDataSet(s);
	~n_ydims = 2;
	~ydata = Dictionary.newFrom([
		"cols",~n_ydims,
		"data",Dictionary.newFrom(Array.fill(~n_points,{
			arg i;
			[i.asString,Array.fill(~n_ydims,{rrand(-1.0,1.0)})];
		}).flatten)
	]);
	~y_ds.load(~ydata);

	s.sync;

	Routine{
		~inBuf = Buffer.alloc(s,~n_xdims);
		~outBuf = Buffer.alloc(s,~n_ydims);
		~tree = FluidKDTree(s,1);
		s.sync;

		~tree.fit(~x_ds);
		s.sync;

		{
			var xstream = LFDNoise3.kr(2.dup(~n_xdims));
			var ystream;

			// ============= if you do want to know the buffers yourself, to access them elsewhere: ====================
			// ystream = ~tree.krstream(Impulse.kr(10),~inBuf,~outBuf,1,~y_ds,xstream);

			// if you don't need to access the buffers elsewhere so they can just be LocalBuf
			ystream = ~tree.krstreamlocal(Impulse.kr(10),~n_xdims,~n_ydims,1,~y_ds,xstream);

			ystream.poll;
		}.play;

	}.play;
});
)