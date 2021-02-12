(
Window.closeAll;
s.waitForBoot({
	Task({
		var synth, yparams;
		SynthDef(\miplaits,{
			arg freq = 100, engine = 7, harm = 0.5, timbre = 0.5, morph = 0.5, level = 1, outBus = 0;
			var sig = MiPlaits.ar(freq.cpsmidi,engine.round,harm,timbre,morph);
			//[freq,engine,harm,timbre,morph,level].poll;
			Out.ar(outBus,sig);
		}).add;

		s.sync;

		synth = Synth(\miplaits);

		s.sync;

		yparams = [
			["freq",ControlSpec(20,20000,\exp)],
			//["engine",ControlSpec(0,15,\lin,1)],
			["harm",ControlSpec(0,1,\lin)],
			["timbre",ControlSpec(0,1,\lin)],
			["morph",ControlSpec(0,1,\lin)],
			//["level",ControlSpec(0,1,\lin)]
		];

		FluidMLPRegressorHandler(s,
			[3,3],FluidMLPRegressor.sigmoid,FluidMLPRegressor.identity,
			learnRate:0.01,batchSize:1,validation:0,inDims:2,outDims:yparams.size,target:synth,
			action:{
				arg mlprh;
				~m = mlprh;
				Task({
					var win = Window(bounds:Rect(100,100,800,400));
					var xview = CompositeView(win,Rect(0,0,400,400));
					var yview = CompositeView(win,Rect(400,0,400,400));
					var yviewy = 0;
					var height = 20;

					Slider2D(xview,Rect(0,0,400,400))
					.action_({
						arg sl;
						//[sl.x,sl.y].postln;
						mlprh.setxIndex(0,sl.x);
						mlprh.setxIndex(1,sl.y);
					});



					yparams.do({
						arg arr, i;
						var name = arr[0], spec = arr[1];
						EZSlider(yview,Rect(0,yviewy,400,height),name,spec,{
							arg sl;
							synth.set(name.asSymbol,sl.value);
							mlprh.setyIndex(i,sl.value);
						});

						yviewy = yviewy + height;
					});

					Button(yview,Rect(0,yviewy,400,height))
					.states_([["add point"]])
					.action_({
						arg but;
						mlprh.addTrainingPoint;
					});

					yviewy = yviewy + height;

					Button(yview,Rect(0,yviewy,400,height))
					.states_([["train"]])
					.action_({
						arg but;
						mlprh.fit({
							// invert the output scaler so it scales back up
							mlprh.outScaler.fluidObj.invert_(1);
							yparams.do({
								arg arr, i;
								var name = arr[0];
								synth.set(name.asSymbol,mlprh.ybus.subBus(i).asMap);
							});
						});
					});

					win.front;
				}).play(AppClock);
		});

	}).play(AppClock);
});
)

~m.xvals
~m.yvals
~m.ybus.getn(5,{
	arg vals;
	vals.postln;
});

~m.xbus.getn(2,{
	arg vals;
	vals.postln;
});

~m.net.inBuf.plot

~m.trainingBufY.set(~m.yvals);

~m.outScaler.fluidObj.invert
~m.trainingBufX.plot
~m.trainingBufY.plot

~m.inDS.print
~m.outDS.print
~m.nonflatbuf.plot