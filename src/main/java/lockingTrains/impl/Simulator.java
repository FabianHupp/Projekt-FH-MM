package lockingTrains.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

		List<TrainSchedule> schedules = problem.schedules();
		Map map = problem.map();

		//Erzeuge Monitor für jeden Stop
		List<Location> locations = map.locations();
		List<OwnMonitor> monitors = new ArrayList<>();
		for(Location loc : locations){
			if(loc.isStation()){
				monitors.add(new OwnMonitor(-1, loc.id()));
			}else{
				if(loc.capacity() == 0) {
					monitors.add(new OwnMonitor(0, loc.id()));
				}else{
					monitors.add(new OwnMonitor((loc.capacity()),loc.id()));
				}
			}
		}

		//Erzeuge Monitor für jedes Gleis
		List<Connection> connections = map.connections();
		List<GleisMonitor> gleise = new ArrayList<>();
		for(Connection con : connections){
			gleise.add(new GleisMonitor(con.id()));
		}

		//Fahrtdienstleitung initialisieren
		FahrtdienstLeitung FdL = new FahrtdienstLeitung(monitors, gleise, schedules.size());

		//Züge initialisieren
		List<Zug> zuege = new ArrayList<Zug>();
		int i = 0;
		for(TrainSchedule ts : schedules){
			zuege.add(new Zug(ts,i,map,FdL,recorder));
			i++;
		}

		//Zug-Threads starten
		List<Thread> zug_threads = new ArrayList<>();
		for(Zug z : zuege){
			zug_threads.add(new Thread(z));
		}
		for(Thread th : zug_threads) {
			th.start();
		}

		//Warten bis alle Threads terminiert sind (join)
		for(Thread th :zug_threads){
			try {
				th.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Alle Threads terminiert");
		//Alle Züge agekommen?
		if(FdL.checkDone()){
			recorder.done();
			return true;
		}

		return false;
	}
}
