package tmt.tcs.m3;

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

public class M3Test extends JavaTestKit {
	private static ActorSystem system;
	private static LoggingAdapter logger;
	private static String hcdName = "m3Hcd";

	private static Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(10, TimeUnit.SECONDS));
	@SuppressWarnings("unused")
	private static IEventService eventService;

	private static List<ActorRef> hcdActors = Collections.emptyList();
	
	public static final Double azValue = 1.0;
	public static final Double elValue = 2.0;
	public static final Double timeValue = 3.0;
	public static final Double xValue = 1.1;
	public static final Double yValue = 1.2;

	public M3Test() {
		super(system);
	}

	@BeforeClass
	public static void setup() throws Exception {
		System.out.println("Inside M3Test setup");

		LocationService.initInterface();

		system = ActorSystem.create("m3Hcd");
		logger = Logging.getLogger(system, system);

		eventService = IEventService.getEventService(IEventService.defaultName, system, timeout).get(5,
				TimeUnit.SECONDS);

		Map<String, String> configMap = Collections.singletonMap("", "hcd/m3Hcd.conf");
		ContainerCmd cmd = new ContainerCmd("m3Hcd", new String[] { "--standalone" }, configMap);
		hcdActors = cmd.getActors();
		if (hcdActors.size() == 0)
			logger.error("Inside M3Test Failed to create M3 HCD");
		Thread.sleep(2000);// XXX FIXME Make sure components have time to
								// register from location service

		SequencerEnv.resolveHcd(hcdName);

	}

	@Test
	public void test1() {
		System.out.println("Inside M3Test test1 Move Command");

		TestProbe fakeSupervisor = new TestProbe(system);
		ActorRef M3Assembly = newM3Assembly(fakeSupervisor.ref());
		TestProbe fakeClient = new TestProbe(system);
		
		SetupConfig moveSc = jadd(new SetupConfig(M3Config.positionDemandCK.prefix()),
			      jset(M3Config.azDemandKey, azValue),
			      jset(M3Config.elDemandKey, elValue),
			      jset(M3Config.timeDemandKey, timeValue));
		
		fakeSupervisor.expectMsg(Initialized);
		fakeSupervisor.send(M3Assembly, Running);

		SetupConfigArg sca = Configurations.createSetupConfigArg("m3MoveCommand", moveSc);

		fakeClient.send(M3Assembly, new Submit(sca));

		CommandResult acceptedMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		assertEquals(acceptedMsg.overall(), Accepted);
		System.out.println("Inside M3Test test1 Command Accepted Result: " + acceptedMsg);

		CommandResult completeMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		System.out.println("Inside M3Test test1 Command Result: " + completeMsg.details().status(0));

		assertEquals(completeMsg.overall(), Accepted);

	}
	
	@Test
	public void test2() {
		System.out.println("Inside M3Test test2 Offset Command");

		TestProbe fakeSupervisor = new TestProbe(system);
		ActorRef M3Assembly = newM3Assembly(fakeSupervisor.ref());
		TestProbe fakeClient = new TestProbe(system);
		
		SetupConfig offsetSc = jadd(new SetupConfig(M3Config.offsetDemandCK.prefix()),
			      jset(M3Config.xDemandKey, xValue),
			      jset(M3Config.yDemandKey, yValue));

		fakeSupervisor.expectMsg(Initialized);
		fakeSupervisor.send(M3Assembly, Running);

		SetupConfigArg sca = Configurations.createSetupConfigArg("m3OffsetCommand", offsetSc);

		fakeClient.send(M3Assembly, new Submit(sca));

		CommandResult acceptedMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		assertEquals(acceptedMsg.overall(), Accepted);
		System.out.println("Inside M3Test test2 Command Accepted Result: " + acceptedMsg);

		CommandResult completeMsg = fakeClient.expectMsgClass(duration("3 seconds"), CommandResult.class);
		System.out.println("Inside M3Test test2 Command Result: " + completeMsg.details().status(0));

		assertEquals(completeMsg.overall(), Accepted);

	}

	Props getM3Props(AssemblyInfo assemblyInfo, Optional<ActorRef> supervisorIn) {
		if (!supervisorIn.isPresent())
			return M3Assembly.props(assemblyInfo, new TestProbe(system).ref());
		return M3Assembly.props(assemblyInfo, supervisorIn.get());
	}

	ActorRef newM3Assembly(ActorRef supervisor) {
		String componentName = "M3Assembly";
		String componentClassName = "tmt.tcs.m3.M3Assembly";
		String componentPrefix = "tcs.m3";

		ComponentId hcdId = JComponentId.componentId("m3Hcd", JComponentType.HCD);
		Component.AssemblyInfo assemblyInfo = JComponent.assemblyInfo(componentName, componentPrefix,
				componentClassName, RegisterAndTrackServices, Collections.singleton(AkkaType),
				Collections.singleton(new Connection.AkkaConnection(hcdId)));

		Props props = getM3Props(assemblyInfo, Optional.of(supervisor));
		expectNoMsg(duration("300 millis"));
		return system.actorOf(props);
	}

	@AfterClass
	public static void teardown() throws InterruptedException {
		System.out.println("Inside M3Test teardown");

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
