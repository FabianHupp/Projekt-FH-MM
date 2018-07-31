package lockingTrains.shared.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import org.junit.BeforeClass;
import org.junit.Test;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;

public class ParserTest {
	private static Map saarbahn;

	@BeforeClass
	public static void setUpClass() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var mapResource = Objects.requireNonNull(loader.getResource("saarbahn.map"));
		final var file = new File(mapResource.toURI());
		final var reader = new BufferedReader(new FileReader(file));
		saarbahn = Parser.parseMap(reader);
	}

	@Test
	public void singleLocation() {
		final var locations = new ArrayList<Location>();
		assertTrue(Parser.parseLocation("Saarbruecken:-1:(1,2)", locations));

		assertEquals(1, locations.size());
		final var location = locations.get(0);

		assertEquals("Saarbruecken", location.name());
		assertTrue(location.isStation());
		assertEquals(1, location.x());
		assertEquals(2, location.y());
	}

	@Test
	public void multipleLocations() {
		final var locations = new ArrayList<Location>();
		assertTrue(Parser.parseLocation("Saarbruecken:-1:(0,0)", locations));
		assertTrue(Parser.parseLocation("Crossing:0:(1,-2)", locations));
		assertTrue(Parser.parseLocation("Siding:3:(-4,5)", locations));

		assertEquals(3, locations.size());
		final var saarbruecken = locations.stream().filter(l -> l.name().equals("Saarbruecken")).findAny().get();
		final var crossing = locations.stream().filter(l -> l.name().equals("Crossing")).findAny().get();
		final var siding = locations.stream().filter(l -> l.name().equals("Siding")).findAny().get();

		assertTrue(saarbruecken.isStation());
		assertFalse(crossing.isStation());
		assertFalse(siding.isStation());

		assertEquals(0, crossing.capacity());
		assertEquals(3, siding.capacity());

		assertEquals(0, saarbruecken.x());
		assertEquals(0, saarbruecken.y());
		assertEquals(1, crossing.x());
		assertEquals(-2, crossing.y());
		assertEquals(-4, siding.x());
		assertEquals(5, siding.y());
	}

	@Test(expected = RuntimeException.class)
	public void invalidLocationCapacity() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Location:-2:(0,0)", locations);
	}

	@Test
	public void missingLocationName() {
		final var locations = new ArrayList<Location>();
		assertFalse(Parser.parseLocation("0:(0,0)", locations));
		assertEquals(0, locations.size());
	}

	@Test
	public void missingLocationCapacity() {
		final var locations = new ArrayList<Location>();
		assertFalse(Parser.parseLocation("Location:(0,0)", locations));
		assertEquals(0, locations.size());
	}

	@Test
	public void missingLocationPosition() {
		final var locations = new ArrayList<Location>();
		assertFalse(Parser.parseLocation("Location:0", locations));
		assertEquals(0, locations.size());
	}

	@Test(expected = RuntimeException.class)
	public void duplicateLocationName() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Saarbruecken:-1:(0,0)", locations);
		Parser.parseLocation("Saarbruecken:-1:(1,2)", locations);
	}

	@Test
	public void singleConnection() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		assertTrue(
				Parser.parseConnection("Crossing1-Crossing2:10", Collections.unmodifiableList(locations), connections));

		assertEquals(1, connections.size());
		final var connection = connections.get(0);

		assertEquals("Crossing1", connection.first().name());
		assertEquals("Crossing2", connection.second().name());
		assertEquals(10, connection.time());
	}

	@Test
	public void multipleConnections() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);
		Parser.parseLocation("Crossing3:0:(0,0)", locations);
		Parser.parseLocation("Crossing4:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		assertTrue(
				Parser.parseConnection("Crossing1-Crossing2:10", Collections.unmodifiableList(locations), connections));
		assertTrue(
				Parser.parseConnection("Crossing2-Crossing3:20", Collections.unmodifiableList(locations), connections));
		assertTrue(
				Parser.parseConnection("Crossing3-Crossing4:30", Collections.unmodifiableList(locations), connections));

		assertEquals(3, connections.size());
		final var conn1 = connections.stream().filter(c -> c.first().name().equals("Crossing1")).findAny().get();
		final var conn2 = connections.stream().filter(c -> c.first().name().equals("Crossing2")).findAny().get();
		final var conn3 = connections.stream().filter(c -> c.first().name().equals("Crossing3")).findAny().get();

		assertEquals("Crossing2", conn1.second().name());
		assertEquals("Crossing3", conn2.second().name());
		assertEquals("Crossing4", conn3.second().name());

		assertEquals(10, conn1.time());
		assertEquals(20, conn2.time());
		assertEquals(30, conn3.time());
	}

	@Test
	public void missingFirstLocation() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		assertFalse(Parser.parseConnection("Crossing2:10", Collections.unmodifiableList(locations), connections));
		assertEquals(0, connections.size());
	}

	@Test
	public void missingSecondLocation() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		assertFalse(Parser.parseConnection("Crossing1:10", Collections.unmodifiableList(locations), connections));
		assertEquals(0, connections.size());
	}

	@Test
	public void missingConnectionTime() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		assertFalse(
				Parser.parseConnection("Crossing1-Crossing2", Collections.unmodifiableList(locations), connections));
		assertEquals(0, connections.size());
	}

	@Test(expected = RuntimeException.class)
	public void firstLocationNotFound() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		Parser.parseConnection("Crossing1-Crossing2:10", Collections.unmodifiableList(locations), connections);
	}

	@Test(expected = RuntimeException.class)
	public void secondLocationNotFound() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		Parser.parseConnection("Crossing1-Crossing2:10", Collections.unmodifiableList(locations), connections);
	}

	@Test(expected = RuntimeException.class)
	public void negativeTravelTime() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("Crossing1:0:(0,0)", locations);
		Parser.parseLocation("Crossing2:0:(0,0)", locations);

		final var connections = new ArrayList<Connection>();
		Parser.parseConnection("Crossing1-Crossing2:-10", Collections.unmodifiableList(locations), connections);
	}

	@Test
	public void simpleTrack() {
		final var locations = new ArrayList<Location>();
		assertTrue(Parser.parseLocation("CityA:-1:(1,1)", locations));
		assertTrue(Parser.parseLocation("Crossing1:0:(2,2)", locations));
		assertTrue(Parser.parseLocation("Siding:1:(3,3)", locations));
		assertTrue(Parser.parseLocation("Crossing2:-1:(4,4)", locations));
		assertTrue(Parser.parseLocation("CityB:-1:(5,5)", locations));

		final var connections = new ArrayList<Connection>();
		assertTrue(Parser.parseConnection("CityA-Crossing1:2", Collections.unmodifiableList(locations), connections));
		assertTrue(Parser.parseConnection("Crossing1-Siding:15", Collections.unmodifiableList(locations), connections));
		assertTrue(Parser.parseConnection("Siding-Crossing2:15", Collections.unmodifiableList(locations), connections));
		assertTrue(Parser.parseConnection("Crossing2-CityB:2", Collections.unmodifiableList(locations), connections));
	}

	@Test
	public void readMapFileSaarbahn() {
		assertEquals("Saarbahn", saarbahn.name());

		final var locations = saarbahn.locations();
		final var connections = saarbahn.connections();

		assertEquals(43, locations.size());
		assertEquals(42, connections.size());
	}

	@Test
	public void singleSchedule() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("CityA:-1:(0,0)", locations);
		Parser.parseLocation("CityB:-1:(0,0)", locations);

		final var schedules = new ArrayList<TrainSchedule>();
		assertTrue(Parser.parseSchedule("CityA->CityB", Collections.unmodifiableList(locations), schedules));

		assertEquals(1, schedules.size());
		final var schedule = schedules.get(0);

		assertEquals("CityA", schedule.origin().name());
		assertEquals("CityB", schedule.destination().name());
	}

	@Test
	public void multipleSchedules() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("CityA:-1:(0,0)", locations);
		Parser.parseLocation("CityB:-1:(0,0)", locations);

		final var schedules = new ArrayList<TrainSchedule>();
		assertTrue(Parser.parseSchedule("CityA->CityB", Collections.unmodifiableList(locations), schedules));
		assertTrue(Parser.parseSchedule("CityB->CityA", Collections.unmodifiableList(locations), schedules));

		assertEquals(2, schedules.size());
		final var schedule1 = schedules.stream().filter(t -> t.origin().name().equals("CityA")).findAny().get();
		final var schedule2 = schedules.stream().filter(t -> t.origin().name().equals("CityB")).findAny().get();

		assertEquals("CityB", schedule1.destination().name());
		assertEquals("CityA", schedule2.destination().name());
	}

	@Test
	public void missingDestination() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("CityA:0:(0,0)", locations);

		final var schedules = new ArrayList<TrainSchedule>();
		assertFalse(Parser.parseSchedule("CityA", Collections.unmodifiableList(locations), schedules));
		assertEquals(0, schedules.size());
	}

	@Test(expected = RuntimeException.class)
	public void destinationNotFound() {
		final var locations = new ArrayList<Location>();
		Parser.parseLocation("CityA:0:(0,0)", locations);

		final var schedules = new ArrayList<TrainSchedule>();
		Parser.parseSchedule("CityA->CityB", Collections.unmodifiableList(locations), schedules);
	}

	@Test
	public void readProblemFileSaarbahnEmpty() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_empty.problem"));
		final var file = new File(problemResource.toURI());
		final var reader = new BufferedReader(new FileReader(file));
		final var problem = Parser.parseProblem(reader, saarbahn);

		final var map = problem.map();
		final var schedules = problem.schedules();

		assertEquals(saarbahn.name(), map.name());
		assertEquals(0, schedules.size());
	}

	@Test
	public void readProblemFileSaarbahnSingle() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_single.problem"));
		final var file = new File(problemResource.toURI());
		final var reader = new BufferedReader(new FileReader(file));
		final var problem = Parser.parseProblem(reader, saarbahn);

		final var map = problem.map();
		final var schedules = problem.schedules();

		assertEquals(saarbahn.name(), map.name());
		assertEquals(1, schedules.size());

		final var schedule = schedules.get(0);

		assertEquals("Sarreguemines", schedule.origin().name());
		assertEquals("Lebach_Jabach", schedule.destination().name());
	}

	@Test
	public void readProblemFileSaarbahnOpposing() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_opposing.problem"));
		final var file = new File(problemResource.toURI());
		final var reader = new BufferedReader(new FileReader(file));
		final var problem = Parser.parseProblem(reader, saarbahn);

		final var map = problem.map();
		final var schedules = problem.schedules();

		assertEquals(saarbahn.name(), map.name());
		assertEquals(2, schedules.size());

		assertEquals(2, schedules.size());
		final var schedule1 = schedules.stream().filter(t -> t.origin().name().equals("Sarreguemines")).findAny().get();
		final var schedule2 = schedules.stream().filter(t -> t.origin().name().equals("Lebach_Jabach")).findAny().get();

		assertEquals("Lebach_Jabach", schedule1.destination().name());
		assertEquals("Sarreguemines", schedule2.destination().name());
	}

	@Test
	public void completeSaarbahnEmpty() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var mapResource = Objects.requireNonNull(loader.getResource("saarbahn.map"));
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_empty.problem"));
		final var mapFile = mapResource.toURI();
		final var problemFile = problemResource.toURI();

		final var problem = Parser.parse(new File(mapFile), new File(problemFile));

		assertEquals("Saarbahn", problem.map().name());
		assertEquals(0, problem.schedules().size());
	}

	@Test
	public void completeSaarbahnSingle() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var mapResource = Objects.requireNonNull(loader.getResource("saarbahn.map"));
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_single.problem"));
		final var mapFile = mapResource.toURI();
		final var problemFile = problemResource.toURI();

		final var problem = Parser.parse(new File(mapFile), new File(problemFile));

		assertEquals("Saarbahn", problem.map().name());
		assertEquals(1, problem.schedules().size());
	}

	@Test
	public void completeSaarbahnOpposing() throws IOException, URISyntaxException {
		final var loader = ClassLoader.getSystemClassLoader();
		final var mapResource = Objects.requireNonNull(loader.getResource("saarbahn.map"));
		final var problemResource = Objects.requireNonNull(loader.getResource("saarbahn_opposing.problem"));
		final var mapFile = mapResource.toURI();
		final var problemFile = problemResource.toURI();

		final var problem = Parser.parse(new File(mapFile), new File(problemFile));

		assertEquals("Saarbahn", problem.map().name());
		assertEquals(2, problem.schedules().size());
	}
}