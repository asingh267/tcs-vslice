package tmt.tcs.m3;

import static javacsw.util.config.JConfigDSL.cs;
import static javacsw.util.config.JItems.jset;

import csw.util.config.BooleanKey;
import csw.util.config.DoubleKey;
import csw.util.config.IntKey;
import csw.util.config.Configurations.ConfigKey;
import csw.util.config.StateVariable.CurrentState;

/*
 * This class contains all the configurations specific to M3 Assembly
 */
public class M3Config {
	public static final String m3Prefix = "tcs.m3";
	public static final String m3TpkPrefix = "tcs.str.m3";
	public static final String initPrefix = m3Prefix + ".init";
	public static final String movePrefix = m3Prefix + ".move";
	public static final String offsetPrefix = m3Prefix + ".offset";
	public static final String m3StatePrefix = m3Prefix + ".m3State";
	public static final String m3StatsPrefix = m3Prefix + ".stats";
	public static final String positionDemandPrefix = m3TpkPrefix + ".positiondemands";
	public static final String offsetDemandPrefix = m3TpkPrefix + ".offsetdemands";
	public static final String currentPosPrefix = m3Prefix + ".currentposition";

	public static final ConfigKey dummyCK = new ConfigKey(m3Prefix);
	public static final ConfigKey initCK = new ConfigKey(initPrefix);
	public static final ConfigKey moveCK = new ConfigKey(movePrefix);
	public static final ConfigKey offsetCK = new ConfigKey(offsetPrefix);
	public static final ConfigKey m3StateCK = new ConfigKey(m3StatePrefix);
	public static final ConfigKey m3StatsCK = new ConfigKey(m3StatsPrefix);
	public static final ConfigKey positionDemandCK = new ConfigKey(positionDemandPrefix);
	public static final ConfigKey offsetDemandCK = new ConfigKey(offsetDemandPrefix);
	public static final ConfigKey currentPosCK = new ConfigKey(currentPosPrefix);

	public static final DoubleKey azDemandKey = new DoubleKey("tcs.str.m3.az");
	public static final DoubleKey elDemandKey = new DoubleKey("tcs.str.m3.el");
	public static final DoubleKey timeDemandKey = new DoubleKey("tcs.str.m3.time");
	public static final DoubleKey xDemandKey = new DoubleKey("tcs.str.m3.x");
	public static final DoubleKey yDemandKey = new DoubleKey("tcs.str.m3.y");

	public static final DoubleKey az = new DoubleKey("tcs.m3.az");
	public static final DoubleKey el = new DoubleKey("tcs.m3.el");
	public static final DoubleKey time = new DoubleKey("tcs.m3.time");
	public static final DoubleKey x = new DoubleKey("tcs.m3.x");
	public static final DoubleKey y = new DoubleKey("tcs.m3.y");

	public static final DoubleKey azPosKey = new DoubleKey("tcs.m3.az_pos");
	public static final DoubleKey azPosDemandKey = new DoubleKey("tcs.m3.az_pos_demand");
	public static final DoubleKey azPosErrorKey = new DoubleKey("tcs.m3.az_pos_error");
	public static final BooleanKey azInpositionKey = new BooleanKey("tcs.m3.az_inposition");
	public static final DoubleKey elPosKey = new DoubleKey("tcs.m3.el_pos");
	public static final DoubleKey elPosDemandKey = new DoubleKey("tcs.m3.el_pos_demand");
	public static final DoubleKey elPosErrorKey = new DoubleKey("cs.m3.el_pos_error");
	public static final BooleanKey elInpositionKey = new BooleanKey("tcs.m3.el_inposition");
	public static final DoubleKey encoderLatchTimeKey = new DoubleKey("tcs.m3.encoder_latching_time");
	public static final IntKey azPosDmdErrcnt = new IntKey("tcs.m3.az_pos_dmd_errcnt");
	public static final IntKey elPosDmdErrcnt = new IntKey("tcs.m3.el_pos_dmd_errcnt");
	public static final DoubleKey posTimeKey = new DoubleKey("tcs.m3.time");

	public static final CurrentState defaultM3StatsState = cs(m3StatsCK.prefix(), jset(az, 1.0));

}
