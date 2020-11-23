package edu.birzeit;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Abed Al Rahman Murrar - 1140155
 */
public class Main {

    final static String DELTA = "@";
    static String START;

    public static void main(String[] args) {
        HashMap<String, FeasiblePair> feasiblePairs = new HashMap<>();
        HashMap<String, State> states = new HashMap<>() {
            @Override
            public String toString() {
                return keySet().toString();
            }
        };
        HashMap<String, State> originalStates = new HashMap<>();
        String[] weights = null;
        String startState;


        try {
            /**
             * Read Data from file
             */
            File inputFile = new File("example.txt");
            Scanner inputFileReader = new Scanner(inputFile);

            for (int i = 0; inputFileReader.hasNextLine(); i++) {
                String data = inputFileReader.nextLine();

                /**
                 * First line represents the set of states
                 */
                if (i == 0) {
                    String[] statesLetters = data.split("\\s+");
                    if (statesLetters.length > 10 || statesLetters.length == 0) {
                        throw new Exception("Number of states should be between 1 and 10");
                    }
                    /**
                     * First symbol is considered the start
                     */
                    START = statesLetters[0];
                    for (String s : statesLetters) {
                        states.put(s, new State(s));
                        originalStates.put(s, new State(s));
                    }
                    continue;
                }

                /**
                 * Second line the is the final states
                 */
                if (i == 1) {
                    String[] endingStates = Arrays.stream(data.split("\\s+")).map(String::trim).toArray(String[]::new);
                    for (String s : endingStates) {
                        states.get(s).isFinalState = true;
                        originalStates.get(s).isFinalState = true;
                        states.get(s).isEndState=true;
                        originalStates.get(s).isEndState = true;
                    }
                    continue;
                }

                /**
                 * The third line is the alphabet of the machine V
                 */
                if (i == 2) {
                    weights = Arrays.stream(data.split("\\s+")).map(String::trim).toArray(String[]::new);
                    for (Map.Entry<String, State> entry : states.entrySet()) {
                        for (String weight : weights) {
                            entry.getValue().addPath(weight);
                        }
                    }
                    for (Map.Entry<String, State> entry : originalStates.entrySet()) {
                        for (String weight : weights) {
                            entry.getValue().addPath(weight);
                        }
                    }
                    continue;
                }

                String[] edge = Arrays.stream(data.split("\\s+")).map(String::trim).toArray(String[]::new);

                states.get(edge[0]).addToPath(edge[2], states.get(edge[1]));
                originalStates.get(edge[0]).addToPath(edge[2], states.get(edge[1]));
            }
            inputFileReader.close();

            Set<State> visitedStates = new HashSet<>();
            if (!reachesFinalState(states, START, visitedStates, 0)) {
                System.out.println("String is not rejected because it does not reach a final state");
                System.exit(0);
            }
            System.out.println("String is accepted\n\n");

            /**
             * merge rows from lambda column
             */
            Set<State> finishedStates = new HashSet<>();
            states.forEach((key, value) -> {
                if (value.getPath(DELTA).size() > 0) {
                    removeLambda(states, null, key, finishedStates, originalStates);
                    finishedStates.add(value);
                }
            });

            /**
             * Remove lambda column
             */
            states.forEach((stateName, StateObject) -> {
                StateObject.removePath(DELTA);
                Set<State> allpaths = new HashSet<>();
                Stream.of(StateObject.getPaths().values()).forEach(e -> e.forEach(allpaths::addAll));
                if (allpaths.stream().allMatch(e -> e.getLetter().equals(stateName))) {
                    StateObject.isFinalState = true;
                }
            });

            String[] finalWeights = weights;

            /**
             * Replace combined states to form a single new state
             * for example if a state has B,C for a column, they will be
             * transformed into a new one symbol state
             */
            while (states.values().stream().anyMatch(state -> state.getPaths().values().stream().anyMatch(path -> path.size() > 1))) {
                try {
                    Iterator statesIterator = states.entrySet().iterator();
                    while (statesIterator.hasNext()) {
                        Map.Entry<String, State> pair = (Map.Entry<String, State>) statesIterator.next();
                        String stateName = pair.getKey();
                        State stateObject = pair.getValue();

                        try {
                            stateObject.getPaths().forEach((pathName, pathSet) -> {
                                try {
                                    if (pathSet != null && pathSet.size() > 1) {
                                        String combinedStatesNames = pathSet
                                                .stream()
                                                .map(State::getLetter)
                                                .collect(Collectors.joining(","));
                                        Optional<State> combinedState = states
                                                .values()
                                                .stream()
                                                .filter(e -> e.represents.equals(combinedStatesNames))
                                                .findFirst();
                                        if (combinedState.isPresent()) {
                                            pathSet.clear();
                                            pathSet.add(combinedState.get());
                                        } else {
                                            String[] alphabets = IntStream.rangeClosed('A', 'Z')
                                                    .mapToObj(c -> (char) c + ",")
                                                    .filter(c -> !states.containsKey(String.valueOf(c.charAt(0))))
                                                    .filter(c -> !originalStates.containsKey(String.valueOf(c.charAt(0))))
                                                    .collect(Collectors.joining())
                                                    .split(",");

                                            State newCombinedState = new State(alphabets[0]);
                                            newCombinedState.represents = combinedStatesNames;
                                            states.put(newCombinedState.getLetter(), newCombinedState);
                                            for (String weight : finalWeights) {
                                                if (weight.equals(DELTA)) {
                                                    continue;
                                                }
                                                newCombinedState.addPath(weight);
                                            }

                                            pathSet.forEach(s -> {
                                                if (s.isFinalState) {
                                                    newCombinedState.isFinalState = true;
                                                }
                                                s.getPaths().forEach((k, v) -> newCombinedState.getPaths().merge(k, v, (old, newS) -> {
                                                    if (k.equals(DELTA)) {
                                                        return old;
                                                    }
                                                    Set<State> combined = new HashSet<>();
                                                    combined.addAll(old);
                                                    combined.addAll(newS);
                                                    return combined;
                                                }));
                                            });

                                            pathSet.clear();
                                            pathSet.add(newCombinedState);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            });

                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }


            /**
             * Recursively mark non reachable states
             * then remove them from the states map
             */
            findNonReachableStates(states, START);
            String[] nonAccessibleStates = states.values().stream().filter(e -> !e.isReachable).map(State::getLetter).toArray(String[]::new);
            for (String nonAccessibleState : nonAccessibleStates) {
                states.remove(nonAccessibleState);
            }


            for (String state1 : states.keySet()) {
                for (String state2 : states.keySet()) {
                    State state1Object = states.get(state1);
                    State state2Object = states.get(state2);
                    String pairRepresentation = state1Object.getLetter() + "," + state2Object.getLetter();
                    String reversePairRepresentation = new StringBuilder(pairRepresentation).reverse().toString();
                    if (feasiblePairs.containsKey(pairRepresentation) || feasiblePairs.containsKey(reversePairRepresentation)) {
                        continue;
                    } else {
                        if (!state1Object.getLetter().equals(state2Object.getLetter()) &&
                                state1Object.isFinalState == state2Object.isFinalState &&
                                state1Object
                                        .getPaths()
                                        .keySet()
                                        .stream()
                                        .allMatch(path -> {
                                            Set<State> state1Path = state1Object.getPath(path);
                                            Set<State> state2Path = state2Object.getPath(path);
                                            return state1Path.size() == state2Path.size();
                                        })
                        ) {
                            FeasiblePair feasiblePair = new FeasiblePair(state1Object, state2Object);
                            feasiblePairs.put(pairRepresentation, feasiblePair);
                            for (String weight : weights) {
                                if (weight.equals(DELTA)) {
                                    continue;
                                }

                                Optional<State> pathStateFromState1 = state1Object.getPath(weight).stream().findFirst();
                                Optional<State> pathStateFromState2 = state2Object.getPath(weight).stream().findFirst();


                                if (pathStateFromState1.isPresent() && pathStateFromState2.isPresent()) {
                                    feasiblePair.addPath(weight, new FeasiblePair(pathStateFromState1.get(), pathStateFromState2.get()));
                                } else {
                                    feasiblePair.addPath(weight);
                                }

                                feasiblePair.isFinal = state1Object.isFinalState;
                            }
                        }
                    }
                }
            }

            for (FeasiblePair feasiblePair : feasiblePairs.values()) {
                for (FeasiblePair feasiblePairRS : feasiblePair.getPaths().values()) {
                    if (feasiblePairRS != null && !feasiblePairRS.isEqualPair()) {
                        if (feasiblePairRS.isMarked || !feasiblePairs.containsKey(feasiblePairRS.getPairRepresentation())) {
                            feasiblePair.isMarked = true;
                        }
                    }
                }
            }

            for (FeasiblePair feasiblePair : feasiblePairs.values()) {
                if (!feasiblePair.isMarked) {
                    feasiblePair.pair2.setLetter(feasiblePair.pair2.getLetter());
                    states.remove(feasiblePair.pair2.getLetter());
                }
            }


            System.out.println("***** FINAL TRANSITION TABLE *****\n\n");
            /**
             * Print final table into the terminal
             */
            ArrayList<String> weightsWithoutDelta = Arrays.stream(weights).filter(e -> !e.equals(DELTA)).collect(Collectors.toCollection(ArrayList::new));
            ArrayList<String> firstLine = new ArrayList<>();
            firstLine.add("State/VT");
            firstLine.addAll(weightsWithoutDelta);
            System.out.format(firstLine.stream().map(e -> "%15s").collect(Collectors.joining("")) + "\n", (String[]) firstLine.toArray(String[]::new));
            for (State s : states.values()) {
                System.out.format("%15s", s.getLetter());
                for (String weight : weightsWithoutDelta) {
                    Optional<State> path = s.getPath(weight).stream().findFirst();
                    path.ifPresentOrElse(state -> System.out.format("%15s", state.getLetter()), () -> System.out.format("%15s", ""));
                }
                System.out.format("\n");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Remove Lambdas
     */
    public static void removeLambda(HashMap<String, State> states, String parentState, String
            currentState, Set<State> finishedStates, HashMap<String, State> originalStates) {
        if (states.get(currentState).getPath(DELTA).size() == 0 || finishedStates.contains(states.get(currentState))) {
            return;
        }
        for (State nextState : states.get(currentState).getPath(DELTA)) {
            if (parentState == null || (!parentState.equals(nextState.getLetter()) && !nextState.getLetter().equals(currentState))) {
                removeLambda(states, currentState, nextState.getLetter(), finishedStates, originalStates);
                states.get(nextState.getLetter()).getPaths().forEach((key, value) -> states.get(currentState).getPaths().merge(key, value, (oldSet, newSet) -> {
                    if (key.equals(DELTA)) {
                        return oldSet;
                    }
                    Set<State> combined = new HashSet<>();
                    combined.addAll(oldSet);
                    combined.addAll(newSet);
                    return combined;
                }));

            }
            if (nextState.isFinalState) {
                states.get(currentState).isFinalState = true;
            }
        }
        return;
    }

    public static void findNonReachableStates(HashMap<String, State> states, String currentState) {
        if (states.get(currentState).isReachable) {
            return;
        }
        states.get(currentState).isReachable = true;
        states.get(currentState).getPaths().forEach((k, v) -> v.forEach(state -> findNonReachableStates(states, state.getLetter())));
    }

    public static boolean reachesFinalState(HashMap<String, State> states, String currentState, Set<State> visitedStates, int tabs) {
        if (states.get(currentState).isFinalState) {
            for (int i = 0; i < tabs; i++) {
                System.out.print("\t");
            }
            System.out.println(states.get(currentState).getLetter() + " is a Final State");
            return true;
        }
        if (visitedStates.contains(states.get(currentState))) {
            for (int i = 0; i < tabs; i++) {
                System.out.print("\t");
            }
            System.out.println(states.get(currentState).getLetter());
            return false;
        }

        visitedStates.add(states.get(currentState));

        for (int i = 0; i < tabs; i++) {
            System.out.print("\t");
        }
        System.out.print(states.get(currentState).getLetter() + " ->\n");
        boolean finalValue = false;
        for (Map.Entry<String, Set<State>> path : states.get(currentState).getPaths().entrySet()) {
            for (State s : path.getValue()) {
                finalValue |= reachesFinalState(states, s.getLetter(), visitedStates, tabs + 2);
            }
        }

        return finalValue;
    }

}
