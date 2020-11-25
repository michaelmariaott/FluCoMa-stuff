FluidPipeline {
	var server, order, order_handlers, dims, <group, output_ds, id, cmd_name;

	*new {
		arg server, order, target, addAction, action;
		^super.new.init(server, order, target, addAction, action);
	}

	init {
		arg server_, order_, target, addAction, action;
		server = server_;
		target = target ? server;
		addAction = addAction ? \addToHead;

		order = order_.select({
			arg obj, i;
			i.odd;
		});
		dims = order_.select({
			arg obj, i;
			i.even;
		});

/*		"order:".postln;
		order.postln;
		"dims:".postln;
		dims.postln;*/

		id = UniqueID.next;
		cmd_name = "/fluidpipeline_%".format(id);

		// construct server
		Routine{
			/*
			"target: %".format(target).postln;
			"addAction: %".format(addAction).postln;
			*/
			output_ds = FluidDataSet(server,id.asSymbol);
			group = Group(target,addAction);

			server.sync;

			order_handlers = order.collect({
				arg obj, i;
				var fh = FluidHandler(obj,dims[i], dims[i+1]);
				server.sync;
				fh;
			});

			server.sync;

			order_handlers.do({
				arg handler;
				handler.attachIO;
				server.sync;
			});

			server.sync;

			order_handlers.do({
				arg handler, i;
				if(i == 0,{
					handler.synth.moveToHead(group);
				},{
					handler.synth.moveAfter(order_handlers[i-1].synth);
					handler.inBuf_(order_handlers[i-1].outBuf);
					handler.inBus_(order_handlers[i-1].outBus);
				});

				server.sync;
			});

/*			"last synth:".postln;
			order_handlers.last.synth.postln;*/

			{
				SendReply.kr(In.kr(order_handlers.last.outBus),cmd_name);
			}.play(order_handlers.last.synth,0,0,\addAfter);

			server.sync;

			action.value(this);

		}.play;
	}

	fit_transform {
		arg input_ds, action;

		output_ds.clear({
			this.fit_transform_r(0,input_ds,output_ds,action)
		});
	}

	fit_transform_r {
		arg curr_i, input_ds, output_ds, action;
		if(curr_i < order.size,{
			var fluid_obj = order[curr_i];
			var in_ds = output_ds;

			if(curr_i == 0,{in_ds = input_ds});

			fluid_obj.fit(in_ds,{
				fluid_obj.transform(in_ds,output_ds,{
					this.fit_transform_r(curr_i + 1, input_ds, output_ds, action);
				});
			})
		},{
			action.value(output_ds);
		});
	}

	predictPoint {
		arg in_buf, action;

		OSCFunc({
			action.value(order_handlers.last.outBuf);
		},cmd_name).oneShot;

		order_handlers.first.inBuf_(in_buf);
		order_handlers.first.inBus.set(1);
	}
}