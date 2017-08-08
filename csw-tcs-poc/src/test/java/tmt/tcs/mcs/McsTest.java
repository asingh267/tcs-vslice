package tmt.tcs.mcs;

import static javacsw.services.ccs.JCommandStatus.Accepted;
import static javacsw.services.loc.JConnectionType.AkkaType;
import static javacsw.services.pkg.JComponent.RegisterAndTrackServices;
import static javacsw.services.pkg.JSupervisor.HaltComponent;
import static javacsw.services.pkg.JSupervisor.Initialized;
import static javacsw.services.pkg.JSupervisor.Running;
import static javacsw.util.config.JItems.jadd;
import static javacsw.util.config.JItems.jset;
import static junit.framework.TestCase.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import akka.util.Timeout;
import csw.services.apps.containerCmd.ContainerCmd;
import csw.services.ccs.AssemblyController.Submit;
import csw.services.ccs.CommandStatus.CommandResult;
import csw.services.loc.ComponentId;
import csw.services.loc.Connection;
import csw.services.loc.LocationService;
import csw.services.pkg.Component;
import csw.services.pkg.Component.AssemblyInfo;
import csw.services.sequencer.SequencerEnv;
import csw.util.config.Configurations;
import csw.util.config.Configurations.SetupConfig;
import csw.util.config.Configurations.SetupConfigArg;
import javacsw.services.events.IEventService;
import javacsw.services.loc.JComponentId;
import javacsw.services.loc.JComponentType;
import javacsw.services.pkg.JComponent;
import scala.concurrent.duration.FiniteDuration;

public class McsTest extends JavaTestKit {
	private static ActorSystem system;
	private static LoggingAdapter logger;
	private static String hcdName = "mcsHcd";

	private static Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(10, TimeUnit.SECONDS));
	@SuppressWarnings("unused")
	private static IEventService eventService;

	private static List<ActorRef> hcdActors = Collections.emptyList();

	public static final Double azValue = 1.0;
	public static final Double elValue = 2.0;
	public static final Double timeValue = 3.0;
	public static final Double xValue = 1.1;
	public static final Double yValue = 1.2;

	public McsTest() {
		super(system);
	}

	@BeforeClass
	public static void setup() throws Exception {
		System.out.println("Inside McsTest setup");

		LocationService.initInterface();

		system = ActorSystem.create("mcsHcd");
		logger = Logging.getLogger(system, system);

		eventService = IEventService.getEventService(IEventService.defaultName, system, timeout).get(5,
				TimeUnit.SECONDS);

		Map<String, String> configMap = Collections.singletonMap("", "hcd/mcsHcd.conf");
		ContainerCmd cmd = new ContainerCmd("mcsHcd", new String[] { "--standalone" }, configMap);
		hcdActors = cmd.getActors();
		if (hcdActors.size() == 0)
			logger.error("Inside McsTest Failed to create Mcs HCD");
		Thread.sleep(2000);// XXX FIXME Make sure components have time to
							// register from location service

		SequencerEnv.resolveHcd(hcdName);

	}

	@Test
	public void test1() {
		System.out.println("Inside McsTest test1 Move Command");

		TestProbe fakeSupervisor = new TestProbe(system);
		ActorRef mcsAssembly = newMcsAssembly(fakeSupervisor.ref());
		TestProbe fakeClient = new TestProbe(system);

		SetupConfig moveSc = jadd(new SetupConfig(McsConfig.positionDemandCK.prefix()),
				jset(McsConfig.azDemandKey, azValue), jset(McsConfig.elDemandKey, elValue),
				jset(McsConfig.timeDemandKey, timeValue));

		fakeSupervisor.expectMsg(Initialized);
		fakeSupervisor.send(mcsAssembly, Running);

		SetupConfigArg sca = Configurations.createSetupConfigArg("mcsMoveCommand", moveSc);

		fakeClient.send(mcsAssembly, new Submit(sca));

		CommandResult acceptedMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		assertEquals(acceptedMsg.overall(), Accepted);
		System.out.println("Inside McsTest test1 Command Accepted Result: " + acceptedMsg);

		CommandResult completeMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		System.out.println("Inside McsTest test1 Command Result: " + completeMsg.details().status(0));

		assertEquals(completeMsg.overall(), Accepted);

	}

	@Test
	public void test2() {
		System.out.println("Inside McsTest test2 Offset Command");

		TestProbe fakeSupervisor = new TestProbe(system);
		ActorRef mcsAssembly = newMcsAssembly(fakeSupervisor.ref());
		TestProbe fakeClient = new TestProbe(system);

		SetupConfig offsetSc = jadd(new SetupConfig(McsConfig.offsetDemandCK.prefix()),
				jset(McsConfig.xDemandKey, xValue), jset(McsConfig.yDemandKey, yValue));

		fakeSupervisor.expectMsg(Initialized);
		fakeSupervisor.send(mcsAssembly, Running);

		SetupConfigArg sca = Configurations.createSetupConfigArg("mcsOffsetCommand", offsetSc);

		fakeClient.send(mcsAssembly, new Submit(sca));

		CommandResult acceptedMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		assertEquals(acceptedMsg.overall(), Accepted);
		System.out.println("Inside McsTest test2 Command Accepted Result: " + acceptedMsg);

		CommandResult completeMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		System.out.println("Inside McsTest test2 Command Result: " + completeMsg.details().status(0));

		assertEquals(completeMsg.overall(), Accepted);

	}

	Props getMcsProps(AssemblyInfo assemblyInfo, Optional<ActorRef> supervisorIn) {
		if (!supervisorIn.isPresent())
			return McsAssembly.props(assemblyInfo, new TestProbe(system).ref());
		return McsAssembly.props(assemblyInfo, supervisorIn.get());
	}

	ActorRef newMcsAssembly(ActorRef supervisor) {
		String componentName = "mcsAssembly";
		String componentClassName = "tmt.tcs.mcs.McsAssembly";
		String componentPrefix = "tcs.mcs";

		ComponentId hcdId = JComponentId.componentId("mcsHcd", JComponentType.HCD);
		Component.AssemblyInfo assemblyInfo = JComponent.assemblyInfo(componentName, componentPrefix,
				componentClassName, RegisterAndTrackServices, Collections.singleton(AkkaType),
				Collections.singleton(new Connection.AkkaConnection(hcdId)));

		Props props = getMcsProps(assemblyInfo, Optional.of(supervisor));
		expectNoMsg(duration("300 millis"));
		return system.actorOf(props);
	}

	@AfterClass
	public static void teardown() throws InterruptedException {
		System.out.println("Inside McsTest teardown");

		hcdActors.forEach(actorRef -> {
			TestProbe probe = new TestProbe(system);
			probe.watch(actorRef);
			actorRef.tell(HaltComponent, ActorRef.noSender());
			probe.expectTerminated(actorRef, timeout.duration());
		});
		JavaTestKit.shutdownActorSystem(system);
		system = null;
		Thread.sleep(10000); // XXX FIXME Make sure components have time to
								// unregister from location service
	}
}
