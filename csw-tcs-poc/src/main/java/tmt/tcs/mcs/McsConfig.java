package tmt.tcs.mcs;

import static javacsw.util.config.JConfigDSL.cs;
import static javacsw.util.config.JItems.jset;

import csw.util.config.BooleanKey;
import csw.util.config.Configurations.ConfigKey;
import csw.util.config.DoubleKey;
import csw.util.config.IntKey;
import csw.util.config.StateVariable.CurrentState;

/*
 * This class contains all the configurations specific to MCS Assembly
 */
public class McsConfig {

	public static final String mcsPrefix = "tcs.mcs";
	public static final String mcsTpkPrefix = "tcs.str.mcs";
	public static final String initPrefix = mcsPrefix + ".init";
	public static final String movePrefix = mcsPrefix + ".move";
	public static final String offsetPrefix = mcsPrefix + ".offset";
	public static final String mcsStatePrefix = mcsPrefix + ".mcsState";
	public static final String mcsStatsPrefix = mcsPrefix + ".stats";
	public static final String positionDemandPrefix = mcsTpkPrefix + ".positiondemands";
	public static final String offsetDemandPrefix = mcsTpkPrefix + ".offsetdemands";
	public static final String currentPosPrefix = mcsPrefix + ".currentposition";

	public static final ConfigKey dummyCK = new ConfigKey(mcsPrefix);
	public static final ConfigKey initCK = new ConfigKey(initPrefix);
	public static final ConfigKey moveCK = new ConfigKey(movePrefix);
	public static final ConfigKey offsetCK = new ConfigKey(offsetPrefix);
	public static final ConfigKey mcsStateCK = new ConfigKey(mcsStatePrefix);
	public static final ConfigKey mcsStatsCK = new ConfigKey(mcsStatsPrefix);
	public static final ConfigKey positionDemandCK = new ConfigKey(positionDemandPrefix);
	public static final ConfigKey offsetDemandCK = new ConfigKey(offsetDemandPrefix);
	public static final ConfigKey currentPosCK = new ConfigKey(currentPosPrefix);
	
	public static final DoubleKey azDemandKey = new DoubleKey("tcs.str.mcs.az");
	public static final DoubleKey elDemandKey = new DoubleKey("tcs.str.mcs.el");
	public static final DoubleKey timeDemandKey = new DoubleKey("tcs.str.mcs.time");
	public static final DoubleKey xDemandKey = new DoubleKey("tcs.str.mcs.x");
	public static final DoubleKey yDemandKey = new DoubleKey("tcs.str.mcs.y");

	public static final DoubleKey az = new DoubleKey("tcs.mcs.az");
	public static final DoubleKey el = new DoubleKey("tcs.mcs.el");
	public static final DoubleKey time = new DoubleKey("tcs.mcs.time");
	public static final DoubleKey x = new DoubleKey("tcs.mcs.x");
	public static final DoubleKey y = new DoubleKey("tcs.mcs.y");
	
	public static final DoubleKey azPosKey = new DoubleKey("tcs.mcs.az_pos");
	public static final DoubleKey azPosDemandKey = new DoubleKey("tcs.mcs.az_pos_demand");
	public static final DoubleKey azPosErrorKey = new DoubleKey("tcs.mcs.az_pos_error");
	public static final BooleanKey azInpositionKey = new BooleanKey("tcs.mcs.az_inposition");
	public static final DoubleKey elPosKey = new DoubleKey("tcs.mcs.el_pos");
	public static final DoubleKey elPosDemandKey = new DoubleKey("tcs.mcs.el_pos_demand");
	public static final DoubleKey elPosErrorKey = new DoubleKey("cs.mcs.el_pos_error");
	public static final BooleanKey elInpositionKey = new BooleanKey("tcs.mcs.el_inposition");
	public static final DoubleKey encoderLatchTimeKey = new DoubleKey("tcs.mcs.encoder_latching_time");
	public static final IntKey azPosDmdErrcnt = new IntKey("tcs.mcs.az_pos_dmd_errcnt");
	public static final IntKey elPosDmdErrcnt = new IntKey("tcs.mcs.el_pos_dmd_errcnt");
	public static final DoubleKey posTimeKey = new DoubleKey("tcs.mcs.time");
	
	public static final CurrentState defaultMcsStatsState = cs(mcsStatsCK.prefix(), jset(az, 1.0));

}
