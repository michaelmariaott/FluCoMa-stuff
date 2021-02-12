// if you highlight all and cmd-return, it should run...
(
s.options.device_("Scarlett 6i6 USB");
s.waitForBoot({
	Task({
		var uvs; // userviews that will flash showing current cluster prediction
		var win; // window where the colors flash
		var bus = Bus.audio(s,2);
		var k_ = 4;
		var colors = k_.collect({arg i; Color.hsv(i.linlin(0,k_,0,1),0.8,1)});
		var synth;
		var audioBuf = Buffer.read(a,"/Volumes/Ted's 10TB My Book (June 2020)/AUDIO ENGINEERING/200722 bruit/from PA/201005 dithers/dithered-HD/06 August 8, 2019 (live).wav");

		~intensity_pow = 2;
		~thresh = 0.8;
		s.sync;

		// play sound
		synth = {
			arg rate = 1;
			var sig = PlayBuf.ar(audioBuf.numChannels,audioBuf,rate,startPos:rrand(0,audioBuf.numFrames-1),loop:1);
			Out.ar(bus,Mix(sig));
			sig * -10.dbamp;
		}.play;

		// window for showing the current cluster prediction
		Window.closeAll;
		win = Window("",Rect(Window.screenBounds.width + 100,100,1800,800)).front;
		win.background_(Color.black);
		uvs = k_.collect({
			arg i;
			var width = win.bounds.width / k_;
			var uv = UserView(win,Rect(width * i,0,width,win.bounds.height));
			uv.background_(colors[i].alpha_(0));
		});

		~lpv = LivePCAVariance(s,bus,k_,{
			arg vals;
			// vals.postln;
			vals = (vals * ~intensity_pow).softclip;
			defer{
				vals.do({
					arg val, i;
					uvs[i].background_(colors[i].alpha_(val));
				});
			};
		});
	},AppClock).play;
});
)

~lpv.lagtime_(2);
~thresh = 0.4;