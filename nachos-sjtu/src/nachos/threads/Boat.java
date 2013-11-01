package nachos.threads;

import java.util.LinkedList;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		lock = new Lock();
		cond = new Condition(lock);
		adultsLeft = adults;
		childrenLeft = children;
		boatAtOahu = true;
		HasPilot = false;
		over = false;
		
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		LinkedList<KThread> threads = new LinkedList<KThread>();
		
		for (int i = 0; i < adults; ++i) {
			KThread t = new KThread(new Runnable(){
				@Override
				public void run() {
					AdultItinerary();
				}
			});
			t.setName("Adult Thread" + i);
			threads.add(t);
			t.fork();
		}
		
		for (int i = 0; i < children; ++i) {
			KThread t = new KThread(new Runnable(){
				@Override
				public void run() {
					ChildItinerary();
				}
			});
			t.setName("Child Thread" + i);
			threads.add(t);
			t.fork();
		}
		
		for (KThread t: threads)
			t.join();
	}

	static void AdultItinerary() {
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		
		lock.acquire();
		
		while (!(boatAtOahu && childrenLeft <= 1))
			cond.sleep();
		
		boatAtOahu = false;
		bg.AdultRowToMolokai();
		adultsLeft--;
		if (adultsLeft == 0 && childrenLeft == 0)
			over = true;
		
		cond.wakeAll();
		//System.out.println("1 adult to Molokai");
		
		lock.release();
	}

	static void ChildItinerary() {
		boolean AtOahu = true;
		lock.acquire();
		
		while (!over) {
			if (boatAtOahu && AtOahu) {
				if (childrenLeft >= 2) {
					if (!HasPilot) { //Take one child as pilot
						HasPilot = true;
						AtOahu = false;
					} else {
						boatAtOahu = false;
						bg.ChildRowToMolokai();
						bg.ChildRideToMolokai();
						AtOahu = false;
						HasPilot = false;
						childrenLeft -= 2;
						if (adultsLeft == 0 && childrenLeft == 0)
							over = true;
					
						cond.wakeAll();
						//System.out.println("2 child to Molokai");
					}
				} else if (childrenLeft == 1 && adultsLeft == 0) {
					boatAtOahu = false;
					bg.ChildRowToMolokai();
					AtOahu = false;
					childrenLeft--;
					over = true;
					
					cond.wakeAll();
					//System.out.println("1 child to Molokai");
				} else cond.sleep();
			} else if (!boatAtOahu && !AtOahu) {
				bg.ChildRowToOahu();
				boatAtOahu = true;
				AtOahu = true;
				childrenLeft += 1;
				
				cond.wakeAll();
				//System.out.println("1 child to Oahu");
			} else cond.sleep();
		}
		
		lock.release();
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out
				.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
	
	
	static private Lock lock;
	static private Condition cond;
	
	static private int adultsLeft, childrenLeft;
	static private boolean over;
	static private boolean boatAtOahu;
	static private boolean HasPilot;
}
