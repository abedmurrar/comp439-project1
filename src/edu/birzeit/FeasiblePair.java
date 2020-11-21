package edu.birzeit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FeasiblePair {
    String pairRepresentation;
    Set<State> pair;
    boolean isFinal = false;
    HashMap<String, FeasiblePair> paths = new HashMap<>();

    FeasiblePair(State pair1, State pair2) {
        pair = new HashSet<>();
        pair.add(pair1);
        pair.add(pair2);

        pairRepresentation = pair1.getLetter() + "," + pair2.getLetter();
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
        return pairRepresentation;
    }

    public String getReversePairRepresentation() {
        return new StringBuilder(pairRepresentation).reverse().toString();
    }
}
