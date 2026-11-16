package ch.realcinema.vendor.jaad.aac.syntax;

import ch.realcinema.vendor.jaad.aac.AACException;
import ch.realcinema.vendor.jaad.aac.DecoderConfig;

class SCE_LFE extends Element {

	private final ICStream ics;

	SCE_LFE(int frameLength) {
		super();
		ics = new ICStream(frameLength);
	}

	void decode(BitStream in, DecoderConfig conf) throws AACException {
		readElementInstanceTag(in);
		ics.decode(in, false, conf);
	}

	public ICStream getICStream() {
		return ics;
	}
}
