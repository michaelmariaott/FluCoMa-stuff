FluidHandler {
	var <fluidObj, <server, <inDims, <outDims, <inBus, <outBus, <inBuf, <outBuf;

	*new {
		arg fluidObj, inDims, outDims;
		^super.new.init(fluidObj,inDims, outDims);
	}

	init {
		arg fluidObj_, inDims_, outDims_;
		fluidObj = fluidObj_;
		server = fluidObj.server;
		inDims = inDims_;
		outDims = outDims_ ? inDims;

		inBus = Bus.control(server);
		outBus = Bus.control(server);
		inBuf = Buffer.alloc(server,inDims);
		outBuf = Buffer.alloc(server,outDims);

		^this;
	}

	synth {
		^fluidObj.synth;
	}

	attachIO {
		fluidObj.inBus_(inBus).outBus_(outBus).inBuffer_(inBuf).outBuffer_(outBuf);
	}

	inBus_ {
		arg ib;
		inBus = ib;
		fluidObj.inBus_(inBus);
	}

	outBus_ {
		arg ob;
		outBus = ob;
		fluidObj.outBus_(outBus);
	}

	inBuf_ {
		arg ib;
		inBuf = ib;
		fluidObj.inBuffer_(inBuf);
	}

	outBuf_ {
		arg ob;
		outBuf = ob;
		fluidObj.outBuffer_(outBuf);
	}
}