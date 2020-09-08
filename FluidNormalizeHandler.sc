FluidNormalizeHandler {
	var fluidNormalize, server, ndims, <inBus, <outBus, <inBuf, <outBuf;

	*new {
		arg server, ndims, min = 0, max = 1, invert = 0;
		^super.new.init(server,ndims,min,max,invert);
	}

	init {
		arg server_, ndims_, min, max, invert;
		server = server_;
		ndims = ndims_;
		fluidNormalize = FluidNormalize(server,min,max,invert);
		inBus = Bus.control(server);
		outBus = Bus.control(server);
		inBuf = Buffer.alloc(server,ndims);
		outBuf = Buffer.alloc(server,ndims);

		fluidNormalize.inBus_(inBus).outBus_(outBus).inBuffer_(inBuf).outBuffer_(outBuf);
		^this;
	}

	fit {
		arg dataSet, action;
		fluidNormalize.fit(dataSet,action);
	}

	fitTransform {
		arg sourceDataSet, destDataSet, action;
		fluidNormalize.fitTransform(sourceDataSet,destDataSet,action);
	}

	transformPoint {
		arg sourceBuffer, destBuffer, action;
		fluidNormalize.transformPoint(sourceBuffer,destBuffer,action);
	}

	invert {
		^fluidNormalize.invert;
	}

	invert_ {
		var inv;
		fluidNormalize.invert_(inv);
	}

	read {
		arg path;
		fluidNormalize.read(path);
	}
}