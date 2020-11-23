package edu.birzeit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FeasiblePair {
    String pairRepresentation;
    Set<State> pair;
    State pair1;
    State pair2;
    boolean isFinal = false;
    boolean isMarked = false;
    HashMap<String, FeasiblePair> paths = new HashMap<>();

    FeasiblePair(State pair1, State pair2) {
        pair = new HashSet<>();
        pair.add(pair1);
        pair.add(pair2);


        pairRepresentation = pair1.getLetter() + "," + pair2.getLetter();

        if (pair1.getLetter().compareTo(pair2.getLetter()) < 0) {
            this.pair1 = pair1;
            this.pair2 = pair2;
        } else {
            this.pair2 = pair1;
            this.pair1 = pair2;
        }
    }

    public void addPath(String edge) {
        paths.put(edge, null);
    }

    public void addPath(String edge, FeasiblePair feasiblePair) {
        paths.put(edge, feasiblePair);
    }


    public HashMap<String, FeasiblePair> getPaths() {
        return paths;
    }

    public String getPairRepresentation() {
        return pair1.getLetter() + "," + pair2.getLetter();
    }

    public String getReversePairRepresentation() {
        return new StringBuilder(this.getPairRepresentation()).reverse().toString();
    }

    public boolean isEqualPair() {
        return pair1.getLetter().equals(pair2.getLetter());
    }
}
