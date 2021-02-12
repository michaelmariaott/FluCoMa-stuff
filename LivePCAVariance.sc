LivePCAVariance {
	var updatePeriod  /*in seconds*/; // how often to recompute
	var totalHistory /*in seconds*/; // how much history to use
	var <lagtime;
	var trigRate = 30;
	var bus, <synth, n_dims;
	var dataset;
	var normed_dataset;
	var scaler;
	var outScaler;
	var pca, action;
	var nHistories;
	var clustered, buffer_filled, serverBuf, update_counter;
	var server;
	var inputBuffer;

	lagtime_ {
		arg lt;
		lagtime = lt;
		synth.set(\lagTime,lagtime);
	}

	*new {
		arg server,inBus,n_dims,action,updatePeriod = 15,totalHistory = 30,lagTime = 1,target, addAction;
		^super.new.init(server,inBus,n_dims,action,updatePeriod,totalHistory,lagTime,target, addAction);
	}

	init {
		arg server_, inBus_, n_dims_, action_, updatePeriod_, totalHistory_, lagTime_,target, addAction;
		var updatePeriodFrames = updatePeriod_ * trigRate;
		bus = inBus_;
		n_dims = n_dims_;
		updatePeriod = updatePeriod_;
		totalHistory = totalHistory_;
		lagtime = lagTime_;
		action = action_;
		server = server_;

		Routine{
			dataset = FluidDataSet(server);
			normed_dataset = FluidDataSet(server);
			scaler = FluidStandardize(server);
			outScaler = FluidStandardize(server);
			pca = FluidPCA(server,n_dims);
			nHistories = trigRate * totalHistory; // total number of histories to keep track of
			clustered = false; // is true once at least one clustering has happened (at one point one can predict on new points)
			buffer_filled = false;
			serverBuf = Buffer.alloc(server,bus.numChannels);
			update_counter = 0;

			server.sync;

			// synth that does the analysis
			synth = {
				arg in_bus, lagTime;
				var vector = In.kr(in_bus,bus.numChannels);
				var trig = Impulse.kr(trigRate);
				var scaled_buf = LocalBuf(serverBuf.numFrames);
				var outBuf = LocalBuf(n_dims);
				var outBufNorm = LocalBuf(n_dims);
				var outStream;

				vector.do({
					arg val, i;
					BufWr.kr(val,serverBuf,i);
				});

				scaler.kr(trig,serverBuf,scaled_buf);
				pca.kr(trig,scaled_buf,outBuf,n_dims);
				outScaler.kr(trig,outBuf,outBufNorm);
				outStream = n_dims.collect({
					arg i;
					BufRd.kr(1,outBufNorm,i,1,1);
				});
				SendReply.kr(trig,"/pca_clock",outStream.lag(lagTime));
			}.play(target,addAction:addAction,args:[\in_bus,bus,\lagTime,lagtime]);

			OSCFunc({
				arg msg;
				var label = (update_counter % nHistories).asString;

				if(buffer_filled,{
					dataset.updatePoint(label,serverBuf);

					if(clustered,{
						action.value(msg[3..]);
					});

					if((update_counter % updatePeriodFrames) == 0,{
						scaler.fitTransform(dataset,normed_dataset,{
							//"normalizing the dataset complete".postln;
							pca.fitTransform(normed_dataset,normed_dataset,{
								//"fitting pca transform complete".postln;
								outScaler.fit(normed_dataset,{
									clustered = true;
								});
							});
						});
					});
				},{
					dataset.addPoint(label,serverBuf);
					if(update_counter == (nHistories - 1),{buffer_filled = true;});
				});

				update_counter = update_counter + 1;
			},"/pca_clock");
		}.play;
	}
}
