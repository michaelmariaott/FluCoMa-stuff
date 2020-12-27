FluidMLPRegressorHandler {
	var <net, <inDims, <outDims, <inScaler, <outScaler,<inDS,<outDS, group, <trainingBufX, <yvals, <xvals, <trainingBufY, <currentLabel = 0, <xbus,<ybus, <nonflatbuf,inBus_synth,outBus_synth, <inDS_norm, <outDS_norm, server;

	*initClass{
		StartUp.defer{
			(1..100).do({
				arg i;
				SynthDef("fmlprh_bus_to_buf_%".format(i).asSymbol,{
					arg inBus,nonFlatBuf,inScaler_inBuf,trig_rate = 25,outBus;
					var t1;
					var sig = In.kr(inBus,i);
					//sig.poll;
					//RecordBuf.kr(sig,nonFlatBuf);
					BufWr.kr(sig,nonFlatBuf,DC.kr(0));
					t1 = FluidBufFlatten.kr(nonFlatBuf,inScaler_inBuf,1,Impulse.kr(trig_rate));
					Out.kr(outBus,t1);
				}).writeDefFile;

				SynthDef("fmlprh_buf_to_bus_%".format(i).asSymbol,{
					arg outScaler_outBuf, outBus;
					var sig = i.collect({
						arg j;
						Index.kr(outScaler_outBuf,j);
					});
					//sig.poll;
					Out.kr(outBus,sig);
				}).writeDefFile;
			});
		}
	}

	*new {
		arg server,shape=[3,3],activation=2,outputActivation=0,tapIn=0,tapOut= -1,maxIter=1000,learnRate=0.0001,momentum=0.9,batchSize=50,validation=0.2,inDims,outDims,target,action;
		^super.new.init(server,shape,activation,outputActivation,tapIn,tapOut,maxIter,learnRate,momentum,batchSize,validation,inDims,outDims,target,action);
	}

	init {
		arg server_,shape,activation,outputActivation,tapIn,tapOut,maxIter,learnRate,momentum,batchSize,validation,inDims_,outDims_,target,action;
		server = server_;
		inDims = inDims_;
		outDims = outDims_ ? inDims;

		Task({

			// make objects on server
			inDS = FluidDataSet(server,UniqueID.next.asSymbol);
			outDS = FluidDataSet(server,UniqueID.next.asSymbol);
			inDS_norm = FluidDataSet(server,UniqueID.next.asSymbol);
			outDS_norm = FluidDataSet(server,UniqueID.next.asSymbol);

			trainingBufX = Buffer.alloc(server,inDims);
			trainingBufY = Buffer.alloc(server,outDims);

			xvals = Array.fill(inDims,{0});
			yvals = Array.fill(outDims,{0});

			net = FluidHandler(
				FluidMLPRegressor(server,shape,activation,outputActivation,tapIn,tapOut,maxIter,learnRate,momentum,batchSize,validation),
				inDims,outDims
			);

			inScaler = FluidHandler(FluidNormalize(server,0,1,0),inDims);
			outScaler = FluidHandler(FluidNormalize(server,0,1,0),outDims);

			group = Group(target,\addAfter);

			xbus = Bus.control(server,inDims);
			ybus = Bus.control(server,outDims);
			nonflatbuf = Buffer.alloc(server,1,inDims);

			server.sync;

			inScaler.attachIO;
			outScaler.attachIO;
			net.attachIO;

			server.sync;

			inBus_synth = Synth("fmlprh_bus_to_buf_%".format(inDims).asSymbol,[
				\inBus,xbus,
				\nonFlatBuf,nonflatbuf,
				\inScaler_inBuf,inScaler.inBuf,
				\outBus,inScaler.inBus
			]);

			outBus_synth = Synth("fmlprh_buf_to_bus_%".format(outDims).asSymbol,[\outBus,ybus,\outScaler_outBuf,outScaler.outBuf]);

			server.sync;

			// organize server
			net.fluidObj.synth.moveToHead(group);
			server.sync;
			inScaler.fluidObj.synth.moveBefore(net.fluidObj.synth);
			server.sync;
			inBus_synth.moveBefore(inScaler.fluidObj.synth);
			server.sync;
			outScaler.fluidObj.synth.moveAfter(net.fluidObj.synth);
			server.sync;
			outBus_synth.moveAfter(outScaler.fluidObj.synth);
			server.sync;
			net.inBuf_(inScaler.outBuf);
			net.inBus_(inScaler.outBus);
			outScaler.inBuf_(net.outBuf);
			outScaler.inBus_(net.outBus);
			server.sync;
			action.value(this);
		}).play;
	}

	setxIndex {
		arg index, val;
		xvals[index] = val;
		xbus.setn(xvals);
		//trainingBufX.loadCollection(xvals);
	}

	setyIndex {
		arg index, val;
		yvals[index] = val;
		//ybus.setn(yvals);
		trainingBufY.setn(0,yvals);
	}

	getCurrentLabel {
		var return = currentLabel;
		currentLabel = currentLabel + 1;
		^return.asString;
	}

	addTrainingPoint {
		var label = this.getCurrentLabel;
		//inDS.addPoint(label,trainingBufX);
		inDS.addPoint(label,inScaler.inBuf);
		outDS.addPoint(label,trainingBufY);
	}

	fit {
		arg action;
		Task({
			inScaler.fluidObj.fitTransform(inDS,inDS_norm);
			server.sync;
			outScaler.fluidObj.fitTransform(outDS,outDS_norm);
			server.sync;
			net.fluidObj.fit(inDS_norm,outDS_norm,action);
		}).play;
	}
}