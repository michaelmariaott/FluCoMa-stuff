LiveKMeansClassifier {
	var updatePeriod  /*in seconds*/; // how often to recompute
	var totalHistory /*in seconds*/; // how much history to use
	var <lagtime;
	var trigRate = 30;
	var bus, <synth, k_;
	var dataset;
	var scaled_dataset;
	var scaler;
	var kmeans, action;
	var nHistories;
	var clustered, buffer_filled, serverBuf, update_counter;
	var server;
	var prev_cluster = nil;
	var confidence = 0;

	*new {
		arg server,inBus,n_categories,action,updatePeriod = 15,totalHistory = 30, target, addAction;
		^super.new.init(server,inBus,n_categories,action,updatePeriod,totalHistory, target, addAction);
	}

	init {
		arg server_, inBus_, n_categories_, action_, updatePeriod_, totalHistory_,target, addAction;
		var updatePeriodFrames = updatePeriod_ * trigRate;
		bus = inBus_;
		k_ = n_categories_;
		updatePeriod = updatePeriod_;
		totalHistory = totalHistory_;
		action = action_;
		server = server_;

		Routine{
			dataset = FluidDataSet(server);
			scaled_dataset = FluidDataSet(server);
			scaler = FluidStandardize(server);
			kmeans = FluidKMeans(server,k_);

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
				var outBuf = LocalBuf(1);
				var outStream;

				vector.do({
					arg val, i;
					BufWr.kr(val,serverBuf,i);
				});

				scaler.kr(trig,serverBuf,scaled_buf);
				kmeans.kr(trig,scaled_buf,outBuf);
				outStream = BufRd.kr(1,outBuf,0,1,1);
				SendReply.kr(trig,"/clock",[outStream]);
			}.play(target,addAction:addAction,args:[\in_bus,bus,\lagTime,lagtime]);

			OSCFunc({
				arg msg;
				var label = (update_counter % nHistories).asString;

				if(buffer_filled,{
					dataset.updatePoint(label,serverBuf);

					if(clustered,{
						var cluster = msg[3];
						if(cluster == prev_cluster,{
							confidence = confidence + 1;
						},{
							confidence = 0;
						});
						action.value(cluster,confidence);
						prev_cluster = cluster;
					});

					if((update_counter % updatePeriodFrames) == 0,{
						scaler.fitTransform(dataset,scaled_dataset,{
							//"normalizing the dataset complete".postln;
							kmeans.fit(scaled_dataset,{
								clustered = true;
							});
						});
					});
				},{
					dataset.addPoint(label,serverBuf);
					if(update_counter == (nHistories - 1),{
						buffer_filled = true;
					});
				});

				update_counter = update_counter + 1;
			},"/clock");

		}.play;
	}
}
