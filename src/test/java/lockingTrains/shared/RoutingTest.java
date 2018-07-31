package lockingTrains.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RoutingTest {
	@Test
	public void alreadyThere() {
		final var locations = new ArrayList<Location>();
		final var saarbruecken = new Location("Saarbruecken", Location.Capacity.INFINITE, 0, 0);
		locations.add(saarbruecken);

		final var map = new Map("", locations, Collections.emptyList());

		final var route = map.route(saarbruecken, saarbruecken, Collections.emptyList());

		assertEquals(0, route.size());
	}

	@Test
	public void singleStep() {
		final var locations = new ArrayList<Location>();
		final var saarbruecken = new Location("Saarbruecken", Location.Capacity.INFINITE, 0, 0);
		final var voelklingen = new Location("Voelklingen", Location.Capacity.INFINITE, 0, 0);
		locations.add(saarbruecken);
		locations.add(voelklingen);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(saarbruecken, voelklingen, 10));

		final var map = new Map("", locations, connections);

		final var route1 = map.route(saarbruecken, voelklingen, Collections.emptyList());
		final var route2 = map.route(voelklingen, saarbruecken, Collections.emptyList());

		assertEquals(1, route1.size());
		assertEquals(1, route2.size());

		assertEquals(10, route1.get(0).time());
		assertEquals(10, route2.get(0).time());
	}

	@Test
	public void multipleSteps() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final var c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		final var d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 10));
		connections.add(new Connection(b, c, 20));
		connections.add(new Connection(c, d, 30));

		final var map = new Map("", locations, connections);

		final var route = map.route(d, a, Collections.emptyList());

		assertEquals(3, route.size());

		assertEquals(30, route.get(0).time());
		assertEquals(20, route.get(1).time());
		assertEquals(10, route.get(2).time());
	}

	@Test
	public void shorterRoute() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final var c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		final var d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 10));
		connections.add(new Connection(b, c, 20));
		connections.add(new Connection(c, d, 30));
		connections.add(new Connection(b, d, 45));

		final var map = new Map("", locations, connections);

		final var route = map.route(d, a, Collections.emptyList());

		assertEquals(2, route.size());

		assertEquals(45, route.get(0).time());
		assertEquals(10, route.get(1).time());
	}

	@Test
	public void avoidShortcut() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final var c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		final var d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		final var e = new Location("E", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);
		locations.add(e);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 10));
		connections.add(new Connection(b, c, 20));
		connections.add(new Connection(c, d, 30));
		final var shortCut = new Connection(a, e, 40);
		connections.add(shortCut);

		final var map = new Map("", locations, connections);

		final var route = map.route(d, a, List.of(shortCut));

		assertEquals(3, route.size());

		assertEquals(30, route.get(0).time());
		assertEquals(20, route.get(1).time());
		assertEquals(10, route.get(2).time());
	}

	@Test
	public void impossibleRouteConnection() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final var c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);

		final var connections = new ArrayList<Connection>();
		final var connection = new Connection(a, b, 10);
		connections.add(connection);
		connections.add(new Connection(b, c, 20));

		final var map = new Map("", locations, connections);

		assertNull(map.route(c, a, List.of(connection)));
	}

	@Test
	public void impossibleRouteLocation() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 10));

		final var map = new Map("", locations, connections);

		assertNull(map.route(b, a, List.of(a)));
		assertNull(map.route(b, a, List.of(b)));
	}

	@Test
	public void avoidStation() {
		final var locations = new ArrayList<Location>();
		final var a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final var b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final var c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 10));
		connections.add(new Connection(b, c, 10));
		connections.add(new Connection(a, c, 50));

		final var map = new Map("", locations, connections);

		final var route = map.route(c, a, List.of(b));

		assertEquals(1, route.size());

		assertEquals(50, route.get(0).time());
	}
}
