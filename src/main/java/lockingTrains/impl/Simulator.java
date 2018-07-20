package lockingTrains.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lockingTrains.shared.*;
import lockingTrains.shared.io.Parser;
import lockingTrains.validation.CatRecorder;
import lockingTrains.validation.Logger;
import lockingTrains.validation.Recorder;
import lockingTrains.validation.Validator;

/**
 * This is the starting point of your implementation. Feel free to add
 * additional method, but carefully read each existing method's documentation
 * before changing them.
 */
public class Simulator {
	private Simulator() {
	}

	/**
	 * Entrypoint for the simulator application.
	 * 
	 * You may extend this (although you should not need to), but <strong>you must
	 * continue supporting the already implemented call scheme</strong>.
	 *
	 * @param args the command line arguments.
	 *
	 * @throws IOException if an error occurs while reading the input files.
	 */
	public static void main(String[] args) throws IOException{
		if (args.length != 2) {
			System.out.println("Usage: <command> <map file> <problem file>");
			System.exit(1);
		}

		final var problem = Parser.parse(new File(args[0]), new File(args[1]));
		final var logger = new Logger();

		final boolean result = run(problem, new CatRecorder(List.of(logger, new Validator(problem))));

		logger.eventLog().forEach(System.out::println);

		if (!result)
			System.exit(1);
	}

	/**
	 * Runs the entire problem simulation. The actions performed must be recorded
	 * using the interface {@link Recorder}. If the {@link Recorder} throws an
	 * exception, this method <strong>must</strong> return {@code false}.
	 * 
	 * <strong>You may not change the signature of this method.</strong>
	 *
	 * @param problem  the problem to simulate.
	 * @param recorder the recorder instance to call.
	 *
	 * @return {@code true} if the simulation ran successfully.
	 */
	public static boolean run(final Problem problem, final Recorder recorder) {
		//zuerst die Informationen des Problems auslesen
		Map map = problem.map();
		List<TrainSchedule> schedules = problem.schedules();

		//erzeuge Monitor für jeden Bahnhof
		List<Location> locations = map.locations();
		List<OwnMonitor> monitors = new ArrayList<OwnMonitor>();

		for(Location loc : locations){
			if(loc.isStation()){
				monitors.add(new OwnMonitor(-1, loc.id()));
			}else{
				monitors.add(new OwnMonitor(loc.capacity(),loc.id()));
			}
		}

		//erzeuge Monitor für jedes Gleis und verteile ids für totale Ordnung
		List<Connection> connections = map.connections();
		List<GleisMonitor> gleise = new ArrayList<GleisMonitor>();

		int total_id = 0;
		for(Connection con : connections){
			gleise.add(new GleisMonitor(total_id));
			total_id++;
		}

		//Fahrtdienstleitung initialisieren
		FahrtdienstLeitung FdL = new FahrtdienstLeitung(monitors, map, gleise);

		//Züge initialisieren
		List<Zug> züge = new ArrayList<Zug>();

		int i = 0;
		for(TrainSchedule ts : schedules){
			züge.add(new Zug(ts,i,map,FdL));
			i++;
		}

		//Threads starten
		List<Thread> zug_threads = new ArrayList<>();
		for(Zug z : züge){
			zug_threads.add(new Thread(z));
		}
		for(Thread th : zug_threads) {
			th.start();
		}

		//wenn alle Threads terminiert sind (join) dann checkt die FdL ob alle angekommen sind (checkDone), wenn ja dann return true
		if(FdL.checkDone()){
				return true;
		}

		return false;
	}
}
