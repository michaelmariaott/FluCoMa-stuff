CSVToFluidDataSet {
	*new {
		arg server, csv_path, hasLabelColumn, hasHeaderRow, action;
		^super.new.init(server, csv_path,hasLabelColumn, hasHeaderRow, action);
	}

	init {
		arg server, csv_path, hasLabelColumn, hasHeaderRow, action;
		Routine{
			var csv_pn = PathName(csv_path);
			var csv = CSVFileReader.read(csv_path,true);
			var headers;
			var dict = Dictionary.newFrom([
				"cols",csv[0].size,
				"data",Dictionary.new
			]);
			var ds;

			csv.do({
				arg line, i;
				if((i == 0) && hasHeaderRow,{
					headers = line;
					if(hasLabelColumn,{
						headers = headers[1..];
					});
					ArrayToCSV(headers.collect{arg head,i;[i,head]},csv_pn.pathOnly+/+"headers.csv");
				},{
					if(hasLabelColumn,{
						dict.at("data").put(line[0],line[1..].collect(_.interpret));
					},{
						dict.at("data").put(i.asString,line.collect(_.interpret));
					});
				});
			});

			ds = FluidDataSet(server);

			server.sync;

			ds.load(dict);

			server.sync;

			ds.write(csv_pn.pathOnly+/+csv_pn.fileNameWithoutExtension++".json");

			server.sync;

			action.value
		}.play;
	}
}