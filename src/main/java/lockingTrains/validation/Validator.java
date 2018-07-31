package lockingTrains.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Problem;
import lockingTrains.shared.TrainSchedule;

/**
 * Implements the {@link Recorder} interface for purposes of functional
 * correctness testing. The methods may throw an {@link IllegalStateException}
 * at any time to indicate that an inconsistent state has been reached. You must
 * handle this gracefully, i.e. your implementation of
 * {@link lockingTrains.impl.Simulator#run Simulartor.run} must return
 * {@code false}.
 */
public class Validator extends Recorder {
	private final Problem problem;
	private final List<TrainSchedule> toStart;

	private final Map<Integer, Integer> capacities;
	private final Map<Integer, Integer> travelling;

	private final Set<Integer> started;
	private final Set<Integer> finished;

	private final Map<Integer, Integer> position;
	private final Map<Integer, Integer> leaving;
	private final Map<Integer, Integer> arriving;
	private final Map<Integer, Integer> pausing;
	private final Map<Integer, Long> eta;

	/**
	 * Constructs a new validator.
	 *
	 * @param problem the problem to validate simulator action for.
	 */
	public Validator(final Problem problem) {
		this.problem = problem;
		this.toStart = new LinkedList<>(problem.schedules());
		this.travelling = new HashMap<>();
		this.started = new HashSet<>();
		this.finished = new HashSet<>();
		this.position = new HashMap<>();
		this.leaving = new HashMap<>();
		this.arriving = new HashMap<>();
		this.pausing = new HashMap<>();
		this.eta = new HashMap<>();

		this.capacities = new HashMap<>();
		problem.map().locations().stream().filter(l -> !l.isStation())
				.forEach(l -> capacities.put(l.id(), l.capacity()));
	}

	private void assertTrue(boolean condition) {
		if (!condition)
			throw new IllegalStateException();
	}

	@Override
	public synchronized void start(final TrainSchedule schedule) {
		start(System.currentTimeMillis(), schedule);
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
		assertTrue(!started.contains(schedule.id()));
		assertTrue(schedule.origin().isStation());

		boolean foundSchedule = false;
		for (final var it = toStart.iterator(); it.hasNext();) {
			final var candidate = it.next();

			if (candidate.origin().equals(schedule.origin())
					&& candidate.destination().equals(schedule.destination())) {
				foundSchedule = true;
				it.remove();
				break;
			}
		}
		assertTrue(foundSchedule);

		started.add(schedule.id());
		position.put(schedule.id(), schedule.origin().id());
	}

	@Override
	public synchronized void leave(final TrainSchedule schedule, final Location location) {
		leave(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(position.get(schedule.id()) != null);
		assertTrue(position.get(schedule.id()) == location.id());

		position.remove(schedule.id());
		leaving.put(schedule.id(), location.id());
	}

	@Override
	public synchronized void travel(final TrainSchedule schedule, final Connection section) {
		travel(System.currentTimeMillis(), schedule, section);
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(!travelling.containsValue(section.id()));

		final var leave = leaving.get(schedule.id());
		assertTrue(leave != null);
		assertTrue(leave == section.first().id() || leave == section.second().id());

		leaving.remove(schedule.id());

		final boolean forwardDirection = leave == section.first().id();
		final int nextLocationID = (forwardDirection ? section.second() : section.first()).id();
		assertTrue(!arriving.containsValue(nextLocationID));
		arriving.put(schedule.id(), nextLocationID);
		travelling.put(schedule.id(), section.id());

		eta.put(schedule.id(), timestamp + section.time());
	}

	@Override
	public synchronized void arrive(final TrainSchedule schedule, final Location location) {
		arrive(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(arriving.get(schedule.id()) != null);
		assertTrue(arriving.get(schedule.id()) == location.id());
		assertTrue(travelling.get(schedule.id()) != null);
		assertTrue(location.isStation() || !position.containsValue(location.id()));
		assertTrue(eta.get(schedule.id()) != null);
		assertTrue(eta.get(schedule.id()) <= timestamp);

		arriving.remove(schedule.id());
		eta.remove(schedule.id());
		travelling.remove(schedule.id());
		position.put(schedule.id(), location.id());
	}

	@Override
	public synchronized void pause(final TrainSchedule schedule, final Location location) {
		pause(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(position.get(schedule.id()) != null);
		assertTrue(position.get(schedule.id()) == location.id());

		assertTrue(!location.isStation());

		position.remove(schedule.id());
		pausing.put(schedule.id(), location.id());
		final int capacity = capacities.get(location.id());
		assertTrue(capacity > 0);
		capacities.put(location.id(), capacity - 1);
	}

	@Override
	public synchronized void resume(final TrainSchedule schedule, final Location location) {
		resume(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(pausing.get(schedule.id()) != null);
		assertTrue(pausing.get(schedule.id()) == location.id());

		assertTrue(!location.isStation());

		pausing.remove(schedule.id());
		position.put(schedule.id(), location.id());
		final int capacity = capacities.get(location.id());
		assertTrue(capacity < location.capacity());
		capacities.put(location.id(), capacity + 1);
	}

	@Override
	public synchronized void finish(final TrainSchedule schedule) {
		finish(System.currentTimeMillis(), schedule);
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
		assertTrue(started.contains(schedule.id()));
		assertTrue(!travelling.containsKey(schedule.id()));
		assertTrue(!finished.contains(schedule.id()));
		assertTrue(!leaving.containsKey(schedule.id()));
		assertTrue(!arriving.containsKey(schedule.id()));
		assertTrue(!eta.containsKey(schedule.id()));
		assertTrue(position.get(schedule.id()) != null);
		assertTrue(position.get(schedule.id()) == schedule.destination().id());
		assertTrue(schedule.destination().isStation());

		finished.add(schedule.id());
	}

	@Override
	public synchronized void done() {
		done(System.currentTimeMillis());
	}

	@Override
	void done(final long timestamp) {
		assertTrue(problem.schedules().size() == finished.size());
	}
}
