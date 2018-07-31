package lockingTrains.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * <p>
 * Representation of a map. A map is simply a (named) collection of locations
 * and connections.
 * </p>
 *
 * <p>
 * Maps do not know where trains are, and you should <strong>not</strong> add
 * this. Instead, trains have to manage their own position.
 * </p>
 *
 * @see Location
 * @see Connection
 */
public class Map {
	private final String name;
	private final List<Location> locations;
	private final List<Connection> connections;

	/**
	 * Constructs a new map.
	 *
	 * @param name        of the map.
	 * @param locations   list of all map locations.
	 * @param connections list of all connections between map locations.
	 */
	public Map(final String name, final List<Location> locations, final List<Connection> connections) {
		this.name = name;
		this.locations = locations;
		this.connections = connections;
	}

	/**
	 * Get the name of the map.
	 *
	 * @return The name of the map.
	 */
	public String name() {
		return name;
	}

	/**
	 * Get the list of map locations. The result cannot be modified.
	 *
	 * @return The (unmodifiable) list of map locations.
	 */
	public List<Location> locations() {
		return Collections.unmodifiableList(locations);
	}

	/**
	 * Get the list of connections between map locations. The result cannot be
	 * modified.
	 *
	 * @return The (unmodifiable) list of connections.
	 */
	public List<Connection> connections() {
		return Collections.unmodifiableList(connections);
	}

	/**
	 * Computes a shortest route between two locations on the map, possibly avoiding
	 * some specific connections.
	 * <p>
	 * <b>Note:</b> The result will be {@code null} if there is no route between
	 * {@code origin} and {@code destination} given the connections to avoid. It
	 * will be an empty route if {@code origin.equals(destination)}, however.
	 * <p>
	 * <b>Further note:</b> On success, the result will be a list of connections. In
	 * particular, this means a route from {@code A} to {@code
	 * C} over {@code B} may be {@code [(A,B), (C,B)]} rather than
	 * {@code [(A,B), (B,C)]}. See {@link Connection} for details.
	 * <p>
	 * <b>Important note:</b> For this method to work properly, {@code avoid} may
	 * only contain references that are stored in this {@link Map}.
	 *
	 * @param origin      starting point of the route.
	 * @param destination end point of the route.
	 * @param avoid       list of {@link Position}s to avoid.
	 *
	 * @return {@code null} if there is no route and a valid route otherwise.
	 *
	 * @see Connection
	 */
	public List<Connection> route(final Location origin, final Location destination, final Collection<Position> avoid) {
		if (avoid.contains(origin) || avoid.contains(destination))
			return null;

		final var locations = new ArrayList<>(this.locations);
		locations.removeIf(avoid::contains);

		final var queue = new PriorityQueue<RoutableLocation>();
		locations.forEach(l -> queue.offer(new RoutableLocation(l, -1)));

		queue.removeIf(l -> l.location.equals(origin));
		queue.offer(new RoutableLocation(origin, 0));

		final var connections = new ArrayList<>(this.connections);
		connections.removeIf(avoid::contains);

		RoutableLocation last = null;
		while (!queue.isEmpty()) {
			final var current = queue.poll();

			assert current != null;
			if (current.distance == -1 || current.location.equals(destination)) {
				last = current;
				break;
			}

			connections.stream().filter(c -> connectsToUnvisitedLocation(c, current.location, queue))
					.map(c -> toRoutableLocation(c, current)).forEach(l -> addIfShorter(l, queue));
		}

		if (last == null || last.distance == -1)
			return null;

		final var route = new LinkedList<Connection>();
		while (last.prev != null) {
			route.addFirst(last.reachedVia);
			last = last.prev;
		}

		return route;
	}

	/**
	 * Tests whether the given connection connects to location not visited yet.
	 *
	 * @param candidate the connection in question.
	 * @param current   the current location.
	 * @param queue     the collection of unvisited location.
	 *
	 * @return {@code true} if the connection leads to an unvisited location.
	 */
	private boolean connectsToUnvisitedLocation(final Connection candidate, final Location current,
			final PriorityQueue<RoutableLocation> queue) {
		if (!candidate.first().equals(current) && !candidate.second().equals(current))
			return false;

		return queue.stream()
				.anyMatch(l -> l.location.equals(candidate.first()) || l.location.equals(candidate.second()));
	}

	/**
	 * Creates a {@link RoutableLocation} for the target of a given location.
	 *
	 * @param connection for which to create the routable location.
	 * @param current    the current location.
	 *
	 * @return The other location of the connection as routable location.
	 */
	private RoutableLocation toRoutableLocation(final Connection connection, final RoutableLocation current) {
		Location location;
		if (connection.first().equals(current.location))
			location = connection.second();
		else
			location = connection.first();

		return new RoutableLocation(location, current.distance + connection.time(), connection, current);
	}

	/**
	 * Updates the unprocessed locations if the given location is reached by a
	 * shorter route. Thus, the location may move forward in processing order.
	 *
	 * @param reached the location that was reached.
	 * @param queue   the collection of unprocessed locations.
	 */
	private void addIfShorter(final RoutableLocation reached, final PriorityQueue<RoutableLocation> queue) {
		if (queue.removeIf(l -> l.location.equals(reached.location) && l.compareTo(reached) > 0))
			queue.offer(reached);
	}

	/**
	 * Wrapper for locations to store routing information.
	 */
	private class RoutableLocation implements Comparable<RoutableLocation> {
		final Location location;
		final int distance;
		final Connection reachedVia;
		final RoutableLocation prev;

		/**
		 * Constructs a new routable location.
		 *
		 * @param location to base on.
		 * @param distance form the routing origin.
		 */
		RoutableLocation(final Location location, final int distance) {
			this.location = location;
			this.distance = distance;
			this.reachedVia = null;
			this.prev = null;
		}

		/**
		 * Constructs a new routable location.
		 *
		 * @param location   to base on.
		 * @param distance   from the routing origin.
		 * @param reachedVia the connection that leads to this location.
		 * @param prev       the location on the other side of the connection.
		 */
		RoutableLocation(final Location location, final int distance, final Connection reachedVia,
				final RoutableLocation prev) {
			this.location = location;
			this.distance = distance;
			this.reachedVia = reachedVia;
			this.prev = prev;
		}

		@Override
		public String toString() {
			return location.toString();
		}

		@Override
		public int compareTo(final RoutableLocation other) {
			int otherDistance = other.distance;

			if ((distance == -1) == (otherDistance == -1))
				return distance - otherDistance;

			if (otherDistance >= 0)
				otherDistance = 1;

			return otherDistance;
		}
	}
}
