(
s.waitForBoot({
	Routine{

		// set up a data set
		~rand_func = {rrand(-100.0,100.0)};
		~n_cols = 100;
		~n_entries = 1000;

		~data = Dictionary.newFrom([
			"cols",~n_cols,
			"data",Dictionary.newFrom(
				Array.fill(~n_entries,{
					arg i;
					var vec = ~n_cols.collect(~rand_func);
					[i.asString,vec]
				}).flatten
			)
		]);

		~ds = FluidDataSet(s,UniqueID.next.asSymbol);

		s.sync;

		~ds.load(~data);

		s.sync;

		// setup the pipe line and "fit" it
		~pl = FluidPipeline(s,[
			~n_cols, // number of input dimensions
			FluidNormalize(s),
			~n_cols, // FluidNormalize will output same number of dimensions
			FluidPCA(s,2),
			2, // FluidPCA will output 2 dimensions (can one get the output dimensions from FluidPCA?)
			FluidNormalize(s),
			2 // FluidNormalize will output same number of dimensions
		],action:{

			~pl.fit_transform(~ds,{
				arg output_ds;
				"---OUT PUT DATA SET:".postln;
				output_ds.print;

				Routine{
					// transform a new point:
					~in_buf = Buffer.alloc(s,~n_cols);
					s.sync;
					~in_buf.setn(0,Array.fill(~n_cols,~rand_func));
					s.sync;

					~pl.predictPoint(~in_buf,{
						arg out_buf;
						out_buf.loadToFloatArray(0,-1,{
							arg fa;
							"out buffer:".postln;
							fa.postln;
						});
					});
				}.play;
			});
		});

	}.play;
});
)